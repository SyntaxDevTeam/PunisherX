#!/usr/bin/env python3
"""Isolated Velocity + MariaDB integration test. No production servers/databases are used."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import socket
import struct
import subprocess
import tempfile
import time
import uuid
import zipfile


def port():
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def wait_for(predicate, seconds=30):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if predicate():
            return
        time.sleep(0.1)
    raise AssertionError("Timed out waiting for runtime test")


def varint(value):
    result = bytearray()
    while value > 127:
        result.append((value & 127) | 128)
        value >>= 7
    result.append(value)
    return bytes(result)


def read_varint(sock):
    result = 0
    for index in range(5):
        byte = sock.recv(1)
        if not byte:
            raise AssertionError("Unexpected EOF in packet")
        result |= (byte[0] & 127) << (7 * index)
        if byte[0] < 128:
            return result
    raise AssertionError("Invalid VarInt")


def wire_login(proxy_port):
    def send(sock, data):
        sock.sendall(varint(len(data)) + data)

    with socket.create_connection(("127.0.0.1", proxy_port), timeout=10) as sock:
        # Minecraft 1.8 login protocol: no game client or backend required to test login denial.
        send(sock, b"\x00" + varint(47) + b"\x09localhost" + struct.pack(">H", proxy_port) + b"\x02")
        send(sock, b"\x00\x09WireProbe")
        length = read_varint(sock)
        packet = bytearray()
        while len(packet) < length:
            chunk = sock.recv(length - len(packet))
            if not chunk:
                raise AssertionError("Incomplete disconnect packet")
            packet.extend(chunk)
        assert packet[0] == 0 and b"wire-ban" in packet and b"PunisherX BAN" in packet, packet


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--proxy-jar", type=Path, required=True)
    parser.add_argument("--bridge-jar", type=Path, required=True)
    parser.add_argument("--java-home", type=Path)
    args = parser.parse_args()
    proxy_jar, bridge_jar = args.proxy_jar.resolve(), args.bridge_jar.resolve()
    java = str(args.java_home / "bin/java") if args.java_home else "java"
    javac = str(args.java_home / "bin/javac") if args.java_home else "javac"
    module = Path(__file__).resolve().parents[3]
    output = module / "build/runtime-test"
    output.mkdir(parents=True, exist_ok=True)
    work = Path(tempfile.mkdtemp(prefix="velocity-", dir=output))
    print(f"Runtime test artifacts: {work}", flush=True)
    db_port, proxy_port, backend_port = port(), port(), port()
    proxy_dir, classes = work / "proxy", work / "classes"
    classes.mkdir()
    plugins = proxy_dir / "plugins"
    config = plugins / "punisherxbridge"
    config.mkdir(parents=True)
    # Keep DB files under /tmp (MariaDB may restrict access to home directories).
    # Unix-domain sockets also need a short path.
    socket_dir = Path(tempfile.mkdtemp(prefix="px-vdb-"))
    datadir = socket_dir / "db"
    datadir.mkdir()
    db_socket = socket_dir / "db.sock"
    processes = []
    try:
        with (work / "db-install.log").open("w") as log:
            subprocess.run(["mariadb-install-db", "--no-defaults", f"--datadir={datadir}",
                            "--auth-root-authentication-method=normal", "--skip-test-db"],
                           stdout=log, stderr=subprocess.STDOUT, check=True, timeout=60)
        with (work / "db.log").open("w") as log:
            db = subprocess.Popen(["mariadbd", "--no-defaults", f"--datadir={datadir}",
                                   f"--socket={db_socket}", f"--pid-file={socket_dir / 'db.pid'}",
                                   "--bind-address=127.0.0.1", f"--port={db_port}"],
                                  stdout=log, stderr=subprocess.STDOUT)
        processes.append(db)

        def sql(statement, check=True):
            return subprocess.run(["mariadb", "--no-defaults", f"--socket={db_socket}",
                                   "-u", "root", "-e", statement], check=check,
                                  stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)

        wait_for(lambda: db.poll() is None and db_socket.exists() and sql("SELECT 1", False).returncode == 0)
        sql("CREATE DATABASE punisherx_probe; CREATE USER 'probe'@'127.0.0.1' IDENTIFIED BY 'probe'; "
            "GRANT ALL ON punisherx_probe.* TO 'probe'@'127.0.0.1';")
        subprocess.run([javac, "--release", "25", "-proc:none", "-cp", str(proxy_jar), "-d", str(classes),
                        str(module / "src/runtimeTest/java/VelocityRuntimeProbe.java")], check=True)
        with zipfile.ZipFile(plugins / "runtime-probe.jar", "w") as jar:
            for compiled in classes.rglob("*.class"):
                jar.write(compiled, compiled.relative_to(classes))
            jar.writestr("velocity-plugin.json", json.dumps({
                "id": "runtimeprobe", "name": "RuntimeProbe", "version": "test", "main": "VelocityRuntimeProbe",
                "dependencies": [{"id": "punisherxbridge", "optional": False}]}))
        shutil.copy2(bridge_jar, plugins / bridge_jar.name)
        (config / "bridge.properties").write_text(
            f"host=127.0.0.1\nport={db_port}\ndatabase=punisherx_probe\nusername=probe\npassword=probe\npoll-interval-ms=200\n")
        (proxy_dir / "velocity.toml").write_text(
            f'config-version = "2.7"\nbind = "127.0.0.1:{proxy_port}"\nonline-mode = false\n'
            'player-info-forwarding-mode = "NONE"\n[servers]\n'
            f'lobby = "127.0.0.1:{backend_port}"\ntry = ["lobby"]\n'
            '[advanced]\nlogin-ratelimit = 0\ncompression-threshold = -1\n'
            '[forced-hosts]\n"lobby.example.com" = ["lobby"]\n')
        log_path = work / "proxy.log"
        with log_path.open("w") as log:
            proxy = subprocess.Popen([java, f"-Dprobe.jdbc=jdbc:mysql://127.0.0.1:{db_port}/punisherx_probe",
                                      "-jar", str(proxy_jar)], cwd=proxy_dir, stdin=subprocess.PIPE,
                                     stdout=log, stderr=subprocess.STDOUT, text=True)
        processes.append(proxy)

        def probe_finished():
            text = log_path.read_text()
            if "RUNTIME_PROBE_FAIL" in text or proxy.poll() is not None:
                raise AssertionError(text)
            return "RUNTIME_PROBE_PASS" in text

        wait_for(probe_finished)
        player_uuid = uuid.UUID(bytes=hashlib.md5(b"OfflinePlayer:WireProbe").digest(), version=3)
        sql("INSERT INTO punisherx_probe.punishments VALUES "
            f"('{player_uuid}', 'wire-ban', -1, 'BAN', 'network', 1)")
        for _ in range(2):
            wire_login(proxy_port)
        wait_for(lambda: log_path.read_text().count("SESSION_PROBE_PASS") == 2)
        assert "SESSION_PROBE_FAIL" not in log_path.read_text()
        proxy.stdin.write("shutdown\n")
        proxy.stdin.flush()
        proxy.wait(timeout=15)
        text = log_path.read_text()
        assert proxy.returncode == 0 and "Shutdown completed" in text, text
        assert "ERROR" not in text, text
        for line in text.splitlines():
            if "PROBE" in line or "Shutdown completed" in line:
                print(line)
        print("PASS: two real TCP login denials; session continuity and reconnect; graceful pool shutdown")
    finally:
        if len(processes) > 1 and processes[-1].poll() is None:
            processes[-1].stdin.write("shutdown\n")
            processes[-1].stdin.flush()
            processes[-1].wait(timeout=15)
        if db_socket.exists():
            subprocess.run(["mariadb-admin", "--no-defaults", f"--socket={db_socket}", "-u", "root", "shutdown"],
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=15)
        for process in reversed(processes):
            if process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait(timeout=5)
        shutil.rmtree(socket_dir)


if __name__ == "__main__":
    main()

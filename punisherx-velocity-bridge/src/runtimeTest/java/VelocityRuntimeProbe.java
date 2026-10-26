import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.proxy.*;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.io.*;
import java.lang.reflect.*;
import java.net.InetSocketAddress;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/** Runs only in the isolated runtime test proxy; never included in the bridge JAR. */
public final class VelocityRuntimeProbe {
    private final ProxyServer server;
    private final Logger logger;
    private int checks;
    private final List<String> failures = new ArrayList<>();
    private final UUID uuid = UUID.randomUUID();
    private final List<Component> disconnects = new CopyOnWriteArrayList<>();
    private volatile UUID inboundSession;
    private volatile UUID previousSession;

    @Subscribe
    public void preLogin(PreLoginEvent event) {
        if (event.getUsername().equals("WireProbe")) inboundSession = event.getConnection().getSessionId();
    }

    @Subscribe
    public void login(LoginEvent event) {
        if (!event.getPlayer().getUsername().equals("WireProbe")) return;
        UUID session = event.getPlayer().getSessionId();
        if (session.equals(inboundSession) && !session.equals(previousSession)) {
            logger.info("SESSION_PROBE_PASS stable during login, fresh on reconnect");
        } else {
            logger.error("SESSION_PROBE_FAIL");
        }
        previousSession = session;
    }

    @Inject
    public VelocityRuntimeProbe(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void initialize(ProxyInitializeEvent event) {
        server.getScheduler().buildTask(this, () -> {
            try {
                runChecks();
                if (!failures.isEmpty()) throw new AssertionError(String.join(", ", failures));
                logger.info("RUNTIME_PROBE_PASS checks={}", checks);
            } catch (Throwable failure) {
                logger.error("RUNTIME_PROBE_FAIL", failure);
            }
        }).delay(Duration.ofSeconds(1)).schedule();
    }

    private void check(boolean condition, String label) {
        checks++;
        logger.info("PROBE {} {}", condition ? "PASS" : "FAIL", label);
        if (!condition) failures.add(label);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private void runChecks() throws Exception {
        Object bridge = server.getPluginManager().getPlugin("punisherxbridge")
                .orElseThrow().getInstance().orElseThrow();
        Player player = proxy(Player.class, (object, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> uuid;
            case "getUsername" -> "RuntimeProbe";
            case "getSessionId" -> UUID.fromString("00000000-0000-0000-0000-000000000001");
            case "getRemoteAddress" -> new InetSocketAddress("127.0.0.2", 12345);
            case "getCurrentServer" -> Optional.empty();
            case "isActive" -> true;
            case "disconnect" -> { disconnects.add((Component) args[0]); yield null; }
            case "toString" -> "RuntimeProbe";
            case "hashCode" -> System.identityHashCode(object);
            case "equals" -> object == args[0];
            default -> throw new UnsupportedOperationException(method.toString());
        });
        // Keep the real scheduler, event dispatcher, channel registrar and plugin manager.
        // Only player discovery is substituted so no Minecraft backend is required.
        ProxyServer discovery = proxy(ProxyServer.class, (object, method, args) -> {
            if (method.getName().equals("getPlayer")) {
                return uuid.equals(args[0]) ? Optional.of(player) : Optional.empty();
            }
            if (method.getName().equals("getAllPlayers")) return List.of(player);
            return method.invoke(server, args);
        });
        Field serverField = bridge.getClass().getDeclaredField("server");
        serverField.setAccessible(true);
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection db = DriverManager.getConnection(System.getProperty("probe.jdbc"), "probe", "probe")) {
            serverField.set(bridge, discovery);
            try (Statement sql = db.createStatement()) {
                sql.execute("CREATE TABLE punishments (uuid VARCHAR(36), reason TEXT, endTime BIGINT, punishmentType VARCHAR(16), server VARCHAR(64), start BIGINT)");
                sql.execute("INSERT INTO punishments VALUES ('" + uuid + "', 'runtime-ban', -1, 'BAN', 'network', 1)");
                LoginEvent login = server.getEventManager().fire(new LoginEvent(player, null)).join();
                check(!login.getResult().isAllowed(), "active network BAN denies login event");
                RegisteredServer backend = server.getAllServers().iterator().next();
                ServerPreConnectEvent transfer = server.getEventManager()
                        .fire(new ServerPreConnectEvent(player, backend)).join();
                check(!transfer.getResult().isAllowed() && !disconnects.isEmpty(), "active BAN denies backend transfer and disconnects");
                sql.execute("UPDATE punishments SET endTime = 1");
                disconnects.clear();
                check(server.getEventManager().fire(new LoginEvent(player, null)).join().getResult().isAllowed()
                        && disconnects.isEmpty(), "expired BAN allows login");
                sql.execute("UPDATE punishments SET endTime = -1, server = 'survival'");
                check(server.getEventManager().fire(new LoginEvent(player, null)).join().getResult().isAllowed(),
                        "backend-local BAN allows proxy login");
                sql.execute("UPDATE punishments SET server = 'network', endTime = " + (System.currentTimeMillis() + 60000));
                check(!server.getEventManager().fire(new LoginEvent(player, null)).join().getResult().isAllowed(),
                        "temporary network BAN denies login");
                sql.execute("DELETE FROM punishments");
                disconnects.clear();
                ServerConnection source = proxy(ServerConnection.class, (o, m, a) -> {
                    if (m.getName().equals("toString")) return "ProbeBackend";
                    throw new UnsupportedOperationException(m.toString());
                });
                PluginMessageEvent ban = message(source, player, "BAN", uuid.toString());
                server.getEventManager().fire(ban).join();
                check(!ban.getResult().isAllowed() && disconnects.size() == 1, "backend BAN message handled and disconnects");
                disconnects.clear();
                server.getEventManager().fire(message(source, player, "BANIP", "127.0.0.2")).join();
                check(disconnects.size() == 1, "backend BANIP message disconnects matching IP");
                disconnects.clear();
                PluginMessageEvent forged = message(player, source, "BAN", uuid.toString());
                server.getEventManager().fire(forged).join();
                check(!forged.getResult().isAllowed() && disconnects.isEmpty(), "client message blocked without enforcing BAN");
                server.getEventManager().fire(new PluginMessageEvent(source, player,
                        MinecraftChannelIdentifier.from("punisherx:bridge"), new byte[]{0})).join();
                check(disconnects.isEmpty(), "truncated message does not disconnect");
                sql.execute("INSERT INTO bridge_events(action,target,reason,endTime) VALUES ('BAN','" + uuid + "','queue-ban',-1)");
                long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
                boolean processed = false;
                while (System.nanoTime() < deadline) {
                    try (ResultSet rows = sql.executeQuery("SELECT processed, processedAt FROM bridge_events")) {
                        processed = rows.next() && rows.getInt(1) == 1 && rows.getLong(2) > 0;
                    }
                    if (processed && !disconnects.isEmpty()) break;
                    Thread.sleep(50);
                }
                check(processed && !disconnects.isEmpty(), "real scheduler polls SQL queue and marks BAN processed");
                disconnects.clear();
                Thread.sleep(600);
                check(disconnects.isEmpty(), "processed queue event is not replayed");
            } finally {
                serverField.set(bridge, server);
            }
        }
    }

    private PluginMessageEvent message(com.velocitypowered.api.proxy.messages.ChannelMessageSource source,
                                       com.velocitypowered.api.proxy.messages.ChannelMessageSink target,
                                       String action, String uuid) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(action);
            out.writeUTF(uuid);
            out.writeUTF("runtime-message");
            out.writeLong(-1);
        }
        return new PluginMessageEvent(source, target, MinecraftChannelIdentifier.from("punisherx:bridge"), bytes.toByteArray());
    }
}

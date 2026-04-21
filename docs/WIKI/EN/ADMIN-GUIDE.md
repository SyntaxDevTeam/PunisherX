# Admin Guide (EN)

> A practical guide for server owners and junior moderators: from installation and configuration to debugging and troubleshooting.

<img width="25%" height="25%" alt="PunisherX" src="../../../assets/PunisherX.png" />

---

## 1) Who is this guide for?

This material is for administrators who want to:
- quickly launch PunisherX,
- safely assign permissions to moderators,
- efficiently use commands and `/punish` templates,
- know what to check when “something doesn’t work”.

If you are a beginner, read section by section.  
If you are advanced, jump to **Debug** and **Known issues**.

---

## 2) Quick start (install in 10 minutes)

### Requirements
- Paper/Folia (latest builds recommended),
- Java 21+,
- access to the `plugins/` folder.

### Steps
1. Download the latest PunisherX release.
2. Upload the `.jar` file to `plugins/`.
3. Start the server and wait until the plugin generates files.
4. Configure `plugins/PunisherX/config.yml`.
5. Restart the server.
6. Join as admin and verify:
   - `/punisherx version`
   - `/punisherx reload`

---

## 3) First configuration

### 3.1 Language and messages
- Open `config.yml` and set `language: en` (or another available language).
- In the lang folder, you will find language files (e.g., `messages_en.yml`).
- Edit them to match your needs.
- Keep message style consistent (short and without caps lock).

### 3.2 Database
For a small server, SQLite/H2 is enough.  
For a network or heavier traffic, move to MySQL/MariaDB/PostgreSQL.

> Check our proxy add-on [PunisherX-Proxy-Bridge](https://modrinth.com/plugin/punisherx-proxy-bridge) if you run a network and want synchronized bans across servers.
<img width="25%" height="25%" alt="PunisherX_Velocity_Bridge" src="../../../assets/PunisherX_Velocity_Bridge_mini.png" />

### 3.3 Permissions (most common mistake)
Do not give `*` to every moderator. Split roles! Example:
- **JuniorMod**: `warn`, `mute`, `check`, `history`
- **Mod**: + `kick`, `jail`, `unjail`
- **Admin**: full operational access
- **Owner/TechAdmin**: migrations, import/export, critical actions

This lowers the risk of accidental mass punishments.

---

## 4) Practical command usage (moderator workflow)

### 4.1 Typical scenario
1. Check player history:
   - `/check <name> all`
   - `/history <name>`
2. Choose an adequate punishment:
   - warning (`/warn`) for minor violations,
   - temporary mute (`/mute`) for spam/profanity,
   - ban (`/ban`) for severe and repeated violations.
3. Use a clear reason (specific rule point).
4. After an appeal: verify and then `/unmute`/`/unban` if justified.

### 4.2 Commands worth memorizing
- `/warn <player> (time) <reason>`
- `/mute <player> (time) <reason>`
- `/jail <player> (time) <reason>`
- `/ban <player> (time) <reason>`
- `/unwarn`, `/unmute`, `/unjail`, `/unban`
- `/check`, `/history`, `/banlist`
- `/change-reason <id> <new_reason>`
- `/clearall <player>` (careful, this is a bulk operation)

Time format: `Xs`, `Xm`, `Xh`, `Xd` (seconds/minutes/hours/days).

---

## 5) `/punish` templates – how to use them wisely

Templates provide consistency and speed. They help junior moderators avoid “inventing” punishments manually each time.

### Best practices for templates
- One template = one specific violation type.
- Keep names short and clear (e.g., `spam_1`, `cheats_perm`).
- Reason should always map to your rules (e.g., “Rules 3.2 – spam”).
- Separate escalation levels (1h → 1d → 7d → perm).

### Example escalation logic
- First offense: `/warn`
- Second offense: `/mute 1h`
- Third offense: `/mute 1d`
- Fourth offense: `/ban 7d`

The biggest value of templates: consistent punishment policy across moderator shifts.

---

# Planned:
## 6) GUI panel and click-based workflow

PunisherX offers a GUI that speeds up daily moderation:
- player selection,
- punishment type selection,
- time and reason selection,
- quick jump to history.

![PunisherX](../../../assets/PunisherX.webp)

If your staff is young/inexperienced, GUI + predefined templates is the best combination (fewer mistakes, fewer typos).

---

## 7) Step-by-step debug (checklist)

When commands do not work correctly:

1. **Plugin version and status**
   - `/punisherx version`
   - check startup logs.

2. **Permissions**
   - does the player/mod have the correct permission nodes,
   - is there a conflict with your permission manager.

3. **Configuration**
   - check whether `config.yml` and language files have syntax errors,
   - after changes use `/punisherx reload` (or preferably restart the server).

4. **Database**
   - verify credentials,
   - verify database availability and connection stability.

5. **Integrations**
   - PlaceholderAPI, webhooks, proxy bridge — test separately, not all at once.
   - check if another plugin conflicts (e.g., another punishment system).

6. **Cache and diagnostics**
   - use `/prx diag` when you need a quick test and support-ready data.

> Rule of thumb: verify version + permissions + database first, then hunt “exotic bugs”.

---

## 8) Known issues and quick fixes

### Issue: “Command exists, but does not work”
Most often missing permissions or an alias conflict with another plugin.  
**Fix:** check permission nodes and alias list in `config.yml`.

### Issue: “Punishment is not synchronized across the network”
Usually a database or bridge problem.  
**Fix:** run a DB connection test and inspect bridge logs.

### Issue: “Wrong colors/message formatting”
Invalid MiniMessage/Legacy syntax in language files.  
**Fix:** rollback recent edits and re-edit section by section.

### Issue: “Moderators punish inconsistently”
No operational standard.  
**Fix:** implement `/punish` templates, an escalation table, and a short SOP for staff.

---

## 9) Short SOP for moderation team

This is a simple standard operating procedure for moderators.

1. Always start with `/check` and `/history`.  
2. Never punish “from memory”; use facts.
3. Use templates instead of free-typing reasons.
4. Every severe punishment must be justified by the rules.
5. If in doubt, escalate to Admin instead of guessing.

This is the difference between “chaotic moderation” and a professional team.

---

## 10) Contact the author and community
**SyntaxDevTeam** is open to feedback, questions, and issue reports.  
### Contact us:
- Community Discord: **https://discord.gg/Zk6mxv7eMh**
- Repository: **https://github.com/SyntaxDevTeam/PunisherX**
- Issue reports: GitHub Issues (preferably with logs and server/plugin versions)

### Always include in your report:
- PunisherX version,
- Paper/Folia version,
- Java version,
- log excerpt,
- reproduction steps.

---

## 11) Experience-based summary

If you want professional moderation, “having a good plugin” is not enough.  
You need: **good configuration + sensible permissions + punishment policy + a debugging procedure**.

PunisherX gives you the tools. Moderation quality depends on the standard you enforce in your team. Invest time in moderator training and clear rules, and the results will show immediately. **Good luck!**

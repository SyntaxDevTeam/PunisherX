# Player reports

[Wiki home](HOME.md) · [Commands](COMMANDS.md) · [Permissions](PERMISSIONS.md) · [Polski](../PL/REPORTS.md)

## For players: submit a report

`/report` is available to players without a dedicated permission. It cannot be used from the console.

1. Run `/report` and select a player, then a reason. The menu includes online players and players who disconnected in the last hour.
2. Use `/report Steve` to go straight to reason selection, or `/report Steve Spamming in chat` to send a custom reason directly.
3. Follow the selected interface to submit; in book mode, confirm on the final page. A success message confirms that the report was saved and online staff with notification access are notified.

Reasons must contain 3–255 characters, without control characters. You cannot report yourself or have two open reports against the same player. By default you can have **3 open reports** in total; the administrator can change this limit. A resolved or rejected report frees a slot and allows another report against that target. Reports do not automatically punish anyone.

The command can also target a known offline player; on Paper the name must be online or cached, or you can supply a UUID. A player missing from the recent-player menu is not necessarily unavailable to the command.

## For staff: investigate and decide

Grant `punisherx.see.reports` for reading and new-report notifications. Grant `punisherx.manage.reports` for reading and decisions. Management permission alone does not grant new-report notifications: grant the visibility node as well. Owner/OP and parent permission rules are described in [Permissions](PERMISSIONS.md).

1. Open `/reports` and select an open report. Read its ID, author, target, reason and filing date.
2. Investigate the evidence, using `/check Steve all` and `/history Steve` if you have the corresponding permissions.
3. Apply any justified punishment separately, for example using a configured `/punish` template.
4. Run `/reports resolve 12 Confirmed after reviewing chat logs` or `/reports reject 12 Insufficient evidence`.
5. Use `/reports history` or `/reports view 12` to review the decision later.

Decision explanations must contain 3–255 characters without control characters. Closing saves the status (`RESOLVED` or `REJECTED`), staff name, explanation and time. A closed decision cannot be overwritten by resolving or rejecting it again. Closing a report **does not issue a punishment**. Reports are shared by servers using the same database, independently of punishment server scope.

| Command | Purpose |
| --- | --- |
| `/reports` | Open the configured staff interface; list open reports in the console. |
| `/reports gui (page)` | Open the staff browser, following the configured book/dialog preference. |
| `/reports book (page)` | Open the book inbox regardless of the configured preference. |
| `/reports list (page)` | Browse open reports; may use a book or Paper dialog in-game. |
| `/reports view <id>` | Read a report and its decision. |
| `/reports history (page)` | Browse resolved and rejected reports. |
| `/reports resolve <id> <explanation>` | Mark an open report as resolved; management access required. |
| `/reports reject <id> <explanation>` | Reject an open report; management access required. |

Pages start at `1`. The console supports list, view, history, resolve and reject; gui and book require a player. Book inbox pages contain 4 reports. Inventory inbox pages contain 45 entries; textual lists use `reports.page-size` (default `10`, clamped to 1–50).

In the book interface, Resolve/Reject displays a chat link that inserts a command. Add the explanation and send it to record the decision. Book buttons expire after five minutes; opening another book menu invalidates the previous buttons. Reopen the menu if a button has expired.

## Choose the interface

Player and staff interfaces can be selected independently in `config.yml`:

```yaml
reports:
  player-interface: AUTO
  admin-interface: BOOK
  page-size: 10
  max-open-per-reporter: 3
  use-dialogs: true
  admin-use-dialogs: true
  reasons:
    - "Cheating"
    - "Spamming"
```

| Value | Behaviour |
| --- | --- |
| `AUTO` | Uses `use-dialogs` for players and `admin-use-dialogs` for staff; otherwise uses inventory/chat. |
| `GUI` | Inventory menus and chat details. |
| `DIALOG` | Native dialogs on Paper 1.21.7+; inventory/chat fallback on older Paper and Spigot. |
| `BOOK` | Clickable books on Paper, Folia and Spigot. |

Books open without adding an item to the inventory. Customize their text under `report-book` in the language file and report messages under `reports`. Long reasons and explanations span pages; keep custom layouts within Minecraft's book page size. The client's “Page … of …” text follows the client language.

After editing configuration, run `/prx validate` and `/prx reload`. Restart the server after replacing the plugin JAR. See [Configuration](CONFIGS.md) for language fallback and safe reload behaviour.

# PunisherX Spigot

This module is the target for the native Spigot implementation of PunisherX.

The existing implementation cannot be shared with Spigot unchanged because it uses Paper-only command,
plugin lifecycle, login, profile, and Folia scheduler APIs. The working Paper/Folia implementation lives in
`punisherx-paper`. Platform-neutral logic should be extracted before Spigot-specific bootstrap, commands,
listeners, and scheduling adapters are added here.

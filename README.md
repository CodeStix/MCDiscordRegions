# MCDiscordRegions

This bukkit plugin allows you to connect your minecraft server to discord, by following these steps:

1. Download a release from the releases tab.
2. Place the jar file in the `plugins/` directory of your server.
3. Reload the server.
4. Register a discord bot for your minecraft server at https://discord.com/developers/applications, and copy the _TOKEN_.
5. Generate a bot invite link, using the bot's _CLIENT ID_ on https://discordapi.com/permissions.html
6. Edit the `plugins/MCDiscordRegions/config.yml` file and provide it with your bot's _TOKEN_.
7. Reload the server.

After configuring, use the system by:

1. Joining the _entry_ (configurable) channel.
2. Send a private message to the discord bot with your minecraft in-game name. (should react with a green tick)
3. Join the minecraft server.
4. For every WorldGuard region you enter, a new discord channel will be created and you and others will be moved to it automaticly.

## Building

In order to build this project, you should add references to the following libraries:

-   [**WGEvents.jar**](https://www.spigotmc.org/resources/worldguard-events.65176/)
-   [**JDA-x.x.x-withDependencies.jar**](https://github.com/DV8FromTheWorld/JDA)
-   **spigot-x.x.x.jar** (use [BuildTools.jar](https://www.spigotmc.org/wiki/buildtools/) to generate this file)
-   [**worldguard-bukkit-x.x.x.jar**](https://dev.bukkit.org/projects/worldguard)

To ensure correct project structure, you should use IntelliJ IDEA.

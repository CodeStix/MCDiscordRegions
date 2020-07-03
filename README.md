# MCDiscordRegions

This bukkit plugin allows you to connect your minecraft server to discord, by following these steps:

1. Download [**MCDiscordRegions-x.x.x.jar**](https://github.com/CodeStix/MCDiscordRegions/releases/latest), [**WGEvents.jar**](https://www.spigotmc.org/resources/worldguard-events.65176/) and [**worldguard-bukkit-x.x.x.jar**](https://dev.bukkit.org/projects/worldguard).
2. Place the jar files in the `plugins/` directory of your server. 
3. Reload the server. (This will create the config.yml)
4. Register a discord bot for your minecraft server at https://discord.com/developers/applications, and copy the _TOKEN_.
5. Generate a bot invite link, using the bot's _CLIENT ID_ on https://discordapi.com/permissions.html, the bot must at least have the following permissions: `View Channel, Move Members, Read Messages, Send Messages, Add Reactions, Manage Channels, Manage Server, Manage Messages (= permission number 16788592)`
6. Edit the `plugins/MCDiscordRegions/config.yml` file and provide it with your bot's _TOKEN_.
7. Reload the server.

After configuring, use the system by:

1. Joining the _entry_ channel. (configurable)
2. Send a private message to the discord bot with your minecraft in-game name. (should react with a green tick)
3. Join the minecraft server.
4. Create some WorldGuard regions.
5. For every WorldGuard region you enter, a new discord channel will be created and you and others will be moved to it automaticly when you enter the region in Minecraft.

## Usage
`/drg [info | save | entry <channelName> | global <channelName> | category <categoryName> | server <serverId>]`
- `info`: display various information about the plugin.
- `save`: save the current settings to the default config file.
- `entry <channelName>`: set the Discord entry channel, this is the channel every user must join to enter the server. When joined, the user must send his minecraft in-game name to the Discord bot **in private**.
- `global <channelName>`: set the Discord global channel, this is the channel users will be placed in when they are in no defined region. (The global region)
- `category <categoryName>`: this is to create/set the Discord category the bot will create channels in.
- `server <serverId>`: forcefully set the Discord server id (or guild id) to use with this plugin, when your bot is in multiple discord servers (for some reason, should not be the case). Normally, the server will be recognized automaticly.

## Discord channel permissions

Currently, you will have to set discord category and channel permissions yourself, it is recommended to **deny** join rights on the category and override the entry channel to **allow** it. Also, **deny** speaking rights on the entry channel.

## Whitelist

When enabling the whitelist, players who register themselves with the bot will be whitelisted by the server and unwhitelisted when they leave the discord channel.

## Building

In order to build this project, you should add references to the following libraries:

-   [**WGEvents.jar**](https://www.spigotmc.org/resources/worldguard-events.65176/)
-   [**JDA-x.x.x-withDependencies.jar**](https://github.com/DV8FromTheWorld/JDA)
-   **spigot-x.x.x.jar** (use [BuildTools.jar](https://www.spigotmc.org/wiki/buildtools/) to generate this file)
-   [**worldguard-bukkit-x.x.x.jar**](https://dev.bukkit.org/projects/worldguard)

To ensure correct project structure, you should use IntelliJ IDEA.
Will move to maven soon.

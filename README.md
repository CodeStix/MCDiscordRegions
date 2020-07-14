# MCDiscordRegions

This bukkit plugin allows you to connect your minecraft server to Discord. By entering a region in your Minecraft world, this bot will place you in the appropriate Discord channel.

## Installation

1. Download [**MCDiscordRegions-x.x.jar**](https://github.com/CodeStix/MCDiscordRegions/releases/latest), [**worldedit-bukkit-x.x.x.jar**](https://dev.bukkit.org/projects/worldedit) and [**worldguard-bukkit-x.x.x.jar**](https://dev.bukkit.org/projects/worldguard).
2. Place the downloaded jar files in the `plugins/` directory of your server.
3. Reload the server. (This will create the config.yml of this plugin)
4. Register a discord bot for your minecraft server at https://discord.com/developers/applications, and copy the _TOKEN_.
5. Generate a bot invite link, using the bot's _CLIENT ID_ on https://discordapi.com/permissions.html, the bot must at least have the following permissions: `View Channel, Move Members, Read Messages, Send Messages, Add Reactions, Manage Channels, Manage Server, Manage Messages (= permission number 16788592)`, then invite the bot to your Discord server using the generated url.
6. Edit the `plugins/MCDiscordRegions/config.yml` file and provide it with your bot's _TOKEN_.
7. Reload the server.

## Usage

1. Joining the _entry_ channel. (configurable)
2. Send a private message to the discord bot with your minecraft in-game name. (should react with a green tick)
3. Join the Minecraft server.
4. Create some WorldGuard regions. [Tutorial](https://worldguard.enginehub.org/en/latest/regions/quick-start/)
5. Set the `discord-channel` region flag to a Discord channel name, the channel will be created automatically when someone enters that region:
   `/region flag your_region discord-channel your_channel_name`

## Command

⚠️ There are a lot of missing configuration features at this moment.

`/dregion [info | save | whitelist [on|off] | entry <channelName> | category <categoryName>]`
or `/drg ...`

-   `info`: display various information about the plugin.
-   `save`: save the current settings to the config file.
-   `entry <channelName>`: set the Discord entry channel, this is the channel every user must join to enter the server. When joined, the user must send his minecraft in-game name to the Discord bot **in private**.
-   `category <categoryName>`: this is to create/set the Discord category the bot will create channels in.
-   `whitelist [on|off]`: when on, players who enter the _entry_ Discord channel will be added to the whitelist, and removed when they disconnect.

## Discord channel permissions

Currently, you will have to set discord category and channel permissions yourself, it is recommended to **deny** join rights on the category and override the entry channel to **allow** it. Also, **deny** speaking rights on the entry channel.

## Building

Use maven to build this project:

```
mvn clean compile assembly:single
```

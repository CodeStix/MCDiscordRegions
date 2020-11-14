# MCDiscordRegions 2.x

This Bukkit plugin allows you to connect your minecraft server to Discord. By entering a **WorldGuard** region in your Minecraft world, you will be placed in the appropriate Discord channel automatically.

## Installation

1. [Invite](https://codestix.nl/) the Minecraft Regions bot to your Discord server.
2. Download [**MCDiscordRegions-x.x.jar**](https://github.com/CodeStix/MCDiscordRegions/releases/latest) ([**WorldEdit**](https://dev.bukkit.org/projects/worldedit) and [**WorldGuard**](https://dev.bukkit.org/projects/worldguard) are also required). Install these Bukkit plugins on your Minecraft server.
3. Restart your Minecraft server and check the server console for a Discord regions code (looks like `###xxxxxxxxxxxxxxxxxxxxxxxx`). Copy this code.
4. Create a new Discord category named after the code you just copied. The category will be set up as a Minecraft regions category automatically.

## Usage

1. Join the Minecraft server, instructions will be provided upon joining.

## Admin usage

1. Create some WorldGuard regions. [Tutorial](https://worldguard.enginehub.org/en/latest/regions/quick-start/)
2. Set the `discord-channel` region flag to a Discord channel name, the channel will be created automatically when someone enters that region: `/region flag your_region discord-channel your_channel_name`

## Command

⚠️ There are a lot of missing configuration features at this moment. Feel free to contribute.

`/dregion [whitelist [on|off] | kick [on|off] [kickMessage...] | limit <maxUsers> <channelName...>]`
or `/drg ...`

-   `whitelist [on|off]`: when on, Discord users will be added to the whitelist when they connect, and removed when they disconnect.
-   `kick`: when on, kicks the player when he/she leaves one of the Minecraft regions Discord channels. (with the specified message)
-   `limit <maxUsers> <channelName...>`: set the user limit on a Discord channel, if more than `maxUsers` players are in the Discord channel, no more players will be allowed in the Minecraft region and Discord channel.

## Building

This repo contains the Discord bot (written in Typescript, under `server/`) and the Bukkit plugin (under `plugin/`).

Use maven to build the Bukkit plugin:

```
mvn clean compile assembly:single
```

The plugin was written using IntelliJ IDEA.

import { Client as DiscordBot, TextChannel } from "discord.js";
import { debug } from "debug";

const logger = debug("mc-discord-bot");
const discord = new DiscordBot();

discord.once("ready", () => {
    logger("connected to Discord");

    sendMessage("this is a test");
});

export async function connect(token: string) {
    logger("connecting to Discord...");
    discord.login(token);
}

discord.on("message", (message) => {
    logger("message", message.content);
});

export async function sendMessage(message: string) {
    let guild = await discord.guilds.fetch("719991074910896159");
    let channel = guild.channels.cache.get("728252249892978698") as TextChannel | undefined;

    if (!channel) {
        logger("channel not found");
    } else {
        channel.send(message);
    }
}

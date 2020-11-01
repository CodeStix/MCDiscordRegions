import { Client as DiscordBot, TextChannel } from "discord.js";
import { debug } from "debug";
import { registerPlayer, revokePlayerBind } from "./redis";

const logger = debug("mc-discord-bot");
const discord = new DiscordBot();

discord.once("ready", () => {
    logger("connected to Discord");
});

export async function connect(token: string) {
    logger("connecting to Discord...");
    discord.login(token);
}

discord.on("message", async (message) => {
    if (!message.content.startsWith("#")) return;

    let code = message.content.substring(1);

    let uuid = await revokePlayerBind(code);
    if (!uuid) {
        logger(`user ${message.author.id} tried to revoke invalid key '${code}'`);
        message.reply("Invalid key.");
    } else {
        logger(`registering player with uuid ${uuid} with userId ${message.author.id}`);
        registerPlayer(uuid, message.author.id);
        message.reply(`Welcome, ${uuid}`);
    }
});

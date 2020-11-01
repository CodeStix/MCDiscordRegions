import { CategoryChannel, Client as DiscordBot, TextChannel } from "discord.js";
import { debug } from "debug";
import { deleteCategory, deleteServer, getServer, registerPlayer, registerServer, revokePlayerBind } from "./redis";

const logger = debug("mc-discord-bot");
const discord = new DiscordBot();
const CATEGORY_PREFIX = "###";
const PLAYER_PREFIX = "#";

discord.once("ready", () => {
    logger("connected to Discord");
});

export async function connect(token: string) {
    logger("connecting to Discord...");
    discord.login(token);
}

discord.on("channelDelete", async (channel) => {
    if (channel.type !== "category") return;
    let category = channel as CategoryChannel;
    let server = await getServer(category.id);
    if (!server) return;
    deleteServer(server);
    deleteCategory(category.id);
    logger(`category (${category.id}) got removed, causing server (${server}) to be removed too`);
});

discord.on("channelCreate", (channel) => {
    if (channel.type !== "category") return;
    let category = channel as CategoryChannel;
    if (category.name.startsWith(CATEGORY_PREFIX)) {
        let serverId = category.name.substring(CATEGORY_PREFIX.length);
        registerServer(serverId, category.id);
        category.setName("Minecraft Regions", "Category got registered as Minecraft Regions category.");
        logger(`category (${category.id}) created for server (${serverId})`);
    }
});

discord.on("message", async (message) => {
    if (!message.content.startsWith(PLAYER_PREFIX)) return;

    let key = message.content.substring(PLAYER_PREFIX.length);
    let uuid = await revokePlayerBind(key);
    if (!uuid) {
        logger(`user ${message.author.id} tried to revoke invalid key '${key}'`);
        message.reply("Invalid key.");
    } else {
        logger(`registering player with uuid ${uuid} with userId ${message.author.id}`);
        registerPlayer(uuid, message.author.id);
        message.reply(`Welcome, ${uuid}`);
    }
});

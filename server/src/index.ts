import { WebSocketServer } from "./server";
import { MinecraftRegionsBot } from "./bot";

let bot = new MinecraftRegionsBot(process.env.DISCORD_TOKEN!);
let socket;
bot.onConnected = async () => {
    socket = new WebSocketServer(bot, process.env.PORT as any);
};

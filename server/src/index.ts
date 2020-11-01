require("dotenv").config();
import { Server as WebSocketServer } from "ws";
import { debug } from "debug";
import { MinecraftRegionsBot } from "./MinecraftRegionsBot";
import { createPlayerBind, getCategory, getServer, getUser } from "./redis";
import { WebSocketMessage } from "./WebSocketMessage";

const logger = debug("websocket");

logger("starting websocket server...");
const server = new WebSocketServer({
    port: process.env.PORT as any,
});
server.once("listening", () => {
    logger(`websocket server is listening on port ${process.env.PORT}`);
});

const bot = new MinecraftRegionsBot(process.env.DISCORD_TOKEN!);
bot.onUserLeaveChannel = (categoryId) => {
    logger("user left channel", categoryId);
};
bot.onUserJoinChannel = (categoryId) => {
    logger("user joined channel", categoryId);
};

server.on("connection", (client, req) => {
    const clientLogger = logger.extend(`[${req.connection.remoteAddress}:${req.connection.remotePort}]`);
    clientLogger(`new connection`);

    client.on("error", (err) => {
        clientLogger(`error: ${err}`);
    });

    client.on("close", (code, reason) => {
        clientLogger(`closed connection: ${code} '${reason}'`);
    });

    client.on("message", async (message) => {
        if (typeof message !== "string") {
            clientLogger(`received invalid data`);
            return;
        }
        let data: WebSocketMessage;
        try {
            data = JSON.parse(message);
        } catch (ex) {
            clientLogger(`received malformed JSON: ${message}`);
            return;
        }

        const categoryId = await getCategory(data.serverId);
        if (!categoryId) {
            clientLogger(`sent command but no category for the server (${data.serverId}) was found`);
            return;
        }

        switch (data.action) {
            case "Move":
                {
                    const userId = await getUser(data.playerUuid);
                    if (userId) {
                        bot.move(categoryId, userId, data.regionName ?? "Global");
                    } else {
                        clientLogger(
                            "could not move user because it was not registered as a player, use key",
                            await createPlayerBind(data.playerUuid)
                        );
                    }
                }
                break;
            case "Join":
                break;
            case "Left":
                break;
            case "Death":
                {
                    const userId = await getUser(data.playerUuid);
                    if (userId) bot.mute(categoryId, userId, true);
                }
                break;
            case "Respawn":
                {
                    const userId = await getUser(data.playerUuid);
                    if (userId) bot.mute(categoryId, userId, false);
                }
                break;
            default:
                clientLogger(`received unknown action`, data);
                break;
        }
    });
});

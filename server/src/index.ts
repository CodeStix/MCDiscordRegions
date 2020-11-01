require("dotenv").config();
import WebSocket from "ws";
import { debug } from "debug";
import { MinecraftRegionsBot } from "./MinecraftRegionsBot";
import { createPlayerBind, getCategory, getPlayer, getServer, getUser } from "./redis";
import { JoinMessage, LeftMessage, WebSocketMessage } from "./WebSocketMessage";

const logger = debug("websocket");

logger("starting websocket server...");
const server = new WebSocket.Server({
    port: process.env.PORT as any,
});

server.once("listening", () => {
    logger(`websocket server is listening on port ${process.env.PORT}`);
});

let connections = new Map<string, WebSocket>();

const bot = new MinecraftRegionsBot(process.env.DISCORD_TOKEN!);
bot.onUserLeaveChannel = async (categoryId, userId) => {
    const serverId = await getServer(categoryId);
    if (!serverId) return;

    let connection = connections.get(serverId);
    if (!connection) return;

    let playerId = await getPlayer(userId);
    if (!playerId) return;

    connection.send(new LeftMessage(playerId).asJSON());
};
bot.onUserJoinChannel = async (categoryId, userId) => {
    const serverId = await getServer(categoryId);
    if (!serverId) return;

    let connection = connections.get(serverId);
    if (!connection) return;

    let playerId = await getPlayer(userId);
    if (!playerId) return;

    connection.send(new JoinMessage(playerId).asJSON());
};

server.on("connection", (client, req) => {
    const clientLogger = logger.extend(`[${req.connection.remoteAddress}:${req.connection.remotePort}]`);
    let serverId: string | null = null;

    client.on("error", (err) => {
        clientLogger(`error: ${err}`);
    });

    client.on("close", (code, reason) => {
        clientLogger(`closed connection: ${code} '${reason}'`);
        if (serverId) connections.delete(serverId);
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

        try {
            switch (data.action) {
                case "Auth":
                    {
                        if (serverId) {
                            logger("server %s is authenticating for the second time.", serverId);
                            connections.delete(serverId);
                        }
                        serverId = data.serverId;
                        connections.set(serverId, client);
                        clientLogger("authenticated as %s", serverId);
                    }
                    break;
                case "Move":
                    {
                        if (!serverId) throw new Error("Not authenticated");
                        const categoryId = await getCategory(serverId);
                        if (!categoryId) throw new Error(`No category found for server (${serverId})`);
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
                case "Death":
                    {
                        if (!serverId) throw new Error("Not authenticated");
                        const categoryId = await getCategory(serverId);
                        if (!categoryId) throw new Error(`No category found for server (${serverId})`);
                        const userId = await getUser(data.playerUuid);
                        if (userId) bot.mute(categoryId, userId, true);
                    }
                    break;
                case "Respawn":
                    {
                        if (!serverId) throw new Error("Not authenticated");
                        const categoryId = await getCategory(serverId);
                        if (!categoryId) throw new Error(`No category found for server (${serverId})`);
                        const userId = await getUser(data.playerUuid);
                        if (userId) bot.mute(categoryId, userId, false);
                    }
                    break;
                default:
                    clientLogger(`received unhandled action`, data);
                    break;
            }
        } catch (ex) {
            clientLogger(`could not execute action %o: %s`, data, ex);
        }
    });

    clientLogger(`new connection`);
});

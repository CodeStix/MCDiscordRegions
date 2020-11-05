require("dotenv").config();
import WebSocket from "ws";
import { debug } from "debug";
import { createPlayerBind, getCategory, getPlayer, getServer, getUser, rateLimit } from "./redis";
import {
    BoundMessage,
    JoinMessage,
    LeftMessage,
    LimitMessage,
    MoveMessage,
    RequireUserMessage,
    WebSocketMessage,
} from "./WebSocketMessage";
import { MinecraftRegionsBot, PLAYER_PREFIX } from "./MinecraftRegionsBot";

const logger = debug("mcdr:server");

logger("starting websocket server...");
const server = new WebSocket.Server({
    port: process.env.PORT as any,
});

let connections = new Map<string, WebSocket>();

server.once("listening", () => {
    logger(`websocket server is listening on port ${process.env.PORT}`);
});

const bot = new MinecraftRegionsBot(process.env.DISCORD_TOKEN!);
bot.onUserLeaveChannel = async (serverId, categoryId, userId) => {
    let connection = connections.get(serverId);
    if (!connection) return;

    let playerId = await getPlayer(userId);
    if (!playerId) return;

    connection.send(new LeftMessage(playerId).asJSON());
};
bot.onUserJoinChannel = async (serverId, categoryId, userId) => {
    let connection = connections.get(serverId);
    if (!connection) return;

    let playerId = await getPlayer(userId);
    if (!playerId) return;

    await bot.deafen(categoryId, userId, true);

    connection.send(new JoinMessage(playerId).asJSON());
};
bot.onUserBound = async (serverId, categoryId, userId, uuid) => {
    logger("user got bound for server %s, %s, %s", serverId, userId, uuid);
    let connection = connections.get(serverId);
    if (!connection) return;

    await bot.deafen(categoryId, userId, true);

    connection.send(new BoundMessage(uuid).asJSON());
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
        if (await rateLimit(req.connection.remoteAddress!)) {
            clientLogger("got rate limited!!");
            return;
        }

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
                case "Join":
                    {
                        if (!serverId) throw new Error("Not authenticated");
                        const categoryId = await getCategory(serverId);
                        if (!categoryId) throw new Error(`No category found for server (${serverId})`);
                        const userId = await getUser(data.playerUuid);
                        if (!userId) {
                            // Generate a temporary key that will be used to bind the Minecraft player to the Discord user
                            let key = await createPlayerBind(data.playerUuid);
                            client.send(new RequireUserMessage(data.playerUuid, PLAYER_PREFIX + key).asJSON());
                            logger("key for player %s: %s", data.playerUuid, key);
                        } else if (!bot.inCategoryChannel(categoryId, userId)) {
                            client.send(new RequireUserMessage(data.playerUuid).asJSON());
                            logger("user needs to be in discord channel");
                        } else {
                            await bot.deafen(categoryId, userId, false);
                        }
                    }
                    break;
                case "Left":
                    {
                        if (!serverId) throw new Error("Not authenticated");
                        const categoryId = await getCategory(serverId);
                        if (!categoryId) throw new Error(`No category found for server (${serverId})`);
                        const userId = await getUser(data.playerUuid);
                        if (userId) await bot.kick(categoryId, userId);
                    }
                    break;
                case "Move":
                    {
                        if (!serverId) throw new Error("Not authenticated");
                        const categoryId = await getCategory(serverId);
                        if (!categoryId) throw new Error(`No category found for server (${serverId})`);
                        const userId = await getUser(data.playerUuid);
                        if (!userId) throw new Error("No user found");
                        let result = await bot.move(categoryId, userId, data.regionName ?? "Global");
                        if (!result) {
                            // Respond with Move message to tell that the move was unsuccessful
                            client.send(new MoveMessage(data.playerUuid, data.regionName).asJSON());
                        }
                    }
                    break;
                case "Death":
                    {
                        if (!serverId) throw new Error("Not authenticated");
                        const categoryId = await getCategory(serverId);
                        if (!categoryId) throw new Error(`No category found for server (${serverId})`);
                        const userId = await getUser(data.playerUuid);
                        if (userId) await bot.mute(categoryId, userId, true);
                    }
                    break;
                case "Respawn":
                    {
                        if (!serverId) throw new Error("Not authenticated");
                        const categoryId = await getCategory(serverId);
                        if (!categoryId) throw new Error(`No category found for server (${serverId})`);
                        const userId = await getUser(data.playerUuid);
                        if (userId) await bot.mute(categoryId, userId, false);
                    }
                    break;
                case "Limit":
                    {
                        if (!serverId) throw new Error("Not authenticated");
                        const categoryId = await getCategory(serverId);
                        if (!categoryId) throw new Error(`No category found for server (${serverId})`);
                        let result = await bot.limit(categoryId, data.regionName, data.limit);
                        client.send(new LimitMessage(data.regionName, result ? data.limit : -1).asJSON());
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

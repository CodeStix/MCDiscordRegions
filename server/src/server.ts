require("dotenv").config();
import WebSocket from "ws";
import { debug } from "debug";
import {
    createPlayerBind,
    deletePlayer,
    deleteUser,
    getCategory,
    getPlayer,
    getServer,
    getUser,
    rateLimit,
} from "./redis";
import {
    BoundEventMessage,
    JoinEventMessage,
    LeaveEventMessage,
    LimitRequestMessage,
    RegionMoveEventMessage,
    Region,
    JoinRequireUserResponseMessage,
    SyncResponseMessage,
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
bot.onUserLeaveChannel = async (serverId, channel, userId) => {
    let connection = connections.get(serverId);
    if (!connection) return;

    let playerId = await getPlayer(userId);
    if (!playerId) return;

    connection.send(new LeaveEventMessage(playerId).asJSON());
};
bot.onUserJoinChannel = async (serverId, channel, userId) => {
    let connection = connections.get(serverId);
    if (!connection) return;

    let playerId = await getPlayer(userId);
    if (!playerId) return;

    await bot.deafen(channel.parentID!, userId, true);

    connection.send(new JoinEventMessage(playerId, channel.name).asJSON());
};
bot.onUserBound = async (serverId, categoryId, userId, uuid) => {
    logger("user got bound for server %s, %s, %s", serverId, userId, uuid);
    let connection = connections.get(serverId);
    if (!connection) return;

    await bot.deafen(categoryId, userId, true);

    connection.send(new BoundEventMessage(uuid).asJSON());
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
                case "PruneRequest":
                    if (!serverId) throw new Error("Not authenticated");
                    const categoryId = await getCategory(serverId);
                    if (!categoryId) throw new Error(`No category found for server (${serverId})`);
                    logger("pruning category", categoryId);
                    await bot.prune(categoryId);
                    break;
                case "SyncRequest":
                    {
                        if (serverId) {
                            logger("server %s is authenticating for the second time.", serverId);
                            connections.delete(serverId);
                        }
                        serverId = data.serverId;
                        connections.set(serverId, client);
                        clientLogger("authenticated as %s", serverId);

                        const categoryId = await getCategory(serverId);
                        let regions: Region[] = [];
                        if (!categoryId) {
                            regions = [];
                        } else {
                            regions = await bot.getRegions(categoryId);
                        }
                        client.send(new SyncResponseMessage(regions).asJSON());
                    }
                    break;
                case "SyncRequest": {
                    if (!serverId) throw new Error("Not authenticated");
                    const categoryId = await getCategory(serverId);
                    if (!categoryId) throw new Error(`No category found for server (${serverId})`);
                    let regions = await bot.getRegions(categoryId);
                    client.send(new SyncResponseMessage(regions).asJSON());
                    break;
                }
                // Fired when a player joined the minecraft server or if a player bound its Discord account
                case "JoinEvent":
                    {
                        if (!serverId) throw new Error("Not authenticated");
                        const categoryId = await getCategory(serverId);
                        if (!categoryId) throw new Error(`No category found for server (${serverId})`);
                        const userId = await getUser(data.playerUuid);
                        if (!userId) {
                            // Generate a temporary key that will be used to bind the Minecraft player to the Discord user
                            let key = await createPlayerBind(data.playerUuid);
                            client.send(
                                new JoinRequireUserResponseMessage(data.playerUuid, PLAYER_PREFIX + key).asJSON()
                            );
                            logger("key for player %s: %s", data.playerUuid, key);
                        } else if (!(await bot.inCategoryChannel(categoryId, userId))) {
                            client.send(new JoinRequireUserResponseMessage(data.playerUuid).asJSON());
                            logger("user needs to be in discord channel");
                        } else {
                            await bot.move(categoryId, userId, data.regionName);
                            await bot.deafen(categoryId, userId, false);
                        }
                    }
                    break;
                case "LeaveEvent":
                    {
                        if (!serverId) throw new Error("Not authenticated");
                        const categoryId = await getCategory(serverId);
                        if (!categoryId) throw new Error(`No category found for server (${serverId})`);
                        const userId = await getUser(data.playerUuid);
                        if (userId) await bot.kick(categoryId, userId);
                    }
                    break;
                case "RegionMoveEvent":
                    {
                        if (!serverId) throw new Error("Not authenticated");
                        const categoryId = await getCategory(serverId);
                        if (!categoryId) throw new Error(`No category found for server (${serverId})`);
                        const userId = await getUser(data.playerUuid);
                        if (!userId) throw new Error("No user found");
                        await bot.move(categoryId, userId, data.regionName ?? "Global");
                    }
                    break;
                case "DeathEvent":
                    {
                        if (!serverId) throw new Error("Not authenticated");
                        const categoryId = await getCategory(serverId);
                        if (!categoryId) throw new Error(`No category found for server (${serverId})`);
                        const userId = await getUser(data.playerUuid);
                        if (userId) await bot.mute(categoryId, userId, true);
                    }
                    break;
                case "RespawnEvent":
                    {
                        if (!serverId) throw new Error("Not authenticated");
                        const categoryId = await getCategory(serverId);
                        if (!categoryId) throw new Error(`No category found for server (${serverId})`);
                        const userId = await getUser(data.playerUuid);
                        if (userId) await bot.mute(categoryId, userId, false);
                    }
                    break;
                case "LimitRequest":
                    {
                        if (!serverId) throw new Error("Not authenticated");
                        const categoryId = await getCategory(serverId);
                        if (!categoryId) throw new Error(`No category found for server (${serverId})`);
                        await bot.limit(categoryId, data.regionName, data.limit);
                    }
                    break;
                case "UnBindRequest":
                    {
                        const userId = await getUser(data.playerUuid);
                        if (!userId) return;
                        await deletePlayer(data.playerUuid);
                        await deleteUser(userId);
                        if (serverId) {
                            const categoryId = await getCategory(serverId);
                            if (categoryId) await bot.kick(categoryId, userId);
                        }
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

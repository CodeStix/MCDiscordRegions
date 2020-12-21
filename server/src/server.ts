require("dotenv").config();
import WebSocket from "ws";
import { debug } from "debug";
import { createPlayerBind, getCategory, getLastRegion, getPlayer, getUser, rateLimit, setLastRegion } from "./redis";
import {
    BoundEventMessage,
    JoinEventMessage,
    LeaveEventMessage,
    Region,
    JoinRequireUserResponseMessage,
    SyncResponseMessage,
    WebSocketMessage,
} from "./messages";
import { GLOBAL_CHANNEL, MinecraftRegionsBot, PLAYER_PREFIX } from "./bot";
import { IncomingMessage } from "http";
import { VoiceChannel } from "discord.js";
import http from "http";

const logger = debug("mcdr:websocket-server");

export class WebSocketServer {
    private server: WebSocket.Server;
    private connections = new Map<string, WebSocket>();
    private bot: MinecraftRegionsBot;

    constructor(bot: MinecraftRegionsBot, server?: http.Server) {
        this.bot = bot;
        this.server = new WebSocket.Server({
            path: "/",
            server,
        });
        this.server.once("listening", this.handleListening.bind(this));
        this.server.on("connection", this.handleConnection.bind(this));
        this.bot.onUserLeaveChannel = this.discordHandleUserLeaveChannel.bind(this);
        this.bot.onUserJoinChannel = this.discordHandleUserJoinChannel.bind(this);
        this.bot.onUserBound = this.discordHandleUserBound.bind(this);
    }

    private async discordHandleUserLeaveChannel(serverId: string, channel: VoiceChannel, userId: string) {
        let connection = this.getConnection(serverId);
        if (!connection) return;

        let playerId = await getPlayer(userId);
        if (!playerId) return;

        connection.send(new LeaveEventMessage(playerId).asJSON());
    }

    private async discordHandleUserJoinChannel(serverId: string, channel: VoiceChannel, userId: string) {
        let connection = this.getConnection(serverId);
        if (!connection) return;

        let playerId = await getPlayer(userId);
        if (!playerId) return;

        await this.bot.deafen(channel.parentID!, userId, true);

        connection.send(new JoinEventMessage(playerId, channel.name).asJSON());
    }

    private async discordHandleUserBound(serverId: string, categoryId: string, userId: string, uuid: string) {
        logger("user got bound for server %s, %s, %s", serverId, userId, uuid);
        let connection = this.getConnection(serverId);
        if (!connection) return;

        if (await this.bot.inCategoryChannel(categoryId, userId)) await this.bot.deafen(categoryId, userId, true);

        connection.send(new BoundEventMessage(uuid).asJSON());
    }

    public getConnection(serverId: string) {
        return this.connections.get(serverId);
    }

    private handleListening() {
        logger(`websocket server is listening on port ${this.server.options.port}`);
    }

    private handleConnection(client: WebSocket, req: IncomingMessage) {
        const clientLogger = logger.extend(`[${req.connection.remoteAddress}:${req.connection.remotePort}]`);
        let serverId: string | null = null;

        client.on("error", (err) => {
            clientLogger(`error: ${err}`);
        });

        client.on("close", (code, reason) => {
            clientLogger(`closed connection: ${code} '${reason}'`);
            if (serverId) this.connections.delete(serverId);
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

            // Check if authentication request
            if (data.action === "SyncRequest") {
                if (this.connections.has(data.serverId)) {
                    clientLogger(
                        "tried to authenticate as an already connected server %s, not authenticating",
                        data.serverId
                    );
                    return;
                }
                serverId = data.serverId;
                this.connections.set(serverId, client);
                clientLogger("authenticated as %s", serverId);
                const categoryId = await getCategory(serverId);
                let regions: Region[] = [];
                if (!categoryId) {
                    regions = [];
                } else {
                    regions = await this.bot.getRegions(categoryId);
                }
                client.send(new SyncResponseMessage(regions).asJSON());
                return;
            }

            // Check if authenticated
            if (!serverId) return;

            // Check if server has category
            let categoryId = await getCategory(serverId);
            if (!categoryId) {
                clientLogger(`no category found for Discord server ${serverId}`);
                return;
            }

            try {
                switch (data.action) {
                    case "PruneRequest": {
                        clientLogger("pruning category", categoryId);
                        await this.bot.prune(categoryId);
                        break;
                    }

                    // Fired when a player joined the minecraft server or if a player binds its Discord account
                    case "JoinEvent": {
                        const userId = await getUser(data.playerUuid);
                        if (!userId) {
                            // Generate a temporary key that will be used to bind the Minecraft player to the Discord user
                            let key = await createPlayerBind(data.playerUuid);
                            client.send(
                                new JoinRequireUserResponseMessage(data.playerUuid, PLAYER_PREFIX + key).asJSON()
                            );
                            clientLogger("key for player %s: %s", data.playerUuid, key);
                        } else if (!(await this.bot.inCategoryChannel(categoryId, userId))) {
                            client.send(new JoinRequireUserResponseMessage(data.playerUuid).asJSON());
                            if (!(await getLastRegion(categoryId, userId)))
                                await this.bot.allowGlobalAccess(categoryId, userId);
                        } else {
                            let channel = data.regionName ?? GLOBAL_CHANNEL;
                            setLastRegion(categoryId, userId, channel);
                            await this.bot.move(categoryId, userId, channel);
                            await this.bot.deafen(categoryId, userId, false);
                        }
                        break;
                    }

                    case "LeaveEvent": {
                        const userId = await getUser(data.playerUuid);
                        if (userId) await this.bot.kick(categoryId, userId);
                        break;
                    }

                    case "RegionMoveEvent": {
                        const userId = await getUser(data.playerUuid);
                        if (!userId) throw new Error("No user found");
                        let channel = data.regionName ?? GLOBAL_CHANNEL;
                        setLastRegion(categoryId, userId, channel);
                        await this.bot.move(categoryId, userId, channel);
                        break;
                    }

                    case "DeathEvent": {
                        const userId = await getUser(data.playerUuid);
                        if (userId) await this.bot.mute(categoryId, userId, true);
                        break;
                    }

                    case "RespawnEvent": {
                        const userId = await getUser(data.playerUuid);
                        if (userId) await this.bot.mute(categoryId, userId, false);
                        break;
                    }

                    case "LimitRequest": {
                        await this.bot.limit(categoryId, data.regionName, data.limit);
                        break;
                    }

                    default: {
                        clientLogger(`received unhandled action`, data);
                        break;
                    }
                }
            } catch (ex) {
                clientLogger(`could not execute action %o: %s`, data, ex);
            }
        });

        clientLogger(`new connection`);
    }
}

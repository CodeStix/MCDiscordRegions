import { RedisClient } from "redis";
import util from "util";
import { nanoid } from "nanoid";
import { debug } from "debug";

// server: a Minecraft server
// guild: a Discord server
// player: a Minecraft player
// user: a Discord user

const logger = debug("redis");
logger("connecting to redis...");

const client = new RedisClient({
    host: process.env.REDIS_HOST,
    port: process.env.REDIS_PORT as any,
    password: process.env.REDIS_PASS,
});
const getAsync = util.promisify(client.get).bind(client);

export function registerServer(serverIp: string, guildId: string) {
    client.set(`server:${serverIp}`, guildId);
    client.set(`guild:${guildId}`, serverIp);
}

export function registerPlayer(playerUuid: string, userId: string) {
    client.set(`player:${playerUuid}`, userId);
    client.set(`user:${userId}`, playerUuid);
}

export async function getGuild(serverIp: string) {
    return await getAsync(`server:${serverIp}`);
}

export async function getServer(guildId: string) {
    return await getAsync(`guild:${guildId}`);
}

export async function getUser(playerUuid: string) {
    return await getAsync(`player:${playerUuid}`);
}

export async function getPlayer(userId: string) {
    return await getAsync(`user:${userId}`);
}

/**
 * Generates a bind key that will be used to identify a player.
 * @param forUuid The player's uuid
 */
export function createPlayerBind(forUuid: string): string {
    const key = nanoid(8);
    client.setex(`playerkey:${key}`, 60 * 60, forUuid);
    return key;
}

/**
 * Checks if the given bind key identifies the specified player.
 * @param key The key generated with createPlayerBind()
 * @param forUuid The player's uuid
 */
export async function revokePlayerBind(key: string): Promise<string | null> {
    let uuid = await getAsync(`playerbind:${key}`);
    if (uuid) {
        client.del(`playerbind:${key}`);
        return uuid;
    } else {
        return null;
    }
}

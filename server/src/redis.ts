import { RedisClient } from "redis";
import util from "util";
import { nanoid } from "nanoid";
import { debug } from "debug";

// server: a Minecraft server
// category: a Discord server category (collection of channels)
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

export function registerServer(serverId: string, categoryId: string) {
    client.set(`server:${serverId}`, categoryId);
    client.set(`category:${categoryId}`, serverId);
}

export function registerPlayer(playerUuid: string, userId: string) {
    client.set(`player:${playerUuid}`, userId);
    client.set(`user:${userId}`, playerUuid);
}

export function deleteCategory(categoryId: string) {
    client.del(`category:${categoryId}`);
}

export function deleteServer(serverId: string) {
    client.del(`server:${serverId}`);
}

export async function getCategory(serverId: string) {
    return await getAsync(`server:${serverId}`);
}

export async function getServer(categoryId: string) {
    return await getAsync(`category:${categoryId}`);
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
    const key = nanoid(6);
    client.setex(`playerbind:${key}`, 60 * 60, forUuid);
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

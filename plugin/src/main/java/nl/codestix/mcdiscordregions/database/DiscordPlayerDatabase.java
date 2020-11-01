package nl.codestix.mcdiscordregions.database;

import java.util.UUID;

/**
 * A database that binds Discord user ids to Minecraft UUIDs.
 */
public interface DiscordPlayerDatabase {
    boolean putPlayer(long userId, UUID playerId);
    boolean removePlayer(long userId);
    UUID getPlayer(long userId);
    int getPlayerCount();
}

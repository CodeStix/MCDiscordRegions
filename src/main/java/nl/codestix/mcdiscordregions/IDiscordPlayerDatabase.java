package nl.codestix.mcdiscordregions;

import java.util.UUID;

/**
 * A database that binds Discord user ids to Minecraft UUIDs.
 */
public interface IDiscordPlayerDatabase {
    boolean putPlayer(long userId, UUID playerId);
    boolean removePlayer(long userId);
    UUID getPlayer(long userId);
    int getPlayerCount();
}

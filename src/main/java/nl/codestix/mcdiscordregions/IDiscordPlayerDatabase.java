package nl.codestix.mcdiscordregions;

import java.util.UUID;

public interface IDiscordPlayerDatabase {
    boolean putPlayer(long userId, UUID playerId);
    boolean removePlayer(long userId);
    UUID getPlayer(long userId);
    int getPlayerCount();
}

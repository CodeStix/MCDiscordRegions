package nl.codestix.mcdiscordregions;

import java.util.UUID;

public interface DiscordConnection {
    void auth(String serverId);
    void join(UUID uuid);
    void left(UUID uuid);
    void death(UUID uuid);
    void respawn(UUID uuid);
    void regionMove(UUID uuid, String regionName);
    void close();
}

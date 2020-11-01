package nl.codestix.mcdiscordregions;

import java.util.UUID;

public interface DiscordConnection {
    void join(UUID uuid);
    void regionMove(UUID uuid, String regionName);
}

package nl.codestix.mcdiscordregions;

import java.util.UUID;

public interface DiscordEvents {
    void playerLeft(UUID uuid);
    void playerJoin(UUID uuid);
    void playerRequireUser(UUID uuid, String userBindKey);
}

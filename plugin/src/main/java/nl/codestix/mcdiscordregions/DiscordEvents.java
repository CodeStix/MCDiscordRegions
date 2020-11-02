package nl.codestix.mcdiscordregions;

import java.util.UUID;

public interface DiscordEvents {
    void userLeft(UUID uuid);
    void userJoined(UUID uuid);
    void userRequired(UUID uuid, String userBindKey);
    void userBoundPlayer(UUID uuid);
}

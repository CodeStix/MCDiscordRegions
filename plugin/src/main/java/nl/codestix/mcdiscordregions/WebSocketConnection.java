package nl.codestix.mcdiscordregions;

import java.util.UUID;

public class WebSocketConnection implements DiscordConnection {

    @Override
    public String join(UUID uuid) {
        // Send websocket message to server
        return null;
    }

    @Override
    public void regionMove(UUID uuid, String regionName) {
        // Send websocket message to server
    }
}

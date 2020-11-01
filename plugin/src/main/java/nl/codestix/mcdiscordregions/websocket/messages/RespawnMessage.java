package nl.codestix.mcdiscordregions.websocket.messages;

import nl.codestix.mcdiscordregions.websocket.WebSocketMessage;
import nl.codestix.mcdiscordregions.websocket.WebSocketMessageType;

public class RespawnMessage extends WebSocketMessage {

    public String playerUuid;

    public RespawnMessage(String serverId, String playerUuid) {
        super(serverId, WebSocketMessageType.Respawn);
        this.playerUuid = playerUuid;
    }
}

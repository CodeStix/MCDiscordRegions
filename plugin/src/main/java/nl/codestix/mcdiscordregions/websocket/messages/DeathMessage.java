package nl.codestix.mcdiscordregions.websocket.messages;

import nl.codestix.mcdiscordregions.websocket.WebSocketMessage;
import nl.codestix.mcdiscordregions.websocket.WebSocketMessageType;

public class DeathMessage extends WebSocketMessage {

    public String playerUuid;

    public DeathMessage(String serverId, String playerUuid) {
        super(serverId, WebSocketMessageType.Death);
        this.playerUuid = playerUuid;
    }
}

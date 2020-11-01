package nl.codestix.mcdiscordregions.websocket.messages;

import nl.codestix.mcdiscordregions.websocket.WebSocketMessage;
import nl.codestix.mcdiscordregions.websocket.WebSocketMessageType;

public class LeftMessage extends WebSocketMessage {

    public String playerUuid;

    public LeftMessage(String serverId, String playerUuid) {
        super(serverId, WebSocketMessageType.Left);
        this.playerUuid = playerUuid;
    }
}

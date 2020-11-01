package nl.codestix.mcdiscordregions.websocket.messages;

import nl.codestix.mcdiscordregions.websocket.WebSocketMessage;
import nl.codestix.mcdiscordregions.websocket.WebSocketMessageType;

public class JoinMessage extends WebSocketMessage {

    public String playerUuid;

    public JoinMessage(String serverId, String playerUuid) {
        super(serverId, WebSocketMessageType.Join);
        this.playerUuid = playerUuid;
    }
}

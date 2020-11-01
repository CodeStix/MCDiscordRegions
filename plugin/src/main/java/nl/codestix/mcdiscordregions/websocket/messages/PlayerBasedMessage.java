package nl.codestix.mcdiscordregions.websocket.messages;

import nl.codestix.mcdiscordregions.websocket.WebSocketMessage;
import nl.codestix.mcdiscordregions.websocket.WebSocketMessageType;

public class PlayerBasedMessage extends WebSocketMessage {

    public String playerUuid;

    public PlayerBasedMessage(WebSocketMessageType action, String playerUuid) {
        super(action);
        this.playerUuid = playerUuid;
    }
}

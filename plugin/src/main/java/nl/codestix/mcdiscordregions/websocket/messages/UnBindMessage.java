package nl.codestix.mcdiscordregions.websocket.messages;

import nl.codestix.mcdiscordregions.websocket.WebSocketMessage;
import nl.codestix.mcdiscordregions.websocket.WebSocketMessageType;

public class UnBindMessage extends PlayerBasedMessage {
    public UnBindMessage(String playerUuid) {
        super(WebSocketMessageType.UnBind, playerUuid);
    }
}

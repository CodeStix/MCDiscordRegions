package nl.codestix.mcdiscordregions.websocket.messages;

import nl.codestix.mcdiscordregions.websocket.WebSocketMessageType;

public class UnBindRequestMessage extends PlayerBasedMessage {
    public UnBindRequestMessage(String playerUuid) {
        super(WebSocketMessageType.UnBindRequest, playerUuid);
    }
}

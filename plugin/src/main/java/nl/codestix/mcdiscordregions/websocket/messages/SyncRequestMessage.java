package nl.codestix.mcdiscordregions.websocket.messages;

import nl.codestix.mcdiscordregions.websocket.WebSocketMessage;
import nl.codestix.mcdiscordregions.websocket.WebSocketMessageType;

public class SyncRequestMessage extends WebSocketMessage {

    public String serverId;

    public SyncRequestMessage(String serverId) {
        super(WebSocketMessageType.SyncRequest);
        this.serverId = serverId;
    }
}

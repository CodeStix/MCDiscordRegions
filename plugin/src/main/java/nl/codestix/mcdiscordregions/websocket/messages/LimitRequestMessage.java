package nl.codestix.mcdiscordregions.websocket.messages;

import nl.codestix.mcdiscordregions.websocket.WebSocketMessage;
import nl.codestix.mcdiscordregions.websocket.WebSocketMessageType;

public class LimitRequestMessage extends WebSocketMessage {

    public String regionName;
    public int limit;

    public LimitRequestMessage(String regionName, int limit) {
        super(WebSocketMessageType.LimitRequest);
        this.regionName = regionName;
        this.limit = limit;
    }
}

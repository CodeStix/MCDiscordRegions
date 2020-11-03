package nl.codestix.mcdiscordregions.websocket.messages;

import nl.codestix.mcdiscordregions.websocket.WebSocketMessage;
import nl.codestix.mcdiscordregions.websocket.WebSocketMessageType;

public class LimitMessage extends WebSocketMessage {

    public String regionName;
    public int limit;

    public LimitMessage(String regionName, int limit) {
        super(WebSocketMessageType.Limit);
        this.regionName = regionName;
        this.limit = limit;
    }
}

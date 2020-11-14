package nl.codestix.mcdiscordregions.websocket.messages;

import nl.codestix.mcdiscordregions.Region;
import nl.codestix.mcdiscordregions.websocket.WebSocketMessage;
import nl.codestix.mcdiscordregions.websocket.WebSocketMessageType;

import java.util.List;

public class SyncResponseMessage extends WebSocketMessage {

    public List<Region> regions;

    public SyncResponseMessage(List<Region> regions) {
        super(WebSocketMessageType.SyncResponse);
        this.regions = regions;
    }
}

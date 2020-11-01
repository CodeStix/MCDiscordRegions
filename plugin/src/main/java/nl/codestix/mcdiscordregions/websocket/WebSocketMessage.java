package nl.codestix.mcdiscordregions.websocket;

import com.google.gson.Gson;

public class WebSocketMessage {
    public WebSocketMessageType action;

    public WebSocketMessage(WebSocketMessageType action) {
        this.action = action;
    }

    public static WebSocketMessage fromJSON(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, WebSocketMessage.class);
    }

    public String toJSON() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    @Override
    public String toString() {
        return toJSON();
    }
}

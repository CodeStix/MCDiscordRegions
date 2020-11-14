package nl.codestix.mcdiscordregions.websocket;

public enum WebSocketMessageType {
    Move,
    JoinEvent,
    Left,
    Death,
    Respawn,
    RequireUser,
    Bound,
    Limit,
    UnBind,
    SyncRequest,
    SyncResponse
}

package nl.codestix.mcdiscordregions.websocket;

public enum WebSocketMessageType {
    Move,
    Join,
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

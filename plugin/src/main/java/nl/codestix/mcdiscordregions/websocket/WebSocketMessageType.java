package nl.codestix.mcdiscordregions.websocket;

public enum WebSocketMessageType {
    RegionMoveEvent,
    JoinEvent,
    JoinRequireUserResponse,
    LeaveEvent,
    DeathEvent,
    RespawnEvent,
    BoundEvent,
    LimitRequest,
    UnBindRequest,
    SyncRequest,
    SyncResponse
}

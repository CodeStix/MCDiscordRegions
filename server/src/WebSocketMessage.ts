interface BaseWebSocketMessage<T extends RegionMessageType> {
    serverId: string;
    action: T;
}

export type RegionMessageType = "Move" | "Join" | "Left" | "Death" | "Respawn";

export type MoveMessage = BaseWebSocketMessage<"Move"> & {
    playerUuid: string;
    regionName: string;
};

export type JoinMessage = BaseWebSocketMessage<"Join"> & {
    playerUuid: string;
};

export type LeftMessage = BaseWebSocketMessage<"Left"> & {
    playerUuid: string;
};

export type DeathMessage = BaseWebSocketMessage<"Death"> & {
    playerUuid: string;
};

export type RespawnMessage = BaseWebSocketMessage<"Respawn"> & {
    playerUuid: string;
};

export type WebSocketMessage = MoveMessage | JoinMessage | DeathMessage | LeftMessage | RespawnMessage;

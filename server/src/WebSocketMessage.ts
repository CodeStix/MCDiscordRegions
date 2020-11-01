interface BaseWebSocketMessage<T extends RegionMessageType> {
    serverId: string;
    action: T;
}

export type RegionMessageType = "Move" | "Join";

export type MoveMessage = BaseWebSocketMessage<"Move"> & {
    playerUuid: string;
    regionName: string;
};

export type JoinMessage = BaseWebSocketMessage<"Join"> & {
    playerUuid: string;
};

export type WebSocketMessage = MoveMessage | JoinMessage;

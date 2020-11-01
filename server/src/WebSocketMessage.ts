export type RegionMessageType = "Move" | "Join" | "Left" | "Death" | "Respawn" | "Auth";

class Message<T extends RegionMessageType> {
    action!: T;
}

class PlayerMessage<T extends RegionMessageType> extends Message<T> {
    playerUuid: string;

    constructor(playerUuid: string) {
        super();
        this.playerUuid = playerUuid;
    }
}

export class MoveMessage extends PlayerMessage<"Move"> {
    regionName: string;

    constructor(playerUuid: string, regionName: string) {
        super(playerUuid);
        this.regionName = regionName;
    }
}

export class JoinMessage extends PlayerMessage<"Join"> {
    constructor(playerUuid: string) {
        super(playerUuid);
    }
}

export class LeftMessage extends PlayerMessage<"Left"> {
    constructor(playerUuid: string) {
        super(playerUuid);
    }
}

export class DeathMessage extends PlayerMessage<"Death"> {
    constructor(playerUuid: string) {
        super(playerUuid);
    }
}

export class RespawnMessage extends PlayerMessage<"Respawn"> {
    constructor(playerUuid: string) {
        super(playerUuid);
    }
}

export class AuthMessage extends Message<"Auth"> {
    serverId: string;

    constructor(serverId: string) {
        super();
        this.serverId = serverId;
    }
}

export type WebSocketMessage = MoveMessage | JoinMessage | DeathMessage | LeftMessage | RespawnMessage | AuthMessage;

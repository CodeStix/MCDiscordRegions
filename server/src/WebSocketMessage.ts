export type RegionMessageType = "Move" | "Join" | "Left" | "Death" | "Respawn" | "Auth" | "RequireUser";

class Message<T extends RegionMessageType> {
    action: T;

    constructor(action: T) {
        this.action = action;
    }

    public asJSON(): string {
        return JSON.stringify(this);
    }
}

class PlayerMessage<T extends RegionMessageType> extends Message<T> {
    playerUuid: string;

    constructor(action: T, playerUuid: string) {
        super(action);
        this.playerUuid = playerUuid;
    }
}

export class MoveMessage extends PlayerMessage<"Move"> {
    regionName: string;

    constructor(playerUuid: string, regionName: string) {
        super("Move", playerUuid);
        this.regionName = regionName;
    }
}

export class JoinMessage extends PlayerMessage<"Join"> {
    constructor(playerUuid: string) {
        super("Join", playerUuid);
    }
}

export class LeftMessage extends PlayerMessage<"Left"> {
    constructor(playerUuid: string) {
        super("Left", playerUuid);
    }
}

export class DeathMessage extends PlayerMessage<"Death"> {
    constructor(playerUuid: string) {
        super("Death", playerUuid);
    }
}

export class RespawnMessage extends PlayerMessage<"Respawn"> {
    constructor(playerUuid: string) {
        super("Respawn", playerUuid);
    }
}

export class RequireUserMessage extends PlayerMessage<"RequireUser"> {
    key?: string;

    constructor(playerUuid: string, key?: string) {
        super("RequireUser", playerUuid);
        this.key = key;
    }
}

export class AuthMessage extends Message<"Auth"> {
    serverId: string;

    constructor(serverId: string) {
        super("Auth");
        this.serverId = serverId;
    }
}

export type WebSocketMessage = MoveMessage | JoinMessage | DeathMessage | LeftMessage | RespawnMessage | AuthMessage;

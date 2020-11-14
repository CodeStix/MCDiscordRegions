export type RegionMessageType =
    | "Move"
    | "JoinEvent"
    | "Left"
    | "Death"
    | "Respawn"
    | "RequireUser"
    | "Bound"
    | "Limit"
    | "UnBind"
    | "SyncResponse"
    | "SyncRequest";

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

/**
 * A message sent from the server to the bot to tell that a player has moved from one region to another.
 */
export class MoveMessage extends PlayerMessage<"Move"> {
    regionName: string;

    constructor(playerUuid: string, regionName: string) {
        super("Move", playerUuid);
        this.regionName = regionName;
    }
}

/**
 * A message sent from the server to the bot to tell that a player has joined the server.
 * A message sent from the bot to the server to tell the server that a user has connected to the Discord category.
 */
export class JoinEventMessage extends PlayerMessage<"JoinEvent"> {
    regionName: string;

    constructor(playerUuid: string, regionName: string) {
        super("JoinEvent", playerUuid);
        this.regionName = regionName;
    }
}

/**
 * A message sent from the server to the bot to tell that a player has left the server.
 * A message sent from the bot to the server to tell the server that a user has left the Discord category.
 */
export class LeftMessage extends PlayerMessage<"Left"> {
    constructor(playerUuid: string) {
        super("Left", playerUuid);
    }
}

/**
 * A message sent from the server to the bot to tell that a player has died.
 */
export class DeathMessage extends PlayerMessage<"Death"> {
    constructor(playerUuid: string) {
        super("Death", playerUuid);
    }
}

/**
 * A message sent from the server to the bot to tell that a player has respawned.
 */
export class RespawnMessage extends PlayerMessage<"Respawn"> {
    constructor(playerUuid: string) {
        super("Respawn", playerUuid);
    }
}

/**
 * A message sent from the bot to the server to tell that a Discord category requires a Discord bound Minecraft account.
 * Can be a result of the Join message.
 */
export class RequireUserMessage extends PlayerMessage<"RequireUser"> {
    key?: string;

    constructor(playerUuid: string, key?: string) {
        super("RequireUser", playerUuid);
        this.key = key;
    }
}

/**
 * A message sent from the server to the bot to tell the bot which server it is.
 */
export class SyncRequestMessage extends Message<"SyncRequest"> {
    serverId: string;

    constructor(serverId: string) {
        super("SyncRequest");
        this.serverId = serverId;
    }
}

/**
 * A message sent from the bot to the server to tell the server that a player uuid was bound
 * to a Discord user id.
 */
export class BoundMessage extends PlayerMessage<"Bound"> {
    constructor(playerUuid: string) {
        super("Bound", playerUuid);
    }
}

/**
 * A message sent from the server to the bot to tell the bot that a region should have a limited amount of people in it
 * A message sent from the bot to the server to tell that the limit for a region has modified using the Discord UI.
 */
export class LimitMessage extends Message<"Limit"> {
    regionName: string;
    limit: number;

    constructor(regionName: string, limit: number) {
        super("Limit");
        this.regionName = regionName;
        this.limit = limit;
    }
}

export class UnBindMessage extends PlayerMessage<"UnBind"> {
    constructor(playerUuid: string) {
        super("UnBind", playerUuid);
    }
}

export interface Region {
    name: string;
    playerUuids: string[];
    limit: number;
}

export class SyncResponseMessage extends Message<"SyncResponse"> {
    regions: Region[];

    constructor(regions: Region[]) {
        super("SyncResponse");
        this.regions = regions;
    }
}

export type WebSocketMessage =
    | MoveMessage
    | JoinEventMessage
    | DeathMessage
    | LeftMessage
    | RespawnMessage
    | SyncRequestMessage
    | BoundMessage
    | LimitMessage
    | UnBindMessage
    | RequireUserMessage
    | SyncResponseMessage;

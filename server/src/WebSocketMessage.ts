export type RegionMessageType =
    | "RegionMoveEvent"
    | "JoinEvent"
    | "JoinRequireUserResponse"
    | "LeaveEvent"
    | "DeathEvent"
    | "RespawnEvent"
    | "BoundEvent"
    | "LimitRequest"
    | "UnBindRequest"
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
export class RegionMoveEventMessage extends PlayerMessage<"RegionMoveEvent"> {
    regionName: string;

    constructor(playerUuid: string, regionName: string) {
        super("RegionMoveEvent", playerUuid);
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
export class LeaveEventMessage extends PlayerMessage<"LeaveEvent"> {
    constructor(playerUuid: string) {
        super("LeaveEvent", playerUuid);
    }
}

/**
 * A message sent from the server to the bot to tell that a player has died.
 */
export class DeathEventMessage extends PlayerMessage<"DeathEvent"> {
    constructor(playerUuid: string) {
        super("DeathEvent", playerUuid);
    }
}

/**
 * A message sent from the server to the bot to tell that a player has respawned.
 */
export class RespawnEventMessage extends PlayerMessage<"RespawnEvent"> {
    constructor(playerUuid: string) {
        super("RespawnEvent", playerUuid);
    }
}

/**
 * A message sent from the bot to the server to tell that a Discord category requires a Discord bound Minecraft account.
 * Can be a result of the Join message.
 */
export class JoinRequireUserResponseMessage extends PlayerMessage<"JoinRequireUserResponse"> {
    key?: string;

    constructor(playerUuid: string, key?: string) {
        super("JoinRequireUserResponse", playerUuid);
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
export class BoundEventMessage extends PlayerMessage<"BoundEvent"> {
    constructor(playerUuid: string) {
        super("BoundEvent", playerUuid);
    }
}

/**
 * A message sent from the server to the bot to tell the bot that a region should have a limited amount of people in it
 * A message sent from the bot to the server to tell that the limit for a region has modified using the Discord UI.
 */
export class LimitRequestMessage extends Message<"LimitRequest"> {
    regionName: string;
    limit: number;

    constructor(regionName: string, limit: number) {
        super("LimitRequest");
        this.regionName = regionName;
        this.limit = limit;
    }
}

export class UnBindRequestMessage extends PlayerMessage<"UnBindRequest"> {
    constructor(playerUuid: string) {
        super("UnBindRequest", playerUuid);
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
    | RegionMoveEventMessage
    | JoinEventMessage
    | DeathEventMessage
    | LeaveEventMessage
    | RespawnEventMessage
    | SyncRequestMessage
    | BoundEventMessage
    | LimitRequestMessage
    | UnBindRequestMessage
    | JoinRequireUserResponseMessage
    | SyncResponseMessage;

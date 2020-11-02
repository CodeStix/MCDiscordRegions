export type RegionMessageType = "Move" | "Join" | "Left" | "Death" | "Respawn" | "Auth" | "RequireUser" | "Bound";

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
export class JoinMessage extends PlayerMessage<"Join"> {
    constructor(playerUuid: string) {
        super("Join", playerUuid);
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
export class AuthMessage extends Message<"Auth"> {
    serverId: string;

    constructor(serverId: string) {
        super("Auth");
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

export type WebSocketMessage =
    | MoveMessage
    | JoinMessage
    | DeathMessage
    | LeftMessage
    | RespawnMessage
    | AuthMessage
    | BoundMessage;

export interface RegionMessage {
    serverId: string;
    action: RegionMessageType;
    [key: string]: any;
}

export type RegionMessageType = "Move" | "Death";

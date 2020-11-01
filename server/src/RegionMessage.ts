export interface RegionMessage {
    serverId: string;
    action: "move" | "death";
    [key: string]: any;
}

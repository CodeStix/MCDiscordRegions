require("dotenv").config();
import { Server as WebSocketServer } from "ws";
import { debug } from "debug";
import { connect } from "./discord";
import { RegionMessage, RegionMessageType } from "./RegionMessage";
import { getCategory, getServer } from "./redis";

const logger = debug("websocket");

logger("starting...");

const server = new WebSocketServer({
    port: process.env.PORT as any,
});

server.once("listening", () => {
    logger(`websocket server is listening on port ${process.env.PORT}`);

    connect(process.env.DISCORD_TOKEN!);
});

server.on("connection", (client, req) => {
    const clientLogger = logger.extend(`[${req.connection.remoteAddress}:${req.connection.remotePort}]`);
    clientLogger(`new connection`);

    client.on("error", (err) => {
        clientLogger(`error: ${err}`);
    });

    client.on("close", (code, reason) => {
        clientLogger(`closed connection: ${code} '${reason}'`);
    });

    client.on("message", (message) => {
        if (typeof message !== "string") {
            clientLogger(`received invalid data`);
            return;
        }
        let data: RegionMessage;
        try {
            data = JSON.parse(message);
        } catch (ex) {
            clientLogger(`received malformed JSON: ${message}`);
            return;
        }

        const category = getCategory(data.serverId);
        if (!category) {
            clientLogger(`sent command but no category for the server (${data.serverId}) was found`);
            return;
        }

        switch (data.action) {
            case "Move":
                clientLogger(`received move message`);
                break;
            default:
                clientLogger(`received unknown action '${data.action}'`);
                break;
        }
    });
});

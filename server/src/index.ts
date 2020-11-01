require("dotenv").config();
import { Server as WebSocketServer } from "ws";
import { debug } from "debug";
import { connect } from "./discord";
import { RegionMessage } from "./RegionMessage";

const logger = debug("mc-websocket");

logger("starting...");

const server = new WebSocketServer({
    port: process.env.PORT as any,
});

server.once("listening", () => {
    logger(`websocket server is listening on port ${process.env.PORT}`);

    connect(process.env.DISCORD_TOKEN!);
});

server.on("connection", (client, req) => {
    const name = `[${req.connection.remoteAddress}:${req.connection.remotePort}]`;
    logger(`${name} new connection`);

    client.on("error", (err) => {
        logger(`${name} error: ${err}`);
    });

    client.on("close", (code, reason) => {
        logger(`${name} closed connection: ${code} '${reason}'`);
    });

    client.on("message", (message) => {
        if (typeof message !== "string") {
            logger(`${name} received invalid data`);
            return;
        }
        let data: RegionMessage;
        try {
            data = JSON.parse(message);
        } catch (ex) {
            logger(`${name} received malformed JSON: ${message}`);
            return;
        }

        logger("received", data);

        switch (data.action) {
            case "move":
                break;
            default:
                logger(`${name} received unknown action '${data.action}'`);
                break;
        }
    });
});

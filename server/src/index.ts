require("dotenv").config();
import { Server as WebSocketServer } from "ws";
import { debug } from "debug";

const logger = debug("mc-discord-regions");

logger("starting...");

const server = new WebSocketServer({
    port: process.env.PORT as any,
});

server.once("listening", () => {
    logger(`websocket server is listening on port ${process.env.PORT}`);
});

server.on("connection", (client, req) => {
    logger(`client connection from ${req.connection.remoteAddress}`);

    client.on("error", (err) => {
        logger(`client error: ${err}`);
    });

    client.on("close", (code, reason) => {
        logger(`client closed connection: ${code} '${reason}'`);
    });

    client.on("message", (data) => {
        logger(`received client message: ${data}`);
    });
});

import { WebSocketServer } from "./server";
import { MinecraftRegionsBot } from "./bot";
import http from "http";
import https from "https";
import fs from "fs";

let server: http.Server;
if (process.env.SSL_KEY_FILE && process.env.SSL_CERT_FILE && process.env.SSL_CA_FILE) {
    server = https
        .createServer({
            key: fs.readFileSync(process.env.SSL_KEY_FILE),
            cert: fs.readFileSync(process.env.SSL_CERT_FILE),
            ca: fs.readFileSync(process.env.SSL_CA_FILE),
        })
        .listen(443);
} else {
    server = http.createServer().listen(80);
}

let bot = new MinecraftRegionsBot(process.env.DISCORD_TOKEN!);
bot.onConnected = async () => {
    new WebSocketServer(bot, server);
};

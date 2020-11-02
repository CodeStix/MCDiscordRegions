import { CategoryChannel, Channel, Client as DiscordBot, Message, VoiceState } from "discord.js";
import { debug } from "debug";
import { deleteCategory, deleteServer, getServer, registerPlayer, registerServer, revokePlayerBind } from "./redis";

const logger = debug("discord-bot");

export const CATEGORY_PREFIX = "###";
export const PLAYER_PREFIX = "#";
export const PLAYER_REGISTER_CHANNEL = "minecraft-bind";
export const PLAYER_REGISTER_CHANNEL_DESCRIPTION = "Use this channel to bind your Discord tag to your Minecraft name.";

export class MinecraftRegionsBot {
    public onUserLeaveChannel: (serverId: string, userId: string) => void = () => {};
    public onUserJoinChannel: (serverId: string, userId: string) => void = () => {};
    public onUserBound: (serverId: string, userId: string, playerUuid: string) => void = () => {};
    private discord: DiscordBot;

    constructor(token: string) {
        logger("connecting to Discord...");
        this.discord = new DiscordBot();
        this.discord.once("ready", this.connectHandler.bind(this));
        this.discord.on("voiceStateUpdate", this.handleVoiceStateUpdate.bind(this));
        this.discord.on("channelDelete", this.handleChannelDelete.bind(this));
        this.discord.on("channelCreate", this.handleChannelCreate.bind(this));
        this.discord.on("channelUpdate", this.handleChannelUpdate.bind(this));
        this.discord.on("message", this.handleMessage.bind(this));
        this.discord.login(token);
    }

    private connectHandler() {
        logger("connected to Discord");
    }

    private async handleVoiceStateUpdate(state: VoiceState, newState: VoiceState) {
        if (!state.channel || !newState.channel || state.channelID !== newState.channelID) {
            if (state.channel && state.channel.parentID && state.channel.parentID !== newState.channel?.parentID) {
                // On user left voice channel
                let serverId = await getServer(state.channel.parentID);
                if (serverId) this.onUserLeaveChannel(serverId, state.id);
            }
            if (
                newState.channel &&
                newState.channel.parentID &&
                newState.channel.parentID !== state.channel?.parentID
            ) {
                // On user join voice channel
                let serverId = await getServer(newState.channel.parentID);
                if (serverId) this.onUserJoinChannel(serverId, state.id);
            }
        }
    }

    private async handleChannelUpdate(channel: Channel, newChannel: Channel) {
        this.handleChannelCreate(newChannel);
    }

    private async handleChannelDelete(channel: Channel) {
        if (channel.type !== "category") return;
        let category = channel as CategoryChannel;
        let server = await getServer(category.id);
        if (!server) return;
        deleteServer(server);
        deleteCategory(category.id);
        logger(`category (${category.id}) got removed, causing server (${server}) to be removed too`);
    }

    private handleChannelCreate(channel: Channel) {
        if (channel.type !== "category") return;
        let category = channel as CategoryChannel;
        if (category.name.startsWith(CATEGORY_PREFIX)) {
            let serverId = category.name.substring(CATEGORY_PREFIX.length);
            registerServer(serverId, category.id);
            category.setName("Minecraft Regions", "Category got registered as Minecraft Regions category.");

            let registerChannel = category.children.find((e) => e.name === PLAYER_REGISTER_CHANNEL);
            if (!registerChannel) {
                category.guild.channels.create(PLAYER_REGISTER_CHANNEL, {
                    parent: category,
                    type: "text",
                    position: 0,
                    rateLimitPerUser: 20,
                    topic: PLAYER_REGISTER_CHANNEL_DESCRIPTION,
                });
            }

            logger(`category (${category.id}) created for server (${serverId})`);
        }
    }

    private async handleMessage(message: Message) {
        if (!message.content.startsWith(PLAYER_PREFIX)) return;

        if (message.channel.type !== "text" || !message.channel.parentID) {
            message.reply(`Please use the #${PLAYER_REGISTER_CHANNEL} channel`);
            return;
        }

        const categoryId = message.channel.parentID;
        const serverId = await getServer(categoryId);
        if (!serverId) {
            message.reply(
                `This channel is not connected to a Minecraft server, please use the #${PLAYER_REGISTER_CHANNEL} channel to register yourself.`
            );
            return;
        }

        let key = message.content.substring(PLAYER_PREFIX.length);
        let uuid = await revokePlayerBind(key);
        if (!uuid) {
            logger(`user ${message.author.id} tried to revoke invalid key '${key}'`);
            message.reply("Invalid key.");
        } else {
            logger(`registering player with uuid ${uuid} with userId ${message.author.id}`);
            registerPlayer(uuid, message.author.id);
            message.reply(`Welcome, ${uuid}`);
            this.onUserBound(serverId, message.author.id, uuid);
        }
    }

    private getCategory(categoryId: string): CategoryChannel | null {
        let channel = this.discord.channels.cache.get(categoryId);
        if (!channel || channel.type !== "category") {
            logger("is not a category:", categoryId);
            return null;
        } else {
            return channel as CategoryChannel;
        }
    }

    public async mute(categoryId: string, userId: string, mute: boolean) {
        let category = this.getCategory(categoryId);
        if (!category) {
            logger("category %s not found", categoryId);
            return;
        }

        let member = category.guild.members.cache.get(userId);
        if (!member) {
            logger("member is not found:", userId);
            return;
        }

        if (!member.voice.channel) {
            logger("cannot mute member because he/she is not connected to a voice channel");
            return;
        }

        await member.voice.setMute(mute);
    }

    public async move(categoryId: string, userId: string, channelName: string) {
        let category = this.getCategory(categoryId);
        if (!category) {
            logger("category %s not found", categoryId);
            return;
        }

        let member = category.guild.members.cache.get(userId);
        if (!member) {
            logger("member is not found:", userId);
            return;
        }

        if (!member.voice.channel) {
            logger("cannot move member because he/she is not connected to a voice channel");
            return;
        }

        let moveChannel = category.children.find((e) => e.name === channelName);
        if (moveChannel == null) {
            moveChannel = await category.guild.channels.create(channelName, {
                type: "voice",
                parent: category,
                reason: "This location does exist in Minecraft",
            });
        }

        await member.voice.setChannel(moveChannel, "Moved to this location in Minecraft");
    }
}

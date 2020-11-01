import { CategoryChannel, Channel, Client as DiscordBot, Message, VoiceState } from "discord.js";
import { debug } from "debug";
import { deleteCategory, deleteServer, getServer, registerPlayer, registerServer, revokePlayerBind } from "./redis";

const logger = debug("discord-bot");
const CATEGORY_PREFIX = "###";
const PLAYER_PREFIX = "#";

export class MinecraftRegionsBot {
    public onUserLeaveChannel: (categoryId: string) => void = () => {};
    public onUserJoinChannel: (categoryId: string) => void = () => {};
    private discord: DiscordBot;

    constructor(token: string) {
        logger("connecting to Discord...");
        this.discord = new DiscordBot();
        this.discord.once("ready", this.connectHandler.bind(this));
        this.discord.on("voiceStateUpdate", this.handleVoiceStateUpdate.bind(this));
        this.discord.on("channelDelete", this.handleChannelDelete.bind(this));
        this.discord.on("channelCreate", this.handleChannelCreate.bind(this));
        this.discord.on("message", this.handleMessage.bind(this));
        this.discord.login(token);
    }

    private connectHandler() {
        logger("connected to Discord");
    }

    private handleVoiceStateUpdate(state: VoiceState, newState: VoiceState) {
        if (!state.channel || !newState.channel || state.channelID !== newState.channelID) {
            if (state.channel && state.channel.parentID && state.channel.parentID !== newState.channel?.parentID) {
                // On user left voice channel
                if (state.channel.parentID) this.onUserLeaveChannel(state.channel.parentID);
            }
            if (
                newState.channel &&
                newState.channel.parentID &&
                newState.channel.parentID !== state.channel?.parentID
            ) {
                // On user join voice channel
                if (newState.channel.parentID) this.onUserJoinChannel(newState.channel.parentID);
            }
        }
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
            logger(`category (${category.id}) created for server (${serverId})`);
        }
    }

    private async handleMessage(message: Message) {
        if (!message.content.startsWith(PLAYER_PREFIX)) return;

        let key = message.content.substring(PLAYER_PREFIX.length);
        let uuid = await revokePlayerBind(key);
        if (!uuid) {
            logger(`user ${message.author.id} tried to revoke invalid key '${key}'`);
            message.reply("Invalid key.");
        } else {
            logger(`registering player with uuid ${uuid} with userId ${message.author.id}`);
            registerPlayer(uuid, message.author.id);
            message.reply(`Welcome, ${uuid}`);
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

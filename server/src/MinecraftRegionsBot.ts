import {
    CategoryChannel,
    Channel,
    Client as DiscordBot,
    Guild,
    GuildMember,
    Message,
    MessageEmbed,
    OverwriteResolvable,
    VoiceChannel,
    VoiceState,
} from "discord.js";
import { debug } from "debug";
import {
    deleteCategory,
    deleteServer,
    getPlayer,
    getServer,
    registerPlayer,
    registerServer,
    revokePlayerBind,
} from "./redis";
import { getIGN } from "./minecraft";

const logger = debug("mcdr:discord-bot");

export const CATEGORY_PREFIX = "###";
export const PLAYER_PREFIX = "#";
export const PLAYER_REGISTER_CHANNEL = "minecraft-bind";
export const PLAYER_REGISTER_CHANNEL_DESCRIPTION = "Use this channel to bind your Discord tag to your Minecraft name.";
export const REGIONS_MANAGER_ROLE = "Minecraft Regions Manager";

export class MinecraftRegionsBot {
    public onUserLeaveChannel: (serverId: string, categoryId: string, userId: string) => void = () => {};
    public onUserJoinChannel: (serverId: string, categoryId: string, userId: string) => void = () => {};
    public onUserBound: (serverId: string, categoryId: string, userId: string, playerUuid: string) => void = () => {};
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
                if (serverId) {
                    this.onUserLeaveChannel(serverId, state.channel.parentID, state.id);
                    // Allow the user to resume where he left off
                    await this.overrideChannelAccess(state.channel, state.id, true);
                }
            }
            if (
                newState.channel &&
                newState.channel.parentID &&
                newState.channel.parentID !== state.channel?.parentID
            ) {
                // On user join voice channel
                let serverId = await getServer(newState.channel.parentID);
                if (serverId) {
                    this.onUserJoinChannel(serverId, newState.channel.parentID, state.id);
                    // Revoke their 'resume' permissions
                    await this.overrideChannelAccess(newState.channel, newState.id, false);
                }
            }
        }
    }

    private async overrideChannelAccess(channel: VoiceChannel, userId: string, allow: boolean) {
        logger("overriding channel access for %s, user %s to %o", channel.name, userId, allow);
        if (allow) {
            await channel!.overwritePermissions([
                ...channel!.permissionOverwrites.values(),
                {
                    type: "member",
                    id: userId,
                    allow: ["CONNECT", "VIEW_CHANNEL"],
                },
            ]);
        } else {
            await channel.overwritePermissions(
                channel.permissionOverwrites.filter((e) => !(e.type === "member" && e.id === userId))
            );
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
        let userId = message.author.id;

        if (message.channel.type !== "text" || !message.channel.parentID) {
            message.channel.send(
                new MessageEmbed()
                    .setColor(16711680)
                    .setTitle(
                        `❌ This channel is not connected to a Minecraft server, please use the #${PLAYER_REGISTER_CHANNEL} channel.`
                    )
            );
            return;
        }

        const categoryId = message.channel.parentID;
        const serverId = await getServer(categoryId);
        if (!serverId) {
            message.channel.send(
                new MessageEmbed()
                    .setColor(16711680)
                    .setTitle(
                        `❌ This channel is not connected to a Minecraft server, please use the #${PLAYER_REGISTER_CHANNEL} channel.`
                    )
            );
            return;
        }

        let key = message.content.substring(PLAYER_PREFIX.length);
        let uuid = await revokePlayerBind(key);
        if (!uuid) {
            logger(`user ${userId} tried to revoke invalid key '${key}'`);
            message.channel.send(
                new MessageEmbed()
                    .setColor(16711680)
                    .setTitle(
                        `❌ That is not a valid Minecraft code. You can receive a code when you join the Minecraft server. These codes are CaSe SeNsiTiVe.`
                    )
            );
        } else {
            logger(`registering player with uuid ${uuid} with userId ${userId}`);
            registerPlayer(uuid, userId);
            this.onUserBound(serverId, categoryId, userId, uuid);

            let ign = await getIGN(uuid);
            message.channel.send(
                new MessageEmbed()
                    .setColor(65280)
                    .setTitle(
                        `✅ ${message.author.username}, your Discord account is now connected to your Minecraft account ${ign}!`
                    )
            );

            // Create or get the global (default channel)
            let globalChannel = await this.getOrCreateRegionChannel(message.channel.parent!, "Global");
            // Allow access to the (normally hidden) global channel
            this.overrideChannelAccess(globalChannel, userId, true);
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

    private getVoiceMember(categoryId: string, userId: string): GuildMember | null {
        let category = this.getCategory(categoryId);
        if (!category) {
            logger("category %s not found", categoryId);
            return null;
        }
        let member = category.guild.members.cache.get(userId);
        if (!member) {
            logger("member is not found:", userId);
            return null;
        }
        if (!member.voice.channel) {
            logger("cannot kick member because he/she is not connected to a voice channel");
            return null;
        }
        return member;
    }

    private async getOrCreateRegionManagerRole(guild: Guild) {
        let role = guild.roles.cache.find((role) => role.name === "Regions Manager");
        if (!role) {
            role = await guild.roles.create({
                data: { name: REGIONS_MANAGER_ROLE, mentionable: true, color: [255, 128, 64] },
            });
        }
        return role;
    }

    private async getOrCreateRegionChannel(category: CategoryChannel, name: string): Promise<VoiceChannel> {
        let channel = category.children.find((e) => e.type === "voice" && e.name === name);
        if (channel == null) {
            // Set permissions: everyone but the bot and Minecraft Regions Managers can see the created regions channels
            let everyone = category.guild.roles.everyone;
            let manager = await this.getOrCreateRegionManagerRole(category.guild);
            return await category.guild.channels.create(name, {
                type: "voice",
                parent: category,
                reason: "This location does exist in Minecraft",
                permissionOverwrites: [
                    {
                        id: everyone,
                        type: "role",
                        deny: ["CONNECT", "VIEW_CHANNEL"],
                    },
                    {
                        id: category.guild.me!,
                        type: "member",
                        allow: ["CONNECT", "VIEW_CHANNEL"],
                    },
                    {
                        id: manager,
                        type: "role",
                        allow: ["CONNECT", "VIEW_CHANNEL"],
                    },
                ],
            });
        } else {
            return channel as VoiceChannel;
        }
    }

    public async kick(categoryId: string, userId: string) {
        const member = this.getVoiceMember(categoryId, userId);
        if (!member) return;

        await member.voice.kick();
    }

    public async deafen(categoryId: string, userId: string, deaf: boolean) {
        const member = this.getVoiceMember(categoryId, userId);
        if (!member) return;

        await member.voice.setDeaf(deaf);
    }

    public async mute(categoryId: string, userId: string, mute: boolean) {
        const member = this.getVoiceMember(categoryId, userId);
        if (!member) return;

        await member.voice.setMute(mute);
    }

    public inCategoryChannel(categoryId: string, userId: string): boolean {
        return !!this.getVoiceMember(categoryId, userId);
    }

    public async move(categoryId: string, userId: string, channelName: string) {
        let category = this.getCategory(categoryId);
        if (!category) {
            logger("move category %s not found", categoryId);
            return false;
        }
        let member = category.guild.members.cache.get(userId);
        if (!member) {
            logger("move member is not found:", userId);
            return false;
        }
        if (!member.voice.channel) {
            logger("cannot move member because he/she is not connected to a voice channel");
            return false;
        }
        if (member.voice.channel.name === channelName) {
            logger("user is already in the right channel");
            return false;
        }

        let moveChannel = await this.getOrCreateRegionChannel(category, channelName);
        await member.voice.setChannel(moveChannel, "Moved to this location in Minecraft");
        return moveChannel.userLimit === 0 || moveChannel.members.size < moveChannel.userLimit;
    }

    public async limit(categoryId: string, regionName: string, userLimit: number) {
        let category = this.getCategory(categoryId);
        if (!category) {
            logger("limit category %s not found", categoryId);
            return false;
        }

        let channel = category.children.find((e) => e.name === regionName) as VoiceChannel;
        if (!channel || channel.type !== "voice") {
            logger("limit channel not found");
            return false;
        }

        await channel.setUserLimit(userLimit);
        logger("set limit for channel %s to %d", regionName, userLimit);
        return true;
    }
}

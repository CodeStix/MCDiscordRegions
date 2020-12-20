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
    deletePlayer,
    deleteServer,
    deleteUser,
    getPlayer,
    getServer,
    registerPlayer,
    registerServer,
    revokePlayerBind,
} from "./redis";
import { getIGN } from "./minecraft";
import { Region } from "./messages";

const logger = debug("mcdr:discord-bot");

export const CATEGORY_PREFIX = "###";
export const PLAYER_PREFIX = "###";
export const PLAYER_REGISTER_CHANNEL = "minecraft-bind";
export const PLAYER_REGISTER_CHANNEL_DESCRIPTION = "Use this channel to bind your Discord tag to your Minecraft name.";
export const REGIONS_MANAGER_ROLE = "Minecraft Regions Manager";
export const INTERVAL_PER_USER = 750;
export const GLOBAL_CHANNEL = "Global";

export class MinecraftRegionsBot {
    public onUserLeaveChannel: (serverId: string, channel: VoiceChannel, userId: string) => void = () => {};
    public onUserJoinChannel: (serverId: string, channel: VoiceChannel, userId: string) => void = () => {};
    public onUserBound: (serverId: string, categoryId: string, userId: string, playerUuid: string) => void = () => {};
    private discord: DiscordBot;

    constructor(token: string) {
        logger("connecting to Discord...");
        this.discord = new DiscordBot({ disableMentions: "all" });
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
        if (newState.member?.user.bot || state.member?.user.bot) return;

        if (!state.channel || !newState.channel || state.channelID !== newState.channelID) {
            if (state.channel && state.channel.parentID && state.channel.parentID !== newState.channel?.parentID) {
                // On user left voice channel
                let serverId = await getServer(state.channel.parentID);
                if (serverId) {
                    this.onUserLeaveChannel(serverId, state.channel, state.id);
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
                    this.onUserJoinChannel(serverId, newState.channel, state.id);
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

    private createErrorMessage(message: string) {
        return new MessageEmbed().setColor(16711680).setTitle("❌ Whoops!").setDescription(message);
    }

    private createSuccessMessage(message: string) {
        return new MessageEmbed().setColor(65280).setTitle("✅ Success!").setDescription(message);
    }

    private async handleMessage(message: Message) {
        if (!message.content.startsWith(PLAYER_PREFIX)) return;
        let userId = message.author.id;

        if (message.channel.type !== "text" || !message.channel.parentID) {
            message.channel.send(
                this.createErrorMessage(
                    `This channel is not connected to a Minecraft server, please use the #${PLAYER_REGISTER_CHANNEL} channel.`
                )
            );
            return;
        }

        const categoryId = message.channel.parentID;
        const serverId = await getServer(categoryId);
        if (!serverId) {
            message.channel.send(
                this.createErrorMessage(
                    `This channel is not connected to a Minecraft server, please use the #${PLAYER_REGISTER_CHANNEL} channel.`
                )
            );
            return;
        }

        let key = message.content.substring(PLAYER_PREFIX.length).trim();
        if (key === "remove") {
            deletePlayer((await getPlayer(userId))!);
            deleteUser(userId);
            message.channel.send(
                this.createSuccessMessage(`${message.author.username}, I removed your Minecraft-Discord connection.`)
            );
            if (await this.inCategoryChannel(categoryId, userId)) await this.kick(categoryId, userId);
            return;
        }

        let uuid = await revokePlayerBind(key);
        if (!uuid) {
            logger(`user ${userId} tried to revoke invalid key '${key}'`);
            message.channel.send(
                this.createErrorMessage(
                    `❌ That is not a valid Minecraft code. You can receive a code when you join the Minecraft server. These codes are CaSe SeNsiTiVe.`
                )
            );
        } else {
            logger(`registering player with uuid ${uuid} with userId ${userId}`);
            registerPlayer(uuid, userId);
            this.onUserBound(serverId, categoryId, userId, uuid);

            let ign = await getIGN(uuid);
            message.channel.send(
                this.createSuccessMessage(
                    `${message.author.username}, your Discord account is now connected to your Minecraft account ${ign}!\nYou can remove this connection by typing ${PLAYER_PREFIX}remove`
                )
            );

            // Create or get the global (default channel)
            let globalChannel = await this.getOrCreateRegionChannel(message.channel.parent!, GLOBAL_CHANNEL);
            // Allow access to the (normally hidden) global channel
            this.overrideChannelAccess(globalChannel, userId, true);
        }
    }

    private async getCategory(categoryId: string): Promise<CategoryChannel> {
        let channel = await this.discord.channels.fetch(categoryId);
        if (!channel || channel.type !== "category") {
            throw new Error("getCategory: not found");
        } else {
            return channel as CategoryChannel;
        }
    }

    public async getRegions(categoryId: string): Promise<Region[]> {
        let category = await this.getCategory(categoryId);
        let regions: Region[] = await Promise.all(
            category.children
                .filter((channel) => channel.type === "voice")
                .map(async (channel) => {
                    const voice = channel as VoiceChannel;
                    const players = await Promise.all(voice.members.map(async (user) => await getPlayer(user.id)));
                    return {
                        limit: voice.userLimit,
                        name: voice.name,
                        playerUuids: players.filter((pl) => pl !== null) as string[],
                    };
                })
        );

        return regions;
    }

    private async getVoiceMember(categoryId: string, userId: string): Promise<GuildMember | null> {
        let category = await this.getCategory(categoryId);
        let member = category.guild.members.cache.get(userId);
        if (!member) throw new Error("getVoiceMember: user not found");
        if (!member.voice.channel) return null;
        return member;
    }

    private async getOrCreateRegionManagerRole(guild: Guild) {
        let role = guild.roles.cache.find((role) => role.name === REGIONS_MANAGER_ROLE);
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
        const member = await this.getVoiceMember(categoryId, userId);
        if (!member) throw new Error("kick: member is not in channel");
        await member.voice.kick();
    }

    public async deafen(categoryId: string, userId: string, deaf: boolean) {
        const member = await this.getVoiceMember(categoryId, userId);
        if (!member) throw new Error("deafen: member is not in channel");
        await member.voice.setDeaf(deaf);
    }

    public async mute(categoryId: string, userId: string, mute: boolean) {
        const member = await this.getVoiceMember(categoryId, userId);
        if (!member) throw new Error("mute: member is not in channel");
        await member.voice.setMute(mute);
    }

    public async inCategoryChannel(categoryId: string, userId: string): Promise<boolean> {
        return !!(await this.getVoiceMember(categoryId, userId));
    }

    public async prune(categoryId: string) {
        let category = await this.getCategory(categoryId);
        await Promise.all(
            category.children.map(async (e) => {
                if (e.type === "voice" && e.members.size === 0 && e.name !== GLOBAL_CHANNEL)
                    await e.delete("Category got pruned");
            })
        );
    }

    private lastMoves: Map<string, number> = new Map();
    private laterMoves: Map<string, NodeJS.Timeout> = new Map();

    public async move(categoryId: string, userId: string, channelName: string) {
        let category = await this.getCategory(categoryId);
        let member = category.guild.members.cache.get(userId);
        if (!member) throw new Error("move: user is not found");
        if (!member.voice.channel) throw new Error("move: user is not in a channel");

        const doMove = async () => {
            if (member!.voice.channel!.name === channelName) return; // user is already in the right channel
            let moveChannel = await this.getOrCreateRegionChannel(category, channelName);
            await member!.voice.setChannel(moveChannel, "Moved to this location in Minecraft");
        };

        const now = new Date().getTime();
        let lastMove = this.lastMoves.get(userId);
        if (lastMove && now - lastMove < INTERVAL_PER_USER) {
            // move the user later, because he got rate limited
            if (this.laterMoves.has(userId)) clearTimeout(this.laterMoves.get(userId)!);
            let timeout = setTimeout(doMove, INTERVAL_PER_USER);
            this.laterMoves.set(userId, timeout);
        } else {
            // Move the user instantly
            await doMove();
            this.lastMoves.set(userId, new Date().getTime());
        }
    }

    public async limit(categoryId: string, regionName: string, userLimit: number) {
        let category = await this.getCategory(categoryId);
        let channel = category.children.find((e) => e.name === regionName) as VoiceChannel;
        if (!channel || channel.type !== "voice") throw new Error("limit: channel is not found");

        await channel.setUserLimit(userLimit);
        logger("set limit for channel %s to %d", regionName, userLimit);
    }
}

package de.maltesermailo.discordchat;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import de.maltesermailo.discordchat.config.DiscordChatConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordChat extends ListenerAdapter implements DedicatedServerModInitializer, ServerMessageEvents.ChatMessage, ServerMessageEvents.GameMessage, ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("discord-chat");

	public static DiscordChatConfig config = DiscordChatConfig.createAndLoad();

	private JDA jda;
	private MinecraftServer server;
	private WebhookClient client;

	public DiscordChat() {
		LOGGER.info("Hi");
	}

	@Override
	public void onInitializeServer() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hi, soon be ready to chat!");

		try {
			jda = JDABuilder.createDefault(config.token(), GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_EMOJIS_AND_STICKERS, GatewayIntent.MESSAGE_CONTENT).build();
			jda.addEventListener(this);

			ServerMessageEvents.CHAT_MESSAGE.register(this);
			ServerMessageEvents.GAME_MESSAGE.register(this);
			ServerLifecycleEvents.SERVER_STARTED.register((server -> {
				this.server = server;
			}));

			client = WebhookClient.withUrl(config.webhookUrl());
		} catch(Exception e) {
			LOGGER.error("Couldn't register with discord, disabling...");
		}
	}

	@Override
	public void onReady(ReadyEvent event) {
		LOGGER.info("Connected to Discord");
	}

	@Override
	public void onChatMessage(SignedMessage message, ServerPlayerEntity sender, MessageType.Parameters params) {
		WebhookMessageBuilder builder = new WebhookMessageBuilder();
		builder.setAvatarUrl(String.format("https://minotar.net/avatar/%s", sender.getName().getString()));
		builder.setUsername(sender.getName().getString());
		builder.setContent(message.getContent().getString().replace("@here", "").replace("@everyone", ""));

		client.send(builder.build());
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if(event.isFromGuild() && !event.isWebhookMessage() && event.getChannel().asGuildMessageChannel().getId().equalsIgnoreCase(config.channelId()) && !event.getAuthor().getId().equalsIgnoreCase("598987731217022976")) {
			String message = event.getMessage().getContentStripped();

			for(ServerPlayerEntity entity : this.server.getPlayerManager().getPlayerList()) {
				entity.sendMessage(Text.literal("[Discord] ").append(event.getMember().getEffectiveName()).append(": ").append(message));
			}
		}
	}

	@Override
	public void onInitialize() {
		LOGGER.info("INITIALIZING");

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("spank")
				.executes(context -> {
					// For versions below 1.19, replace "Text.literal" with "new LiteralText".
					// For versions below 1.20, remode "() ->" directly.
					ServerCommandSource source = context.getSource();

					for(ServerPlayerEntity entity : source.getServer().getPlayerManager().getPlayerList()) {
						if(entity.getName().getString().equalsIgnoreCase("lavaclara")) {
							entity.damage(entity.getWorld().getDamageSources().magic(), 0.1F);

							entity.sendMessage(Text.literal("You just got spanked by ").append(source.getEntity().getName()));
							source.sendMessage(Text.literal("You spanked Clara! Wobble wobble."));

							return 1;
						}
					}

					source.sendMessage(Text.literal("Clara is not online!"));

					return 1;
				})));
	}

	@Override
	public void onGameMessage(MinecraftServer server, Text message, boolean overlay) {
		WebhookMessageBuilder builder = new WebhookMessageBuilder();
		builder.setUsername("Server");
		builder.setContent("**`" + message.getString() + "`**");

		client.send(builder.build());
	}
}
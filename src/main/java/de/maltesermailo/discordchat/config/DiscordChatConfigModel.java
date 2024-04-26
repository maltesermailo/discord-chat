package de.maltesermailo.discordchat.config;

import io.wispforest.owo.config.annotation.Config;

import java.util.HashMap;
import java.util.UUID;

@Config(name = "discord-chat", wrapperName = "DiscordChatConfig")
public class DiscordChatConfigModel {

    public String token;
    public String webhookUrl;

    public String channelId;
    public String guildId;

}

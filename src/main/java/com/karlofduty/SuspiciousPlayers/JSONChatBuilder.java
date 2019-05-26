package com.karlofduty.SuspiciousPlayers;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;

public class JSONChatBuilder
{
	public static String plainText(String text)
	{
		return "{\"text\":\"" + text + "\"}";
	}
	public static String plainText(String text, ChatColor colour)
	{
		return "{\"text\":\"" + text + "\",\"color\":\"" + colour.getName() + "\"}";
	}
	public static String runCommand(String text, String command)
	{
		return "{\"text\":\"" + text + "\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"" + command + "\"}}";
	}
	public static String suggestCommand(String text, String command)
	{
		return "{\"text\":\"" + text + "\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"" + command + "\"}}";
	}
}

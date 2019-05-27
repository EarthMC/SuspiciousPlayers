package com.karlofduty.SuspiciousPlayers.models;

import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;

import java.text.SimpleDateFormat;
import java.util.UUID;

public abstract class PlayerEntry
{
	public static SimpleDateFormat displayDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	public abstract TextComponent getInteractiveMessage();

	static String getUsername(String uuid)
	{
		try
		{
			return Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
		}
		catch (Exception e)
		{
			return uuid;
		}
	}
}

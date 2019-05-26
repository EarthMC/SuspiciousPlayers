package com.karlofduty.SuspiciousPlayers.models;

import org.bukkit.Bukkit;

import java.text.SimpleDateFormat;
import java.util.UUID;

public abstract class PlayerEntry
{
	public static SimpleDateFormat displayDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	public abstract String getFormattedString();

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

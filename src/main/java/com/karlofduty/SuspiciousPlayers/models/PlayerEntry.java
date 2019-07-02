package com.karlofduty.SuspiciousPlayers.models;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.shanerx.mojang.Mojang;

import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static net.md_5.bungee.api.ChatColor.*;

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

	static Map<String, Long> getUsernameHistory(String uuid)
	{
		try
		{
			Mojang api = new Mojang().connect();

			if (api.getStatus(Mojang.ServiceType.API_MOJANG_COM) != Mojang.ServiceStatus.GREEN)
			{
				SuspiciousPlayers.instance.getLogger().severe("The API is not available right now.");
			}

			return api.getNameHistoryOfPlayer(uuid);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public static TextComponent getUsernameComponent(String uuid, ChatColor color)
	{
		try
		{
			Map<String, Long> usernameHistory = getUsernameHistory(uuid);

			if(usernameHistory == null || usernameHistory.isEmpty())
			{
				return new TextComponent(TextComponent.fromLegacyText(getUsername(uuid)));
			}

			String[] usernames = usernameHistory.keySet().stream().toArray(String[]::new);

			StringBuilder popup = new StringBuilder();

			for (String username : usernames)
			{
				popup.append(GRAY);
				popup.append(String.format("%s-32", username));
				popup.append(DARK_GRAY);
				popup.append(ITALIC);
				popup.append(displayDateFormat.format(usernameHistory.get(username)));
				popup.append('\n');
				popup.append(RESET);
			}

			return new TextComponent(
					new ComponentBuilder(usernames[0])
							.color(color)
							.append("WADDUP")
							.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(DARK_GRAY + "Known aliases:" + popup.toString())))
							.create());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return new TextComponent(TextComponent.fromLegacyText(color + getUsername(uuid)));
		}
	}
}

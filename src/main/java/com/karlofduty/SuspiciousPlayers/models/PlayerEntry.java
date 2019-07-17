package com.karlofduty.SuspiciousPlayers.models;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import static net.md_5.bungee.api.ChatColor.*;

public abstract class PlayerEntry
{
	public static SimpleDateFormat displayDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	public abstract TextComponent getInteractiveMessage();

	static String getUsername(UUID uuid)
	{
		try
		{
			return Bukkit.getOfflinePlayer(uuid).getName();
		}
		catch (Exception e)
		{
			return uuid.toString();
		}
	}

	public static TextComponent getNameComponent(UUID uuid, ChatColor color)
	{
		try
		{
			TreeMap<String, String> usernameHistory = getUsernameHistory(uuid);

			if(usernameHistory == null || usernameHistory.isEmpty())
			{
				return new TextComponent(
						new ComponentBuilder(getUsername(uuid))
								.color(color)
								.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(DARK_GRAY + "Known aliases:\n\n" + RED + "ERROR: COULD NOT CONNECT TO MOJANG API")))
								.create());
			}

			StringBuilder popup = new StringBuilder();

			for(Map.Entry<String, String> entry : usernameHistory.entrySet())
			{
				popup.append('\n');
				popup.append(GRAY);
				popup.append(String.format("%-32s", entry.getValue()));
				popup.append(DARK_GRAY);
				popup.append(ITALIC);
				popup.append(" ");
				popup.append(String.format("%-20s", entry.getKey()));
				popup.append(RESET);
			}

			return new TextComponent(
					new ComponentBuilder(usernameHistory.firstEntry().getValue())
							.color(color)
							.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(DARK_GRAY + "Known aliases:\n" + popup.toString())))
							.create());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return new TextComponent(TextComponent.fromLegacyText(color + getUsername(uuid)));
		}
	}

	private static TreeMap<String, String> getUsernameHistory(UUID uuid)
	{
		Type type = new TypeToken<HashMap<String, String>>(){}.getType();
		Gson gson = new GsonBuilder().registerTypeAdapter(type, new NameMapDeserializer()).create();
		String compactUUID = uuid.toString().replace("-", "");
		try
		{
			URL url = new URL("https://api.mojang.com/user/profiles/" + compactUUID + "/names");
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			TreeMap<String, String> nameHistory = gson.fromJson(reader, type);
			reader.close();
			conn.disconnect();

			return nameHistory;
		}
		catch (Exception e)
		{
			SuspiciousPlayers.instance.getLogger().severe("Error occurred while contacting MojangAPI: \n" + e);
		}
		return null;
	}
}

/**
 * The input map is actually an array of unnamed objects with a name and timestamp, this gets turned into a Map<Long, String>
 */
class NameMapDeserializer implements JsonDeserializer<TreeMap<String, String>>
{
	@Override
	public TreeMap<String, String> deserialize(JsonElement json, Type type, JsonDeserializationContext context)
	{
		TreeMap<String, String> map = new TreeMap<>(Collections.reverseOrder());

		for (JsonElement jItem : json.getAsJsonArray())
		{
			JsonObject jObject = jItem.getAsJsonObject();
			String name = jObject.get("name").getAsString();
			String timestamp = "-";
			if(jObject.get("changedToAt") != null)
			{
				timestamp = PlayerEntry.displayDateFormat.format(jObject.get("changedToAt").getAsLong());
			}
			map.put(timestamp, name);
		}
		return map;
	}
}

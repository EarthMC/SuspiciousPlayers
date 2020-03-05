package com.karlofduty.SuspiciousPlayers.models;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

import static net.md_5.bungee.api.ChatColor.*;

public abstract class PlayerEntry
{
	/**
	 * Date formatting for plugin messages.
	 */
	public static final SimpleDateFormat displayDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	/**
	 * A cache of name history lookups.
	 */
	private static ConcurrentSkipListMap<UUID, ConcurrentSkipListMap<String, String>> nameCache = new ConcurrentSkipListMap<>();

	private static long lastCacheClearing = System.currentTimeMillis();

	/**
	 * Gets an interactive message for a list entry with different buttons and actions depending on the type of entry.
	 * @return A TextComponent containing the information and functionality of this entry.
	 */
	public abstract TextComponent getInteractiveMessage();

	/**
	 * Gets a player's username from local server files.
	 * @param uuid ID of the player.
	 * @return The name of the player.
	 */
	public static String getUsername(UUID uuid)
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

	/**
	 * Gets a TextComponent containing the name and a popup with name history of a player. MAKE SURE THIS IS ALWAYS CALLED ASYNCHRONOUSLY!
	 * @param uuid ID of the player.
	 * @param color Color of the player name in the text component.
	 * @return The finished TextComponent.
	 */
	public static TextComponent getNameComponent(UUID uuid, ChatColor color)
	{
		try
		{
			// Get the user from the cache
			ConcurrentSkipListMap<String, String> usernameHistory = nameCache.get(uuid);

			// If the cache does not have this player in it, check the mojang api
			if(usernameHistory == null)
			{
				usernameHistory = getUsernameHistory(uuid);

				// If the mojang api does not return a proper result, get the username from the local server files
				if(usernameHistory == null || usernameHistory.isEmpty())
				{
					// Return TextComponent with error popup
					return new TextComponent(
							new ComponentBuilder(getUsername(uuid))
									.color(color)
									.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(DARK_GRAY + "Known aliases:\n" + RED + "ERROR: COULD NOT CONNECT TO MOJANG API")))
									.create());
				}

				// Save the entry to the cache
				cacheHistory(uuid, usernameHistory);
			}

			// Builds the hover popup section of the message
			StringBuilder popup = new StringBuilder();
			for(Map.Entry<String, String> entry : usernameHistory.entrySet())
			{
				popup.append('\n');
				popup.append(GRAY);
				popup.append(String.format("    %-32s", entry.getValue()));
				popup.append(DARK_GRAY);
				popup.append(ITALIC);
				popup.append(String.format("%-20s", entry.getKey()));
				popup.append(RESET);
			}

			popup.append('\n');
			popup.append('\n');
			popup.append(YELLOW);
			popup.append("Click to open susplist for this player.");
			popup.append(RESET);

			// Builds the final TextComponent and returns it
			return new TextComponent(
					new ComponentBuilder(usernameHistory.firstEntry().getValue())
							.color(color)
							.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(DARK_GRAY + "Known aliases:" + popup.toString())))
							.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/susplist " + usernameHistory.firstEntry().getValue()))
							.create());
		}
		catch (Exception e)
		{
			// Print error and return TextComponent with error popup
			e.printStackTrace();
			return new TextComponent(
					new ComponentBuilder(getUsername(uuid))
							.color(color)
							.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(DARK_GRAY + "Known aliases:\n" + RED + "ERROR: COULD NOT CONNECT TO MOJANG API")))
							.create());
		}
	}

	/**
	 * Contacts the Mojang API and requests the name history of a player.
	 * @param uuid ID of the player to check.
	 * @return A map of timestamps and player names.
	 */
	private static ConcurrentSkipListMap<String, String> getUsernameHistory(UUID uuid)
	{
		Type type = new TypeToken<HashMap<String, String>>(){}.getType();
		Gson gson = new GsonBuilder().registerTypeAdapter(type, new NameMapDeserializer()).create();
		String compactUUID = uuid.toString().replace("-", "");
		try
		{
			// Contacts the Mojang API and requests the player's name history
			URL url = new URL("https://api.mojang.com/user/profiles/" + compactUUID + "/names");
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			// Deserializes the json response using the NameMapDeserializer implemented below
			ConcurrentSkipListMap<String, String> nameHistory = gson.fromJson(reader, type);
			reader.close();
			conn.disconnect();

			return nameHistory;
		}
		catch (Exception e)
		{
			SuspiciousPlayers.instance.getLogger().severe("Error occurred while contacting MojangAPI: \n" + e);
			return null;
		}
	}

	/**
	 * Caches a player's name history and also clears the cache if it has gotten old enough.
	 * @param uuid ID of the player.
	 * @param playerHistory The name history of the player, as time/name pairs.
	 */
	private static void cacheHistory(UUID uuid, ConcurrentSkipListMap<String, String> playerHistory)
	{
		if(lastCacheClearing + TimeUnit.HOURS.toMillis(6) < System.currentTimeMillis())
		{
			nameCache.clear();
			lastCacheClearing = System.currentTimeMillis();
		}
		nameCache.put(uuid, playerHistory);
	}
}

/**
 * Deserializer for the name history package from the Mojang API.
 * The input map is actually an array of unnamed objects with a name and (usually) timestamp, this gets turned into a Map<Long, String> below.
 */
class NameMapDeserializer implements JsonDeserializer<ConcurrentSkipListMap<String, String>>
{
	@Override
	public ConcurrentSkipListMap<String, String> deserialize(JsonElement json, Type type, JsonDeserializationContext context)
	{
		try
		{
			ConcurrentSkipListMap<String, String> map = new ConcurrentSkipListMap<>(Collections.reverseOrder());
			for (JsonElement jItem : json.getAsJsonArray())
			{
				JsonObject jObject = jItem.getAsJsonObject();

				// Gets the player name, this is always included in each object
				String name = jObject.get("name").getAsString();

				// Timestamps are not included on the player's first name so it is set to "-" in that case
				String timestamp = "-";
				if(jObject.get("changedToAt") != null)
				{
					timestamp = PlayerEntry.displayDateFormat.format(jObject.get("changedToAt").getAsLong());
				}
				map.put(timestamp, name);
			}
			return map;
		}
		catch (Exception e)
		{
			SuspiciousPlayers.instance.getLogger().severe("Error occurred while parsing json package: \n" + e);
			return null;
		}
	}
}

package com.karlofduty.SuspiciousPlayers.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

public abstract class PlayerEntry {
	private static final Type type = new TypeToken<HashMap<String, String>>(){}.getType();
	private static final Gson gson = new GsonBuilder().registerTypeAdapter(type, new NameMapDeserializer()).create();

	/**
	 * Date formatting for plugin messages.
	 */
	public static final SimpleDateFormat displayDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	/**
	 * Gets an interactive message for a list entry with different buttons and actions depending on the type of entry.
	 *
	 * @return A TextComponent containing the information and functionality of this entry.
	 */
	public abstract Component getInteractiveMessage();

	/**
	 * Gets a player's username from the uuid cache
	 * @param uuid ID of the player.
	 * @return The name of the player.
	 */
	public static String getUsername(Connection connection, UUID uuid) {
		try (PreparedStatement statement = connection.prepareStatement("select * from player_history where uuid = ? limit 1")) {
			statement.setString(1, uuid.toString());

			ResultSet resultSet = statement.executeQuery();

			if (resultSet.next())
				return resultSet.getString("name");
		} catch (SQLException e) {
			SuspiciousPlayers.plugin().logger().error("An exception occurred when getting username for " + uuid, e);
		}

		return uuid.toString();
	}

	/**
	 * Gets a TextComponent containing the name and a popup with name history of a player. MAKE SURE THIS IS ALWAYS CALLED ASYNCHRONOUSLY!
	 * @param uuid ID of the player.
	 * @param color Color of the player name in the text component.
	 * @return The finished TextComponent.
	 */
	public static Component getNameComponent(UUID uuid, NamedTextColor color) {

		// Attempt to get the username history
		try (Connection connection = SuspiciousPlayers.plugin().getConnection(); PreparedStatement statement = connection.prepareStatement("select * from player_history where uuid = ?")) {
			statement.setString(1, uuid.toString());
			ResultSet resultSet = statement.executeQuery();

			ConcurrentSkipListMap<String, String> usernameHistory;
			boolean hasHistory = resultSet.next();
			if (hasHistory && resultSet.getString("name_history") != null) {
				// The player exists in the player_history table and has name history
				usernameHistory = gson.fromJson(resultSet.getString("name_history"), type);
			} else {
				String history = getUsernameHistory(uuid);

				usernameHistory = gson.fromJson(history, type);
				if (hasHistory) {
					try (PreparedStatement statement1 = connection.prepareStatement("update player_history set name_history = ? where uuid = ?")) {
						statement1.setString(1, history);
						statement1.setString(2, uuid.toString());

						statement1.execute();
					}
				} else if (usernameHistory != null) {
					// No previous history, insert into player history table
					try (PreparedStatement statement1 = connection.prepareStatement("insert into player_history (uuid, name, name_history) values (?, ?, ?)")) {
						statement1.setString(1, uuid.toString());
						statement1.setString(2, usernameHistory.firstEntry().getValue());
						statement1.setString(3, history);

						statement1.execute();
					}
				}
			}

			if (usernameHistory == null || usernameHistory.isEmpty()) {
				// Return TextComponent with error popup
				return Component.text(getUsername(connection, uuid), color)
						.hoverEvent(HoverEvent.showText(Component.text("Known aliases:\n", NamedTextColor.DARK_GRAY)
								.append(Component.text("ERROR: COULD NOT CONNECT TO MOJANG API", NamedTextColor.RED))));
			}

			// Builds the hover popup section of the message
			Component popup = Component.empty();
			for (Map.Entry<String, String> entry : usernameHistory.entrySet()) {
				popup = popup.append(Component.text("\n" + String.format("    %-32s", entry.getValue()), NamedTextColor.GRAY))
						.append(Component.text(String.format("%-20s", entry.getKey()), NamedTextColor.DARK_GRAY).decorate(TextDecoration.ITALIC));
			}

			popup = popup.append(Component.text("\n\nClick to open susplist for this player.", NamedTextColor.YELLOW));

			// Builds the final TextComponent and returns it
			return Component.text(usernameHistory.firstEntry().getValue(), color)
					.hoverEvent(HoverEvent.showText(Component.text("Known aliases:", NamedTextColor.DARK_GRAY).append(popup)))
					.clickEvent(ClickEvent.runCommand("/susplist " + usernameHistory.firstEntry().getValue()));
		} catch (Exception e) {
			// Print error and return TextComponent with error popup
			e.printStackTrace();

			return Component.text(uuid.toString(), color)
					.hoverEvent(HoverEvent.showText(Component.text("Known aliases:\n", NamedTextColor.DARK_GRAY)
							.append(Component.text("ERROR: COULD NOT CONNECT TO MOJANG API", NamedTextColor.RED))));
		}
	}

	/**
	 * Contacts the Mojang API and requests the name history of a player.
	 * @param uuid ID of the player to check.
	 * @return A string of timestamps and player names.
	 */
	public static String getUsernameHistory(UUID uuid) {
		String compactUUID = uuid.toString().replace("-", "");
		try {
			// Contacts the Mojang API and requests the player's name history
			URL url = new URL("https://api.mojang.com/user/profiles/" + compactUUID + "/names");
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()))) {
				return reader.lines().collect(Collectors.joining());
			}
		} catch (Exception e) {
			SuspiciousPlayers.plugin().logger().error("Error occurred while contacting MojangAPI", e);
			return null;
		}
	}
}

/**
 * Deserializer for the name history package from the Mojang API.
 * The input map is actually an array of unnamed objects with a name and (usually) timestamp, this gets turned into a Map<Long, String> below.
 */
class NameMapDeserializer implements JsonDeserializer<ConcurrentSkipListMap<String, String>> {
	@Override
	public ConcurrentSkipListMap<String, String> deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
		try {
			ConcurrentSkipListMap<String, String> map = new ConcurrentSkipListMap<>(Collections.reverseOrder());
			for (JsonElement jItem : json.getAsJsonArray()) {
				JsonObject jObject = jItem.getAsJsonObject();

				// Gets the player name, this is always included in each object
				String name = jObject.get("name").getAsString();

				// Timestamps are not included on the player's first name so it is set to "-" in that case
				String timestamp = "-";
				if (jObject.get("changedToAt") != null)
					timestamp = PlayerEntry.displayDateFormat.format(jObject.get("changedToAt").getAsLong());

				map.put(timestamp, name);
			}
			return map;
		} catch (Exception e) {
			SuspiciousPlayers.plugin().logger().error("Error occurred while parsing json package", e);
			return null;
		}
	}
}

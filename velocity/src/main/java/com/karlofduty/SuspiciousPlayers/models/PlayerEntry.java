package com.karlofduty.SuspiciousPlayers.models;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.history.UsernameHistory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;

public abstract class PlayerEntry {

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
			if (hasHistory) {
				String history = resultSet.getString("name_history");
				if (history != null) {
					// The player exists in the player_history table and has name history
					usernameHistory = UsernameHistory.deserialize(resultSet.getString("name_history"));
				} else {
					// We know the player's username, but they don't have any name history.
					usernameHistory = new ConcurrentSkipListMap<>();
					usernameHistory.put("-", resultSet.getString("name"));
				}
			} else {
				// We don't have any history for this player at all, just use their uuid instead.
				usernameHistory = new ConcurrentSkipListMap<>();
				usernameHistory.put("-", uuid.toString());
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
			SuspiciousPlayers.plugin().logger().warn("while formatting name component for " + uuid, e);

			return Component.text(uuid.toString(), color)
					.hoverEvent(HoverEvent.showText(Component.text("Known aliases:\n", NamedTextColor.DARK_GRAY)
							.append(Component.text("ERROR: " + e.getMessage(), NamedTextColor.RED))));
		}
	}
}

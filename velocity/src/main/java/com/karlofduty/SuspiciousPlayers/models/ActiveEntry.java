package com.karlofduty.SuspiciousPlayers.models;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

public class ActiveEntry extends PlayerEntry {
	public int id;
	public Timestamp createdTime;
	public String creatorUUID;
	public String suspiciousUUID;
	public String entry;

	public static final String INSERT = "INSERT INTO active_entries(created_time, creator_uuid, suspicious_uuid, entry) VALUES (?,?,?,?)";
	private static final String SELECT = "SELECT * FROM active_entries WHERE id = ?;";
	public static final String SELECT_PLAYER = "SELECT * FROM active_entries WHERE suspicious_uuid = ? ORDER BY created_time LIMIT ?;";
	private static final String DELETE = "DELETE FROM active_entries WHERE id = ?";

	public ActiveEntry(ResultSet table) throws SQLException {
		this.id = table.getInt("id");
		this.createdTime = table.getTimestamp("created_time");
		this.creatorUUID = table.getString("creator_uuid");
		this.suspiciousUUID = table.getString("suspicious_uuid");
		this.entry = table.getString("entry");
	}

	/**
	 * Selects a single row from the active entries table in the database and returns an ActiveEntry object representing it
	 * @param c The Connection object used to contact the database
	 * @param id The id of the row to select
	 * @return An ActiveEntry object representing the row, null if not found or sql error
	 */
	public static ActiveEntry select(Connection c, int id) {
		try {
			PreparedStatement selectStatement = c.prepareStatement(ActiveEntry.SELECT);
			selectStatement.setInt(1, id);
			ResultSet resultSet = selectStatement.executeQuery();
			if(resultSet.next())
			{
				return new ActiveEntry(resultSet);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Selects all online players entries from the database
	 * @param c The Connection object used to contact the database
	 * @return A HashMap where the keys are players uuids and the values are a list of that player's entries
	 */
	public static Map<String, LinkedList<ActiveEntry>> selectOnline(Connection c, RegisteredServer server) {
		Map<String, LinkedList<ActiveEntry>> onlineEntries = new LinkedHashMap<>();

		try {
			String query = "SELECT * FROM active_entries WHERE suspicious_uuid IN (";
			for (Player player : server.getPlayersConnected())
				query += "'" + player.getUniqueId().toString() + "'" + ",";

			query = query.substring(0, query.length() - 1);
			query = query + ")";

			PreparedStatement selectStatement = c.prepareStatement(query);
			ResultSet resultSet = selectStatement.executeQuery();
			while (resultSet.next()) {
				// Each player gets their own list of entries as that is how they will be grouped in chat later on
				LinkedList<ActiveEntry> playerEntries = onlineEntries.getOrDefault(resultSet.getString("suspicious_uuid"), new LinkedList<>());
				playerEntries.add(new ActiveEntry(resultSet));
				onlineEntries.put(resultSet.getString("suspicious_uuid"), playerEntries);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return onlineEntries;
	}

	/**
	 * Archives a single active entry in the database
	 * @param c The connection obejct used to contact the database
	 * @param archiverUUID The uuid of the player archiving the entry
	 * @return The response message to send to the player
	 */
	public Component archive(Connection c, String archiverUUID) {
		try {
			PreparedStatement insertStatement = c.prepareStatement(ArchivedEntry.INSERT);
			insertStatement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
			insertStatement.setString(2, archiverUUID);
			insertStatement.setTimestamp(3, createdTime);
			insertStatement.setString(4, creatorUUID);
			insertStatement.setString(5, suspiciousUUID);
			insertStatement.setString(6, entry);
			insertStatement.executeUpdate();
			try {
				PreparedStatement statement = c.prepareStatement(DELETE);
				statement.setInt(1, id);
				statement.execute();
			} catch (SQLException e) {
				e.printStackTrace();
				return Component.text("Error occurred while deleting active entry from database: " + e.getMessage(), NamedTextColor.RED);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return Component.text("Error occurred while inserting entry into archived table: " + e.getMessage(), NamedTextColor.RED);
		}

		return Component.text("Entry archived.", NamedTextColor.RED);
	}

	@Override
	public Component getInteractiveMessage() {
		Component reporterComponent = Component.text(creatorUUID, NamedTextColor.YELLOW);

		try {
			// If this entry was added by console, catch the IllegalArgumentException
			reporterComponent = getNameComponent(UUID.fromString(creatorUUID), NamedTextColor.YELLOW);
		} catch (IllegalArgumentException ignored) {}

		return Component.empty()
				.append(Component.text("[", NamedTextColor.GREEN))
				.append(Component.text("-", NamedTextColor.GOLD)
						.clickEvent(ClickEvent.runCommand("/susparchive " + id))
						.hoverEvent(HoverEvent.showText(Component.text("Archive entry.", NamedTextColor.GOLD))))
				.append(Component.text("] [", NamedTextColor.GREEN))
				.append(Component.text(displayDateFormat.format(createdTime), NamedTextColor.YELLOW))
				.append(Component.text("] Reported by: ", NamedTextColor.GREEN))
				.append(reporterComponent)
				.append(Component.text("\n" + entry + "\n", NamedTextColor.GREEN));
	}
}

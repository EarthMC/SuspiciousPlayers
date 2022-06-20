package com.karlofduty.SuspiciousPlayers.models;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;


public class ArchivedEntry extends PlayerEntry {
	public int id;
	public Timestamp archivedTime;
	public String archiverUUID;
	public Timestamp createdTime;
	public String creatorUUID;
	public String suspiciousUUID;
	public String entry;


	public static final String INSERT = "INSERT INTO archived_entries(archived_time, archiver_uuid, created_time, creator_uuid, suspicious_uuid, entry) VALUES (?,?,?,?,?,?)";
	public static final String SELECT_PLAYER = "SELECT * FROM archived_entries WHERE suspicious_uuid = ? ORDER BY created_time LIMIT ?;";
	private static final String SELECT = "SELECT * FROM archived_entries WHERE id = ?;";
	private static final String DELETE = "DELETE FROM archived_entries WHERE id = ?";

	public ArchivedEntry(ResultSet table) throws SQLException {
		this.id = table.getInt("id");
		this.archivedTime = table.getTimestamp("archived_time");
		this.archiverUUID = table.getString("archiver_uuid");
		this.createdTime = table.getTimestamp("created_time");
		this.creatorUUID = table.getString("creator_uuid");
		this.suspiciousUUID = table.getString("suspicious_uuid");
		this.entry = table.getString("entry");
	}

	/**
	 * Selects a single row from the archived entries table in the database and returns an ArchivedEntry object representing it
	 * @param c The Connection object used to contact the database
	 * @param id The id of the row to select
	 * @return An ArchivedEntry object representing the row, null if not found or sql error
	 */
	public static ArchivedEntry select(Connection c, int id) {
		try {
			PreparedStatement selectStatement = c.prepareStatement(ArchivedEntry.SELECT);
			selectStatement.setInt(1, id);
			ResultSet resultSet = selectStatement.executeQuery();
			if (resultSet.next())
				return new ArchivedEntry(resultSet);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}

	public Component delete(Connection c, String deleterUUID) {
		try {
			PreparedStatement insertStatement = c.prepareStatement(DeletedEntry.INSERT);
			insertStatement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
			insertStatement.setString(2, deleterUUID);
			insertStatement.setTimestamp(3, archivedTime);
			insertStatement.setString(4, archiverUUID);
			insertStatement.setTimestamp(5, createdTime);
			insertStatement.setString(6, creatorUUID);
			insertStatement.setString(7, suspiciousUUID);
			insertStatement.setString(8, entry);
			insertStatement.executeUpdate();

			try {
				PreparedStatement statement = c.prepareStatement(DELETE);
				statement.setInt(1, id);
				statement.execute();
			} catch (SQLException e) {
				e.printStackTrace();
				return Component.text("Error occurred while deleting archived entry from database: " + e.getMessage(), NamedTextColor.RED);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return Component.text("Error occurred while inserting entry into deleted table: " + e.getMessage(), NamedTextColor.RED);
		}

		return Component.text("Entry deleted.", NamedTextColor.GREEN);
	}

	public Component unarchive(Connection c) {
		try {
			PreparedStatement insertStatement = c.prepareStatement(ActiveEntry.INSERT);
			insertStatement.setTimestamp(1, createdTime);
			insertStatement.setString(2, creatorUUID);
			insertStatement.setString(3, suspiciousUUID);
			insertStatement.setString(4, entry);
			insertStatement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			return Component.text("Error occurred while inserting entry into active table: " + e.getMessage(), NamedTextColor.RED);
		}

		try {
			PreparedStatement statement = c.prepareStatement(DELETE);
			statement.setInt(1, id);
			statement.execute();
		} catch (SQLException e) {
			e.printStackTrace();
			return Component.text("Error occurred while deleting archived entry from database: " + e.getMessage(), NamedTextColor.RED);
		}

		return Component.text("Entry unarchived.", NamedTextColor.GREEN);
	}

	@Override
	public Component getInteractiveMessage() {
		Component reporterComponent = Component.text(creatorUUID, NamedTextColor.GRAY);
		Component archiverComponent = Component.text(archiverUUID, NamedTextColor.GRAY);

		try {
			// If this entry was added by console, catch the IllegalArgumentException
			reporterComponent = getNameComponent(UUID.fromString(creatorUUID), NamedTextColor.GRAY);
		} catch (IllegalArgumentException ignored) {}

		try {
			archiverComponent = getNameComponent(UUID.fromString(archiverUUID), NamedTextColor.GRAY);
		} catch (IllegalArgumentException ignored) {}

		return Component.empty()
				.append(Component.text("[", NamedTextColor.DARK_GRAY))
				.append(Component.text("+", NamedTextColor.GREEN)
						.clickEvent(ClickEvent.runCommand("/suspunarchive " + id))
						.hoverEvent(HoverEvent.showText(Component.text("Unarchive this entry.", NamedTextColor.GREEN))))
				.append(Component.text("] [", NamedTextColor.DARK_GRAY))
				.append(Component.text(displayDateFormat.format(createdTime), NamedTextColor.GRAY))
				.append(Component.text("] Reported by: ", NamedTextColor.DARK_GRAY))
				.append(reporterComponent)
				.append(Component.text("\n[", NamedTextColor.DARK_GRAY))
				.append(Component.text("x", NamedTextColor.RED)
						.clickEvent(ClickEvent.runCommand("/suspdelete " + id))
						.hoverEvent(HoverEvent.showText(Component.text("Delete this entry.", NamedTextColor.RED))))
				.append(Component.text("] [", NamedTextColor.DARK_GRAY))
				.append(Component.text(displayDateFormat.format(archivedTime), NamedTextColor.GRAY))
				.append(Component.text("] Archived by: ", NamedTextColor.DARK_GRAY))
				.append(archiverComponent)
				.append(Component.text("\n" + entry + "\n", NamedTextColor.DARK_GRAY));
	}
}

package com.karlofduty.SuspiciousPlayers.models;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.sql.*;

import static net.md_5.bungee.api.ChatColor.*;

public class ArchivedEntry extends PlayerEntry
{
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

	public ArchivedEntry(ResultSet table) throws SQLException
	{
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
	public static ArchivedEntry select(Connection c, int id)
	{
		try
		{
			PreparedStatement selectStatement = c.prepareStatement(ArchivedEntry.SELECT);
			selectStatement.setInt(1, id);
			ResultSet resultSet = selectStatement.executeQuery();
			if(resultSet.next())
			{
				return new ArchivedEntry(resultSet);
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public String delete(Connection c, String deleterUUID)
	{
		try
		{
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

			try
			{
				PreparedStatement statement = c.prepareStatement(DELETE);
				statement.setInt(1,id);
				statement.execute();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
				return RED + "Error occurred while deleting archived entry from database: " + e.getMessage();
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return RED + "Error occurred while inserting entry into deleted table: " + e.getMessage();
		}
		return GREEN + "Entry deleted.";
	}

	public String unarchive(Connection c)
	{
		try
		{
			PreparedStatement insertStatement = c.prepareStatement(ActiveEntry.INSERT);
			insertStatement.setTimestamp(1, createdTime);
			insertStatement.setString(2, creatorUUID);
			insertStatement.setString(3, suspiciousUUID);
			insertStatement.setString(4, entry);
			insertStatement.executeUpdate();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return RED + "Error occurred while inserting entry into active table: " + e.getMessage();
		}

		try
		{
			PreparedStatement statement = c.prepareStatement(DELETE);
			statement.setInt(1,id);
			statement.execute();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return RED + "Error occurred while deleting archived entry from database: " + e.getMessage();
		}
		return GREEN + "Entry unarchived.";
	}

	@Override
	public TextComponent getInteractiveMessage()
	{
		return new TextComponent(
				new ComponentBuilder("[")
						.color(DARK_GRAY)
					.append("+")
						.color(GREEN)
						.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/suspunarchive " + id))
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(GREEN + "Unarchive this entry.")))
					.append("")
						.reset()
					.append(TextComponent.fromLegacyText(DARK_GRAY + "] [" + GRAY + displayDateFormat.format(createdTime) + DARK_GRAY + "] Reported by: " + GRAY + getUsername(creatorUUID) + "\n" + DARK_GRAY + "["))
					.append("x")
						.color(RED)
						.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/suspdelete " + id))
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(RED + "Delete this entry.")))
					.append("")
						.reset()
					.append(TextComponent.fromLegacyText(DARK_GRAY + "] [" + GRAY + displayDateFormat.format(archivedTime) + DARK_GRAY + "] Archived by: " + GRAY + getUsername(archiverUUID) + "\n" + entry + "\n"))
					.create()
		);
	}
}

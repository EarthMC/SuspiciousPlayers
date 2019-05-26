package com.karlofduty.SuspiciousPlayers.models;

import java.sql.*;

import static org.bukkit.ChatColor.*;

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
	public static final String SELECT = "SELECT * FROM archived_entries WHERE suspicious_uuid = ? ORDER BY created_time LIMIT ?;";
	public static final String DELETE = "DELETE FROM archived_entries WHERE id = ?";

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
	public String getFormattedString()
	{
		return String.format("%s Reported by: %s\n%s Archived by: %s\n%s\n \n",
				DARK_GRAY + "[" + GRAY + displayDateFormat.format(createdTime) + DARK_GRAY + "]" + GRAY,
				DARK_GRAY + getUsername(creatorUUID),
				DARK_GRAY + "[" + GRAY + displayDateFormat.format(archivedTime) + DARK_GRAY + "]" + GRAY,
				DARK_GRAY + getUsername(archiverUUID),
				GRAY + entry);
	}
}

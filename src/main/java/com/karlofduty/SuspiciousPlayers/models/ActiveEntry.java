package com.karlofduty.SuspiciousPlayers.models;

import java.sql.*;

import static org.bukkit.ChatColor.*;

public class ActiveEntry extends PlayerEntry
{
	public int id;
	public Timestamp createdTime;
	public String creatorUUID;
	public String suspiciousUUID;
	public String entry;

	public static final String INSERT = "INSERT INTO active_entries(created_time, creator_uuid, suspicious_uuid, entry) VALUES (?,?,?,?)";
	public static final String SELECT = "SELECT * FROM active_entries WHERE suspicious_uuid = ? ORDER BY created_time LIMIT ?;";
	private static final String DELETE = "DELETE FROM active_entries WHERE id = ?";

	public ActiveEntry(ResultSet table) throws SQLException
	{
		this.id = table.getInt("id");
		this.createdTime = table.getTimestamp("created_time");
		this.creatorUUID = table.getString("creator_uuid");
		this.suspiciousUUID = table.getString("suspicious_uuid");
		this.entry = table.getString("entry");
	}

	public String archive(Connection c, String archiverUUID)
	{
		try
		{
			PreparedStatement insertStatement = c.prepareStatement(ArchivedEntry.INSERT);
			insertStatement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
			insertStatement.setString(2, archiverUUID);
			insertStatement.setTimestamp(3, createdTime);
			insertStatement.setString(4, creatorUUID);
			insertStatement.setString(5, suspiciousUUID);
			insertStatement.setString(6, entry);
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
				return RED + "Error occurred while deleting active entry from database: " + e.getMessage();
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return RED + "Error occurred while inserting entry into archived table: " + e.getMessage();
		}
		return GREEN + "Entry archived.";
	}

	@Override
	public String getFormattedString()
	{
		return String.format("%s Reported by: %s\n%s\n \n",
				GREEN + "[" + YELLOW + displayDateFormat.format(createdTime) + GREEN + "]",
				YELLOW + getUsername(creatorUUID),
				entry);
	}
}

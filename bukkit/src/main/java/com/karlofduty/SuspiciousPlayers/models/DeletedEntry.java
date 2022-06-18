package com.karlofduty.SuspiciousPlayers.models;
import net.md_5.bungee.api.chat.TextComponent;

import java.sql.*;

public class DeletedEntry extends PlayerEntry
{
	public int id;
	public Timestamp deletedTime;
	public String deleterUUID;
	public Timestamp archivedTime;
	public String archiverUUID;
	public Timestamp createdTime;
	public String creatorUUID;
	public String suspiciousUUID;
	public String entry;


	public static final String INSERT = "INSERT INTO deleted_entries(deleted_time, deleter_uuid, archived_time, archiver_uuid, created_time, creator_uuid, suspicious_uuid, entry) VALUES (?,?,?,?,?,?,?,?)";
	public static final String SELECT = "SELECT * FROM deleted_entries WHERE id = ?;";
	public static final String SELECT_PLAYER = "SELECT * FROM deleted_entries WHERE suspicious_uuid = ? ORDER BY created_time LIMIT ?;";
	private static final String DELETE = "DELETE FROM deleted_entries WHERE id = ?";

	public DeletedEntry(ResultSet table) throws SQLException
	{
		this.id = table.getInt("id");
		this.deletedTime = table.getTimestamp("deleted_time");
		this.deleterUUID = table.getString("deleter_uuid");
		this.archivedTime = table.getTimestamp("archived_time");
		this.archiverUUID = table.getString("archiver_uuid");
		this.createdTime = table.getTimestamp("created_time");
		this.creatorUUID = table.getString("creator_uuid");
		this.suspiciousUUID = table.getString("suspicious_uuid");
		this.entry = table.getString("entry");
	}

	@Override
	public TextComponent getInteractiveMessage()
	{
		return null;
	}
}
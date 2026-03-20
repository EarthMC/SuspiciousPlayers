package com.karlofduty.SuspiciousPlayers.models;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import java.sql.*;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ActiveEntry extends PlayerEntry {
  public int id;
  public Timestamp createdTime;
  public String creatorUUID;
  public String suspiciousUUID;
  public String entry;

  public static final String INSERT =
      "INSERT INTO active_entries(created_time, creator_uuid, suspicious_uuid, entry) VALUES (?,?,?,?)";
  private static final String SELECT = "SELECT * FROM active_entries WHERE id = ?;";
  public static final String SELECT_PLAYER =
      "SELECT * FROM active_entries WHERE suspicious_uuid = ? ORDER BY created_time LIMIT ?;";
  private static final String DELETE = "DELETE FROM active_entries WHERE id = ?";
  private static final String EDIT =
      "UPDATE active_entries SET entry = ?, created_time = ? WHERE id = ? ";

  public ActiveEntry(ResultSet table) throws SQLException {
    this.id = table.getInt("id");
    this.createdTime = table.getTimestamp("created_time");
    this.creatorUUID = table.getString("creator_uuid");
    this.suspiciousUUID = table.getString("suspicious_uuid");
    this.entry = table.getString("entry");
  }

  /**
   * Selects a single row from the active entries table in the database and returns an ActiveEntry
   * object representing it
   *
   * @param c The Connection object used to contact the database
   * @param id The id of the row to select
   * @return An ActiveEntry object representing the row, null if not found or sql error
   */
  public static ActiveEntry select(Connection c, int id) {
    try (PreparedStatement selectStatement = c.prepareStatement(ActiveEntry.SELECT)) {
      selectStatement.setInt(1, id);
      ResultSet resultSet = selectStatement.executeQuery();

      if (resultSet.next()) return new ActiveEntry(resultSet);
    } catch (SQLException e) {
      SuspiciousPlayers.plugin().logger().warn("SQLException while selecting entries:", e);
    }

    return null;
  }

  /**
   * Selects all online players entries from the database
   *
   * @param c The Connection object used to contact the database
   * @return A HashMap where the keys are players uuids and the values are a list of that player's
   *     entries
   */
  public static Map<String, LinkedList<ActiveEntry>> selectOnline(Connection c) {
    Map<String, LinkedList<ActiveEntry>> onlineEntries = new LinkedHashMap<>();

    try {
      String query = "SELECT * FROM active_entries WHERE suspicious_uuid IN (";
      for (Player player : Bukkit.getOnlinePlayers())
        query += "'" + player.getUniqueId().toString() + "'" + ",";

      query = query.substring(0, query.length() - 1);
      query = query + ")";

      try (PreparedStatement selectStatement = c.prepareStatement(query)) {
        ResultSet resultSet = selectStatement.executeQuery();
        while (resultSet.next()) {
          // Each player gets their own list of entries as that is how they will be grouped in chat
          // later on
          LinkedList<ActiveEntry> playerEntries =
              onlineEntries.getOrDefault(
                  resultSet.getString("suspicious_uuid"), new LinkedList<>());
          playerEntries.add(new ActiveEntry(resultSet));
          onlineEntries.put(resultSet.getString("suspicious_uuid"), playerEntries);
        }
      }
    } catch (SQLException e) {
      SuspiciousPlayers.plugin().logger().warn("SQLException while selecting online entries:", e);
    }

    return onlineEntries;
  }

  /**
   * Archives a single active entry in the database
   *
   * @param c The connection object used to contact the database
   * @param archiverUUID The uuid of the player archiving the entry
   * @return The response message to send to the player
   */
  public Component archive(Connection c, String archiverUUID) {
    try (PreparedStatement insertStatement = c.prepareStatement(ArchivedEntry.INSERT)) {
      insertStatement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
      insertStatement.setString(2, archiverUUID);
      insertStatement.setTimestamp(3, createdTime);
      insertStatement.setString(4, creatorUUID);
      insertStatement.setString(5, suspiciousUUID);
      insertStatement.setString(6, entry);
      insertStatement.executeUpdate();

      try (PreparedStatement statement = c.prepareStatement(DELETE)) {
        statement.setInt(1, id);
        statement.execute();
      } catch (SQLException e) {
        SuspiciousPlayers.plugin().logger().warn("SQLException while archiving entry:", e);
        return Component.text(
            "Error occurred while deleting active entry from database: " + e.getMessage(),
            NamedTextColor.RED);
      }
    } catch (SQLException e) {
      SuspiciousPlayers.plugin().logger().warn("SQLException while archiving entry:", e);
      return Component.text(
          "Error occurred while inserting entry into archived table: " + e.getMessage(),
          NamedTextColor.RED);
    }

    return Component.text("Entry archived.", NamedTextColor.RED);
  }

  public Component edit(final Connection connection, String message) {
    try (PreparedStatement statement = connection.prepareStatement(ActiveEntry.EDIT)) {
      statement.setString(1, message);
      statement.setTimestamp(
          2, this.createdTime); // Prevent mysql from automatically updating the created_time to the
      // current time
      statement.setInt(3, this.id);

      statement.execute();
    } catch (SQLException e) {
      SuspiciousPlayers.plugin().logger().warn("SQLException while editing entry:", e);
      return Component.text(
          "Error occurred while editing message: " + e.getMessage(), NamedTextColor.RED);
    }

    return Component.text("Edited message.", NamedTextColor.GREEN);
  }

  @Override
  public Component getInteractiveMessage() {
    Component reporterComponent = Component.text(creatorUUID, NamedTextColor.YELLOW);

    try {
      reporterComponent = getNameComponent(UUID.fromString(creatorUUID), NamedTextColor.YELLOW);
    } catch (IllegalArgumentException ignored) {
    }

    return Component.empty()
        .append(Component.text("[", NamedTextColor.GREEN))
        .append(
            Component.text("-", NamedTextColor.GOLD)
                .clickEvent(
                    net.kyori.adventure.text.event.ClickEvent.runCommand("/susparchive " + id))
                .hoverEvent(
                    net.kyori.adventure.text.event.HoverEvent.showText(
                        Component.text("Archive entry.", NamedTextColor.GOLD))))
        .append(Component.text("] [", NamedTextColor.GREEN))
        .append(
            Component.text("✎")
                .clickEvent(
                    net.kyori.adventure.text.event.ClickEvent.suggestCommand(
                        "/suspedit " + id + " "))
                .hoverEvent(
                    HoverEvent.showText(Component.text("Edit entry.", NamedTextColor.GOLD))))
        .append(Component.text("] [", NamedTextColor.GREEN))
        .append(Component.text(displayDateFormat.format(createdTime), NamedTextColor.YELLOW))
        .append(Component.text("] Reported by: ", NamedTextColor.GREEN))
        .append(reporterComponent)
        .append(Component.text("\n" + entry + "\n", NamedTextColor.GREEN));
  }
}

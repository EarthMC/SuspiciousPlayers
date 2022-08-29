package com.karlofduty.SuspiciousPlayers.commands;

import com.google.common.collect.ImmutableList;
import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.models.ActiveEntry;
import com.karlofduty.SuspiciousPlayers.models.ArchivedEntry;
import com.karlofduty.SuspiciousPlayers.models.PlayerEntry;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ListCommand extends BaseCommand implements SimpleCommand {
    private final SuspiciousPlayers plugin;

    public ListCommand(SuspiciousPlayers plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (invocation.arguments().length < 1) {
            invocation.source().sendMessage(Component.text("Invalid arguments. Usage: /susplist [player]", NamedTextColor.RED));
            return;
        }

        if (!invocation.source().hasPermission("susp.list")) {
            invocation.source().sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }

        plugin.proxy().getScheduler().buildTask(plugin, () -> {
            try (Connection connection = plugin.getConnection()) {
                UUID suspiciousUUID = null;

                // Get the uuid of this player using the history table
                try (PreparedStatement statement = connection.prepareStatement("select * from player_history where upper(name) = ?")) {
                    statement.setString(1, invocation.arguments()[0].toUpperCase(Locale.ROOT));

                    ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next())
                        suspiciousUUID = UUID.fromString(resultSet.getString("uuid"));
                } catch (SQLException e) {
                    SuspiciousPlayers.plugin().logger().error("An exception occurred when getting player name", e);
                }

                if (suspiciousUUID == null) {
                    invocation.source().sendMessage(Component.text("Can not find a player by that name, make sure you are using their current username.", NamedTextColor.RED));
                    return;
                }

                int maxEntries = invocation.arguments().length >= 2 && SuspiciousPlayers.isInt(invocation.arguments()[1]) ? Integer.parseInt(invocation.arguments()[1]) : 10;
                List<PlayerEntry> entries = new ArrayList<>();

                // Reads all active entries about the player
                PreparedStatement statement = connection.prepareStatement(ActiveEntry.SELECT_PLAYER);
                statement.setString(1, suspiciousUUID.toString());
                statement.setInt(2, maxEntries);
                ResultSet activeResults = statement.executeQuery();

                while (activeResults.next())
                    entries.add(new ActiveEntry(activeResults));

                // Reads archived entries about the player if there are still spots open in the list
                if (entries.size() < maxEntries) {

                    statement = connection.prepareStatement(ArchivedEntry.SELECT_PLAYER);
                    statement.setString(1, suspiciousUUID.toString());
                    statement.setInt(2, maxEntries - entries.size());
                    ResultSet archiveResults = statement.executeQuery();

                    while (archiveResults.next())
                        entries.add(new ArchivedEntry(archiveResults));
                }

                // Send feedback message if there are no entries
                if (entries.isEmpty()) {
                    invocation.source().sendMessage(Component.text("User does not have any entries.", NamedTextColor.RED));
                    return;
                }

                Component message = Component.text("----- Displaying (Max: ", NamedTextColor.GOLD)
                        .append(Component.text(maxEntries, NamedTextColor.YELLOW))
                        .append(Component.text(") entries for ", NamedTextColor.GOLD))
                        .append(PlayerEntry.getNameComponent(suspiciousUUID, NamedTextColor.YELLOW))
                        .append(Component.text(" -----\n", NamedTextColor.GOLD));

                for (PlayerEntry entry : entries)
                    message = message.append(entry.getInteractiveMessage());

                invocation.source().sendMessage(message);

            } catch (SQLException e) {
                invocation.source().sendMessage(Component.text("Error occurred while listing entries. " + e.getMessage(), NamedTextColor.RED));
                plugin.logger().error("Error occurred while listing entries", e);
            }
        }).schedule();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("susp.list");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        switch (invocation.arguments().length) {
            case 0 -> plugin.proxy().getAllPlayers().stream().map(Player::getUsername).toList();
            case 1 -> filterByStart(plugin.proxy().getAllPlayers().stream().map(Player::getUsername).toList(), invocation.arguments()[0]);
        }

        return ImmutableList.of();
    }
}

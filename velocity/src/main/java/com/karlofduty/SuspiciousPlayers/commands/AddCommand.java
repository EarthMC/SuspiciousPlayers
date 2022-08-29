package com.karlofduty.SuspiciousPlayers.commands;

import com.google.common.collect.ImmutableList;
import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.models.ActiveEntry;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AddCommand extends BaseCommand implements SimpleCommand {
    private final SuspiciousPlayers plugin;

    public AddCommand(SuspiciousPlayers plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!invocation.source().hasPermission("susp.add")) {
            invocation.source().sendMessage(Component.text("You are not allowed to use that command.", NamedTextColor.RED));
            return;
        }

        if (invocation.arguments().length < 2) {
            invocation.source().sendMessage(Component.text("Invalid arguments, usage: /suspadd [player] [message]", NamedTextColor.RED));
            return;
        }

        final String creatorUUID = invocation.source() instanceof Player player ? player.getUniqueId().toString() : "Console";

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

                List<String> argumentList = new ArrayList<>(Arrays.asList(invocation.arguments()));
                argumentList.remove(0);
                String playerEntry = String.join(" ", argumentList);

                try (PreparedStatement statement = connection.prepareStatement(ActiveEntry.INSERT)) {
                    statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    statement.setString(2, creatorUUID);
                    statement.setString(3, suspiciousUUID.toString());
                    statement.setString(4, playerEntry);
                    statement.executeUpdate();

                    invocation.source().sendMessage(Component.text("Entry added.", NamedTextColor.GREEN));
                } catch (SQLException e) {
                    invocation.source().sendMessage(Component.text("Error occurred while adding entry to database. " + e.getMessage(), NamedTextColor.RED));
                    e.printStackTrace();
                }
            } catch (Exception e) {
                invocation.source().sendMessage(Component.text("Error occurred while preparing to add entry. " + e.getMessage(), NamedTextColor.RED));
                e.printStackTrace();
            }
        }).schedule();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("susp.add");
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

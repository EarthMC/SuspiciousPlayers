package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.TPHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TPNextCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission("susp.tp")) {
            sender.sendMessage(Component.text("You are not allowed to use that command.", NamedTextColor.RED));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("You cannot run this as console!", NamedTextColor.RED));
            return true;
        }

        Player tpTarget = TPHandler.next(player);
        if (tpTarget == null) {
            sender.sendMessage(Component.text("No one online to teleport to.", NamedTextColor.RED));
            return true;
        }

        player.teleport(tpTarget);
        sender.sendMessage(TPHandler.getTPStatus(tpTarget, false));
        return true;
    }
}

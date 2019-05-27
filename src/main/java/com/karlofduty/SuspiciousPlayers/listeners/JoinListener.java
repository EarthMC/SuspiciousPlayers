package com.karlofduty.SuspiciousPlayers.listeners;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;

import com.karlofduty.SuspiciousPlayers.models.ActiveEntry;
import static net.md_5.bungee.api.ChatColor.*;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JoinListener implements Listener
{
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        BukkitRunnable r = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                try(Connection c = SuspiciousPlayers.instance.getConnection())
                {
                    PreparedStatement statement = c.prepareStatement(ActiveEntry.SELECT_PLAYER);
                    statement.setString(1, event.getPlayer().getUniqueId().toString());
                    statement.setInt(2, 1000);
                    ResultSet results = statement.executeQuery();

                    int count = 0;
                    while(results.next())
                    {
                        count++;
                    }
                    if(count == 1)
                    {
                        results.first();
                        SuspiciousPlayers.instance.notify(new ComponentBuilder(event.getPlayer().getName() + " has been marked as suspicious:\n").color(RED).append(new ActiveEntry(results).getInteractiveMessage()).create());
                    }
                    else if(count > 1)
                    {
                        SuspiciousPlayers.instance.notify(new ComponentBuilder(event.getPlayer().getName() + " has been marked as suspicious " + count + " times!\nDo ")
                                    .color(RED)
                                .append("/susplist " + event.getPlayer().getName())
                                    .color(YELLOW)
                                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/susplist " + event.getPlayer().getName()))
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(YELLOW + "Show susplist.")))
                                .append(" to see why.")
                                    .color(RED)
                                .create());
                    }
                }
                catch (SQLException e)
                {
                    e.printStackTrace();
                }
            }
        };
        r.runTaskAsynchronously(SuspiciousPlayers.instance);
    }
}

package com.karlofduty.SuspiciousPlayers;

import com.karlofduty.SuspiciousPlayers.commands.AddCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

public class SuspiciousPlayers extends JavaPlugin
{
    public static SuspiciousPlayers instance;
    public Connection connection;

    private HashMap<String, String> currentList = new HashMap<>();
    private HashMap<String, String> archivedList = new HashMap<>();
    private String host = "localhost";
    private int port = 3306;
    private String database = "suspiciousplayers";
    private String username = "karl";
    private String password = "hello";

    @Override
    public void onEnable()
    {
        instance = this;
        BukkitRunnable r = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                try
                {
                    openConnection();
                    Statement statement = connection.createStatement();
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS active_entries(" +
                            "created TIMESTAMP NOT NULL UNIQUE," +
                            "creator_uuid VARCHAR(36) NOT NULL," +
                            "suspicious_uuid VARCHAR(36) NOT NULL," +
                            "entry VARCHAR(2000) NOT NULL)");

                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS archived_entries(" +
                            "archived TIMESTAMP NOT NULL UNIQUE," +
                            "archiver_uuid VARCHAR(36) NOT NULL," +
                            "created TIMESTAMP NOT NULL UNIQUE," +
                            "staff_uuid VARCHAR(36) NOT NULL," +
                            "suspicious_uuid VARCHAR(36) NOT NULL," +
                            "entry VARCHAR(2000) NOT NULL)");
                }
                catch (ClassNotFoundException e)
                {
                    e.printStackTrace();
                }
                catch (SQLException e)
                {
                    e.printStackTrace();
                }
            }
        };
        r.run();
        // TODO: Create command handler
        this.getCommand("suspadd").setExecutor(new AddCommand(this));
        log("Suspicious Players Loaded.");
    }

    public void openConnection() throws SQLException, ClassNotFoundException
    {
        if (connection != null && !connection.isClosed())
        {
            return;
        }

        synchronized (this)
        {
            if (connection != null && !connection.isClosed())
            {
                return;
            }
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + this.host+ ":" + this.port + "/" + this.database, this.username, this.password);
        }
    }

    public static boolean executeCommand(String command)
    {
        return instance.getServer().dispatchCommand(instance.getServer().getConsoleSender(), command);
    }

    public static void log(String message)
    {
        instance.getServer().getLogger().info(message);
    }
}
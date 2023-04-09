package com.karlofduty.SuspiciousPlayers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.karlofduty.SuspiciousPlayers.commands.*;
import com.karlofduty.SuspiciousPlayers.listeners.JoinListener;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Plugin(id = "suspiciousplayers", name = "SuspiciousPlayers", version = "1.3.4", authors = {"KarlOfDuty", "creatorfromhell", "Warriorrr"})
public class SuspiciousPlayers {
    private static SuspiciousPlayers plugin;
    public final Cache<UUID, Set<UUID>> seenNotifyCache = CacheBuilder.newBuilder().expireAfterAccess(15, TimeUnit.MINUTES).build();
    private Toml config;
    private HikariDataSource dataSource;
    private static final String CONFIG_FILE_NAME = "config.toml";
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataFolder;

    @Inject
    public SuspiciousPlayers(ProxyServer proxy, Logger logger, @DataDirectory Path dataFolder) {
        plugin = this;
        this.proxy = proxy;
        this.logger = logger;
        this.dataFolder = dataFolder;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        loadConfig();
        initializeDatasource();
        createTables();

        CommandManager commandManager = proxy.getCommandManager();

        commandManager.register("suspadd", new AddCommand(this));
        commandManager.register("susplist", new ListCommand(this));

        commandManager.register("susparchive", new ArchiveCommand(this));
        commandManager.register("suspunarchive", new UnarchiveCommand(this));
        commandManager.register("suspdelete", new DeleteCommand(this));
        commandManager.register("suspreload", new ReloadCommand(this));
        commandManager.register("susponline", new OnlineCommand(this));

        commandManager.register("tpnext", new TPNextCommand(this));
        commandManager.register("tpprev", new TPPrevCommand(this));
        commandManager.register("suspnext", new SuspNextCommand(this));
        commandManager.register("suspprev", new SuspPrevCommand(this));
        commandManager.register("suspedit", new EditCommand(this));

        proxy.getEventManager().register(this, new JoinListener(this));
        logger.info("Suspicious Players Loaded.");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (this.dataSource != null) {
            try {
                this.dataSource.close();
            } finally {
                this.dataSource = null;
            }
        }
    }


    public void notify(RegisteredServer server, UUID suspiciousUUID, Component message) {
        for (Player player : server.getPlayersConnected()) {
            if (!player.hasPermission("susp.notify"))
                continue;

            Set<UUID> seen = seenNotifyCache.getIfPresent(player.getUniqueId());
            if (seen == null) {
                seen = new HashSet<>();
                seenNotifyCache.put(player.getUniqueId(), seen);
            }

            if (seen.add(suspiciousUUID))
                player.sendMessage(message);
        }
    }

    public Component reload() {
        boolean error = false;

        if (dataSource != null) {
            try {
                dataSource.close();
            } finally {
                dataSource = null;
            }
        }

        if (!loadConfig())
            error = true;

        initializeDatasource();

        return error ? Component.text("Plugin reloaded with errors.", NamedTextColor.RED) : Component.text("Plugin reloaded successfully", NamedTextColor.GREEN);
    }

    public boolean loadConfig() {
        saveDefaultConfig();

        try {
            this.config = new Toml().read(dataFolder.resolve(CONFIG_FILE_NAME).toFile());
            return true;
        } catch (IllegalStateException e) {
            logger.error("An exception occurred when loading the config", e);
            return false;
        }
    }

    public void saveDefaultConfig() {
        if (Files.exists(dataFolder.resolve(CONFIG_FILE_NAME)))
            return;

        try {
            Files.createDirectory(dataFolder);
        } catch (IOException ignored) {}

        try (InputStream is = SuspiciousPlayers.class.getResourceAsStream("/" + CONFIG_FILE_NAME)) {
            if (is == null) {
                logger.warn("Could not find file " + CONFIG_FILE_NAME + " in the plugin jar.");
                return;
            }

            Files.copy(is, dataFolder.resolve(CONFIG_FILE_NAME));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Sets up the mysql datasource
     */
    private void initializeDatasource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setMinimumIdle(3);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setUsername(config.getString("database.user"));
        hikariConfig.setPassword(config.getString("database.password"));
        hikariConfig.setJdbcUrl("jdbc:mysql://" + config.getString("database.hostname") + ":" + config.getString("database.port") + "/" + config.getString("database.name"));

        dataSource = new HikariDataSource(hikariConfig);
    }

    /**
     * Creates all tables that do not already exist in the database
     */
    private void createTables() {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS active_entries(" +
                "id INT UNSIGNED NOT NULL UNIQUE PRIMARY KEY AUTO_INCREMENT," +
                "created_time TIMESTAMP NOT NULL," +
                "creator_uuid VARCHAR(36) NOT NULL," +
                "suspicious_uuid VARCHAR(36) NOT NULL," +
                "entry VARCHAR(2000) NOT NULL," +
                "INDEX(suspicious_uuid))");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS archived_entries(" +
                "id INT UNSIGNED NOT NULL UNIQUE PRIMARY KEY AUTO_INCREMENT," +
                "archived_time TIMESTAMP NOT NULL," +
                "archiver_uuid VARCHAR(36) NOT NULL," +
                "created_time TIMESTAMP NOT NULL," +
                "creator_uuid VARCHAR(36) NOT NULL," +
                "suspicious_uuid VARCHAR(36) NOT NULL," +
                "entry VARCHAR(2000) NOT NULL," +
                "INDEX(suspicious_uuid))");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS deleted_entries(" +
                "id INT UNSIGNED NOT NULL UNIQUE PRIMARY KEY AUTO_INCREMENT," +
                "deleted_time TIMESTAMP NOT NULL," +
                "deleter_uuid VARCHAR(36) NOT NULL," +
                "archived_time TIMESTAMP NOT NULL," +
                "archiver_uuid VARCHAR(36) NOT NULL," +
                "created_time TIMESTAMP NOT NULL," +
                "creator_uuid VARCHAR(36) NOT NULL," +
                "suspicious_uuid VARCHAR(36) NOT NULL," +
                "entry VARCHAR(2000) NOT NULL," +
                "INDEX(suspicious_uuid))");

            statement.executeUpdate("create table if not exists player_history(" +
                    "uuid varchar(36) not null," +
                    "name varchar(16) not null," +
                    "name_history mediumtext default null," +
                    "primary key (uuid))");
        } catch (SQLException e) {
            logger.error("An exception occurred when initializing tables: ", e);
        }
    }

    /**
     * Small utility function to check if the contents of a string is an int
     * @param s The string
     * @return True if yes, false if no.
     */
    public static boolean isInt(String s) {
        try {
            Integer.parseInt(s);
        } catch(NumberFormatException e) {
            return false;
        }

        return true;
    }

    public ProxyServer proxy() {
        return this.proxy;
    }

    public Logger logger() {
        return this.logger;
    }

    public static SuspiciousPlayers plugin() {
        return plugin;
    }
}
package net.localecho.kenneaal.SpongeGeoIP;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.entity.living.player.PlayerJoinEvent;
import org.spongepowered.api.event.state.PreInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.config.DefaultConfig;
import org.spongepowered.api.text.message.Messages;
import org.spongepowered.api.util.event.Subscribe;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;

@Plugin(id = spongeGeoIP.NAME, name = "SpongeGeoIP", version = "0.1")
public class spongeGeoIP {
    public static InputStream getDatabase() {
        return database;
    }

    public static Game getGame() {
        return game;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static DatabaseReader getReader() {
        return dbreader;
    }

    private Optional<Server> server;
    private Optional<PluginContainer> pluginContainer;
    public static final String NAME = "SpongeGeoIP";
    private ConfigurationNode config = null;
    private static Game game;
    private static Logger logger;
    private static InputStream database = null;
    private static DatabaseReader dbreader = null;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private File defaultConfig;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader configManager;

    public ConfigurationLoader getConfigManager() {
        return this.configManager;
    }

    public File getDefaultConfig() {
        return this.defaultConfig;
    }

    @Subscribe
    public void onPlayerJoin(PlayerJoinEvent event) {
        InetAddress address = null;
        address = event.getPlayer().getConnection().getAddress().getAddress();
        try {
            final CountryResponse country = getReader().country(address);
            this.server.get().broadcastMessage(Messages.of("Player " + event.getPlayer().getName() +
                    " is connecting from " + country.getCountry() + "."));
            getLogger().info(
                    "[SpongeGeoIP]: Player " + event.getPlayer().getName()
                            + " connected from " + country.getCountry() + ".");
        } catch (final GeoIp2Exception exception) {
            getLogger().error(
                    "[SpongeGeoIP]: Failed to get resolved address for "
                            + event.getPlayer().getName() + " (address "
                            + address.toString() + ")");
            return;
        } catch (final IOException exception) {
            getLogger().error(
                    "[SpongeGeoIP]: IO exception reading GeoIP database!");
            return;
        }
        return;

    }

    @Subscribe
    public void onPreInitialization(PreInitializationEvent event)
            throws UnknownHostException {
        game = event.getGame();
        this.server = event.getGame().getServer();
        this.pluginContainer = game.getPluginManager().getPlugin(
                spongeGeoIP.NAME);
        logger = game.getPluginManager().getLogger(this.pluginContainer.get());
        database = getClass().getResourceAsStream("/GeoLite2-Country.mmdb");
        try {
            dbreader = new DatabaseReader.Builder(database).build();
        } catch (final IOException exception) {
            getLogger().error("Couldn't read database!");
        }
        try {
            if (!getDefaultConfig().exists()) {
                getDefaultConfig().createNewFile();
                this.config = getConfigManager().load();
                config.getNode("announceConnect").setValue(true);
                config.getNode("informServerLocation").setValue(true);
                getConfigManager().save(config);
            }
            config = getConfigManager().load();
        } catch (final IOException exception) {
            getLogger()
                    .error("[SpongeGeoIP]: Couldn't create default configuration file!");
        }
    }
}

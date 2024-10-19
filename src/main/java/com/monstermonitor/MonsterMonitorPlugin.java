package com.monstermonitor;

import com.google.inject.Provides;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The main plugin class for Monster Monitor.
 * This plugin tracks NPC kills, allows setting kill limits, and provides an overlay and panel for monitoring.
 * It also manages event subscriptions and handles the lifecycle of the plugin.
 */
@PluginDescriptor(
        name = "Monster Monitor",
        description = "Tracks NPC kills and allows setting kill limits",
        tags = {"npc", "kill", "limit"}
)
public class MonsterMonitorPlugin extends Plugin
{
    @Inject
    MonsterMonitorConfig config;

    @Inject
    MonsterMonitorLogger logger;

    @Inject
    MonsterMonitorOverlay overlay;

    @Inject
    MonsterMonitorPanel panel;

    @Inject
    OverlayManager overlayManager;

    @Inject
    ClientToolbar clientToolbar;

    @Inject
    Client client;

    @Inject
    ClientThread clientThread;

    @Inject
    DeathTracker deathTracker; // Updated reference to DeathTracker

    private NavigationButton navButton;
    private boolean initialized = false;

    /**
     * Provides the configuration for the Monster Monitor plugin.
     *
     * @param configManager the configuration manager
     * @return the Monster Monitor plugin configuration
     */
    @Provides
    MonsterMonitorConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(MonsterMonitorConfig.class);
    }

    /**
     * Starts up the Monster Monitor plugin.
     * Initializes the logger and sets up the UI components such as the navigation button.
     *
     * @throws Exception if an error occurs during startup
     */
    @Override
    protected void startUp() throws Exception
    {
        clientThread.invoke(() -> {
            if (client.getLocalPlayer() != null)
            {
                logger.initialize(); // Initialize logger with player-specific directory
            }
        });

        // Set up the navigation button in the RuneLite sidebar
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/net/runelite/client/plugins/MonsterMonitor/icon.png");
        navButton = NavigationButton.builder()
                .tooltip("Monster Monitor")
                .icon(icon)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);

        updateOverlayVisibility(); // Apply the overlay visibility setting

        clientThread.invokeLater(() -> {
            Component parent = panel.getParent();
            while (parent != null && !(parent instanceof JScrollPane)) {
                parent = parent.getParent();
            }

            if (parent instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) parent;
                scrollPane.getVerticalScrollBar().setUI(new MonsterMonitorPanel.CustomScrollBarUI(new Color(200, 150, 0)));
                scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(2, 0));
            }
        });
    }

    /**
     * Shuts down the Monster Monitor plugin.
     * Cleans up the UI components and saves the log data.
     *
     * @throws Exception if an error occurs during shutdown
     */
    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        clientToolbar.removeNavigation(navButton);
        if (logger != null && initialized)
        {
            logger.saveLog();
        }
        initialized = false;
    }

    /**
     * Handles the game tick event to initialize the plugin components when the player is present.
     *
     * @param event the game tick event
     */
    @Subscribe
    public void onGameTick(GameTick event) {
        if (!initialized && client.getLocalPlayer() != null) {
            logger.initialize();
            logger.loadLog();
            updateOverlay();
            panel.updatePanel();
            initialized = true;
        }
    }

    /**
     * Logs the death of an NPC.
     * Updates the UI and checks if the kill limit has been reached.
     *
     * @param npcName the name of the NPC
     */
    public void logDeath(String npcName) {
        if (initialized)
        {
            logger.logDeath(npcName);
            updateUI();
            checkKillLimit(npcName);
        }
    }

    /**
     * Updates the plugin's UI components.
     * Ensures the updates are run on the main client thread.
     */
    public void updateUI()
    {
        if (initialized)
        {
            clientThread.invoke(() -> {
                updateOverlay();
                panel.updatePanel();
            });
        }
    }

    /**
     * Checks if the kill limit for an NPC has been reached and notifies the player if necessary.
     *
     * @param npcName the name of the NPC
     */
    private void checkKillLimit(String npcName)
    {
        if (!initialized)
        {
            return;
        }

        NpcData npcData = logger.getNpcLog().get(npcName);
        if (npcData == null)
        {
            return;
        }

        int killLimit = npcData.getKillLimit();
        int killCountForLimit = npcData.getKillCountForLimit();

        if (killLimit > 0 && killCountForLimit >= killLimit && npcData.isNotifyOnLimit())
        {
            if (config.notifyOnLimit())
            {
                if (config.enableSoundAlerts())
                {
                    Toolkit.getDefaultToolkit().beep();
                }
                if (config.showChatNotifications())
                {
                    String message = "<col=ff0000>" + config.customNotificationMessage().replace("{npc}", npcName) + "</col>";
                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
                }
            }
        }
    }

    /**
     * Retrieves the list of NPCs currently being tracked by the plugin.
     *
     * @return a list of NpcData objects representing tracked NPCs
     */
    public List<NpcData> getTrackedNpcs()
    {
        return logger.getNpcLog().values().stream().collect(Collectors.toList());
    }

    /**
     * Updates the data displayed on the overlay.
     */
    public void updateOverlay()
    {
        overlay.updateOverlayData(getTrackedNpcs());
    }

    /**
     * Updates the visibility of the overlay based on the plugin configuration.
     */
    private void updateOverlayVisibility()
    {
        if (config.showOverlay())
        {
            overlayManager.add(overlay);
        }
        else
        {
            overlayManager.remove(overlay);
        }
        updateOverlay();
    }

    /**
     * Handles configuration changes and updates the plugin settings immediately.
     *
     * @param event the config changed event
     */
    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals("monster monitor")) {
            return;
        }

        switch (event.getKey()) {
            case "showOverlay":
                updateOverlayVisibility();
                break;
            case "notifyOnLimit":
            case "showChatNotifications":
            case "customNotificationMessage":
            case "enableSoundAlerts":
                updateUI();
                break;
        }
    }
}

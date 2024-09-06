package com.monstermonitor;

import com.google.inject.Provides;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
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
    private MonsterMonitorConfig config;

    @Inject
    private MonsterMonitorLogger logger;

    @Inject
    private MonsterMonitorOverlay overlay;

    @Inject
    private MonsterMonitorPanel panel;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private NpcAnimationTracker npcAnimationTracker;

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

        // Apply the custom scrollbar on the client thread to ensure proper initialization
        clientThread.invokeLater(() -> {
            Component parent = panel.getParent(); // Get parent of the panel
            while (parent != null && !(parent instanceof JScrollPane)) {
                parent = parent.getParent(); // Traverse upwards to find the JScrollPane
            }

            if (parent instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) parent;
                scrollPane.getVerticalScrollBar().setUI(new MonsterMonitorPanel.CustomScrollBarUI(new Color(200, 150, 0)));
                scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(2, 0)); // Set scrollbar width
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
            logger.saveLog(); // Save log on shutdown
        }
        initialized = false; // Reset initialization flag
    }

    /**
     * Handles the game tick event to initialize the plugin components when the player is present.
     *
     * @param event the game tick event
     */
    @Subscribe
    public void onGameTick(GameTick event) {
        if (!initialized && client.getLocalPlayer() != null) {
            logger.initialize(); // Initialize the logger
            logger.loadLog(); // Load the log data

            updateOverlay(); // Update the overlay
            panel.updatePanel(); // Update the panel

            initialized = true; // Mark as initialized
        }
    }

    /**
     * Handles the animation changed event for NPCs.
     * Tracks the NPC animations for further processing by the NPC animation tracker.
     *
     * @param event the animation changed event
     */
    @Subscribe
    public void onAnimationChanged(AnimationChanged event)
    {
        if (event.getActor() instanceof NPC)
        {
            NPC npc = (NPC) event.getActor();
            npcAnimationTracker.trackNpc(npc);  // Track the NPC animation
        }
    }

    /**
     * Handles the NPC despawned event.
     * Tracks the NPC state when it despawns using the NPC animation tracker.
     *
     * @param event the NPC despawned event
     */
    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        NPC npc = event.getNpc();
        npcAnimationTracker.trackNpc(npc);  // Handle despawn by tracking the NPC state
    }

    /**
     * Logs the death animation of an NPC.
     * Updates the UI and checks if the kill limit has been reached.
     *
     * @param npcName the name of the NPC
     * @param animationId the ID of the death animation
     */
    public void logDeathAnimation(String npcName, int animationId) {
        if (initialized)
        {
            logger.logDeath(npcName, animationId);
            updateUI();
            checkKillLimit(npcName);
        }
    }

    /**
     * Logs an unknown death animation for an NPC.
     * Updates the UI and checks if the kill limit has been reached. Notifies player on First Unknown Death Animation
     * Instance.
     *
     * @param npcName the name of the NPC
     * @param animationId the ID of the unknown animation
     */
    public void logUnknownDeathAnimation(String npcName, int animationId) {
        if (initialized)
        {
            // Check if this NPC and animation have already been notified as unknown
            int lastLoggedAnimation = logger.getLastUnknownAnimations(npcName);

            // Log the unknown animation every time
            logger.logUnknownAnimations(npcName, animationId);

            // Only notify the player if it's the first time seeing this unknown animation for the NPC
            if (lastLoggedAnimation != animationId && config.notifyOnUnknownDeathAnimation())
            {
                if (config.enableSoundAlerts())
                {
                    Toolkit.getDefaultToolkit().beep(); // Sound alert
                }
                if (config.showChatNotifications())
                {
                    String message = "<col=ff0000>Unknown death animation logged for {npc}</col>".replace("{npc}", npcName);
                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
                }
            }

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
            // Run the UI update on the main client thread to ensure it happens immediately
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
            return; // No data available for this NPC, skip checking
        }

        int killLimit = npcData.getKillLimit();
        int killCountForLimit = npcData.getKillCountForLimit();

        if (killLimit > 0 && killCountForLimit >= killLimit && npcData.isNotifyOnLimit())
        {
            if (config.notifyOnLimit())
            {
                // Notify the player when kill limit is reached
                if (config.enableSoundAlerts())
                {
                    Toolkit.getDefaultToolkit().beep(); // Sound alert
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
     * Retrieves the logger instance used by the plugin.
     *
     * @return the MonsterMonitorLogger instance
     */
    public MonsterMonitorLogger getLogger()
    {
        return logger;
    }

    /**
     * Updates the data displayed on the overlay.
     * This method is called whenever tracked NPC data changes.
     */
    public void updateOverlay()
    {
        overlay.updateOverlayData(getTrackedNpcs());
    }

    /**
     * Updates the visibility of the overlay based on the plugin configuration.
     * Adds or removes the overlay from the overlay manager accordingly.
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

        // Force update overlay data
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
            case "notifyOnUnknownDeathAnimation":
            case "enableSoundAlerts":
                // Apply changes immediately
                updateUI();
                break;
        }
    }
}

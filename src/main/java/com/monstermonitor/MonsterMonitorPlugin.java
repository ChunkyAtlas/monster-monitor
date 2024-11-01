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
import net.runelite.client.eventbus.EventBus;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;

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
    private EventBus eventBus;

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
    DeathTracker deathTracker;

    @Inject
    MonsterMonitorMenuHandler menuHandler;

    private NavigationButton navButton;
    private boolean initialized = false;

    @Provides
    MonsterMonitorConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(MonsterMonitorConfig.class);
    }

    /**
     * Initializes the plugin, setting up UI components and registering events.
     */
    @Override
    protected void startUp() throws Exception
    {
        // Ensure the logger is initialized once the player is loaded.
        clientThread.invoke(() -> {
            if (client.getLocalPlayer() != null)
            {
                logger.initialize();
            }
        });

        // Load the plugin icon and create a navigation button.
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/net/runelite/client/plugins/MonsterMonitor/icon.png");
        navButton = NavigationButton.builder()
                .tooltip("Monster Monitor")
                .icon(icon)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);
        updateOverlayVisibility();

        // Register DeathTracker and menu handler for event handling.
        eventBus.register(deathTracker);
        eventBus.register(menuHandler);
    }

    /**
     * Handles plugin shutdown, including saving logs and removing UI elements.
     */
    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        clientToolbar.removeNavigation(navButton);

        // Unregister DeathTracker and menu handler to stop event handling.
        eventBus.unregister(deathTracker);
        eventBus.unregister(menuHandler);

        // Save the current log state if the logger was initialized.
        if (logger != null && initialized)
        {
            logger.saveLog();
        }
        initialized = false;
    }

    /**
     * Handles the game tick event to initialize components if needed.
     */
    @Subscribe
    public void onGameTick(GameTick event)
    {
        // Initialize logger and UI elements if not yet initialized.
        if (!initialized && client.getLocalPlayer() != null)
        {
            logger.initialize();
            logger.loadLog();
            updateOverlay();
            updateOverlayVisibility();
            panel.updatePanel();
            initialized = true;
        }
    }

    /**
     * Logs a death for a specific NPC and updates the UI accordingly.
     *
     * @param npcName The name of the NPC to log the death for.
     */
    public void logDeath(String npcName)
    {
        if (initialized)
        {
            logger.logDeath(npcName);
            updateUI();
            checkKillLimit(npcName);
        }
    }

    /**
     * Updates the UI elements, including the overlay and panel.
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
     * Checks if a kill limit has been reached for a specific NPC and sends notifications.
     *
     * @param npcName The name of the NPC to check the kill limit for.
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

        // If the kill limit is reached, notify the player.
        if (killLimit > 0 && killCountForLimit >= killLimit && npcData.isNotifyOnLimit())
        {
            if (config.notifyOnLimit())
            {
                // Play a sound if enabled.
                if (config.enableSoundAlerts())
                {
                    Toolkit.getDefaultToolkit().beep();
                }
                // Display a chat message if enabled.
                if (config.showChatNotifications())
                {
                    String message = "<col=ff0000>" + config.customNotificationMessage().replace("{npc}", npcName) + "</col>";
                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
                }
            }
        }
    }

    /**
     * Retrieves the list of tracked NPCs.
     *
     * @return A list of tracked NPC data.
     */
    public List<NpcData> getTrackedNpcs()
    {
        return logger.getNpcLog().values().stream().collect(Collectors.toList());
    }

    /**
     * Updates the overlay with the current list of tracked NPCs.
     * Only displays NPCs that have a set kill limit.
     */
    public void updateOverlay() {
        List<NpcData> filteredNpcs = getTrackedNpcs().stream()
                .filter(npcData -> npcData.isLimitSet() && !npcData.isIgnored())
                .collect(Collectors.toList());

        overlay.updateOverlayData(filteredNpcs);
    }

    /**
     * Updates the visibility of the overlay based on user settings and tracked NPCs.
     */
    public void updateOverlayVisibility() {
        // Check if there are any tracked NPCs with a set kill limit.
        boolean hasKillLimitSet = logger.getNpcLog().values().stream()
                .anyMatch(npcData -> npcData.isLimitSet() && !npcData.isIgnored());

        // Only show the overlay if enabled in the config and at least one kill limit is set.
        if (hasKillLimitSet && config.showOverlay()) {
            overlayManager.add(overlay);
        } else {
            overlayManager.remove(overlay);
        }

        updateOverlay(); // Refresh the overlay with the current data
    }

    /**
     * Sets the specified NPC to be monitored with a given kill limit.
     * If the limit is zero, it is considered as having no limit.
     *
     * @param npcName The name of the NPC to monitor.
     * @param killLimit The kill limit to set.
     */
    public void setNpcToMonitor(String npcName, int killLimit) {
        NpcData npcData = logger.getNpcLog().computeIfAbsent(npcName, NpcData::new);

        npcData.setKillLimit(killLimit);
        npcData.setLimitSet(true); // Ensure the limit is set
        npcData.setIgnored(false); // Ensure the NPC is not ignored
        npcData.resetKillCountForLimit(); // Reset progress towards the limit if changed

        // Update the logger with the new NPC data and refresh the UI.
        logger.updateNpcData(npcData);
        panel.updatePanel(); // Update the panel to reflect the checkbox state

        // Force the UI elements to reflect the updated state
        SwingUtilities.invokeLater(() -> panel.updatePanel());
        updateOverlayVisibility();
    }

    /**
     * Sets the specified NPC to be ignored, preventing it from being tracked.
     *
     * @param npcName The name of the NPC to ignore.
     */
    public void setNpcToIgnore(String npcName) {
        NpcData npcData = logger.getNpcLog().computeIfAbsent(npcName, NpcData::new);

        npcData.setIgnored(true);
        npcData.setLimitSet(false);
        npcData.setKillLimit(0);
        npcData.resetKillCountForLimit();

        // Update the logger with the new NPC data and refresh the UI.
        logger.updateNpcData(npcData);
        updateUI();
        updateOverlayVisibility();
    }

    /**
     * Handles changes in the plugin's configuration settings.
     *
     * @param event The configuration change event.
     */
    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        // Ensure the config change is related to the "monster monitor" plugin.
        if (!event.getGroup().equals("monster monitor")) {
            return;
        }

        // Respond to specific configuration changes.
        switch (event.getKey()) {
            case "showOverlay":
            case "showRightClickMenuEntries":
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

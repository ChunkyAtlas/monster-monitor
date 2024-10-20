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

    private NavigationButton navButton;
    private boolean initialized = false;

    @Provides
    MonsterMonitorConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(MonsterMonitorConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        clientThread.invoke(() -> {
            if (client.getLocalPlayer() != null)
            {
                logger.initialize();
            }
        });

        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/net/runelite/client/plugins/MonsterMonitor/icon.png");
        navButton = NavigationButton.builder()
                .tooltip("Monster Monitor")
                .icon(icon)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);
        updateOverlayVisibility();

        eventBus.register(deathTracker);

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

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        clientToolbar.removeNavigation(navButton);

        eventBus.unregister(deathTracker);

        if (logger != null && initialized)
        {
            logger.saveLog();
        }
        initialized = false;
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (!initialized && client.getLocalPlayer() != null)
        {
            logger.initialize();
            logger.loadLog();
            updateOverlay();
            panel.updatePanel();
            initialized = true;
        }
    }

    public void logDeath(String npcName)
    {
        if (initialized)
        {
            logger.logDeath(npcName);
            updateUI();
            checkKillLimit(npcName);
        }
    }

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

    public List<NpcData> getTrackedNpcs()
    {
        return logger.getNpcLog().values().stream().collect(Collectors.toList());
    }

    public void updateOverlay()
    {
        overlay.updateOverlayData(getTrackedNpcs());
    }

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

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
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

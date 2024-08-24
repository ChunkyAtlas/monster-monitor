package com.monstermonitor;

import com.google.inject.Provides;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private NavigationButton navButton;
    private boolean initialized = false;
    private String lastKilledNpcName; // Track the last killed NPC's name

    // Map to track recent interactions with NPCs
    private final Map<Integer, Boolean> npcInteractingMap = new HashMap<>();

    @Provides
    MonsterMonitorConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(MonsterMonitorConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        updateOverlayVisibility(); // Apply the overlay visibility setting

        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/net/runelite/client/plugins/MonsterMonitor/icon.png");
        navButton = NavigationButton.builder()
                .tooltip("Monster Monitor")
                .icon(icon)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);

        clientThread.invokeLater(() -> {
            logger.initialize(); // Initialize logger with player-specific directory
            logger.loadLog(); // Load log data
            updateOverlay(); // Ensure the overlay is updated immediately upon startup
            panel.updatePanel(); // Initial panel update
            initialized = true; // Mark as initialized
        });
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        clientToolbar.removeNavigation(navButton);
        logger.saveLog(); // Save log on shutdown
        initialized = false; // Reset initialization flag
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (!initialized && client.getLocalPlayer() != null)
        {
            logger.initialize(); // Initialize logger with player-specific directory
            logger.loadLog();
            updateOverlay(); // Ensure the overlay is updated on game tick
            panel.updatePanel(); // Initial panel update
            initialized = true; // Mark as initialized
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event)
    {
        if (event.getActor() instanceof NPC)
        {
            NPC npc = (NPC) event.getActor();
            int npcIndex = npc.getIndex();
            int animationId = npc.getAnimation();
            String npcName = npc.getName();

            // Skip logging if the animation ID is -1 (idle or no animation)
            if (animationId == -1)
            {
                return;
            }

            // Track if the player was recently interacting with this NPC
            boolean wasInteracting = npcInteractingMap.getOrDefault(npcIndex, false);

            clientThread.invokeLater(() -> {
                if (isPlayerKillingNpc(npc))
                {
                    npcInteractingMap.put(npcIndex, true);

                    if (DeathAnimationIDs.isDeathAnimation(animationId))
                    {
                        lastKilledNpcName = npcName; // Track the last killed NPC
                        logger.logDeath(npcName, animationId);
                        updateOverlay();
                        panel.updatePanel();
                        checkKillLimit(npcName);
                    }
                }
                else if (wasInteracting && DeathAnimationIDs.isDeathAnimation(animationId))
                {
                    // If the player was recently interacting and now it's playing any known death animation, log it
                    lastKilledNpcName = npcName; // Track the last killed NPC
                    logger.logDeath(npcName, animationId);
                    updateOverlay();
                    panel.updatePanel();
                    checkKillLimit(npcName);
                }
                else
                {
                    npcInteractingMap.put(npcIndex, false);
                }
            });
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        NPC npc = event.getNpc();
        int lastAnimationId = npc.getAnimation();

        // Skip logging for animation ID -1
        if (lastAnimationId == -1)
        {
            return;
        }

        // Ensure we only log the NPC if the player was involved
        if (isPlayerKillingNpc(npc))
        {
            String npcName = npc.getName();
            logger.logUnknownAnimation(npcName, lastAnimationId);
        }
    }

    private boolean isPlayerKillingNpc(NPC npc)
    {
        if (npc == null || client.getLocalPlayer() == null)
        {
            return false;
        }

        Player localPlayer = client.getLocalPlayer();

        // 1. Check if the NPC is directly interacting with the player
        if (npc.getInteracting() == localPlayer)
        {
            return true;
        }

        // 2. Check if the player is interacting with the NPC
        if (localPlayer.getInteracting() == npc)
        {
            return true;
        }

        return false;
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

        // Force update overlay data
        updateOverlay();
    }

    private void checkKillLimit(String npcName)
    {
        NpcData npcData = logger.getNpcLog().get(npcName);
        int killLimit = npcData.getKillLimit();
        int killCountForLimit = npcData.getKillCountForLimit();

        if (killLimit > 0 && killCountForLimit >= killLimit && npcData.isNotifyOnLimit())
        {
            Toolkit.getDefaultToolkit().beep();
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Kill limit reached for " + npcName, null);
        }
    }

    public List<NpcData> getTrackedNpcs()
    {
        return logger.getNpcLog().values().stream().collect(Collectors.toList());
    }

    public String getLastKilledNpcName()
    {
        return lastKilledNpcName;
    }

    public MonsterMonitorLogger getLogger()
    {
        return logger;
    }

    public void updateOverlay()
    {
        overlay.updateOverlayData(getTrackedNpcs());
    }
}

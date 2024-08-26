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
    private final Map<Integer, Integer> npcLastValidAnimationMap = new HashMap<>();

    @Provides
    MonsterMonitorConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(MonsterMonitorConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        clientThread.invoke(() -> {
            logger.initialize(); // Initialize logger with player-specific directory
            if (client.getLocalPlayer() != null)
            {
                logger.loadLog(); // Load log data only if the player is initialized
                updateOverlay(); // Ensure the overlay is updated immediately upon startup
                panel.updatePanel(); // Initial panel update
                initialized = true; // Mark as initialized
            }
        });

        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/net/runelite/client/plugins/MonsterMonitor/icon.png");
        navButton = NavigationButton.builder()
                .tooltip("Monster Monitor")
                .icon(icon)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);

        updateOverlayVisibility(); // Apply the overlay visibility setting
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

            // Track if the player was recently interacting with this NPC
            boolean wasInteracting = npcInteractingMap.getOrDefault(npcIndex, false);

            // Only update the last valid animation if it's not -1
            if (animationId != -1)
            {
                npcLastValidAnimationMap.put(npcIndex, animationId);
            }

            clientThread.invokeLater(() -> {
                if (isPlayerKillingNpc(npc))
                {
                    npcInteractingMap.put(npcIndex, true);

                    if (DeathAnimationIDs.isDeathAnimation(animationId))
                    {
                        lastKilledNpcName = npcName; // Track the last killed NPC
                        logger.logDeath(npcName, animationId); // Log the death
                        updateOverlay();
                        panel.updatePanel();
                    }
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
        int npcIndex = npc.getIndex();

        // Retrieve the last valid animation ID before despawn
        int lastAnimationId = npcLastValidAnimationMap.getOrDefault(npcIndex, -1);

        // Ensure we only log the NPC if the player was involved and there was a valid last animation
        if (lastAnimationId != -1 && isPlayerKillingNpc(npc))
        {
            String npcName = npc.getName();
            logger.logUnknownAnimations(npcName, lastAnimationId);

            // Update the overlay and panel to reflect the new NPC kill
            updateOverlay();
            panel.updatePanel();

            // Check kill limit only for unknown animations
            checkKillLimit(npcName);
        }

        // Clean up after despawn
        npcLastValidAnimationMap.remove(npcIndex);
        npcInteractingMap.remove(npcIndex);
    }

    private boolean isPlayerKillingNpc(NPC npc)
    {
        if (npc == null || client.getLocalPlayer() == null)
        {
            return false;
        }

        Player localPlayer = client.getLocalPlayer();

        // Check if the NPC is currently interacting with the player
        if (npc.getInteracting() == localPlayer)
        {
            return true;
        }

        // Check if the player is currently interacting with the NPC
        if (localPlayer.getInteracting() == npc)
        {
            return true;
        }

        // Only consider recent interactions if they were directly involving the player
        boolean wasInteractingWithPlayer = npcInteractingMap.getOrDefault(npc.getIndex(), false);
        if (wasInteractingWithPlayer)
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
        if (npcData == null)
        {
            return; // No data available for this NPC, skip checking
        }

        int killLimit = npcData.getKillLimit();
        int killCountForLimit = npcData.getKillCountForLimit();

        if (killLimit > 0 && killCountForLimit >= killLimit && npcData.isNotifyOnLimit())
        {
            // Notify the player
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

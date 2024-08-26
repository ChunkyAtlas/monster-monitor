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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
    private final Map<Integer, Long> npcLastInteractionTimeMap = new HashMap<>(); // Track the last interaction time
    private final Map<Integer, Boolean> npcAwaitingDeathAnimation = new HashMap<>(); // Tracks NPCs awaiting death animation
    private final Map<Integer, Integer> npcDeathAnimationAttempts = new HashMap<>(); // Tracks attempts to get death animation
    private final Map<Integer, Boolean> npcLoggedMap = new HashMap<>(); // Track if an NPC's death has already been logged

    private static final int INTERACTION_TIMEOUT_MS = 4000; // 4 seconds timeout
    private static final int MAX_DEATH_ANIMATION_ATTEMPTS = 5; // Max attempts to wait for death animation
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

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
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        clientToolbar.removeNavigation(navButton);
        logger.saveLog(); // Save log on shutdown
        initialized = false; // Reset initialization flag
        scheduler.shutdown();
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (!initialized && client.getLocalPlayer() != null)
        {
            logger.initialize(); // Initialize the logger
            logger.loadLog(); // Load the log data

            updateOverlay(); // Update the overlay
            panel.updatePanel(); // Update the panel

            initialized = true; // Mark as initialized
        }

        // Process each NPC in the game
        for (NPC npc : client.getNpcs())
        {
            int npcIndex = npc.getIndex();

            // Handle interaction timeout
            if (npcInteractingMap.getOrDefault(npcIndex, false))
            {
                long lastInteractionTime = npcLastInteractionTimeMap.getOrDefault(npcIndex, 0L);
                if (System.currentTimeMillis() - lastInteractionTime > INTERACTION_TIMEOUT_MS)
                {
                    npcInteractingMap.put(npcIndex, false);
                }
            }

            // Check for NPC death
            if (isPlayerKillingNpc(npc) && npc.getHealthRatio() == 0 && !npcAwaitingDeathAnimation.containsKey(npcIndex))
            {
                npcAwaitingDeathAnimation.put(npcIndex, true);
                npcDeathAnimationAttempts.put(npcIndex, 0);
            }

            // Increment attempts and handle timeout for death animation
            if (npcAwaitingDeathAnimation.containsKey(npcIndex))
            {
                int attempts = npcDeathAnimationAttempts.getOrDefault(npcIndex, 0);
                if (attempts >= MAX_DEATH_ANIMATION_ATTEMPTS)
                {
                    logDeathAnimation(npcIndex, npc.getName(), -1); // Log as unknown
                }
                else
                {
                    npcDeathAnimationAttempts.put(npcIndex, attempts + 1);
                }
            }
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

            // Track the last valid animation for each NPC
            if (animationId != -1)
            {
                npcLastValidAnimationMap.put(npcIndex, animationId);
            }

            // If the NPC is awaiting a death animation, log it
            if (npcAwaitingDeathAnimation.containsKey(npcIndex))
            {
                if (DeathAnimationIDs.isDeathAnimation(animationId))
                {
                    logDeathAnimation(npcIndex, npc.getName(), animationId);
                }
                else
                {
                    logDeathAnimation(npcIndex, npc.getName(), animationId);
                }
            }
            else if (isPlayerKillingNpc(npc))
            {
                npcInteractingMap.put(npcIndex, true);
                npcLastInteractionTimeMap.put(npcIndex, System.currentTimeMillis());
            }
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        NPC npc = event.getNpc();
        int npcIndex = npc.getIndex();

        // Cleanup NPC tracking data after it despawns
        cleanupNpcTracking(npcIndex);
    }

    private void logDeathAnimation(int npcIndex, String npcName, int animationId)
    {
        if (npcLoggedMap.getOrDefault(npcIndex, false))
        {
            return; // If already logged, skip logging again
        }

        if (animationId != -1)
        {
            if (DeathAnimationIDs.isDeathAnimation(animationId))
            {
                lastKilledNpcName = npcName;
                logger.logDeath(npcName, animationId); // Log known death animation
            }
            else
            {
                logger.logUnknownAnimations(npcName, animationId); // Log unknown animation
            }
        }
        else
        {
            logger.logUnknownAnimations(npcName, -1); // Log as unknown if no valid animation found
        }

        npcLoggedMap.put(npcIndex, true); // Mark the NPC as logged
        cleanupNpcTracking(npcIndex);

        checkKillLimit(npcName);

        clientThread.invoke(() -> {
            updateOverlay();
            panel.updatePanel();
        });
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

    private boolean isPlayerKillingNpc(NPC npc)
    {
        if (npc == null || client.getLocalPlayer() == null)
        {
            return false;
        }

        Player localPlayer = client.getLocalPlayer();

        // Direct interaction checks
        if (npc.getInteracting() == localPlayer || localPlayer.getInteracting() == npc)
        {
            return true;
        }

        // Recent interaction check
        boolean wasInteracting = npcInteractingMap.getOrDefault(npc.getIndex(), false);
        long lastInteractionTime = npcLastInteractionTimeMap.getOrDefault(npc.getIndex(), 0L);

        return wasInteracting && (System.currentTimeMillis() - lastInteractionTime <= INTERACTION_TIMEOUT_MS);
    }

    private void cleanupNpcTracking(int npcIndex)
    {
        npcLastValidAnimationMap.remove(npcIndex);
        npcInteractingMap.remove(npcIndex);
        npcLastInteractionTimeMap.remove(npcIndex);
        npcAwaitingDeathAnimation.remove(npcIndex);
        npcDeathAnimationAttempts.remove(npcIndex);
        npcLoggedMap.remove(npcIndex);
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
}

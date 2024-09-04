package com.monstermonitor;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Actor;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.Hitsplat;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Tracks and manages NPC animations and interactions for the Monster Monitor.
 * This class handles tracking NPC interactions, buffering animations, and detecting death animations
 * to log significant events related to NPCs during gameplay.
 */
@Getter
@Setter
public class NpcAnimationTracker
{
    private final Client client;
    private final MonsterMonitorPlugin plugin;
    private final MonsterMonitorLogger logger;
    private final MonsterMonitorPanel panel;
    private final ClientThread clientThread;
    private final Map<Integer, List<Integer>> animationBuffers = new HashMap<>(); // Buffer for unique animations
    private final Map<Integer, String> lastKnownNpcName = new HashMap<>(); // Stores last known NPC name
    private final Map<Integer, Integer> lastInteractionTicks = new HashMap<>(); // Tracks last interaction tick
    private final Map<Integer, Boolean> hasLoggedAnimation = new HashMap<>(); // Tracks if an animation was logged
    private final Map<Integer, Boolean> isInGracePeriod = new HashMap<>(); // Tracks if NPC has entered grace period
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); // Handles delayed tasks

    private static final int INTERACTION_TIMEOUT_TICKS = 7; // Timeout for interactions to be considered expired
    private static final int GRACE_PERIOD_TICKS = 2; // Number of ticks to wait after despawn for final animations

    /**
     * Constructor for NpcAnimationTracker.
     * Initializes the tracker with dependencies and registers event subscriptions.
     *
     * @param client       the client instance
     * @param plugin       the main plugin instance
     * @param logger       the logger instance for logging events
     * @param panel        the panel instance for UI updates
     * @param eventBus     the event bus for subscribing to game events
     * @param clientThread the client thread for executing tasks on the game thread
     */
    @Inject
    public NpcAnimationTracker(Client client, MonsterMonitorPlugin plugin, MonsterMonitorLogger logger, MonsterMonitorPanel panel, EventBus eventBus, ClientThread clientThread) {
        this.client = client;
        this.plugin = plugin;
        this.logger = logger;
        this.panel = panel;
        this.clientThread = clientThread;
        eventBus.register(this);  // Ensure the event bus is correctly set up
    }

    /**
     * Handles the event when the player starts interacting with an NPC.
     * Initializes tracking data for the NPC including animation buffers and interaction tick counters.
     *
     * @param event the interacting changed event
     */
    @Subscribe
    public void onInteractingChanged(InteractingChanged event) {
        Actor source = event.getSource();
        Actor target = event.getTarget();

        // Check if the player started interacting with an NPC
        if (source == client.getLocalPlayer() && target instanceof NPC) {
            NPC npc = (NPC) target;
            int npcIndex = npc.getIndex();

            // Initialize animation buffer for this NPC
            animationBuffers.put(npcIndex, new ArrayList<>());  // Use a list to capture unique animations in order
            lastInteractionTicks.put(npcIndex, client.getTickCount());

            // Initialize logging flag and grace period flag for this NPC
            hasLoggedAnimation.put(npcIndex, false);
            isInGracePeriod.put(npcIndex, false);

            // Store NPC name
            lastKnownNpcName.put(npcIndex, npc.getName() != null ? npc.getName() : "Unknown");
        }
    }

    /**
     * Tracks NPC animations by buffering unique animations for further processing.
     * Updates the interaction tick if the NPC is being tracked and the animation is valid.
     *
     * @param npc the NPC being tracked
     */
    public void trackNpc(NPC npc) {
        if (npc == null) {
            return;  // Ignore null NPCs
        }

        int npcIndex = npc.getIndex();
        int animationId = npc.getAnimation();

        // Buffer unique animations if interaction is valid
        if (animationId != -1 && isInteractionValid(npcIndex)) {
            bufferAnimation(npcIndex, animationId);
            lastInteractionTicks.put(npcIndex, client.getTickCount()); // Update the interaction tick
        }
    }

    /**
     * Handles the event when an NPC despawns.
     * Marks the NPC as in a grace period and prepares to log the final animations.
     *
     * @param event the NPC despawned event
     */
    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        NPC npc = event.getNpc();
        int npcIndex = npc.getIndex();

        if (!animationBuffers.containsKey(npcIndex)) {
            // Skip logging for NPCs not being tracked
            return;
        }

        // Mark NPC as in grace period
        isInGracePeriod.put(npcIndex, true);

        // Schedule the death processing after the grace period
        scheduler.schedule(() -> {
            clientThread.invokeLater(() -> processDeath(npcIndex));
        }, GRACE_PERIOD_TICKS * 600, TimeUnit.MILLISECONDS);
        // 600ms approximates the time for one game tick
    }

    /**
     * Processes the game tick to manage NPC interactions.
     * Cleans up tracking data for NPCs whose interactions have expired.
     *
     * @param event the game tick event
     */
    @Subscribe
    public void onGameTick(GameTick event) {
        // Handle NPCs that might need cleanup after interaction has expired
        List<Integer> npcIndicesToRemove = new ArrayList<>();
        int currentTick = client.getTickCount();

        lastInteractionTicks.forEach((npcIndex, lastTick) -> {
            boolean interactionExpired = (currentTick - lastTick) > INTERACTION_TIMEOUT_TICKS;

            // Clear buffer and remove if interaction expired and not in grace period
            if (interactionExpired && !isInGracePeriod.getOrDefault(npcIndex, false)) {
                npcIndicesToRemove.add(npcIndex);
            }
        });

        // Remove NPCs after iteration
        npcIndicesToRemove.forEach(this::cleanupAfterLogging);
    }

    /**
     * Handles the event when a hitsplat is applied to an NPC.
     * Updates the tracking if the NPC is being tracked, or reinitializes tracking if necessary.
     *
     * @param event the hitsplat applied event
     */
    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event) {
        Actor target = event.getActor();
        Hitsplat hitsplat = event.getHitsplat();

        // Check if the target is an NPC and the hitsplat is from the local player
        if (target instanceof NPC && hitsplat.isMine()) {
            NPC npc = (NPC) target;
            int npcIndex = npc.getIndex();

            // If the NPC is being tracked and interaction is still valid, just update the tick
            if (animationBuffers.containsKey(npcIndex)) {
                int lastTick = lastInteractionTicks.getOrDefault(npcIndex, 0);
                int currentTick = client.getTickCount();
                if ((currentTick - lastTick) <= INTERACTION_TIMEOUT_TICKS) {
                    lastInteractionTicks.put(npcIndex, currentTick);
                    return;
                }
            }

            // If not tracked or interaction had expired, reinitialize tracking
            animationBuffers.put(npcIndex, new ArrayList<>());
            lastInteractionTicks.put(npcIndex, client.getTickCount());
            hasLoggedAnimation.put(npcIndex, false);
            isInGracePeriod.put(npcIndex, false);
            lastKnownNpcName.put(npcIndex, npc.getName() != null ? npc.getName() : "Unknown");
        }
    }

    /**
     * Processes the death of an NPC by logging the final buffered animation.
     * This method is invoked after the grace period to ensure the correct death animation is logged.
     *
     * @param npcIndex the index of the NPC in the game
     */
    private void processDeath(int npcIndex) {
        // Proceed with your logic to log the death animation
        List<Integer> bufferedAnimations = animationBuffers.get(npcIndex);
        String npcName = lastKnownNpcName.getOrDefault(npcIndex, "Unknown");

        // Get the last unique animation in the buffer
        Integer lastAnimationId = bufferedAnimations != null && !bufferedAnimations.isEmpty()
                ? bufferedAnimations.get(bufferedAnimations.size() - 1)
                : -1;

        if (lastAnimationId != -1) {
            hasLoggedAnimation.put(npcIndex, true); // Mark that we have logged something
            if (DeathAnimationIDs.isDeathAnimation(lastAnimationId)) {
                clientThread.invoke(() -> plugin.logDeathAnimation(npcName, lastAnimationId));
            } else {
                clientThread.invoke(() -> plugin.logUnknownDeathAnimation(npcName, lastAnimationId));
            }
        }

        cleanupAfterLogging(npcIndex);
    }

    /**
     * Checks if the current interaction with an NPC is still valid based on the tick count.
     *
     * @param npcIndex the index of the NPC in the game
     * @return true if the interaction is valid, false otherwise
     */
    private boolean isInteractionValid(int npcIndex) {
        int currentTick = client.getTickCount();
        return lastInteractionTicks.containsKey(npcIndex)
                && (currentTick - lastInteractionTicks.get(npcIndex)) <= INTERACTION_TIMEOUT_TICKS;
    }

    /**
     * Buffers a unique animation for an NPC, ensuring that only valid and new animations are stored.
     *
     * @param npcIndex    the index of the NPC in the game
     * @param animationId the animation ID to be buffered
     */
    private void bufferAnimation(int npcIndex, int animationId) {
        List<Integer> buffer = animationBuffers.get(npcIndex);
        if (animationId == -1 || (buffer.contains(animationId) && buffer.get(buffer.size() - 1).equals(animationId))) {
            return; // Do not buffer invalid or duplicate animations that were just logged
        }
        buffer.add(animationId);
    }

    /**
     * Cleans up all tracking data associated with an NPC after logging is complete.
     *
     * @param npcIndex the index of the NPC in the game
     */
    private void cleanupAfterLogging(int npcIndex) {
        animationBuffers.remove(npcIndex);
        lastKnownNpcName.remove(npcIndex);
        lastInteractionTicks.remove(npcIndex);
        hasLoggedAnimation.remove(npcIndex);
        isInGracePeriod.remove(npcIndex);
    }
}

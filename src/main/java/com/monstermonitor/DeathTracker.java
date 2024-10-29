package com.monstermonitor;

import net.runelite.api.Client;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.Actor;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks and manages NPC deaths and interactions for Monster Monitor.
 * This class handles tracking of kills and ensures that ignored NPCs are not logged.
 */
public class DeathTracker {
    private final Client client;
    private final MonsterMonitorPlugin plugin;
    private final MonsterMonitorLogger logger;
    private final MonsterMonitorPanel panel;
    private final ClientThread clientThread;

    // Tracks NPCs with recent player interactions
    private final Map<Integer, String> lastKnownNpcName = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> lastInteractionTicks = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> wasNpcEngaged = new ConcurrentHashMap<>();

    private static final int INTERACTION_TIMEOUT_TICKS = 7;

    private static final Map<String, Integer> FINAL_PHASE_IDS = Map.of(
            "Kalphite Queen", 965,
            "The Nightmare", 378,
            "Phosani's Nightmare", 377,
            "Alchemical Hydra", 8622,
            "Hydra", 8609,
            "Phantom Muspah", 12082,
            "The Hueycoatl", 14013,
            "Dusk", 7889,
            "Abyssal Sire", 5891,
            "Kephri", 11722
    );

    private static final Set<String> EXCLUDED_NPC_NAMES = Set.of(
            "Hueycoatl Tail",
            "Hueycoatl Tail Broken",
            "Hueycoatl Body",
            "Dawn"
    );

    @Inject
    public DeathTracker(Client client, MonsterMonitorPlugin plugin, MonsterMonitorLogger logger, MonsterMonitorPanel panel, ClientThread clientThread) {
        this.client = client;
        this.plugin = plugin;
        this.logger = logger;
        this.panel = panel;
        this.clientThread = clientThread;
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged event) {
        Actor source = event.getSource();
        Actor target = event.getTarget();

        if (source == client.getLocalPlayer() && target instanceof NPC) {
            NPC npc = (NPC) target;
            int npcIndex = npc.getIndex();
            String npcName = Optional.ofNullable(npc.getName()).orElse("Unnamed NPC");

            lastKnownNpcName.put(npcIndex, npcName);
            lastInteractionTicks.put(npcIndex, client.getTickCount());
            wasNpcEngaged.put(npcIndex, false);
        }
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event) {
        Actor target = event.getActor();
        Hitsplat hitsplat = event.getHitsplat();

        if (target instanceof NPC && hitsplat.isMine()) {
            NPC npc = (NPC) target;
            int npcIndex = npc.getIndex();
            String npcName = Optional.ofNullable(npc.getName()).orElse(lastKnownNpcName.getOrDefault(npcIndex, "Unnamed NPC"));

            lastKnownNpcName.put(npcIndex, npcName);
            lastInteractionTicks.put(npcIndex, client.getTickCount());
            wasNpcEngaged.put(npcIndex, true);
        }
    }

    @Subscribe
    public void onActorDeath(ActorDeath event) {
        Actor actor = event.getActor();

        if (actor instanceof NPC) {
            NPC npc = (NPC) actor;
            int npcIndex = npc.getIndex();
            int npcId = npc.getId();
            String npcName = lastKnownNpcName.getOrDefault(npcIndex, "Unnamed NPC");

            // Skip logging if the NPC is in the exclusion list
            if (EXCLUDED_NPC_NAMES.contains(npcName)) {
                return;
            }

            // Check if this NPC is a multi-phase boss with intermediate phases
            if (FINAL_PHASE_IDS.containsKey(npcName)) {
                // Only log if this is the final phase ID for the multi-phase boss
                if (FINAL_PHASE_IDS.get(npcName) != npcId) {
                    return; // Skip logging for intermediate phases
                }
            }

            // Log death only if the player was recently engaged with the NPC and it's in the final phase or a regular NPC
            if (wasNpcEngaged.getOrDefault(npcIndex, false) && isInteractionValid(npcIndex)) {
                clientThread.invoke(() -> plugin.logDeath(npcName));
                cleanupAfterLogging(npcIndex);
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        int currentTick = client.getTickCount();

        // Clean up entries where interaction has timed out
        lastInteractionTicks.keySet().removeIf(npcIndex ->
                (currentTick - lastInteractionTicks.get(npcIndex)) > INTERACTION_TIMEOUT_TICKS
        );
    }

    /**
     * Checks if the interaction with an NPC is valid based on the last interaction time.
     *
     * @param npcIndex The index of the NPC to check.
     * @return true if the interaction is within the allowed time window, false otherwise.
     */
    private boolean isInteractionValid(int npcIndex) {
        Integer lastTick = lastInteractionTicks.get(npcIndex);
        return lastTick != null && (client.getTickCount() - lastTick) <= INTERACTION_TIMEOUT_TICKS;
    }

    /**
     * Cleans up tracking information after an NPC's interaction has been logged or expired.
     *
     * @param npcIndex The index of the NPC to clean up.
     */
    private void cleanupAfterLogging(int npcIndex) {
        lastKnownNpcName.remove(npcIndex);
        lastInteractionTicks.remove(npcIndex);
        wasNpcEngaged.remove(npcIndex);
    }
}

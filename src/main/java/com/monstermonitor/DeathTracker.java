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

    private static final Map<String, Set<Integer>> FINAL_PHASE_IDS = Map.ofEntries(
            Map.entry("Kalphite Queen", Set.of(965)),
            Map.entry("The Nightmare", Set.of(378)),
            Map.entry("Phosani's Nightmare", Set.of(377)),
            Map.entry("Alchemical Hydra", Set.of(8622)),
            Map.entry("Hydra", Set.of(8609)),
            Map.entry("Phantom Muspah", Set.of(12082)),
            Map.entry("The Hueycoatl", Set.of(14013)),
            Map.entry("Dusk", Set.of(7889)),
            Map.entry("Abyssal Sire", Set.of(5891)),
            Map.entry("Kephri", Set.of(11722)),
            Map.entry("Verzik Vitur", Set.of(10832, 8371, 10849))
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
                Set<Integer> finalIds = FINAL_PHASE_IDS.get(npcName);
                // Only log if the NPC ID is one of the final phase IDs for this multi-phase boss
                if (!finalIds.contains(npcId)) {
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

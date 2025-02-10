package com.monstermonitor;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

/**
 * Tracks and manages NPC deaths and interactions for Monster Monitor.
 * This class handles tracking of kills, including special NPCs that do not fire the ActorDeath event.
 */
public class DeathTracker {
    private final Client client;
    private final MonsterMonitorPlugin plugin;
    private final MonsterMonitorLogger logger;
    private final MonsterMonitorPanel panel;
    private final ClientThread clientThread;
    private final SpecialNpcTracker specialNpcTracker;

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
            Map.entry("Dusk", Set.of(7889)),
            Map.entry("Abyssal Sire", Set.of(5891)),
            Map.entry("Kephri", Set.of(11722)),
            Map.entry("Verzik Vitur", Set.of(10832, 8371, 10849)),
            Map.entry("Great Olm", Set.of(7551))
    );

    private static final Map<String, Set<Integer>> EXCLUDED_NPC_IDS = Map.ofEntries(
            Map.entry("The Hueycoatl", Set.of(14010, 14011, 14013)),
            Map.entry("Hueycoatl Tail", Set.of(14014, 14015)),
            Map.entry("Hueycoatl Tail (Broken)", Set.of(14014, 14015)),
            Map.entry("Hueycoatl body", Set.of(14017, 14018)),
            Map.entry("Dawn", Set.of(7888)),
            Map.entry("Unstable ice", Set.of(13688)),
            Map.entry("Cracked Ice", Set.of(13026)),
            Map.entry("Rubble", Set.of(14018)),
            Map.entry("Great Olm Right Claw", Set.of(7550, 7553)),
            Map.entry("Great Olm Left Claw", Set.of(7552, 7555))
    );

    @Inject
    public DeathTracker(Client client, MonsterMonitorPlugin plugin, MonsterMonitorLogger logger,
                        MonsterMonitorPanel panel, ClientThread clientThread, SpecialNpcTracker specialNpcTracker) {
        this.client = client;
        this.plugin = plugin;
        this.logger = logger;
        this.panel = panel;
        this.clientThread = clientThread;
        this.specialNpcTracker = specialNpcTracker;
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

            // Start tracking if the NPC is a special NPC
            if (specialNpcTracker.isSpecialNpc(npc)) {
                specialNpcTracker.trackNpc(npc);
            }
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
            String npcName = normalizeName(lastKnownNpcName.getOrDefault(npcIndex, npc.getName()));

            // Skip logging if the NPC is in the exclusion list
            if (isExcludedNpc(npcName, npcId)) {
                return;
            }

            // For special NPCs, let SpecialNpcTracker handle logging
            if (specialNpcTracker.isSpecialNpc(npc)) {
                cleanupAfterLogging(npcIndex);
                return;
            }

            // For multi-phase bosses, only log if the NPC is in its final phase
            if (FINAL_PHASE_IDS.containsKey(npcName)) {
                Set<Integer> finalIds = FINAL_PHASE_IDS.get(npcName);
                if (!finalIds.contains(npcId)) {
                    return; // Skip logging for intermediate phases
                }
            }

            // Log death for standard NPCs if the interaction was valid
            if (wasNpcEngaged.getOrDefault(npcIndex, false) && isInteractionValid(npcIndex)) {
                clientThread.invoke(() -> plugin.logDeath(npcName));
            }

            // Clean up tracking for this NPC
            cleanupAfterLogging(npcIndex);
        }
    }

    /**
     * Normalizes an NPC name by stripping out color tags or extra formatting.
     *
     * @param npcName The original name of the NPC.
     * @return The normalized NPC name.
     */
    private String normalizeName(String npcName) {
        if (npcName == null) {
            return "Unnamed NPC";
        }
        return npcName.replaceAll("<.*?>", "").trim(); // Remove color tags and extra whitespace
    }

    /**
     * Checks if an NPC should be excluded from logging based on its name and ID.
     *
     * @param npcName The name of the NPC.
     * @param npcId   The ID of the NPC.
     * @return True if the NPC is excluded, otherwise false.
     */
    private boolean isExcludedNpc(String npcName, int npcId) {
        // Check by name and associated IDs
        if (EXCLUDED_NPC_IDS.containsKey(npcName)) {
            return EXCLUDED_NPC_IDS.get(npcName).contains(npcId);
        }

        for (Set<Integer> excludedIds : EXCLUDED_NPC_IDS.values()) {
            if (excludedIds.contains(npcId)) {
                return true;
            }
        }

        return false;
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        int currentTick = client.getTickCount();

        // Update health-based tracking for special NPCs
        specialNpcTracker.updateTrackedNpcs();

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

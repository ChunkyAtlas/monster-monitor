package com.monstermonitor;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.Hitsplat;
import net.runelite.api.Actor;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks special NPCs for Monster Monitor.
 * Special NPCs do not trigger the ActorDeath event and require monitoring their health
 * to determine when they die.
 */
public class SpecialNpcTracker {
    private final Map<Integer, NPC> trackedNpcs = new HashMap<>(); // Tracks NPCs by their unique index
    private final Set<Integer> specialNpcIds = new HashSet<>(); // Set of special NPC IDs to track
    private final Set<Integer> loggedNpcIndices = new HashSet<>(); // Prevents duplicate logging by storing NPC indices that have been logged
    private final Object lock = new Object(); // Single lock to manage synchronization for both trackedNpcs and loggedNpcIndices
    private final Client client;
    private final MonsterMonitorPlugin plugin;
    private final ClientThread clientThread;

    @Inject
    public SpecialNpcTracker(Client client, MonsterMonitorPlugin plugin, ClientThread clientThread) {
        this.client = client;
        this.plugin = plugin;
        this.clientThread = clientThread;

        // Register special NPC IDs
        registerSpecialNpc(14009); // The Hueycoatl
        registerSpecialNpc(14012); // The Final Hueycoatl
        registerSpecialNpc(13685); // Amoxliatl
        registerSpecialNpc(12166); // Duke Sucellus
        registerSpecialNpc(13013); // Blue Moon
        registerSpecialNpc(13012); // Eclipse Moon
        registerSpecialNpc(13011); // Blood Moon
    }

    /**
     * Registers a special NPC ID for tracking.
     *
     * @param npcId The ID of the special NPC.
     */
    public void registerSpecialNpc(int npcId) {
        specialNpcIds.add(npcId);
    }

    /**
     * Checks if the given NPC is a special NPC.
     *
     * @param npc The NPC to check.
     * @return True if the NPC is special, otherwise false.
     */
    public boolean isSpecialNpc(NPC npc) {
        return specialNpcIds.contains(npc.getId());
    }

    /**
     * Determines if the NPC should be logged.
     * For Huey, only log if it is in its final phase (ID 14012).
     *
     * @param npc The NPC to check.
     * @return True if the NPC should be logged, false otherwise.
     */
    private boolean shouldLogNpc(NPC npc) {
        if ("The Hueycoatl".equalsIgnoreCase(npc.getName())) {
            return npc.getId() == 14012;
        }
        return true;
    }

    /**
     * Starts tracking the health of a special NPC.
     *
     * @param npc The NPC to start tracking.
     */
    public void trackNpc(NPC npc) {
        if (isSpecialNpc(npc)) {
            synchronized (lock) {
                trackedNpcs.put(npc.getIndex(), npc);
            }
        }
    }

    /**
     * Updates the health of tracked NPCs and handles their death and respawn.
     * This method is called on every game tick.
     */
    public void updateTrackedNpcs() {
        clientThread.invokeLater(() -> {
            synchronized (lock) {
                // Create a snapshot to avoid concurrent modification issues.
                for (NPC npc : new HashSet<>(trackedNpcs.values())) {
                    if (npc == null) {
                        continue;
                    }
                    int healthRatio = npc.getHealthRatio();
                    int npcIndex = npc.getIndex();

                    // LOG DEATH: Only log if health is 0, it hasn't been logged yet, and it passes the check.
                    if (healthRatio <= 1 && !loggedNpcIndices.contains(npcIndex) && shouldLogNpc(npc)) {
                        loggedNpcIndices.add(npcIndex);
                        plugin.logDeath(npc.getName());
                    }

                    // RETRACK: If the NPC "respawns" (health > 0) and was previously logged,
                    // remove it from logged indices and add it back to tracking.
                    else if (healthRatio > 0 && loggedNpcIndices.contains(npcIndex)) {
                        loggedNpcIndices.remove(npcIndex);
                        trackNpc(npc);
                    }
                }
            }
        });
    }

    /**
     * Event listener for when a hitsplat is applied.
     * This ensures retracking of an NPC if it is hit again.
     *
     * @param event The HitsplatApplied event.
     */
    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event) {
        Actor target = event.getActor();
        Hitsplat hitsplat = event.getHitsplat();

        if (target instanceof NPC && hitsplat.isMine()) {
            NPC npc = (NPC) target;
            int npcIndex = npc.getIndex();

            synchronized (lock) {
                if (loggedNpcIndices.contains(npcIndex)) {
                    loggedNpcIndices.remove(npcIndex);
                    trackNpc(npc);
                }
            }
        }
    }

    /**
     * Clears all tracked and logged NPCs on shutdown.
     */
    public void shutdown() {
        synchronized (lock) {
            trackedNpcs.clear();
            loggedNpcIndices.clear();
        }
    }
}

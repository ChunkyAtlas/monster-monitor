package com.monstermonitor;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.client.callback.ClientThread;

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

    private final Map<Integer, NPC> trackedNpcs = new HashMap<>(); // Tracks NPCs by ID
    private final Set<Integer> specialNpcIds = new HashSet<>(); // Registry of special NPC IDs
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
        registerSpecialNpc(13685); // Amoxliatl
        registerSpecialNpc(12166); // Duke Sucellus
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
     * Starts tracking the health of a special NPC.
     *
     * @param npc The NPC to start tracking.
     */
    public void trackNpc(NPC npc) {
        if (isSpecialNpc(npc)) {
            trackedNpcs.put(npc.getIndex(), npc);
        }
    }

    /**
     * Updates the health of tracked NPCs and handles their death.
     * This method is called on every game tick.
     */
    public void updateTrackedNpcs() {
        clientThread.invoke(() -> {
            trackedNpcs.values().removeIf(npc -> {
                if (npc.getHealthRatio() == 0) {
                    plugin.logDeath(npc.getName());
                    return true; // Remove the NPC from tracking
                }
                return false;
            });
        });
    }

    /**
     * Removes the special NPC from tracking
     *
     * @param npc The NPC to remove from tracking.
     */
    public void stopTrackingNpc(NPC npc) {
        trackedNpcs.remove(npc.getIndex());
    }
}

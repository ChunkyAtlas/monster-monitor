package com.monstermonitor;


import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.Actor;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import java.util.*;

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

    // Multi-phase NPCs, including bosses with separate phases
    private static final Set<Integer> MULTI_PHASE_BOSS_IDS = Set.of(
            8360, 8361, 8362, // Zalcano
            9425, 9426, 9427, // Nightmare
            11051, 11052, 11053, // Phosani's Nightmare
            2042, 2043, 2044, // Zulrah
            8059, 8060, // Grotesque Guardians (Dusk and Dawn)
            963, 965, // Giant Mole (known for digging)
            319, 320, // Corporeal Beast and Core
            7888, 7889, 7890, // Alchemical Hydra phases
            8615, 8616, 8617, 8618, // Vorkath (post-quest versions)
            9021, 9022, // Hydra phases
            9033, 9034, // Verzik Vitur (Raids)
            12344, 12345, // Muspah (multi-phase encounter)
            4303, 4304, // Kalphite Queen (phases 1 and 2)
            8240, // Vanstrom Klause
            8242, 8243, 8244, 8245, 8246, // Ranis Drakan phases
            8250, 8251 // Verzik Vitur variants
    );

    // Final phases of these multi-phase NPCs
    private static final Set<Integer> MULTI_PHASE_FINAL_IDS = Set.of(
            8362, // Zalcano final phase
            9427, // Nightmare final phase
            11053, // Phosani's Nightmare final phase
            2044, // Zulrah final phase
            8060, // Grotesque Guardians (Dusk)
            965, // Giant Mole final phase
            320, // Corporeal Beast core
            7890, // Alchemical Hydra final phase
            8618, // Vorkath final phase
            9034, // Verzik Vitur final phase
            12345, // Phantom Muspah final phase
            4304, // Kalphite Queen final phase
            8240, // Vanstrom Klause final phase
            8247, 8248, // Ranis Drakan final phases
            8250 // Verzik Vitur final phase in Theatre of Blood
    );

    // Exclude these NPCs from counting their despawn as a kill
    private static final Set<Integer> DESPAWN_EXCLUSION_IDS = Set.of(
            10530, // Tempoross
            6503, 6504, // Wintertodt
            2042, 2043, 2044, // Zulrah phases
            963, 965, // Giant Mole phases
            8360, 8361, 8362, // Zalcano phases
            4342, 4343, 4344, // Huey Helicopter NPC ID
            8059, 8060 // Grotesque Guardians
    );

    private final Map<Integer, String> lastKnownNpcName = new HashMap<>();
    private final Map<Integer, Integer> lastInteractionTicks = new HashMap<>();
    private final Map<Integer, Boolean> wasNpcEngaged = new HashMap<>();

    private static final int INTERACTION_TIMEOUT_TICKS = 7;

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

            if (MULTI_PHASE_BOSS_IDS.contains(npcId) && MULTI_PHASE_FINAL_IDS.contains(npcId)) {
                clientThread.invoke(() -> plugin.logDeath(npcName));
                cleanupAfterLogging(npcIndex);
            } else if (isInteractionValid(npcIndex)) {
                clientThread.invoke(() -> plugin.logDeath(npcName));
                cleanupAfterLogging(npcIndex);
            }
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        NPC npc = event.getNpc();
        int npcIndex = npc.getIndex();
        int npcId = npc.getId();
        String npcName = Optional.ofNullable(npc.getName()).orElse("Unnamed NPC");

        if (shouldSkipDespawn(npcId)) {
            return;
        }

        if (isMultiPhaseNpc(npcId) && !isMultiPhaseFinalPhase(npcId)) {
            return;
        }

        NpcData npcData = logger.getNpcLog().get(npcName);
        if (npcData != null && npcData.isIgnored()) {
            return; // Skip logging if the NPC is set to be ignored.
        }

        if (wasNpcEngaged.getOrDefault(npcIndex, false) && isInteractionValid(npcIndex)) {
            clientThread.invoke(() -> plugin.logDeath(npcName));
        }

        cleanupAfterLogging(npcIndex);
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        List<Integer> npcIndicesToRemove = new ArrayList<>();
        int currentTick = client.getTickCount();

        lastInteractionTicks.forEach((npcIndex, lastTick) -> {
            boolean interactionExpired = (currentTick - lastTick) > INTERACTION_TIMEOUT_TICKS;
            if (interactionExpired) {
                npcIndicesToRemove.add(npcIndex);
            }
        });

        npcIndicesToRemove.forEach(this::cleanupAfterLogging);
    }

    /**
     * Determines if the despawn of an NPC should be skipped.
     *
     * @param npcId The ID of the NPC.
     * @return True if the despawn should be skipped, false otherwise.
     */
    private boolean shouldSkipDespawn(int npcId) {
        return DESPAWN_EXCLUSION_IDS.contains(npcId);
    }

    /**
     * Checks if the NPC is a multi-phase boss.
     *
     * @param npcId The ID of the NPC.
     * @return True if the NPC is a multi-phase boss, false otherwise.
     */
    private boolean isMultiPhaseNpc(int npcId) {
        return MULTI_PHASE_BOSS_IDS.contains(npcId);
    }

    /**
     * Checks if the NPC is in its final phase.
     *
     * @param npcId The ID of the NPC.
     * @return True if the NPC is in its final phase, false otherwise.
     */
    private boolean isMultiPhaseFinalPhase(int npcId) {
        return MULTI_PHASE_FINAL_IDS.contains(npcId);
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

package com.monstermonitor;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.Actor;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;

/**
 * Tracks and manages NPC deaths and interactions for Monster Monitor.
 * This class handles tracking NPC interactions and detecting NPC deaths to log significant events related to NPCs during gameplay.
 */
@Getter
@Setter
public class DeathTracker {
    private final Client client;
    private final MonsterMonitorPlugin plugin;
    private final MonsterMonitorLogger logger;
    private final MonsterMonitorPanel panel;
    private final ClientThread clientThread;
    private final EventBus eventBus;

    private static final Set<Integer> MULTI_PHASE_BOSS_IDS = Set.of(
            // Verzik Vitur
            8360, 8361, 8362,
            // The Nightmare / Phosani's Nightmare
            9425, 9426, 9427, 11051, 11052, 11053,
            // Zulrah
            2042, 2043, 2044,
            // Vorkath
            8059, 8060,
            // Kalphite Queen
            963, 965,
            // Corporeal Beast
            319, 320,
            // Grotesque Guardians (Dusk and Dawn)
            7888, 7889, 7890,
            // Alchemical Hydra
            8615, 8616, 8617, 8618,
            // Gauntlet/Corrupted Gauntlet Boss (Hunllef)
            9021, 9022, 9033, 9034,
            // Xamphur (Desert Treasure II)
            12344, 12345
    );

    private static final Set<Integer> MULTI_PHASE_FINAL_IDS = Set.of(
            // Verzik Vitur (final form)
            8362,
            // The Nightmare / Phosani's Nightmare (final form)
            9427, 11053,
            // Zulrah (remains the same, but different forms)
            2044,
            // Vorkath (post-special phase)
            8060,
            // Kalphite Queen (second form)
            965,
            // Corporeal Beast (final form)
            320,
            // Grotesque Guardians (last one standing, typically Dusk)
            7890,
            // Alchemical Hydra (enraged phase)
            8618,
            // Gauntlet/Corrupted Gauntlet Boss (Hunllef)
            9034,
            // Xamphur (Desert Treasure II final phase)
            12345
    );

    private final Map<Integer, String> lastKnownNpcName = new HashMap<>();
    private final Map<Integer, Integer> lastInteractionTicks = new HashMap<>();
    private final Map<Integer, Boolean> isInGracePeriod = new HashMap<>();
    private final Map<Integer, Boolean> hasLoggedDeath = new HashMap<>();
    private final Map<Integer, Boolean> wasNpcEngaged = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final int INTERACTION_TIMEOUT_TICKS = 7;

    @Inject
    public DeathTracker(Client client, MonsterMonitorPlugin plugin, MonsterMonitorLogger logger, MonsterMonitorPanel panel, EventBus eventBus, ClientThread clientThread) {
        this.client = client;
        this.plugin = plugin;
        this.logger = logger;
        this.panel = panel;
        this.clientThread = clientThread;
        this.eventBus = eventBus;

        eventBus.register(this);
    }

    /**
     * Unregisters the DeathTracker from the EventBus.
     */
    public void unregister() {
        eventBus.unregister(this);
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged event) {
        Actor source = event.getSource();
        Actor target = event.getTarget();

        if (source == client.getLocalPlayer() && target instanceof NPC) {
            NPC npc = (NPC) target;
            int npcIndex = npc.getIndex();

            // Update the NPC name directly if available.
            String npcName = Optional.ofNullable(npc.getName()).orElse("Unnamed NPC");
            lastKnownNpcName.put(npcIndex, npcName);
            lastInteractionTicks.put(npcIndex, client.getTickCount());
            isInGracePeriod.put(npcIndex, false);
            hasLoggedDeath.put(npcIndex, false);

            // Reset engagement status when a new interaction starts.
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

            // Update the interaction tick and ensure the NPC name is correct.
            String npcName = Optional.ofNullable(npc.getName()).orElse(lastKnownNpcName.getOrDefault(npcIndex, "Unnamed NPC"));
            lastKnownNpcName.put(npcIndex, npcName);
            lastInteractionTicks.put(npcIndex, client.getTickCount());

            // Mark the NPC as engaged when a hitsplat is applied.
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

            // Check if the NPC is a multi-phase boss.
            if (MULTI_PHASE_BOSS_IDS.contains(npcId)) {
                if (MULTI_PHASE_FINAL_IDS.contains(npcId)) {
                    clientThread.invoke(() -> plugin.logDeath(npcName));
                    cleanupAfterLogging(npcIndex);
                }
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

        // Only log the despawn if the NPC was actually engaged (e.g., through hitsplats).
        if (wasNpcEngaged.getOrDefault(npcIndex, false)) {
            if (MULTI_PHASE_BOSS_IDS.contains(npcId) && MULTI_PHASE_FINAL_IDS.contains(npcId)) {
                lastKnownNpcName.put(npcIndex, npcName);
                clientThread.invoke(() -> plugin.logDeath(npcName));
                cleanupAfterLogging(npcIndex);
            } else if (isInteractionValid(npcIndex)) {
                lastKnownNpcName.put(npcIndex, npcName);
                clientThread.invoke(() -> plugin.logDeath(npcName));
                cleanupAfterLogging(npcIndex);
            }
        }

        // Clean up the engagement status regardless of whether it was logged.
        wasNpcEngaged.remove(npcIndex);
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        List<Integer> npcIndicesToRemove = new ArrayList<>();
        int currentTick = client.getTickCount();

        lastInteractionTicks.forEach((npcIndex, lastTick) -> {
            boolean interactionExpired = (currentTick - lastTick) > INTERACTION_TIMEOUT_TICKS;
            if (interactionExpired && !isInGracePeriod.getOrDefault(npcIndex, false)) {
                npcIndicesToRemove.add(npcIndex);
            }
        });

        npcIndicesToRemove.forEach(this::cleanupAfterLogging);
    }

    private boolean isInteractionValid(int npcIndex) {
        Integer lastTick = lastInteractionTicks.get(npcIndex);
        return lastTick != null && (client.getTickCount() - lastTick) <= INTERACTION_TIMEOUT_TICKS;
    }

    private void cleanupAfterLogging(int npcIndex) {
        lastKnownNpcName.remove(npcIndex);
        lastInteractionTicks.remove(npcIndex);
        isInGracePeriod.remove(npcIndex);
        hasLoggedDeath.remove(npcIndex);
        wasNpcEngaged.remove(npcIndex);
    }
}

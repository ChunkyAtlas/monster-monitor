package com.monstermonitor;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Actor;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ActorDeath;
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

    // Store multi-phase NPC IDs that represent their final phases
    private static final Set<Integer> MULTI_PHASE_BOSS_IDS = Set.of(
            8360, // Verzik Vitur (last phase)
            9425, // The Nightmare
            10444, // Phosani's Nightmare
            8061, // Vorkath (last form)
            2044, // Zulrah (blue form)
            8612, // Alchemical Hydra (last phase)
            964, // Kalphite Queen (second form)
            319, // Corporeal Beast
            7888, // Grotesque Guardians (Dusk)
            5862, // Cerberus
            7553, // The Great Olm (head)
            7706, // TzKal-Zuk (Inferno)
            3127 // TzTok-Jad (Fight Caves)
    );

    private final Map<Integer, String> lastKnownNpcName = new HashMap<>();
    private final Map<Integer, Integer> lastInteractionTicks = new HashMap<>();
    private final Map<Integer, Boolean> isInGracePeriod = new HashMap<>();
    private final Map<Integer, Boolean> hasLoggedDeath = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final int INTERACTION_TIMEOUT_TICKS = 7;

    @Inject
    public DeathTracker(Client client, MonsterMonitorPlugin plugin, MonsterMonitorLogger logger, MonsterMonitorPanel panel, EventBus eventBus, ClientThread clientThread) {
        this.client = client;
        this.plugin = plugin;
        this.logger = logger;
        this.panel = panel;
        this.clientThread = clientThread;

        eventBus.register(this);
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged event) {
        Actor source = event.getSource();
        Actor target = event.getTarget();

        if (source == client.getLocalPlayer() && target instanceof NPC) {
            NPC npc = (NPC) target;
            int npcIndex = npc.getIndex();

            lastInteractionTicks.put(npcIndex, client.getTickCount());
            isInGracePeriod.put(npcIndex, false);
            hasLoggedDeath.put(npcIndex, false);
            lastKnownNpcName.put(npcIndex, npc.getName() != null ? npc.getName() : "Unknown");
        }
    }

    @Subscribe
    public void onActorDeath(ActorDeath event) {
        Actor actor = event.getActor();

        if (actor instanceof NPC) {
            NPC npc = (NPC) actor;
            int npcIndex = npc.getIndex();
            String npcName = lastKnownNpcName.getOrDefault(npcIndex, "Unknown");

            // Check if this is a multi-phase NPC and ensure it only logs once.
            if (MULTI_PHASE_BOSS_IDS.contains(npc.getId())) {
                if (!hasLoggedDeath.getOrDefault(npcIndex, false)) {
                    clientThread.invoke(() -> plugin.logDeath(npcName));
                    hasLoggedDeath.put(npcIndex, true);
                    cleanupAfterLogging(npcIndex);
                }
            } else if (isInteractionValid(npcIndex)) {
                clientThread.invoke(() -> plugin.logDeath(npcName));
                cleanupAfterLogging(npcIndex);
            }
        }
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
        int currentTick = client.getTickCount();
        return lastInteractionTicks.containsKey(npcIndex)
                && (currentTick - lastInteractionTicks.get(npcIndex)) <= INTERACTION_TIMEOUT_TICKS;
    }

    private void cleanupAfterLogging(int npcIndex) {
        lastKnownNpcName.remove(npcIndex);
        lastInteractionTicks.remove(npcIndex);
        isInGracePeriod.remove(npcIndex);
        hasLoggedDeath.remove(npcIndex);
    }
}

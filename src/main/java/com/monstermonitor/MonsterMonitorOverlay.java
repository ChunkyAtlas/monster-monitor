package com.monstermonitor;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Collections;
import java.util.List;

/**
 * Represents the overlay used in Monster Monitor.
 * This overlay displays information about tracked NPCs, such as their names and the number of kills
 * towards a specified kill limit. The overlay is positioned in the top left of the game screen,
 * with a semi-transparent dark background.
 */
public class MonsterMonitorOverlay extends Overlay
{
    private List<NpcData> trackedNpcs; // List of NPCs being tracked for the overlay

    @Getter
    private final Client client;
    private final MonsterMonitorPlugin plugin;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    private ClientThread clientThread;

    /**
     * Constructs the MonsterMonitorOverlay and initializes the overlay's properties.
     *
     * @param client The RuneLite client instance.
     * @param plugin The MonsterMonitorPlugin instance that this overlay is part of.
     */
    @Inject
    public MonsterMonitorOverlay(Client client, MonsterMonitorPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT); // Position the overlay at the top left of the screen
        setLayer(OverlayLayer.ABOVE_WIDGETS); // Layer the overlay above widgets
        setPriority(50); // Give the overlay medium priority to ensure it is visible

        // Configure the panelComponent for the overlay
        panelComponent.setBackgroundColor(new Color(45, 45, 45, 200)); // Dark gray with semi-transparency
        panelComponent.setPreferredSize(new Dimension(150, 0)); // Set a preferred width for the overlay
    }

    /**
     * Updates the list of tracked NPCs to be displayed on the overlay.
     *
     * @param trackedNpcs The list of NPCs to be tracked and displayed.
     */
    public void updateOverlayData(List<NpcData> trackedNpcs)
    {
        Collections.reverse(trackedNpcs); // Reverse the order to display the most recent NPCs first
        this.trackedNpcs = trackedNpcs;
    }

    /**
     * Renders the overlay on the game screen, displaying tracked NPCs and their kill counts.
     *
     * @param graphics The Graphics2D object used for drawing the overlay.
     * @return The dimension of the rendered overlay.
     */
    @Override
    public Dimension render(Graphics2D graphics)
    {
        panelComponent.getChildren().clear(); // Clear previous overlay components

        if (trackedNpcs == null || trackedNpcs.isEmpty())
        {
            return null; // Return early if there are no tracked NPCs to display
        }

        // Add a title to the overlay with an orange color
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Monster Monitor")
                .color(new Color(200, 150, 0)) // Orange color for the title
                .build());

        // Display only NPCs with a set kill limit
        for (NpcData npcData : trackedNpcs)
        {
            int limit = npcData.getKillLimit();
            int killsTowardLimit = npcData.getKillCountForLimit();

            if (limit > 0)
            {
                // Add each NPC with a name and kill count, showing progress towards the kill limit
                panelComponent.getChildren().add(LineComponent.builder()
                        .left(npcData.getNpcName())
                        .right(killsTowardLimit + "/" + limit) // Show the progress towards the kill limit
                        .leftColor(new Color(200, 200, 200)) // Light gray text for NPC name
                        .rightColor(new Color(200, 200, 200)) // Light gray text for kill count
                        .build());
            }
        }

        return panelComponent.render(graphics); // Render the overlay and return its dimension
    }

    /**
     * Gets the plugin instance associated with this overlay.
     *
     * @return The MonsterMonitorPlugin instance.
     */
    @Nullable
    @Override
    public MonsterMonitorPlugin getPlugin() {
        return plugin;
    }
}

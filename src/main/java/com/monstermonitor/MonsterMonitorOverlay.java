package com.monstermonitor;

import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;

public class MonsterMonitorOverlay extends Overlay
{
    private List<NpcData> trackedNpcs; // List of NPCs being tracked for the overlay

    private final Client client;
    private final MonsterMonitorPlugin plugin;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    private ClientThread clientThread;

    @Inject
    public MonsterMonitorOverlay(Client client, MonsterMonitorPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT); // Position the overlay at the top left
        setLayer(OverlayLayer.ABOVE_WIDGETS); // Layer it above widgets
        setPriority(OverlayPriority.HIGH); // Give the overlay high priority
    }

    // Update the overlay data with the list of tracked NPCs
    public void updateOverlayData(List<NpcData> trackedNpcs)
    {
        this.trackedNpcs = trackedNpcs;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        panelComponent.getChildren().clear(); // Clear previous overlay components

        // If no NPCs are being tracked, don't display the overlay
        if (trackedNpcs == null || trackedNpcs.isEmpty())
        {
            return null; // Return early if there are no tracked NPCs to display
        }

        // Add a title to the overlay
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Monster Monitor")
                .color(Color.ORANGE)
                .build());

        // Display only NPCs with a set kill limit
        for (NpcData npcData : trackedNpcs)
        {
            int limit = npcData.getKillLimit();
            int killsTowardLimit = npcData.getKillCountForLimit();

            // Only display NPCs that have a kill limit set
            if (limit > 0)
            {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left(npcData.getNpcName())
                        .right(killsTowardLimit + "/" + limit) // Show the progress towards the kill limit
                        .build());
            }
        }

        return panelComponent.render(graphics); // Render the overlay
    }
}

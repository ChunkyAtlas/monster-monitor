package com.monstermonitor;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents the overlay used in Monster Monitor.
 * This overlay displays information about tracked NPCs, such as their names and the number of kills
 * towards a specified kill limit, along with a dynamic progress bar.
 */
public class MonsterMonitorOverlay extends Overlay
{
    private static final int NAME_CHARACTER_LIMIT = 16;  // Define a character limit for truncation
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
        setPosition(OverlayPosition.TOP_CENTER);
        setLayer(OverlayLayer.ABOVE_SCENE); // Layer the overlay above widgets
        setPriority(50); // Give the overlay medium priority to ensure it is visible

        // Configure the panelComponent for the overlay
        panelComponent.setBackgroundColor(new Color(45, 45, 45, 200)); // Dark gray with semi-transparency
        panelComponent.setPreferredSize(new Dimension(150, 0)); // Set a preferred width for the overlay
        panelComponent.setBorder(new Rectangle(5, 5, 5, 5)); // Creates padding around the content
    }

    /**
     * Updates the list of tracked NPCs to be displayed on the overlay.
     * Filters out any NPCs that are currently ignored before updating.
     *
     * @param trackedNpcs The list of NPCs to be tracked and displayed.
     */
    public void updateOverlayData(List<NpcData> trackedNpcs)
    {
        // Filter out ignored NPCs before updating the overlay data
        List<NpcData> filteredNpcs = trackedNpcs.stream()
                .filter(npcData -> !npcData.isIgnored())
                .collect(Collectors.toList());

        Collections.reverse(filteredNpcs); // Reverse the order to display the most recent NPCs first
        this.trackedNpcs = filteredNpcs;
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

        // Add a title to the overlay with a box around it using PanelComponent
        panelComponent.getChildren().add(
                TitleComponent.builder()
                        .text("Monster Monitor")
                        .color(new Color(200, 150, 0))
                        .build()
        );

        int yOffset = 26; // Initial offset for drawing the first progress bar
        int overlayWidth = panelComponent.getPreferredSize().width - 10; // Padding for bar width
        int barHeight = 14; // Height of each progress bar

        // Display NPCs with a set kill limit and their progress
        for (NpcData npcData : trackedNpcs)
        {
            int limit = npcData.getKillLimit();
            int killsTowardLimit = npcData.getKillCountForLimit();

            // Only display NPCs that have a set kill limit
            if (limit > 0)
            {
                // Calculate progress percentage and cap it at 100%
                double progressPercentage = Math.min(killsTowardLimit / (double) limit, 1.0);

                // Determine the bar color based on progress
                Color barColor = progressPercentage >= 0.75 ? Color.GREEN.darker()
                        : progressPercentage >= 0.5 ? Color.ORANGE.darker()
                        : Color.RED.darker();

                // Draw the progress bar background (gray)
                graphics.setColor(new Color(60, 60, 60));
                graphics.fillRect(5, yOffset, overlayWidth, barHeight);

                // Draw the filled portion of the progress bar
                int filledWidth = (int) (progressPercentage * overlayWidth);
                graphics.setColor(barColor);
                graphics.fillRect(5, yOffset, filledWidth, barHeight);

                // Draw a border around the progress bar
                graphics.setColor(Color.BLACK);
                graphics.drawRect(5, yOffset, overlayWidth, barHeight);

                // Set the font for the NPC name and progress text
                graphics.setFont(new Font("Arial", Font.BOLD, 11));
                FontMetrics metrics = graphics.getFontMetrics();
                int textY = yOffset + ((barHeight + metrics.getAscent() - metrics.getDescent()) / 2) + 1;

                // Draw the truncated NPC name on the left
                String npcName = truncateName(npcData.getNpcName());
                graphics.setColor(Color.BLACK);
                graphics.drawString(npcName, 9, textY + 1); // Shadow
                graphics.setColor(Color.LIGHT_GRAY);
                graphics.drawString(npcName, 8, textY);

                // Draw the progress text on the right
                String progressText = formatNumber(killsTowardLimit) + "/" + formatNumber(limit);
                int textWidth = metrics.stringWidth(progressText);
                int textX = overlayWidth - textWidth + 5;
                graphics.setColor(Color.BLACK);
                graphics.drawString(progressText, textX + 1, textY + 1); // Shadow
                graphics.setColor(Color.LIGHT_GRAY);
                graphics.drawString(progressText, textX, textY);

                // Adjust yOffset for the next NPC entry
                yOffset += barHeight + 4;
            }
        }

        // Render the panel to ensure correct background and dimensions
        return panelComponent.render(graphics);
    }

    /**
     * Truncates the NPC name if it exceeds the specified character limit, appending an ellipsis if truncated.
     *
     * @param name The original NPC name.
     * @return A truncated name with ellipsis if it exceeds the limit, or the original name otherwise.
     */
    private String truncateName(String name)
    {
        if (name.length() <= NAME_CHARACTER_LIMIT) {
            return name;
        }
        return name.substring(0, NAME_CHARACTER_LIMIT - 3) + "...";
    }

    /**
     * Formats the given number to a more readable string, such as 1k for 1000, without rounding up.
     *
     * @param number The number to format.
     * @return A formatted string representing the number.
     */
    private String formatNumber(int number) {
        if (number >= 1000000) {
            double millions = Math.floor(number / 1_000_000.0 * 10) / 10;
            return String.format("%.1fm", millions);
        } else if (number >= 1000) {
            double thousands = Math.floor(number / 1_000.0 * 10) / 10;
            return String.format("%.1fk", thousands);
        } else {
            return Integer.toString(number);
        }
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

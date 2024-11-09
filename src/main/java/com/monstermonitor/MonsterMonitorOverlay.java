package com.monstermonitor;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

import javax.inject.Inject;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Represents the overlay used in Monster Monitor.
 * This overlay displays information about tracked NPCs, such as their names and the number of kills
 * towards a specified kill limit, along with a dynamic progress bar.
 */
public class MonsterMonitorOverlay extends Overlay {

    private static final int NAME_CHARACTER_LIMIT = 16;  // Define a character limit for truncation
    private final AtomicReference<List<NpcData>> trackedNpcs = new AtomicReference<>(Collections.emptyList()); // Thread-safe list of tracked NPCs

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
    public MonsterMonitorOverlay(Client client, MonsterMonitorPlugin plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_CENTER);
        setLayer(OverlayLayer.ABOVE_SCENE); // Layer the overlay above widgets
        setResizable(true); // Allow users to resize the overlay
        setMovable(true); // Allow users to move the overlay
        setPriority(55); // Increase priority slightly for better visibility

        // Configure the panelComponent for the overlay
        panelComponent.setBackgroundColor(new Color(45, 45, 45, 200)); // Dark gray with semi-transparency
        panelComponent.setPreferredSize(new Dimension(160, 0)); // Set preferred width for the overlay
        panelComponent.setBorder(new Rectangle(5, 5, 5, 5)); // Adds padding around the content
    }

    /**
     * Updates the list of tracked NPCs to be displayed on the overlay.
     * Filters out any NPCs that are currently ignored before updating.
     *
     * @param trackedNpcs The list of NPCs to be tracked and displayed.
     */
    public void updateOverlayData(List<NpcData> trackedNpcs) {
        clientThread.invokeLater(() -> {
            List<NpcData> filteredNpcs = trackedNpcs.stream()
                    .filter(npcData -> !npcData.isIgnored())
                    .collect(Collectors.toList());

            Collections.reverse(filteredNpcs); // Reverse the order to display the most recent NPCs first
            this.trackedNpcs.set(filteredNpcs); // Atomically update the tracked NPCs
        });
    }

    /**
     * Renders the overlay on the game screen, displaying tracked NPCs and their kill counts.
     *
     * @param graphics The Graphics2D object used for drawing the overlay.
     * @return The dimension of the rendered overlay.
     */
    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.getChildren().clear(); // Clear previous overlay components

        List<NpcData> currentNpcs = trackedNpcs.get(); // Get the current NPC list atomically

        if (currentNpcs == null || currentNpcs.isEmpty()) {
            return null; // Return early if there are no tracked NPCs to display
        }

        int overlayWidth = 160;
        int yOffset = 0;
        int totalHeight = 0;

        // Check if title should be displayed, and add it if necessary
        if (plugin.config.showTitle()) {
            graphics.setFont(new Font("Arial", Font.BOLD, 14));
            FontMetrics metrics = graphics.getFontMetrics();
            int titleX = (overlayWidth - metrics.stringWidth("Monster Monitor")) / 2;
            int titleY = yOffset + metrics.getAscent();

            // Draw the title with a shadow for visibility
            graphics.setColor(Color.BLACK);
            graphics.drawString("Monster Monitor", titleX + 1, titleY + 1);
            graphics.setColor(new Color(200, 150, 0));
            graphics.drawString("Monster Monitor", titleX, titleY);

            yOffset += 20;
            totalHeight += 20;
        } else {
            panelComponent.getChildren().add(LineComponent.builder().left("").build());
            yOffset += 10;
            totalHeight += 10;
        }

        // Display each tracked NPC with a progress bar
        for (NpcData npcData : currentNpcs) {
            if (npcData.getKillLimit() > 0) {
                drawProgressBar(graphics, npcData, overlayWidth, 14, yOffset);
                yOffset += 18;
                totalHeight += 18;
            }
        }

        panelComponent.setPreferredSize(new Dimension(overlayWidth, totalHeight));
        return new Dimension(overlayWidth, totalHeight);
    }

    /**
     * Draws a progress bar for a specific NPC's kill progress toward the limit.
     *
     * @param graphics     The Graphics2D object used for drawing.
     * @param npcData      The NPC data containing kill information.
     * @param overlayWidth The width of the overlay.
     * @param barHeight    The height of the progress bar.
     * @param yOffset      The vertical offset for drawing the bar.
     */
    private void drawProgressBar(Graphics2D graphics, NpcData npcData, int overlayWidth, int barHeight, int yOffset) {
        int limit = npcData.getKillLimit();
        int killsTowardLimit = npcData.getKillCountForLimit();

        // Calculate progress percentage and cap it at 100%
        double progressPercentage = Math.min(killsTowardLimit / (double) limit, 1.0);
        Color barColor = getBarColor(progressPercentage);

        int barWidth = overlayWidth - 10;
        int startX = (overlayWidth - barWidth) / 2;

        // Draw the progress bar background (gray)
        graphics.setColor(new Color(60, 60, 60));
        graphics.fillRect(startX, yOffset, barWidth, barHeight);

        // Draw the filled portion of the progress bar
        int filledWidth = (int) (progressPercentage * barWidth);
        graphics.setColor(barColor);
        graphics.fillRect(startX, yOffset, filledWidth, barHeight);

        // Draw a border around the progress bar
        graphics.setColor(Color.BLACK);
        graphics.drawRect(startX, yOffset, barWidth, barHeight);

        // Draw NPC name and progress text
        drawText(graphics, npcData, killsTowardLimit, limit, overlayWidth, startX, yOffset, barHeight, barWidth);
    }

    /**
     * Determines the color of the progress bar based on the progress percentage.
     *
     * @param progressPercentage The progress percentage of the kill count toward the limit.
     * @return A color representing the progress stage.
     */
    private Color getBarColor(double progressPercentage) {
        if (progressPercentage >= 0.75) return Color.GREEN.darker();
        if (progressPercentage >= 0.5) return Color.ORANGE.darker();
        return Color.RED.darker();
    }

    /**
     * Draws the NPC name and progress count on top of the progress bar.
     *
     * @param graphics         The Graphics2D object used for drawing.
     * @param npcData          The NPC data containing kill information.
     * @param killsTowardLimit The current kill count.
     * @param limit            The kill limit.
     * @param overlayWidth     The width of the overlay.
     * @param startX           The starting X position for the text.
     * @param yOffset          The vertical offset for drawing the text.
     * @param barHeight        The height of the progress bar.
     * @param barWidth         The width of the progress bar.
     */
    private void drawText(Graphics2D graphics, NpcData npcData, int killsTowardLimit, int limit, int overlayWidth, int startX, int yOffset, int barHeight, int barWidth) {
        graphics.setFont(new Font("Arial", Font.BOLD, 11));
        FontMetrics metrics = graphics.getFontMetrics();
        int textY = yOffset + ((barHeight + metrics.getAscent() - metrics.getDescent()) / 2) + 1;

        // Draw the truncated NPC name on the left
        String npcName = truncateName(npcData.getNpcName());
        graphics.setColor(Color.BLACK);
        graphics.drawString(npcName, startX + 4, textY + 1); // Shadow
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.drawString(npcName, startX + 3, textY);

        // Draw the progress text on the right
        String progressText = formatNumber(killsTowardLimit) + "/" + formatNumber(limit);
        int textWidth = metrics.stringWidth(progressText);
        int textX = startX + barWidth - textWidth - 4;
        graphics.setColor(Color.BLACK);
        graphics.drawString(progressText, textX + 1, textY + 1); // Shadow
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.drawString(progressText, textX, textY);
    }

    /**
     * Truncates the NPC name if it exceeds the specified character limit, appending an ellipsis if truncated.
     *
     * @param name The original NPC name.
     * @return A truncated name with ellipsis if it exceeds the limit, or the original name otherwise.
     */
    private String truncateName(String name) {
        return name.length() <= NAME_CHARACTER_LIMIT ? name : name.substring(0, NAME_CHARACTER_LIMIT - 3) + "...";
    }

    /**
     * Formats the given number to a more readable string, such as 1k for 1000, without rounding up.
     *
     * @param number The number to format.
     * @return A formatted string representing the number.
     */
    private String formatNumber(int number) {
        if (number >= 1_000_000) {
            return String.format("%.1fm", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fk", number / 1_000.0);
        } else {
            return Integer.toString(number);
        }
    }
}
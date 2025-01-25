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
    private final MonsterMonitorConfig config;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    private ClientThread clientThread;

    /**
     * Constructs the MonsterMonitorOverlay and initializes the overlay's properties.
     *
     * @param client The RuneLite client instance.
     * @param plugin The MonsterMonitorPlugin instance that this overlay is part of.
     * @param config The MonsterMonitorConfig instance for user settings.
     */
    @Inject
    public MonsterMonitorOverlay(Client client, MonsterMonitorPlugin plugin, MonsterMonitorConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
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
        Dimension preferredSize = getPreferredSize();
        int overlayWidth = (preferredSize != null && preferredSize.width > 0) ? preferredSize.width : 160;
        panelComponent.getChildren().clear(); // Clear previous overlay components

        List<NpcData> currentNpcs = trackedNpcs.get();
        if (currentNpcs == null || currentNpcs.isEmpty()) {
            return null;
        }

        int yOffset = 0;
        int totalHeight = 0;

        // Calculate dynamic bar height based on the available space
        int totalBars = currentNpcs.size();
        int availableHeight = (preferredSize != null && preferredSize.height > 0) ? preferredSize.height : 0;
        int barHeight = Math.max(14, Math.min(availableHeight / (totalBars + 1), 30)); // Dynamically scale bar height

        // Render the title if enabled
        if (plugin.config.showTitle()) {
            graphics.setFont(new Font("Arial", Font.BOLD, 14));
            FontMetrics metrics = graphics.getFontMetrics();
            int titleX = (overlayWidth - metrics.stringWidth("Monster Monitor")) / 2;
            int titleY = yOffset + metrics.getAscent();

            // Draw the title
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

        // Render progress bars for each tracked NPC
        for (NpcData npcData : currentNpcs) {
            if (npcData.getKillLimit() > 0) {
                drawProgressBar(graphics, npcData, overlayWidth, barHeight, yOffset);
                yOffset += barHeight + 4; // Increment yOffset for the next bar
                totalHeight += barHeight + 4; // Add bar height and spacing to total height
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

        // Interpolate color based on custom configuration
        Color barColor = interpolateColor(progressPercentage);

        int barWidth = overlayWidth - 10;
        int startX = (overlayWidth - barWidth) / 2;

        // Draw the progress bar background (gray)
        graphics.setColor(new Color(60, 60, 60));
        graphics.fillRect(startX, yOffset, barWidth, barHeight);

        // Draw the filled progress bar
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
     * Interpolates between three colors (customizable) based on a ratio.
     * The color transitions from the start color (0%) to the midpoint color (50%) to the end color (100%).
     *
     * @param ratio The progress ratio (0.0 to 1.0) used to determine the interpolated color.
     * @return The interpolated color corresponding to the given ratio.
     */
    private Color interpolateColor(double ratio) {
        Color startColor = config.progressBarStartColor();
        Color midColor = config.progressBarMidColor();
        Color endColor = config.progressBarEndColor();

        Color interpolatedColor;
        if (ratio <= 0.5) {
            // Interpolate between Start Color and Midpoint Color
            interpolatedColor = interpolate(startColor, midColor, ratio * 2);
        } else {
            // Interpolate between Midpoint Color and End Color
            interpolatedColor = interpolate(midColor, endColor, (ratio - 0.5) * 2);
        }

        return interpolatedColor;
    }

    /**
     * Interpolates between two colors based on a given ratio.
     *
     * @param startColor The starting color.
     * @param endColor   The ending color.
     * @param ratio      The interpolation ratio (0.0 to 1.0).
     * @return The interpolated color.
     */
    private Color interpolate(Color startColor, Color endColor, double ratio) {
        int red = (int) (startColor.getRed() * (1 - ratio) + endColor.getRed() * ratio);
        int green = (int) (startColor.getGreen() * (1 - ratio) + endColor.getGreen() * ratio);
        int blue = (int) (startColor.getBlue() * (1 - ratio) + endColor.getBlue() * ratio);

        return new Color(red, green, blue);
    }

    /**
     * Draws the NPC name and kill progress text on top of the progress bar.
     * Dynamically adjusts font size, text alignment, and truncation based on available space.
     *
     * @param graphics         The Graphics2D object used for drawing.
     * @param npcData          The NPC data containing kill information.
     * @param killsTowardLimit The current kill count for the NPC.
     * @param limit            The kill limit for the NPC.
     * @param overlayWidth     The width of the overlay.
     * @param startX           The starting X position for the text.
     * @param yOffset          The vertical offset for drawing the text.
     * @param barHeight        The height of the progress bar.
     * @param barWidth         The width of the progress bar.
     */
    private void drawText(Graphics2D graphics, NpcData npcData, int killsTowardLimit, int limit, int overlayWidth, int startX, int yOffset, int barHeight, int barWidth) {
        // Dynamically calculate font size based on bar height
        int fontSize = Math.max(11, barHeight - 2);
        graphics.setFont(new Font("Arial", Font.BOLD, fontSize));
        FontMetrics metrics = graphics.getFontMetrics();
        int textY = yOffset + ((barHeight + metrics.getAscent() - metrics.getDescent()) / 2);

        // Calculate available width for the NPC name and progress text
        int availableTextWidth = barWidth - 10; // Leave some padding

        // NPC Name (Left-aligned)
        String npcName = truncateText(npcData.getNpcName(), availableTextWidth / 2, metrics); // Allocate half width
        graphics.setColor(Color.BLACK);
        graphics.drawString(npcName, startX + 4, textY + 1); // Shadow
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.drawString(npcName, startX + 3, textY);

        // Kill Progress Text (Right-aligned)
        String progressText = formatNumberDynamic(killsTowardLimit, limit, availableTextWidth / 2, metrics);
        int textWidth = metrics.stringWidth(progressText);
        int textX = startX + barWidth - textWidth - 4; // Align to the right
        graphics.setColor(Color.BLACK);
        graphics.drawString(progressText, textX + 1, textY + 1); // Shadow
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.drawString(progressText, textX, textY);
    }

    /**
     * Truncates a string to fit within a specified width, appending an ellipsis if necessary.
     *
     * @param text      The text to truncate.
     * @param maxWidth  The maximum width allowed for the text.
     * @param metrics   The FontMetrics used to measure the text width.
     */
    private String truncateText(String text, int maxWidth, FontMetrics metrics) {
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }

        // Truncate text and add ellipsis
        for (int i = text.length() - 1; i > 0; i--) {
            String truncated = text.substring(0, i) + "...";
            if (metrics.stringWidth(truncated) <= maxWidth) {
                return truncated;
            }
        }
        return "..."; // Fallback
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

    /**
     * Dynamically formats the kill progress text (e.g., "125/500" or "125K/500K").
     * Expands or truncates the text based on available width.
     *
     * @param current   The current kill count.
     * @param limit     The kill limit for the NPC.
     * @param maxWidth  The maximum width allowed for the text.
     * @param metrics   The FontMetrics used to measure the text width.
     * @return A formatted string representing the kill progress, truncated if necessary.
     */
    private String formatNumberDynamic(int current, int limit, int maxWidth, FontMetrics metrics) {
        // Check if the full text fits
        String fullText = String.format("%d/%d", current, limit);
        if (metrics.stringWidth(fullText) <= maxWidth) {
            return fullText; // Return full text if it fits
        }

        // Use shortened format (e.g., 125K/500K)
        String shortText = formatNumber(current) + "/" + formatNumber(limit);
        if (metrics.stringWidth(shortText) <= maxWidth) {
            return shortText;
        }

        return "..."; // Fallback
    }
}
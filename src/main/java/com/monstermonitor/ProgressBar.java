package com.monstermonitor;

import javax.swing.*;
import java.awt.*;

/**
 * A custom progress bar component used in Monster Monitor.
 * This class displays a progress bar that fills up based on the current value relative to the max value.
 * It is used to show the progress towards an NPC's kill limit in the overlay.
 */
public class ProgressBar extends JPanel
{
    private int currentValue; // The current progress value (e.g., number of kills towards the limit)
    private int maxValue; // The maximum value that represents 100% progress (e.g., the kill limit)
    private Color barColor; // The color of the progress bar

    /**
     * Constructs a new ProgressBar with the specified values and color.
     *
     * @param currentValue The current progress value.
     * @param maxValue The maximum value for the progress bar.
     * @param barColor The color of the progress bar.
     */
    public ProgressBar(int currentValue, int maxValue, Color barColor)
    {
        this.currentValue = currentValue;
        this.maxValue = maxValue;
        this.barColor = barColor;
        setPreferredSize(new Dimension(150, 20)); // Set the preferred size of the progress bar
    }

    /**
     * Sets the current progress value and repaints the progress bar.
     *
     * @param currentValue The new current progress value.
     */
    public void setCurrentValue(int currentValue)
    {
        this.currentValue = currentValue;
        SwingUtilities.invokeLater(this::repaint); // Ensure repaint is called on the EDT
    }

    /**
     * Sets the maximum value for the progress bar and repaints it.
     *
     * @param maxValue The new maximum value.
     */
    public void setMaxValue(int maxValue)
    {
        this.maxValue = maxValue;
        SwingUtilities.invokeLater(this::repaint); // Ensure repaint is called on the EDT
    }

    /**
     * Sets the color of the progress bar and repaints it.
     *
     * @param barColor The new color for the progress bar.
     */
    public void setBarColor(Color barColor)
    {
        this.barColor = barColor;
        SwingUtilities.invokeLater(this::repaint); // Ensure repaint is called on the EDT
    }

    /**
     * Paints the progress bar component, drawing the background and the filled portion.
     *
     * @param g The Graphics object used for drawing.
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw the background of the progress bar (gray)
        g2d.setColor(Color.GRAY);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Draw the filled portion of the progress bar using the barColor
        if (maxValue > 0)
        {
            int width = (int) ((currentValue / (double) maxValue) * getWidth());
            g2d.setColor(barColor);
            g2d.fillRect(0, 0, width, getHeight());
        }

        // Draw the progress text (e.g., "50/100") centered within the bar
        String progressText = currentValue + "/" + maxValue;
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(progressText);
        int textHeight = fontMetrics.getAscent();
        int textX = (getWidth() - textWidth) / 2;
        int textY = (getHeight() + textHeight) / 2 - 2; // Adjust for better centering

        g2d.setColor(Color.BLACK); // Set text color to black for contrast
        g2d.drawString(progressText, textX, textY);
    }
}

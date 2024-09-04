package com.monstermonitor;

import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the main panel of the Monster Monitor plugin.
 * This panel displays a list of NPCs that the player has been tracking, including their kill counts and
 * various options for managing the tracked data.
 * The panel also includes a title, total kill count display, and scrollable list of NPCs.
 */

public class MonsterMonitorPanel extends PluginPanel
{
    private final MonsterMonitorPlugin plugin;
    private final JPanel npcListPanel;
    private JLabel totalKillCountLabel;
    private final Map<NpcData, Boolean> dropdownStates = new HashMap<>();

    /**
     * Constructs the MonsterMonitorPanel and initializes the UI components.
     *
     * @param plugin The MonsterMonitorPlugin instance that this panel is a part of.
     */
    @Inject
    public MonsterMonitorPanel(MonsterMonitorPlugin plugin)
    {
        this.plugin = plugin;

        // Define colors for the UI components
        Color backgroundColor = new Color(45, 45, 45); // Unified background color
        Color textColor = new Color(200, 200, 200); // Light gray color for text
        Color arrowColor = new Color(200, 150, 0); // Orange color for arrows

        // Set the panel's background color and layout
        setBackground(backgroundColor);
        setLayout(new BorderLayout());

        // Initialize and configure the panel title
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(backgroundColor);
        titlePanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Adjust padding

        JLabel titleLabel = new JLabel("Monster Monitor");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(new Color(200, 150, 0)); // Orange color for the title text

        ImageIcon icon = new ImageIcon(getClass().getResource("/net/runelite/client/plugins/MonsterMonitor/icon.png"));
        titleLabel.setIcon(icon);

        // Initialize and configure the total kill count label
        totalKillCountLabel = new JLabel("Total kills: 0");
        totalKillCountLabel.setFont(new Font("Arial", Font.BOLD, 12)); // Bold text
        totalKillCountLabel.setForeground(textColor); // Light gray for the total kills text
        totalKillCountLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Add the title and total kill count label to the title panel
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(totalKillCountLabel, BorderLayout.SOUTH);

        add(titlePanel, BorderLayout.NORTH); // Add the title panel at the top of the main panel

        // Initialize the NPC List Panel
        this.npcListPanel = new JPanel(new GridBagLayout()); // GridBagLayout for better dynamic resizing
        this.npcListPanel.setBackground(backgroundColor); // Matching background color

        // Add the npcListPanel to a JScrollPane for scrollable NPC list
        JScrollPane scrollPane = new JScrollPane(npcListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // No horizontal scrollbar
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI(arrowColor)); // Custom scrollbar UI
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(backgroundColor); // Matching background color

        add(scrollPane, BorderLayout.CENTER); // Add the scroll pane to the center of the main panel
    }

    /**
     * Collapses all NPC dropdowns in the panel.
     *
     * @param e The ActionEvent triggered by the collapse all action.
     */
    private void collapseAllDropdowns(ActionEvent e)
    {
        for (NpcData npcData : dropdownStates.keySet())
        {
            dropdownStates.put(npcData, false); // Set all dropdowns to collapsed
        }
        updatePanel(); // Refresh panel to apply changes
    }

    /**
     * Expands all NPC dropdowns in the panel.
     *
     * @param e The ActionEvent triggered by the expand all action.
     */
    private void expandAllDropdowns(ActionEvent e)
    {
        for (NpcData npcData : dropdownStates.keySet())
        {
            dropdownStates.put(npcData, true); // Set all dropdowns to expanded
        }
        updatePanel(); // Refresh panel to apply changes
    }

    /**
     * Updates the panel by refreshing the list of tracked NPCs.
     * This method repopulates the panel with NPCs and their current states.
     */
    public void updatePanel()
    {
        npcListPanel.removeAll(); // Clear the panel before adding updated NPC data

        // Get the list of tracked NPCs from the plugin
        List<NpcData> trackedNpcs = new ArrayList<>(plugin.getTrackedNpcs());

        // Reverse the order of the list to display the most recent NPC first
        Collections.reverse(trackedNpcs);

        int totalKills = 0;
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0); // Add padding between each NPC panel

        // Populate the panel with NPC data
        for (NpcData npcData : trackedNpcs)
        {
            MonsterMonitorBox npcPanel = new MonsterMonitorBox(plugin, npcData, dropdownStates.getOrDefault(npcData, false));
            npcPanel.setDropdownVisible(dropdownStates.getOrDefault(npcData, false)); // Restore dropdown state

            // Track the state of the dropdown for future refreshes
            dropdownStates.put(npcData, npcPanel.isDropdownVisible());

            totalKills += npcData.getTotalKillCount(); // Sum the total kills for the label

            gbc.gridy++;
            npcListPanel.add(npcPanel, gbc); // Add NPC panel with GridBagLayout constraints

            // Right-click context menu for resetting and global options
            JPopupMenu contextMenu = new JPopupMenu();
            JMenuItem resetMenuItem = new JMenuItem("Reset");
            resetMenuItem.addActionListener(ev -> {
                plugin.getLogger().getNpcLog().remove(npcData.getNpcName()); // Remove the NPC from the log
                plugin.getLogger().saveLog(); // Save the updated log
                plugin.updateOverlay(); // Update the overlay
                updatePanel(); // Refresh the panel
            });

            JMenuItem resetKillLimitMenuItem = new JMenuItem("Reset Kill Limit");
            resetKillLimitMenuItem.addActionListener(ev -> {
                npcData.setKillLimit(0); // Reset the kill limit to zero
                npcData.resetKillCountForLimit(); // Reset the kill count towards the limit
                plugin.getLogger().updateNpcData(npcData); // Save the updated data
                plugin.updateOverlay(); // Update the overlay
                updatePanel(); // Refresh the panel
            });

            JMenuItem collapseAllItem = new JMenuItem("Collapse All");
            collapseAllItem.addActionListener(this::collapseAllDropdowns);

            JMenuItem expandAllItem = new JMenuItem("Expand All");
            expandAllItem.addActionListener(this::expandAllDropdowns);

            contextMenu.add(resetMenuItem);
            contextMenu.add(resetKillLimitMenuItem);
            contextMenu.addSeparator(); // Separator for clarity
            contextMenu.add(collapseAllItem);
            contextMenu.add(expandAllItem);

            npcPanel.setComponentPopupMenu(contextMenu);

            // Add mouse listener to show context menu on right-click
            npcPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        contextMenu.show(npcPanel, e.getX(), e.getY());
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        contextMenu.show(npcPanel, e.getX(), e.getY());
                    }
                }
            });
        }

        // Update the total kills label
        totalKillCountLabel.setText("Total kills: " + totalKills);

        // Refresh the panel
        revalidate();
        repaint();
    }

    /**
     * Custom UI class for the scroll bar used in the MonsterMonitorPanel.
     * This class customizes the appearance of the scroll bar to match the plugin's theme.
     */
    private static class CustomScrollBarUI extends BasicScrollBarUI
    {
        private final Color arrowColor;

        /**
         * Constructs a CustomScrollBarUI with the specified arrow color.
         *
         * @param arrowColor The color of the arrows on the scrollbar.
         */
        public CustomScrollBarUI(Color arrowColor)
        {
            this.arrowColor = arrowColor;
        }

        @Override
        protected void configureScrollBarColors()
        {
            this.thumbColor = new Color(200, 150, 0); // Adjust scrollbar thumb color to orange
            this.trackColor = new Color(30, 30, 30); // Darker gray for the scrollbar track
        }

        @Override
        protected JButton createDecreaseButton(int orientation)
        {
            return createZeroButton(); // No arrow buttons for a clean look
        }

        @Override
        protected JButton createIncreaseButton(int orientation)
        {
            return createZeroButton(); // No arrow buttons for a clean look
        }

        /**
         * Creates a zero-sized button to effectively hide the scroll bar buttons.
         *
         * @return A JButton with zero dimensions.
         */
        private JButton createZeroButton()
        {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0)); // Effectively hides the buttons
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }
    }
}

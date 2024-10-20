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
 * Represents the main panel of Monster Monitor.
 * This panel displays a list of NPCs that the player has been tracking, including their kill counts and
 * various options for managing the tracked data.
 * The panel also includes a title, total kill count display, and scrollable list of NPCs.
 */
public class MonsterMonitorPanel extends PluginPanel
{
    private final MonsterMonitorPlugin plugin;
    private final JPanel npcListPanel;
    private final JPanel fillerBox; // The filler box at the bottom
    private JLabel totalKillCountLabel;
    private final Map<NpcData, Boolean> dropdownStates = new HashMap<>(); // Stores the state of dropdowns

    @Inject
    public MonsterMonitorPanel(MonsterMonitorPlugin plugin)
    {
        this.plugin = plugin;

        // Set layout using BorderLayout for the main panel
        setLayout(new BorderLayout());

        // Define colors for the UI components
        Color backgroundColor = new Color(45, 45, 45); // Unified background color
        Color textColor = new Color(200, 200, 200); // Light gray color for text

        // Set the panel's background color
        setBackground(backgroundColor);

        // Initialize and configure the panel title
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(backgroundColor);

        // Title label, now centered in the middle of the panel
        JLabel titleLabel = new JLabel("Monster Monitor", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(new Color(200, 150, 0)); // Orange color for the title text
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        ImageIcon icon = new ImageIcon(getClass().getResource("/net/runelite/client/plugins/MonsterMonitor/icon.png"));
        titleLabel.setIcon(icon);

        // Initialize and configure the total kill count label
        totalKillCountLabel = new JLabel("Total kills: 0");
        totalKillCountLabel.setFont(new Font("Arial", Font.BOLD, 12)); // Bold text
        totalKillCountLabel.setForeground(textColor); // Light gray for the total kills text
        totalKillCountLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Add the title and total kill count to the title panel
        titlePanel.add(titleLabel, BorderLayout.NORTH); // Place title at the top
        titlePanel.add(totalKillCountLabel, BorderLayout.SOUTH); // Below the title

        // Add the title panel to the top of the main panel (fixed)
        add(titlePanel, BorderLayout.NORTH); // Title panel remains fixed at the top

        // Initialize the NPC List Panel using GridBagLayout
        this.npcListPanel = new JPanel(new GridBagLayout());
        this.npcListPanel.setBackground(backgroundColor); // Matching background color

        // Create the scroll pane for the NPC list to enable scrolling
        JScrollPane scrollPane = new JScrollPane(npcListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(2, 0)); // Custom narrow scrollbar
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI(new Color(200, 150, 0))); // Custom scroll bar UI
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // Add the scroll pane to the center of the main panel
        add(scrollPane, BorderLayout.CENTER);

        // Initialize the dynamic filler box that adjusts based on NPC count
        this.fillerBox = new JPanel();
        fillerBox.setBackground(backgroundColor); // Same background as the panel (invisible)
        fillerBox.setPreferredSize(new Dimension(0, 200)); // Initial height is large
    }

    /**
     * Captures the current dropdown state for all NPC panels before refreshing.
     * This method stores whether each dropdown in the NPC list is expanded or collapsed.
     */
    private void captureDropdownStates() {
        Component[] components = npcListPanel.getComponents();
        for (Component component : components) {
            if (component instanceof MonsterMonitorBox) {
                MonsterMonitorBox box = (MonsterMonitorBox) component;
                NpcData npcData = box.getNpcData();
                dropdownStates.put(npcData, box.isDropdownVisible());
            }
        }
    }

    /**
     * Restores the dropdown states after the panel is refreshed.
     * This method restores the expanded/collapsed state of each dropdown after a panel update.
     */
    private void restoreDropdownStates() {
        Component[] components = npcListPanel.getComponents();
        for (Component component : components) {
            if (component instanceof MonsterMonitorBox) {
                MonsterMonitorBox box = (MonsterMonitorBox) component;
                NpcData npcData = box.getNpcData();
                box.setDropdownVisible(dropdownStates.getOrDefault(npcData, false)); // Restore the dropdown state
            }
        }
    }

    /**
     * Updates the panel by refreshing the list of tracked NPCs.
     * This method repopulates the panel with NPCs and their current states, updates the kill counts, and restores dropdown visibility.
     */
    public void updatePanel()
    {
        captureDropdownStates(); // Capture dropdown states before refreshing

        npcListPanel.removeAll(); // Clear the panel before adding updated NPC data

        // Get the list of tracked NPCs from the plugin
        List<NpcData> trackedNpcs = new ArrayList<>(plugin.getTrackedNpcs());

        // Reverse the order of the list to display the most recent NPC first
        Collections.reverse(trackedNpcs);

        int totalKills = 0;
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0; // Ensure horizontal expansion
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0); // Add padding between NPC panels

        // Populate the panel with NPC data
        for (NpcData npcData : trackedNpcs)
        {
            MonsterMonitorBox npcPanel = new MonsterMonitorBox(plugin, npcData, dropdownStates.getOrDefault(npcData, false));

            totalKills += npcData.getTotalKillCount(); // Sum the total kills for the label

            npcListPanel.add(npcPanel, gbc); // Add NPC panel with GridBagLayout constraints

            // Add right-click context menu
            JPopupMenu contextMenu = createContextMenu(npcData);
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

            gbc.gridy++; // Increment grid position for the next NPC panel
        }

        // Adjust filler box size and behavior based on the number of NPCs
        adjustFillerBox(trackedNpcs.size());

        // Add the filler box to the bottom
        npcListPanel.add(fillerBox, gbc);

        // Update the total kills label
        totalKillCountLabel.setText("Total kills: " + totalKills);

        npcListPanel.revalidate();
        npcListPanel.repaint();

        restoreDropdownStates(); // Restore dropdown states after refreshing
    }

    /**
     * Adjusts the size and visibility of the filler box based on the number of NPCs in the list.
     *
     * @param npcCount The current number of NPCs in the panel.
     */
    private void adjustFillerBox(int npcCount)
    {
        int minHeight = 100; // Minimum height for filler box
        int maxHeight = 400; // Maximum height

        if (npcCount > 15) // If there are too many NPCs, make filler box disappear
        {
            fillerBox.setPreferredSize(new Dimension(0, 0));
        }
        else
        {
            int dynamicHeight = maxHeight - (npcCount * 15); // Shrink based on number of NPCs
            dynamicHeight = Math.max(minHeight, dynamicHeight); // Ensure it doesn't get too small
            fillerBox.setPreferredSize(new Dimension(0, dynamicHeight));
        }

        fillerBox.revalidate();
        fillerBox.repaint();
    }

    /**
     * Creates a context menu with options like Reset, Reset Kill Limit, Collapse All, and Expand All.
     *
     * @param npcData The NPC data associated with the context menu.
     * @return The constructed JPopupMenu.
     */
    private JPopupMenu createContextMenu(NpcData npcData)
    {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem resetMenuItem = new JMenuItem("Reset");
        resetMenuItem.addActionListener(ev -> {
            plugin.logger.getNpcLog().remove(npcData.getNpcName()); // Remove the NPC from the log
            plugin.logger.saveLog(); // Save the updated log
            plugin.updateOverlay(); // Update the overlay
            updatePanel(); // Refresh the panel
        });

        JMenuItem resetKillLimitMenuItem = new JMenuItem("Reset Kill Limit");
        resetKillLimitMenuItem.addActionListener(ev -> {
            npcData.setKillLimit(0); // Reset the kill limit to zero
            npcData.resetKillCountForLimit(); // Reset the kill count towards the limit
            plugin.logger.updateNpcData(npcData); // Save the updated data
            plugin.updateOverlay(); // Update the overlay
            updatePanel(); // Refresh the panel
        });

        JMenuItem collapseAllItem = new JMenuItem("Collapse All");
        collapseAllItem.addActionListener(this::collapseAllDropdowns);

        JMenuItem expandAllItem = new JMenuItem("Expand All");
        expandAllItem.addActionListener(this::expandAllDropdowns);

        contextMenu.add(resetMenuItem);
        contextMenu.add(resetKillLimitMenuItem);
        contextMenu.addSeparator();
        contextMenu.add(collapseAllItem);
        contextMenu.add(expandAllItem);

        return contextMenu;
    }

    /**
     * Collapses all NPC dropdowns by triggering the toggleOptionsVisibility method, simulating arrow clicks.
     * This method is triggered by the 'Collapse All' context menu option.
     *
     * @param e The ActionEvent triggered by the collapse all action.
     */
    private void collapseAllDropdowns(ActionEvent e)
    {
        Component[] components = npcListPanel.getComponents();
        for (Component component : components) {
            if (component instanceof MonsterMonitorBox) {
                MonsterMonitorBox box = (MonsterMonitorBox) component;
                if (box.isDropdownVisible()) {
                    box.setDropdownVisible(false);  // Collapse if currently expanded
                }
            }
        }
        npcListPanel.revalidate();
        npcListPanel.repaint();
    }

    /**
     * Expands all NPC dropdowns by triggering the toggleOptionsVisibility method, simulating arrow clicks.
     * This method is triggered by the 'Expand All' context menu option.
     *
     * @param e The ActionEvent triggered by the expand all action.
     */
    private void expandAllDropdowns(ActionEvent e)
    {
        Component[] components = npcListPanel.getComponents();
        for (Component component : components) {
            if (component instanceof MonsterMonitorBox) {
                MonsterMonitorBox box = (MonsterMonitorBox) component;
                if (!box.isDropdownVisible()) {
                    box.setDropdownVisible(true);  // Expand if currently collapsed
                }
            }
        }
        npcListPanel.revalidate();
        npcListPanel.repaint();
    }

    /**
     * Custom UI class for the scroll bar used in the MonsterMonitorPanel.
     * This class customizes the appearance of the scroll bar to match the plugin's theme.
     */
    public static class CustomScrollBarUI extends BasicScrollBarUI
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
package com.monstermonitor;

import javax.swing.*;
import java.awt.*;

/**
 * Represents a UI component (box) in Monster Monitor that displays information
 * about a specific NPC. The box shows the NPC's name, kill count, and provides options to
 * set kill limits, enable notifications, and other related settings.
 * This component also supports expanding and collapsing additional options via a dropdown.
 */

public class MonsterMonitorBox extends JPanel
{
    private final MonsterMonitorPlugin plugin;
    private final NpcData npcData; // Now accessible via a getter
    private boolean optionsVisible; // Tracks whether the dropdown is open
    private final JPanel optionsPanel;
    private final JButton toggleButton;
    private JCheckBox setLimitCheckbox;
    private JSpinner limitSpinner;
    private JCheckBox notifyCheckbox; // Moved to be a class-level field for better control

    /**
     * Constructs the MonsterMonitorBox component and initializes its UI elements.
     *
     * @param plugin The MonsterMonitorPlugin instance that this box is part of.
     * @param npcData The NpcData associated with the NPC displayed in this box.
     * @param initialDropdownState The initial state of the dropdown (expanded or collapsed).
     */
    public MonsterMonitorBox(MonsterMonitorPlugin plugin, NpcData npcData, boolean initialDropdownState)
    {
        this.plugin = plugin;
        this.npcData = npcData;
        this.optionsVisible = initialDropdownState; // Use the passed state to set initial dropdown visibility

        // Set up the panel with a dark background and padding
        setLayout(new BorderLayout());
        setBackground(new Color(60, 60, 60));
        setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // NPC name and kill count label, aligned to the left
        JLabel npcNameLabel = new JLabel(npcData.getNpcName() + " x " + npcData.getTotalKillCount());
        npcNameLabel.setForeground(new Color(200, 200, 200)); // Light gray text
        npcNameLabel.setFont(new Font("Arial", Font.BOLD, 12)); // Bold text
        npcNameLabel.setHorizontalAlignment(SwingConstants.LEFT);
        npcNameLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5)); // Adjust padding inside the label
        add(npcNameLabel, BorderLayout.WEST);

        // Toggle button for the dropdown, aligned to the right
        toggleButton = new JButton(optionsVisible ? "▲" : "▼");
        toggleButton.setPreferredSize(new Dimension(16, 16));
        toggleButton.setBorder(BorderFactory.createEmptyBorder());
        toggleButton.setContentAreaFilled(false);
        toggleButton.setFocusPainted(false);
        toggleButton.setForeground(new Color(200, 150, 0)); // Orange arrow color

        toggleButton.addActionListener(e -> toggleOptionsVisibility());
        add(toggleButton, BorderLayout.EAST);

        // Create the options panel
        optionsPanel = createOptionsPanel();
        if (optionsVisible) {
            add(optionsPanel, BorderLayout.SOUTH); // Show the options panel if the dropdown is initially open
        }
    }

    /**
     * Toggles the visibility of the options panel (dropdown) in the NPC box.
     * Expands or collapses the options panel and updates the toggle button text.
     */
    private void toggleOptionsVisibility()
    {
        if (optionsVisible)
        {
            remove(optionsPanel); // Remove the options panel if it's visible
            toggleButton.setText("▼"); // Update button to indicate collapse
        }
        else
        {
            add(optionsPanel, BorderLayout.SOUTH); // Add options below the NPC panel
            toggleButton.setText("▲"); // Update button to indicate expansion
        }

        optionsVisible = !optionsVisible;

        revalidate();
        getParent().revalidate(); // Revalidate the parent to correctly adjust the layout
        getParent().repaint(); // Repaint parent to ensure correct display
    }

    /**
     * Creates the options panel that allows users to set a kill limit, enable notifications,
     * and interact with other related settings for the NPC.
     *
     * @return A JPanel containing the options controls.
     */
    private JPanel createOptionsPanel()
    {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0)); // Tighter layout with reduced margins
        panel.setBackground(new Color(60, 60, 60));

        // SET LIMIT Checkbox
        setLimitCheckbox = new JCheckBox("Set Limit");
        setLimitCheckbox.setForeground(Color.LIGHT_GRAY);
        setLimitCheckbox.setBackground(new Color(60, 60, 60));
        setLimitCheckbox.setFont(new Font("Arial", Font.BOLD, 12)); // Bold text
        setLimitCheckbox.setSelected(npcData.isLimitSet());
        setLimitCheckbox.addActionListener(e -> {
            boolean isChecked = setLimitCheckbox.isSelected();
            npcData.setLimitSet(isChecked);
            limitSpinner.setEnabled(isChecked); // Enable/disable spinner based on checkbox state
            notifyCheckbox.setEnabled(isChecked); // Enable/disable notify checkbox based on limit checkbox state

            if (!isChecked) {
                npcData.setKillLimit(0);  // Remove the kill limit if unchecked
                npcData.setNotifyOnLimit(false); // Uncheck the notify box
                notifyCheckbox.setSelected(false); // Ensure notify checkbox is unchecked
                plugin.updateOverlay(); // Update the overlay to remove this NPC
            }
            plugin.getLogger().updateNpcData(npcData); // Save state
            updatePanelDirectly(); // Directly refresh the panel
        });

        // JSpinner for Setting Kill Limit
        int currentLimit = npcData.getKillLimit(); // Load saved kill limit value
        limitSpinner = new JSpinner(new SpinnerNumberModel(
                currentLimit, 0, 999999, 1)); // Spinner with enough space for 6 digits
        limitSpinner.setPreferredSize(new Dimension(65, 20)); // Increase width to fit large numbers
        limitSpinner.setEnabled(npcData.isLimitSet()); // Enable only if the limit is set
        limitSpinner.setFont(new Font("Arial", Font.BOLD, 12)); // Bold text
        limitSpinner.addChangeListener(e -> {
            int limit = (Integer) limitSpinner.getValue();
            int killCountForLimit = npcData.getKillCountForLimit(); // Preserve progress
            npcData.setKillLimit(limit);
            npcData.setKillCountForLimit(killCountForLimit); // Set the preserved progress
            plugin.getLogger().updateNpcData(npcData); // Save the NPC data with its new limit
            plugin.updateOverlay(); // Ensure overlay updates with new limit
        });

        // NOTIFY Checkbox
        notifyCheckbox = new JCheckBox("Notify");
        notifyCheckbox.setForeground(Color.LIGHT_GRAY);
        notifyCheckbox.setBackground(new Color(60, 60, 60));
        notifyCheckbox.setFont(new Font("Arial", Font.BOLD, 12)); // Bold text
        notifyCheckbox.setSelected(npcData.isNotifyOnLimit());
        notifyCheckbox.setEnabled(npcData.isLimitSet()); // Disable if limit is not set
        notifyCheckbox.addActionListener(e -> {
            boolean notifyChecked = notifyCheckbox.isSelected();
            npcData.setNotifyOnLimit(notifyChecked);
            plugin.getLogger().updateNpcData(npcData); // Save state
        });

        panel.add(setLimitCheckbox);
        panel.add(limitSpinner);
        panel.add(notifyCheckbox);

        return panel;
    }

    /**
     * Updates the parent panel directly to reflect any changes made in this component.
     * This ensures that the UI remains consistent and updated with the latest data.
     */
    private void updatePanelDirectly()
    {
        // Directly access the parent panel and refresh it
        Container parent = getParent();
        if (parent instanceof MonsterMonitorPanel) {
            ((MonsterMonitorPanel) parent).updatePanel();
        }
    }

    /**
     * Checks if the dropdown options panel is currently visible.
     *
     * @return True if the dropdown is visible, otherwise false.
     */
    public boolean isDropdownVisible()
    {
        return optionsVisible;
    }

    /**
     * Sets the visibility of the dropdown options panel to the specified state.
     *
     * @param visible True to make the dropdown visible, false to hide it.
     */
    public void setDropdownVisible(boolean visible)
    {
        if (visible != optionsVisible)
        {
            toggleOptionsVisibility(); // Ensure dropdown matches the desired state
        }
    }

    /**
     * Returns the NpcData associated with this box.
     *
     * @return The NpcData object.
     */
    public NpcData getNpcData()
    {
        return npcData;
    }
}
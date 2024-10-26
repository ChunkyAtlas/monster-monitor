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
    private final NpcData npcData;
    private boolean optionsVisible; // Tracks whether the dropdown is open
    private final JPanel optionsPanel;
    private JButton toggleButton;
    private JCheckBox setLimitCheckbox;
    private JSpinner limitSpinner;
    private JCheckBox notifyCheckbox;

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
        this.optionsVisible = initialDropdownState;

        setLayout(new BorderLayout());
        setBackground(new Color(60, 60, 60));
        setBorder(BorderFactory.createLineBorder(Color.GRAY));

        add(createNpcNameLabel(), BorderLayout.WEST);
        add(createToggleButton(), BorderLayout.EAST);

        // Create the options panel
        optionsPanel = createOptionsPanel();
        if (optionsVisible) {
            add(optionsPanel, BorderLayout.SOUTH);
        }

        // Set the initial state of the checkbox and other UI elements
        refreshUI();
        updateOptionStateBasedOnIgnored();
    }

    /**
     * Creates the NPC name label displaying the NPC's name and kill count.
     *
     * @return The JLabel for the NPC's name.
     */
    private JLabel createNpcNameLabel()
    {
        JLabel npcNameLabel = new JLabel(npcData.getNpcName() + " x " + npcData.getTotalKillCount());
        npcNameLabel.setForeground(new Color(200, 200, 200));
        npcNameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        npcNameLabel.setHorizontalAlignment(SwingConstants.LEFT);
        npcNameLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        return npcNameLabel;
    }

    /**
     * Creates the toggle button for expanding or collapsing the options panel.
     *
     * @return The JButton used to toggle the dropdown visibility.
     */
    private JButton createToggleButton()
    {
        toggleButton = new JButton(optionsVisible ? "▲" : "▼");
        toggleButton.setPreferredSize(new Dimension(16, 16));
        toggleButton.setBorder(BorderFactory.createEmptyBorder());
        toggleButton.setContentAreaFilled(false);
        toggleButton.setFocusPainted(false);
        toggleButton.setForeground(new Color(200, 150, 0));

        toggleButton.addActionListener(e -> toggleOptionsVisibility());
        return toggleButton;
    }

    /**
     * Toggles the visibility of the options panel (dropdown) in the NPC box.
     * Expands or collapses the options panel and updates the toggle button text.
     */
    private void toggleOptionsVisibility() {
        SwingUtilities.invokeLater(() -> {
            if (optionsVisible) {
                remove(optionsPanel);
                toggleButton.setText("▼");
            } else {
                add(optionsPanel, BorderLayout.SOUTH);
                toggleButton.setText("▲");
            }
            optionsVisible = !optionsVisible;
            revalidate();
            getParent().revalidate();
            getParent().repaint();
        });
    }

    /**
     * Creates the options panel that allows users to set a kill limit, enable notifications,
     * and interact with other related settings for the NPC.
     *
     * @return A JPanel containing the options controls.
     */
    private JPanel createOptionsPanel()
    {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(60, 60, 60));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);

        setLimitCheckbox = createSetLimitCheckbox();
        limitSpinner = createLimitSpinner();
        notifyCheckbox = createNotifyCheckbox();

        gbc.gridx = 0;
        gbc.weightx = 0.1;
        panel.add(setLimitCheckbox, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.3;
        panel.add(limitSpinner, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.1;
        panel.add(notifyCheckbox, gbc);

        return panel;
    }

    /**
     * Creates the checkbox to set a kill limit.
     *
     * @return A JCheckBox to enable or disable kill limit.
     */
    private JCheckBox createSetLimitCheckbox()
    {
        JCheckBox checkBox = new JCheckBox("Set Limit");
        checkBox.setForeground(Color.LIGHT_GRAY);
        checkBox.setBackground(new Color(60, 60, 60));
        checkBox.setFont(new Font("SansSerif", Font.BOLD, 12));
        checkBox.setSelected(npcData.isLimitSet());
        checkBox.addActionListener(e -> handleSetLimitCheckbox());
        return checkBox;
    }

    /**
     * Creates the spinner to adjust the kill limit.
     *
     * @return A JSpinner for selecting the kill limit.
     */
    private JSpinner createLimitSpinner()
    {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(npcData.getKillLimit(), 0, 9999999, 1)) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(70, 20);
            }

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(90, 20);
            }
        };
        spinner.setEnabled(npcData.isLimitSet());
        spinner.setFont(new Font("SansSerif", Font.BOLD, 12));
        spinner.addChangeListener(e -> {
            int limit = (Integer) spinner.getValue();
            npcData.setKillLimit(limit);
            plugin.logger.updateNpcData(npcData);
            plugin.updateOverlay();
        });
        return spinner;
    }

    /**
     * Creates the checkbox to enable notifications for reaching the kill limit.
     *
     * @return A JCheckBox to enable or disable notifications.
     */
    private JCheckBox createNotifyCheckbox()
    {
        JCheckBox checkBox = new JCheckBox("Notify");
        checkBox.setForeground(Color.LIGHT_GRAY);
        checkBox.setBackground(new Color(60, 60, 60));
        checkBox.setFont(new Font("SansSerif", Font.BOLD, 12));
        checkBox.setSelected(npcData.isNotifyOnLimit());
        checkBox.setEnabled(npcData.isLimitSet());
        checkBox.addActionListener(e -> {
            npcData.setNotifyOnLimit(checkBox.isSelected());
            plugin.logger.updateNpcData(npcData);
        });
        return checkBox;
    }

    /**
     * Handles the action when the set limit checkbox is toggled.
     */
    private void handleSetLimitCheckbox()
    {
        boolean isChecked = setLimitCheckbox.isSelected();
        npcData.setLimitSet(isChecked);
        limitSpinner.setEnabled(isChecked);
        notifyCheckbox.setEnabled(isChecked);

        if (!isChecked) {
            npcData.setKillLimit(0);
            npcData.resetKillCountForLimit();
            plugin.updateOverlay();
        } else {
            npcData.setKillLimit((Integer) limitSpinner.getValue());
        }
        plugin.logger.updateNpcData(npcData);
        updatePanelDirectly();
    }

    /**
     * Refreshes the UI elements based on the current state of the npcData.
     */
    private void refreshUI()
    {
        setLimitCheckbox.setSelected(npcData.isLimitSet());
        limitSpinner.setEnabled(npcData.isLimitSet());
        notifyCheckbox.setEnabled(npcData.isLimitSet());
        revalidate();
        repaint();
    }

    /**
     * Disables or enables option controls based on whether the NPC is ignored.
     */
    private void updateOptionStateBasedOnIgnored() {
        SwingUtilities.invokeLater(() -> {
            boolean isIgnored = npcData.isIgnored();
            setLimitCheckbox.setEnabled(!isIgnored);
            limitSpinner.setEnabled(!isIgnored && npcData.isLimitSet());
            notifyCheckbox.setEnabled(!isIgnored && npcData.isLimitSet());

            setBackground(isIgnored ? new Color(50, 50, 50) : new Color(60, 60, 60));
            toggleButton.setBackground(isIgnored ? new Color(50, 50, 50) : new Color(60, 60, 60));
            toggleButton.setForeground(isIgnored ? Color.GRAY : new Color(200, 150, 0));

            revalidate();
            repaint();
        });
    }

    /**
     * Updates the parent panel directly to reflect any changes made in this component.
     */
    private void updatePanelDirectly()
    {
        Container parent = getParent();
        if (parent instanceof MonsterMonitorPanel) {
            ((MonsterMonitorPanel) parent).updatePanel();
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
            toggleOptionsVisibility();
        }
    }
}

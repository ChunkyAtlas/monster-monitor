package com.monstermonitor;

import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Represents the main panel of Monster Monitor.
 * This panel displays a list of NPCs that the player has been tracking, including their kill counts and
 * various options for managing the tracked data, including ignore/monitor status.
 * The panel also includes a title, total kill count display, and scrollable list of NPCs.
 */
public class MonsterMonitorPanel extends PluginPanel {
    private final MonsterMonitorPlugin plugin;
    private final MonsterMonitorSearchBar searchBar;
    private final JPanel npcListPanel;
    private final JPanel ignoredNpcListPanel;
    private final JPanel fillerBox; // The filler box at the bottom
    private JLabel totalKillCountLabel;
    private final Map<NpcData, Boolean> dropdownStates = new HashMap<>(); // Stores the state of dropdowns
    private String searchText = "";

    @Inject
    public MonsterMonitorPanel(MonsterMonitorPlugin plugin) {
        this.plugin = plugin;

        setLayout(new BorderLayout());
        Color backgroundColor = new Color(45, 45, 45);
        Color textColor = new Color(200, 200, 200);
        setBackground(backgroundColor);

        // Title Panel setup
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(backgroundColor);

        JLabel titleLabel = new JLabel("Monster Monitor", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(new Color(200, 150, 0));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        ImageIcon icon = new ImageIcon(getClass().getResource("/net/runelite/client/plugins/MonsterMonitor/icon.png"));
        titleLabel.setIcon(icon);

        totalKillCountLabel = new JLabel("Total kills: 0");
        totalKillCountLabel.setFont(new Font("Arial", Font.BOLD, 12));
        totalKillCountLabel.setForeground(textColor);
        totalKillCountLabel.setHorizontalAlignment(SwingConstants.CENTER);

        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(totalKillCountLabel, BorderLayout.SOUTH);

        // Add the title panel at the top
        add(titlePanel, BorderLayout.NORTH);

        // Initialize the search bar and add it below the title panel
        searchBar = new MonsterMonitorSearchBar(searchText -> {
            this.searchText = searchText.toLowerCase();
            updatePanel();
        });
        add(searchBar, BorderLayout.CENTER);

        // NPC List Panels setup
        this.npcListPanel = new JPanel(new GridBagLayout());
        this.npcListPanel.setBackground(backgroundColor);
        this.npcListPanel.setBorder(BorderFactory.createEmptyBorder());
        this.ignoredNpcListPanel = new JPanel(new GridBagLayout());
        this.ignoredNpcListPanel.setBackground(backgroundColor);
        add(npcListPanel, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(() -> {
            Component parent = this.getParent();
            while (parent != null && !(parent instanceof JScrollPane)) {
                parent = parent.getParent();
            }
            if (parent instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) parent;
                scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI(new Color(200, 150, 0)));
                scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(2, 0));
                scrollPane.getVerticalScrollBar().setUnitIncrement(6);
            }
        });

        this.fillerBox = new JPanel();
        fillerBox.setBackground(backgroundColor); // Same background as the panel (invisible)
        fillerBox.setPreferredSize(new Dimension(0, 200)); // Initial height is large
    }

    /**
     * Captures the current dropdown state for all NPC panels before refreshing.
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
     */
    private void restoreDropdownStates() {
        Component[] components = npcListPanel.getComponents();
        for (Component component : components) {
            if (component instanceof MonsterMonitorBox) {
                MonsterMonitorBox box = (MonsterMonitorBox) component;
                NpcData npcData = box.getNpcData();
                box.setDropdownVisible(dropdownStates.getOrDefault(npcData, false));
            }
        }
    }

    /**
     * Updates the panel by refreshing the list of tracked and ignored NPCs.
     */
    public void updatePanel() {
        SwingUtilities.invokeLater(() -> {
            captureDropdownStates();

            npcListPanel.removeAll();
            ignoredNpcListPanel.removeAll();

            List<NpcData> trackedNpcs = new ArrayList<>(plugin.getTrackedNpcs());
            List<NpcData> ignoredNpcs = new ArrayList<>();

            for (NpcData npcData : trackedNpcs) {
                if (npcData.isIgnored()) {
                    ignoredNpcs.add(npcData);
                }
            }

            // Apply search filter to both tracked and ignored NPCs
            trackedNpcs.removeIf(npcData -> !npcData.getNpcName().toLowerCase().contains(searchText));
            ignoredNpcs.removeIf(npcData -> !npcData.getNpcName().toLowerCase().contains(searchText));

            Collections.reverse(trackedNpcs);
            Collections.reverse(ignoredNpcs);

            int totalKills = 0;
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 0, 5, 0);

            for (NpcData npcData : trackedNpcs) {
                if (!npcData.isIgnored()) {
                    MonsterMonitorBox npcPanel = new MonsterMonitorBox(plugin, npcData, dropdownStates.getOrDefault(npcData, false));
                    totalKills += npcData.getTotalKillCount();
                    npcListPanel.add(npcPanel, gbc);
                    addContextMenuToNpcPanel(npcPanel, npcData);
                    gbc.gridy++;
                }
            }

            if (!ignoredNpcs.isEmpty()) {
                JLabel ignoredLabel = new JLabel("Ignored NPCs", SwingConstants.CENTER);
                ignoredLabel.setForeground(new Color(180, 180, 180));
                ignoredLabel.setFont(new Font("Arial", Font.BOLD, 12));
                npcListPanel.add(ignoredLabel, gbc);
                gbc.gridy++;

                for (NpcData npcData : ignoredNpcs) {
                    MonsterMonitorBox npcPanel = new MonsterMonitorBox(plugin, npcData, dropdownStates.getOrDefault(npcData, false));
                    npcPanel.setBackground(new Color(50, 50, 50));
                    npcListPanel.add(npcPanel, gbc);
                    addContextMenuToNpcPanel(npcPanel, npcData);
                    gbc.gridy++;
                }
            }

            adjustFillerBox(trackedNpcs.size() + ignoredNpcs.size());
            npcListPanel.add(fillerBox, gbc);

            totalKillCountLabel.setText("Total kills: " + totalKills);
            npcListPanel.revalidate();
            npcListPanel.repaint();

            restoreDropdownStates();
        });
    }

    /**
     * Adds a context menu to the given NPC panel.
     */
    private void addContextMenuToNpcPanel(MonsterMonitorBox npcPanel, NpcData npcData) {
        JPopupMenu contextMenu = createContextMenu(npcData);
        npcPanel.setComponentPopupMenu(contextMenu);
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

    private void adjustFillerBox(int npcCount) {
        int minHeight = 100;
        int maxHeight = 400;

        if (npcCount > 15) {
            fillerBox.setPreferredSize(new Dimension(0, 0));
        } else {
            int dynamicHeight = maxHeight - (npcCount * 15);
            dynamicHeight = Math.max(minHeight, dynamicHeight);
            fillerBox.setPreferredSize(new Dimension(0, dynamicHeight));
        }

        fillerBox.revalidate();
        fillerBox.repaint();
    }

    private JPopupMenu createContextMenu(NpcData npcData) {
        JPopupMenu contextMenu = new JPopupMenu();

        if (!npcData.isIgnored()) {
            JMenuItem ignoreMenuItem = new JMenuItem("Ignore NPC");
            ignoreMenuItem.addActionListener(ev -> {
                npcData.setIgnored(true);
                plugin.logger.updateNpcData(npcData);
                plugin.updateOverlay();
                updatePanel();
            });
            contextMenu.add(ignoreMenuItem);
        }

        JMenu monitorMenu = new JMenu("Monitor");
        String[] presets = {"1", "10", "100", "1000"};
        for (String preset : presets) {
            JMenuItem presetItem = new JMenuItem(preset);
            presetItem.addActionListener(ev -> {
                npcData.setKillLimit(Integer.parseInt(preset));
                npcData.setLimitSet(true); // Ensure the Set Limit checkbox is checked
                npcData.setIgnored(false);
                plugin.logger.updateNpcData(npcData);
                plugin.updateOverlay();
                updatePanel();
            });
            monitorMenu.add(presetItem);
        }

        // Add custom limit option
        JMenuItem customMonitorItem = new JMenuItem("Custom...");
        customMonitorItem.addActionListener(ev -> {
            String input = JOptionPane.showInputDialog("Enter custom kill limit:");
            try {
                int customLimit = Integer.parseInt(input);
                npcData.setKillLimit(customLimit);
                npcData.setLimitSet(true); // Ensure the Set Limit checkbox is checked
                npcData.setIgnored(false);
                plugin.logger.updateNpcData(npcData);
                plugin.updateOverlay();
                updatePanel();
            } catch (NumberFormatException ignored) {
                // Handle invalid input silently
            }
        });
        monitorMenu.add(customMonitorItem);
        contextMenu.add(monitorMenu);

        JMenuItem resetMenuItem = new JMenuItem("Clear NPC Data");
        resetMenuItem.addActionListener(ev -> {
            plugin.logger.removeNpcFromLog(npcData.getNpcName());
            plugin.logger.saveLog();
            plugin.updateOverlay();
            updatePanel();
        });

        JMenuItem resetKillLimitMenuItem = new JMenuItem("Reset Kill Limit");
        resetKillLimitMenuItem.addActionListener(ev -> {
            npcData.setKillLimit(0);
            npcData.setLimitSet(false);
            npcData.resetKillCountForLimit();
            plugin.logger.updateNpcData(npcData);
            plugin.updateOverlay();
            updatePanel();
        });

        contextMenu.add(resetMenuItem);
        contextMenu.add(resetKillLimitMenuItem);

        // "Edit NPC Data" option to edit NPC Data from log
        JMenuItem editNpcDataMenuItem = new JMenuItem("Edit NPC Data");
        editNpcDataMenuItem.addActionListener(ev -> showEditNpcDataDialog(npcData));
        contextMenu.add(editNpcDataMenuItem);

        // Add Expand All and Collapse All
        JMenuItem collapseAllItem = new JMenuItem("Collapse All");
        collapseAllItem.addActionListener(this::collapseAllDropdowns);

        JMenuItem expandAllItem = new JMenuItem("Expand All");
        expandAllItem.addActionListener(this::expandAllDropdowns);

        contextMenu.addSeparator();
        contextMenu.add(collapseAllItem);
        contextMenu.add(expandAllItem);

        return contextMenu;
    }

    private void showEditNpcDataDialog(NpcData npcData) {
        JPanel panel = new JPanel(new GridLayout(4, 2));

        // Create input fields with current values prefilled
        JLabel npcNameField = new JLabel(String.valueOf(npcData.getNpcName()));
        JTextField totalKillsField = new JTextField(String.valueOf(npcData.getTotalKillCount()));
        JTextField killLimitField = new JTextField(String.valueOf(npcData.getKillLimit()));
        JTextField killsForLimitField = new JTextField(String.valueOf(npcData.getKillCountForLimit()));

        // Add labels and fields to the panel
        panel.add(new JLabel(getName()));
        panel.add(npcNameField);
        panel.add(new JLabel("Total Kills:"));
        panel.add(totalKillsField);
        panel.add(new JLabel("Kill Limit:"));
        panel.add(killLimitField);
        panel.add(new JLabel("Kills For Limit:"));
        panel.add(killsForLimitField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Edit NPC Data", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                // Parse and set new values if valid
                int totalKills = Integer.parseInt(totalKillsField.getText());
                int killLimit = Integer.parseInt(killLimitField.getText());
                int killsForLimit = Integer.parseInt(killsForLimitField.getText());

                npcData.setTotalKillCount(totalKills);
                npcData.setKillLimit(killLimit);
                npcData.setKillCountForLimit(killsForLimit);

                plugin.logger.updateNpcData(npcData);
                plugin.updateOverlay();
                updatePanel();  // Refresh the panel to show updated values

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Please enter valid numbers.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void collapseAllDropdowns(ActionEvent e) {
        for (Component component : npcListPanel.getComponents()) {
            if (component instanceof MonsterMonitorBox) {
                MonsterMonitorBox box = (MonsterMonitorBox) component;
                box.setDropdownVisible(false);
            }
        }
        npcListPanel.revalidate();
        npcListPanel.repaint();
    }

    private void expandAllDropdowns(ActionEvent e) {
        for (Component component : npcListPanel.getComponents()) {
            if (component instanceof MonsterMonitorBox) {
                MonsterMonitorBox box = (MonsterMonitorBox) component;
                box.setDropdownVisible(true);
            }
        }
        npcListPanel.revalidate();
        npcListPanel.repaint();
    }

    public static class CustomScrollBarUI extends BasicScrollBarUI {
        private final Color arrowColor;

        public CustomScrollBarUI(Color arrowColor) {
            this.arrowColor = arrowColor;
        }

        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(200, 150, 0);
            this.trackColor = new Color(30, 30, 30);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            return button;
        }
    }
}

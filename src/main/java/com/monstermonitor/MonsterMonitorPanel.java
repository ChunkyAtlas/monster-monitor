package com.monstermonitor;

import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private final JPanel npcListPanel;
    private final JPanel ignoredNpcListPanel;
    private final JPanel fillerBox; // The filler box at the bottom
    private JLabel totalKillCountLabel;
    private final Map<NpcData, Boolean> dropdownStates = new HashMap<>(); // Stores the state of dropdowns

    @Inject
    public MonsterMonitorPanel(MonsterMonitorPlugin plugin) {
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
        add(titlePanel, BorderLayout.NORTH);

        // Initialize the NPC List Panels using GridBagLayout
        this.npcListPanel = new JPanel(new GridBagLayout());
        this.npcListPanel.setBackground(backgroundColor);

        this.ignoredNpcListPanel = new JPanel(new GridBagLayout());
        this.ignoredNpcListPanel.setBackground(backgroundColor);

        // Create the scroll pane for the NPC list to enable scrolling
        JScrollPane scrollPane = new JScrollPane(npcListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(2, 0)); // Custom narrow scrollbar
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI(new Color(200, 150, 0)));
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

            // Separate tracked and ignored NPCs for different displays
            for (NpcData npcData : trackedNpcs) {
                if (npcData.isIgnored()) {
                    ignoredNpcs.add(npcData);
                }
            }

            Collections.reverse(trackedNpcs);
            Collections.reverse(ignoredNpcs);

            int totalKills = 0;
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 0, 5, 0);

            // Add tracked NPCs
            for (NpcData npcData : trackedNpcs) {
                if (!npcData.isIgnored()) {
                    MonsterMonitorBox npcPanel = new MonsterMonitorBox(plugin, npcData, dropdownStates.getOrDefault(npcData, false));
                    totalKills += npcData.getTotalKillCount();
                    npcListPanel.add(npcPanel, gbc);
                    addContextMenuToNpcPanel(npcPanel, npcData);
                    gbc.gridy++;
                }
            }

            // Add ignored NPCs section
            if (!ignoredNpcs.isEmpty()) {
                JLabel ignoredLabel = new JLabel("Ignored NPCs", SwingConstants.CENTER);
                ignoredLabel.setForeground(new Color(180, 180, 180));
                ignoredLabel.setFont(new Font("Arial", Font.BOLD, 12));
                npcListPanel.add(ignoredLabel, gbc);
                gbc.gridy++;

                for (NpcData npcData : ignoredNpcs) {
                    MonsterMonitorBox npcPanel = new MonsterMonitorBox(plugin, npcData, dropdownStates.getOrDefault(npcData, false));
                    npcPanel.setBackground(new Color(50, 50, 50)); // Solid grey background for ignored NPCs
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

        JMenuItem resetMenuItem = new JMenuItem("Reset NPC Data");
        resetMenuItem.addActionListener(ev -> {
            plugin.logger.getNpcLog().remove(npcData.getNpcName());
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

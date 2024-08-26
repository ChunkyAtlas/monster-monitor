package com.monstermonitor;

import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class MonsterMonitorPanel extends PluginPanel
{
    private final MonsterMonitorPlugin plugin;
    private final JPanel npcListPanel;
    private final JScrollPane scrollPane;

    private JLabel totalKillCountLabel;

    @Inject
    public MonsterMonitorPanel(MonsterMonitorPlugin plugin)
    {
        this.plugin = plugin;

        // Setup the panel title with an icon
        JLabel titleLabel = new JLabel("Monster Monitor");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(Color.ORANGE);

        ImageIcon icon = new ImageIcon(getClass().getResource("/net/runelite/client/plugins/MonsterMonitor/icon.png"));
        titleLabel.setIcon(icon);

        // Label for displaying the total number of kills
        totalKillCountLabel = new JLabel("Total kills: 0");
        totalKillCountLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        totalKillCountLabel.setForeground(Color.LIGHT_GRAY);
        totalKillCountLabel.setHorizontalAlignment(SwingConstants.CENTER);

        this.npcListPanel = new JPanel();
        this.npcListPanel.setLayout(new BoxLayout(npcListPanel, BoxLayout.Y_AXIS));
        this.npcListPanel.setBackground(Color.DARK_GRAY);
        this.scrollPane = new JScrollPane(npcListPanel);
        this.scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        setLayout(new BorderLayout());
        add(titleLabel, BorderLayout.NORTH);
        add(totalKillCountLabel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);
    }

    // Update the panel with the list of tracked NPCs
    public void updatePanel()
    {
        npcListPanel.removeAll();

        List<NpcData> trackedNpcs = new ArrayList<>(plugin.getTrackedNpcs());
        String lastKilledNpcName = plugin.getLastKilledNpcName(); // Get the last killed NPC's name

        // Sort NPCs, but keep last killed NPC and unknown animation NPCs on top
        List<NpcData> topNpcs = new ArrayList<>();
        List<NpcData> otherNpcs = new ArrayList<>();

        int totalKills = 0; // Initialize the total kills counter

        for (NpcData npcData : trackedNpcs)
        {
            totalKills += npcData.getTotalKillCount(); // Accumulate total kills

            if (npcData.getNpcName().equals(lastKilledNpcName) || !DeathAnimationIDs.isDeathAnimation(npcData.getDeathAnimationId()))
            {
                topNpcs.add(npcData);
            }
            else
            {
                otherNpcs.add(npcData);
            }
        }

        // Add top NPCs (last killed or unknown) to the panel first
        for (NpcData npcData : topNpcs)
        {
            addNpcPanel(npcData);
        }

        // Add other NPCs to the panel
        for (NpcData npcData : otherNpcs)
        {
            addNpcPanel(npcData);
        }

        // Update the total kills label
        totalKillCountLabel.setText("Total kills: " + totalKills);

        revalidate();
        repaint();
    }

    // Helper method to add an individual NPC panel
    private void addNpcPanel(NpcData npcData)
    {
        JPanel npcPanel = new JPanel(new BorderLayout());
        npcPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        npcPanel.setBackground(Color.DARK_GRAY);

        JLabel npcNameLabel = new JLabel(npcData.getNpcName() + " x " + npcData.getTotalKillCount());
        npcNameLabel.setForeground(Color.WHITE);
        npcPanel.add(npcNameLabel, BorderLayout.WEST);

        JPanel optionsPanel = new JPanel(new GridLayout(2, 2));
        optionsPanel.setBackground(Color.DARK_GRAY);

        // SET LIMIT Checkbox
        JCheckBox setLimitCheckbox = new JCheckBox("Set Limit");
        setLimitCheckbox.setForeground(Color.LIGHT_GRAY);
        setLimitCheckbox.setBackground(Color.DARK_GRAY);
        setLimitCheckbox.setSelected(npcData.isLimitSet());
        setLimitCheckbox.addActionListener(e -> {
            boolean isChecked = setLimitCheckbox.isSelected();
            npcData.setLimitSet(isChecked);
            if (!isChecked) {
                npcData.setKillLimit(0);  // Remove the kill limit if unchecked
                plugin.updateOverlay(); // Update the overlay to remove this NPC
            }
            plugin.getLogger().updateNpcData(npcData); // Save state
            updatePanel(); // Refresh panel
        });

        // JSpinner for Setting Kill Limit
        int currentLimit = npcData.getKillLimit(); // Load saved kill limit value
        JSpinner limitSpinner = new JSpinner(new SpinnerNumberModel(
                currentLimit, 0, 999999, 1));
        limitSpinner.addChangeListener(e -> {
            int limit = (Integer) limitSpinner.getValue();
            int killCountForLimit = npcData.getKillCountForLimit(); // Preserve progress
            npcData.setKillLimit(limit);
            npcData.setKillCountForLimit(killCountForLimit); // Set the preserved progress
            plugin.getLogger().updateNpcData(npcData); // Save the NPC data with its new limit
            plugin.updateOverlay(); // Ensure overlay updates with new limit
        });
        limitSpinner.setEnabled(npcData.isLimitSet()); // Enable only if the limit is set

        // NOTIFY Checkbox
        JCheckBox notifyCheckbox = new JCheckBox("Notify");
        notifyCheckbox.setForeground(Color.LIGHT_GRAY);
        notifyCheckbox.setBackground(Color.DARK_GRAY);
        notifyCheckbox.setSelected(npcData.isNotifyOnLimit());
        notifyCheckbox.addActionListener(e -> {
            boolean notifyChecked = notifyCheckbox.isSelected();
            npcData.setNotifyOnLimit(notifyChecked);
            plugin.getLogger().updateNpcData(npcData);
        });

        optionsPanel.add(setLimitCheckbox);
        optionsPanel.add(limitSpinner);
        optionsPanel.add(notifyCheckbox);

        npcPanel.add(optionsPanel, BorderLayout.SOUTH);

        // Right-click context menu for resetting
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem resetMenuItem = new JMenuItem("Reset");
        resetMenuItem.addActionListener(e -> {
            plugin.getLogger().getNpcLog().remove(npcData.getNpcName()); // Remove the NPC from the log
            plugin.getLogger().saveLog(); // Save the updated log
            plugin.updateOverlay(); // Update the overlay
            updatePanel(); // Refresh the panel
        });

        JMenuItem resetKillLimitMenuItem = new JMenuItem("Reset Kill Limit");
        resetKillLimitMenuItem.addActionListener(e -> {
            npcData.setKillLimit(0); // Reset the kill limit to zero
            npcData.resetKillCountForLimit(); // Reset the kill count towards the limit
            plugin.getLogger().updateNpcData(npcData); // Save the updated data
            plugin.updateOverlay(); // Update the overlay
            updatePanel(); // Refresh the panel
        });

        contextMenu.add(resetMenuItem);
        contextMenu.add(resetKillLimitMenuItem);

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

        npcListPanel.add(npcPanel); // Add to the panel
    }
}

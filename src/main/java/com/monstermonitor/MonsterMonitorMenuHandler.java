package com.monstermonitor;

import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;

/**
 * Handles adding custom menu entries for Monster Monitor.
 * Manages the creation of "Monitor" and "Ignore" options.
 */
public class MonsterMonitorMenuHandler {
    private static final String MONITOR_OPTION = "Monitor";
    private static final String IGNORE_OPTION = "Ignore";
    private final MonsterMonitorPlugin plugin;
    private final MonsterMonitorLogger logger;
    private final MonsterMonitorPanel panel;
    private final MonsterMonitorConfig config;
    private final Client client;

    @Inject
    public MonsterMonitorMenuHandler(MonsterMonitorPlugin plugin, MonsterMonitorLogger logger, Client client, MonsterMonitorPanel panel, MonsterMonitorConfig config) {
        this.plugin = plugin;
        this.logger = logger;
        this.panel = panel;
        this.config = config;
        this.client = client;
    }

    /**
     * Inserts custom options when the target is an NPC.
     *
     * @param event The event triggered when the menu is opened.
     */
    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        if (!config.showRightClickMenuEntries()) {
            return; // Exit early if the user has disabled the right-click menu entries
        }

        MenuEntry[] menuEntries = event.getMenuEntries();
        NPC targetNpc = findTargetNpc(menuEntries);

        if (targetNpc != null) {
            addCustomMenuOptions(targetNpc);
        }
    }

    /**
     * Finds the target NPC from the menu entries.
     *
     * @param menuEntries The array of menu entries.
     * @return The target NPC if found, otherwise null.
     */
    private NPC findTargetNpc(MenuEntry[] menuEntries) {
        for (MenuEntry menuEntry : menuEntries) {
            MenuAction menuType = menuEntry.getType();
            NPC target = menuEntry.getNpc();

            if (target != null && isValidMenuType(menuType, menuEntry.getOption(), target.getCombatLevel())) {
                return target;
            }
        }
        return null;
    }

    /**
     * Checks if a menu type and option are valid for adding custom entries.
     *
     * @param menuType The type of the menu.
     * @param optionText The option text from the menu.
     * @param combatLevel The combat level of the NPC.
     * @return True if the menu type and option are valid for adding entries, otherwise false.
     */
    private boolean isValidMenuType(MenuAction menuType, String optionText, int combatLevel) {
        return (menuType == MenuAction.EXAMINE_NPC || menuType == MenuAction.NPC_SECOND_OPTION || menuType == MenuAction.NPC_FIFTH_OPTION)
                && optionText.equals("Attack")
                && combatLevel > 0;
    }

    /**
     * Adds custom "Monitor" and "Ignore" menu options for the specified NPC.
     *
     * @param npc The NPC to add the options for.
     */
    private void addCustomMenuOptions(NPC npc) {
        String npcName = npc.getName();
        int combatLevel = npc.getCombatLevel();

        if (npcName == null) {
            return;
        }

        NpcData npcData = logger.getNpcLog().get(npcName);

        if (shouldAddMonitorOption(npcData)) {
            addMonitorMenuOption(npcName, combatLevel);
        }

        if (shouldAddIgnoreOption(npcData)) {
            addIgnoreMenuOption(npcName, combatLevel);
        }
    }

    /**
     * Determines if the "Monitor" option should be added for the NPC.
     *
     * @param npcData The data of the NPC.
     * @return True if the "Monitor" option should be added, otherwise false.
     */
    private boolean shouldAddMonitorOption(NpcData npcData) {
        return npcData == null || (!npcData.isLimitSet() && !npcData.isIgnored());
    }

    /**
     * Adds the "Monitor" option to the menu.
     *
     * @param npcName The name of the NPC.
     * @param combatLevel The combat level of the NPC.
     */
    private void addMonitorMenuOption(String npcName, int combatLevel) {
        client.getMenu()
                .createMenuEntry(1)
                .setOption(MONITOR_OPTION)
                .setTarget(npcName + " (level-" + combatLevel + ")")
                .setType(MenuAction.RUNELITE_HIGH_PRIORITY)
                .onClick(e -> handleMonitorAction(npcName));
    }

    /**
     * Determines if the "Ignore" option should be added for the NPC.
     *
     * @param npcData The data of the NPC.
     * @return True if the "Ignore" option should be added, otherwise false.
     */
    private boolean shouldAddIgnoreOption(NpcData npcData) {
        return npcData == null || (!npcData.isIgnored() && !npcData.isLimitSet());
    }

    /**
     * Adds the "Ignore" option to the menu.
     *
     * @param npcName The name of the NPC.
     * @param combatLevel The combat level of the NPC.
     */
    private void addIgnoreMenuOption(String npcName, int combatLevel) {
        client.getMenu()
                .createMenuEntry(1)
                .setOption(IGNORE_OPTION)
                .setTarget(npcName + " (level-" + combatLevel + ")")
                .setType(MenuAction.RUNELITE_HIGH_PRIORITY)
                .onClick(e -> handleIgnoreAction(npcName));
    }

    /**
     * Handles the "Monitor" action from the menu.
     *
     * @param npcName The name of the NPC to monitor.
     */
    private void handleMonitorAction(String npcName) {
        plugin.setNpcToMonitor(npcName, 10); // Default to a kill limit of 10 when monitoring
        NpcData npcData = logger.getNpcLog().get(npcName);
        if (npcData != null) {
            npcData.setLimitSet(true); // Make sure the checkbox is checked
            logger.updateNpcData(npcData);
            plugin.updateOverlay();
            panel.updatePanel();
        }
    }

    /**
     * Handles the "Ignore" action from the menu.
     *
     * @param npcName The name of the NPC to ignore.
     */
    private void handleIgnoreAction(String npcName) {
        plugin.setNpcToIgnore(npcName);
    }
}

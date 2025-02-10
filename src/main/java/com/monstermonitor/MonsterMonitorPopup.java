package com.monstermonitor;

import net.runelite.api.Client;
import net.runelite.api.Varbits;
import net.runelite.api.WidgetNode;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetModalMode;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Handles displaying a modal popup when a player reaches the kill limit for an NPC.
 */
public class MonsterMonitorPopup {
    private static final int FIXED_CLASSIC_LAYOUT = (548 << 16) | 13;
    private static final int RESIZABLE_CLASSIC_LAYOUT = (161 << 16) | 13;
    private static final int RESIZABLE_MODERN_LAYOUT = (164 << 16) | 13;
    private static final int INTERFACE_GROUP_ID = 660;
    private static final int SCRIPT_ID = 3343;

    private WidgetNode popupWidgetNode;
    private final ConcurrentLinkedQueue<String> queuedPopups = new ConcurrentLinkedQueue<>();

    private final Client client;
    private final ClientThread clientThread;
    private final MonsterMonitorConfig config;
    private volatile boolean isDisplayingPopup = false;

    @Inject
    public MonsterMonitorPopup(Client client, ClientThread clientThread, MonsterMonitorConfig config) {
        this.client = client;
        this.clientThread = clientThread;
        this.config = config;
    }

    /**
     * Triggers a popup for the given NPC name when its kill limit is reached.
     * @param npcName The name of the NPC.
     */
    public void showPopup(String npcName) {
        if (!config.showPopup()) {
            return;
        }

        queuedPopups.add(npcName);
        if (!isDisplayingPopup) {
            clientThread.invokeLater(this::displayNextPopup);
        }
    }

    /**
     * Displays the next popup in the queue.
     */
    private void displayNextPopup() {
        if (queuedPopups.isEmpty()) {
            isDisplayingPopup = false;
            return;
        }

        isDisplayingPopup = true;
        String npcName = queuedPopups.poll();

        clientThread.invokeLater(() -> {
            try {
                int componentId = getParentComponentId();
                Widget parentWidget = client.getWidget(componentId);

                if (parentWidget == null) {
                    isDisplayingPopup = false;
                    return;
                }

                popupWidgetNode = client.openInterface(componentId, INTERFACE_GROUP_ID, WidgetModalMode.MODAL_CLICKTHROUGH);

                if (popupWidgetNode == null) {
                    isDisplayingPopup = false;
                    return;
                }

                client.runScript(SCRIPT_ID, "Monster Monitor", "You have reached the kill limit for " + npcName, -1);
                clientThread.invokeLater(this::tryClearMessage);
            } catch (IllegalStateException ex) {
                isDisplayingPopup = false;
                clientThread.invokeLater(this::tryClearMessage);
            }
        });
    }

    /**
     * Determines the correct parent component ID based on the UI layout.
     */
    private int getParentComponentId() {
        if (client.isResized()) {
            int sidePanel = client.getVarbitValue(Varbits.SIDE_PANELS);
            return (sidePanel == 1) ? RESIZABLE_MODERN_LAYOUT : RESIZABLE_CLASSIC_LAYOUT;
        }
        return FIXED_CLASSIC_LAYOUT;
    }

    /**
     * Handles the fade-out effect and removal of the popup.
     */
    private boolean tryClearMessage() {
        Widget w = client.getWidget(INTERFACE_GROUP_ID, 1);

        if (w != null && w.getWidth() > 0) {
            return false;
        }

        try {
            if (popupWidgetNode != null) {
                client.closeInterface(popupWidgetNode, true);
            }
        } catch (Exception ignored) {}

        popupWidgetNode = null;
        isDisplayingPopup = false;

        if (!queuedPopups.isEmpty()) {
            clientThread.invokeLater(this::displayNextPopup);
        }
        return true;
    }

    /**
     * Properly clears all popups and resets the queue on shutdown.
     */
    public void shutdownPopup() {
        queuedPopups.clear();
        isDisplayingPopup = false;

        clientThread.invokeLater(() -> {
            try {
                if (popupWidgetNode != null) {
                    client.closeInterface(popupWidgetNode, true);
                }
            } catch (Exception ignored) {}
            popupWidgetNode = null;
        });
    }
}

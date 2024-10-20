package com.monstermonitor;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

/**
 * The MonsterMonitorConfig interface defines the configuration options for Monster Monitor.
 * It allows users to toggle settings such as displaying the overlay, receiving notifications
 * when kill limits are reached, and customizing the notification messages.
 */
@ConfigGroup("monster monitor")
public interface MonsterMonitorConfig extends Config {

    /**
     * Configures whether the Monster Monitor overlay should be displayed.
     *
     * @return true if the overlay should be shown, false otherwise
     */
    @ConfigItem(
            keyName = "showOverlay",
            name = "Show Overlay",
            description = "Toggle to display or hide the Monster Monitor overlay"
    )
    default boolean showOverlay() {
        return true; // Default to showing the overlay
    }

    /**
     * Configures whether a notification should be sent when a kill limit is reached.
     *
     * @return true if notifications should be sent when the kill limit is reached, false otherwise
     */
    @ConfigItem(
            keyName = "notifyOnLimit",
            name = "Notify on Kill Limit",
            description = "Send a notification when the kill limit is reached"
    )
    default boolean notifyOnLimit() {
        return true;  // Default to notifying when the kill limit is reached
    }

    /**
     * Configures whether chat notifications should be shown when kill limits are reached.
     *
     * @return true if chat notifications should be shown, false otherwise
     */
    @ConfigItem(
            keyName = "showChatNotifications",
            name = "Show Chat Notifications",
            description = "Show notifications about reaching kill limits in the chat window."
    )
    default boolean showChatNotifications() {
        return true; // Default to showing chat notifications
    }

    /**
     * Configures the custom notification message displayed when a kill limit is reached.
     *
     * @return the custom notification message
     */
    @ConfigItem(
            keyName = "customNotificationMessage",
            name = "Custom Notification Message",
            description = "Lets you personalize the message displayed when a kill limit is reached. add {npc} for npc name"
    )
    default String customNotificationMessage() {
        return "Kill limit reached for {npc}"; // Default message
    }

    /**
     * Configures whether sound alerts should be enabled for notifications.
     *
     * @return true if sound alerts should be enabled, false otherwise
     */
    @ConfigItem(
            keyName = "enableSoundAlerts",
            name = "Enable Sound Alerts",
            description = "Enable sound alerts for notifications."
    )
    default boolean enableSoundAlerts() {
        return true; // Default to enabling sound alerts
    }
}
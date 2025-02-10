package com.monstermonitor;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import java.awt.*;

/**
 * The MonsterMonitorConfig interface defines the configuration options for Monster Monitor.
 * It allows users to customize settings such as progress bar colors, overlay visibility,
 * and kill limit notifications.
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

    /**
     * Configures whether right-click menu entries should be displayed.
     *
     * @return true if the right-click menu entries should be shown, false otherwise
     */
    @ConfigItem(
            keyName = "showRightClickMenuEntries",
            name = "Show Right-Click Menu Entries",
            description = "Toggle to show or hide the right-click menu entries for NPCs."
    )
    default boolean showRightClickMenuEntries() {
        return true; // Default to showing right-click menu entries
    }

    @ConfigItem(
            keyName = "showTitle",
            name = "Show Overlay Title",
            description = "Toggle the display of the overlay title"
    )
    default boolean showTitle() {
        return true;
    }
    /**
     * Configures the colors for the progress bar.
     *
     * @return the start color of the progress bar.
     */
    @ConfigItem(
            keyName = "progressBarStartColor",
            name = "Progress Bar Start Color",
            description = "Select the color for the start of the progress bar.",
            position = 4
    )
    default Color progressBarStartColor() {
        return new Color(139, 0, 0); // Dark red
    }

    /**
     * Configures the midpoint color for the progress bar.
     *
     * @return the midpoint color of the progress bar.
     */
    @ConfigItem(
            keyName = "progressBarMidColor",
            name = "Progress Bar Midpoint Color",
            description = "Select the color for the midpoint of the progress bar.",
            position = 5
    )
    default Color progressBarMidColor() {
        return new Color(204, 102, 0); // Dark orange
    }

    /**
     * Configures the end color for the progress bar.
     *
     * @return the end color of the progress bar.
     */
    @ConfigItem(
            keyName = "progressBarEndColor",
            name = "Progress Bar End Color",
            description = "Select the color for the end of the progress bar.",
            position = 6
    )
    default Color progressBarEndColor() {
        return new Color(0, 128, 0); // Dark green
    }
    /**
     * Adds a button to reset progress bar colors to their default values.
     */
    @ConfigItem(
            keyName = "resetProgressBarColors",
            name = "Reset Colors",
            description = "Click to reset all progress bar colors to their default values.",
            position = 10
    )
    default Button resetProgressBarColors() {
        return new Button("Reset Colors");
    }

    /**
     * Configures whether the popup notification should be displayed when a kill limit is reached.
     *
     * @return true if the popup should be shown, false otherwise
     */
    @ConfigItem(
            keyName = "showPopup",
            name = "Show Popup",
            description = "Display a popup when the kill limit is reached"
    )
    default boolean showPopup() {
        return true;
    }
}

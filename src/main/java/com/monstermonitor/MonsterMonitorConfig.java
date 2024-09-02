package com.monstermonitor;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

/**
 * The MonsterMonitorConfig interface defines the configuration options for the Monster Monitor plugin.
 * It allows users to toggle settings such as displaying the overlay and receiving notifications when kill limits are reached.
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
    default boolean showOverlay()
    {
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
}

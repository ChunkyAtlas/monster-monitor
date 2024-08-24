package com.monstermonitor;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("monster monitor")
public interface MonsterMonitorConfig extends Config {

    @ConfigItem(
            keyName = "showOverlay",
            name = "Show Overlay",
            description = "Toggle to display or hide the Monster Monitor overlay"
    )
    default boolean showOverlay()
    {
        return true; // Default to showing the overlay
    }

    @ConfigItem(
            keyName = "notifyOnLimit",
            name = "Notify on Kill Limit",
            description = "Send a notification when the kill limit is reached"
    )
    default boolean notifyOnLimit() {
        return true;  // Default to notifying when the kill limit is reached
    }
}

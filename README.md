# Monster Monitor

## Overview

**Monster Monitor** is a RuneLite plugin that enhances gameplay by allowing players to track and manage their NPC interactions, log kill counts, and set personalized kill limits. It’s designed for players who want to monitor progress on specific NPC goals, track rare drops, or reach boss milestones. The plugin provides a user-friendly overlay, customizable options, and right-click menu entries for convenient tracking.

## Features

- **Automatic Kill Logging**:
    - Tracks and logs each NPC kill, recording the NPC's name, total kill count, and other details.
    - Logs are saved per player for long-term tracking and can be viewed in the plugin’s panel.

- **Set Custom Kill Limits**:
    - Allows players to set custom kill limits for any NPC.
    - Notifications are triggered when kill limits are reached, providing progress feedback.
    - Different kill limits can be set for multiple NPCs independently.

- **Dynamic Overlay**:
    - Displays actively tracked NPCs, their current kill count, and progress toward any set kill limits.
    - A progress bar shows kill limits, with configurations for visibility in the overlay.

- **In-Depth Configurations**:
    - **Notification Customization**: Notifications can be configured to show in chat, with sound alerts, or both. Customize the message format, including dynamic NPC names.
    - **Right-Click Menu Options**: Quickly add “Monitor” or “Ignore” options to NPCs via right-click, streamlining tracking or ignoring NPCs during gameplay.

- **Panel Controls**:
    - The plugin panel provides tools for managing NPC tracking:
        - **Kill Count Reset**: Reset kill counts or remove kill limits for any NPC.
        - **NPC Ignoring**: Ignore specific NPCs, which prevents them from appearing in the overlay.
        - **Edit NPC Data**: Directly modify kill counts and limits for tracked NPCs.
        - **Expand/Collapse Options**: For easier viewing, expand or collapse all NPC entries.

### Log File Locations

- **Main Log File**: `~/.runelite/monstermonitor/your_player_name/monster_monitor_log.json`

## Usage

1. **Track NPC Kills**:
    - Once the plugin is installed, it will automatically track NPC kills as you play.

2. **Set Kill Limits**:
    - Right-click on an NPC in the plugin panel to set a kill limit. Notifications will appear when the limit is reached.

3. **View Kill Counts**:
    - View tracked kill counts and limits in the overlay on the RuneLite client.

4. **Reset Kill Counts or Limits**:
    - Right-click on an NPC in the plugin panel to reset its kill count or remove it from tracking.

## Contribution

Contributions are welcome! If you find any bugs or have suggestions for improvements, feel free to open an issue or submit a pull request.

### How to Contribute

1. Fork the repository.
2. Create a new branch (`git checkout -b feature-name`).
3. Make your changes.
4. Commit your changes (`git commit -am 'Add new feature'`).
5. Push to the branch (`git push origin feature-name`).
6. Open a Pull Request.

## Contact

For questions or support, open an issue on GitHub or email at monstermonitor@proton.me!

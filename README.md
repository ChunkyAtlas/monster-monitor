# Monster Monitor

## Overview

**Monster Monitor** is a RuneLite plugin that enhances your gameplay by tracking and managing your NPC interactions, logging kill counts, and setting personalized kill limits. Designed for players who want to monitor progress on specific NPC goals, track rare drops, or achieve boss milestones, the plugin provides a user-friendly overlay, customizable options, and intuitive right-click menu entries for easy management.

## Features

- **Automatic Kill Logging**:
    - Automatically tracks and logs every NPC kill, recording the NPC's name, total kill count, and other details.
    - Logs are maintained on a per-player basis for long-term tracking and can be viewed within the plugin’s panel.

- **Custom Kill Limits**:
    - Set personalized kill limits for any NPC.
    - Receive notifications when kill limits are reached, offering real-time progress feedback.
    - Supports independent kill limits for multiple NPCs.

- **Dynamic Overlay**:
    - Displays actively tracked NPCs along with their current kill counts and progress toward any set kill limits.
    - Features a progress bar that visually represents kill limits, with configurable visibility options.

- **Popup Notifications**:
    - New kill limit popup notifications alert you when a set kill limit is reached.
    - Popups queue properly to display multiple notifications in order.
    - A configurable toggle in the settings allows you to enable or disable popup notifications.

- **In-Depth Configurations**:
    - **Notification Customization**: Configure notifications to appear in chat, with sound alerts, or as popup messages. Personalize the message format with dynamic NPC names.
    - **Right-Click Menu Options**: Quickly add “Monitor” or “Ignore” options to NPCs via right-click, streamlining tracking or ignoring during gameplay.

- **Panel Controls**:
    - Manage NPC tracking through a dedicated panel:
        - **Reset Kill Counts**: Reset kill counts or remove kill limits for any NPC.
        - **NPC Ignoring**: Ignore specific NPCs so they do not appear in the overlay.
        - **Edit NPC Data**: Directly modify kill counts and limits for tracked NPCs.
        - **Expand/Collapse Options**: Toggle the detailed view of NPC entries for easier navigation.

### Log File Locations

- **Main Log File**: `~/.runelite/monstermonitor/your_player_name/monster_monitor_log.json`

## Usage

1. **Track NPC Kills**:
    - Once installed, the plugin automatically tracks your NPC kills during gameplay.

2. **Set Kill Limits**:
    - Right-click on an NPC in the plugin panel to set a kill limit. Notifications and popups will alert you once the limit is reached.

3. **View Kill Counts**:
    - The overlay on the RuneLite client displays your tracked kill counts and progress toward any set limits.

4. **Reset Kill Counts or Limits**:
    - Right-click on an NPC in the plugin panel to reset its kill count or remove it from tracking.

## Contribution

Contributions are welcome! If you find any bugs or have suggestions for improvements, please open an issue or submit a pull request.

### How to Contribute

1. Fork the repository.
2. Create a new branch (`git checkout -b feature-name`).
3. Make your changes.
4. Commit your changes (`git commit -am 'Add new feature'`).
5. Push to the branch (`git push origin feature-name`).
6. Open a Pull Request.

## Contact

For questions or support, please open an issue on GitHub or email monstermonitor@proton.me!

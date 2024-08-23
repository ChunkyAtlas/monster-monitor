# Monster Monitor Plugin

## Overview

The **Monster Monitor** plugin is a RuneLite plugin designed to help players track their NPC kills and set specific kill limits. This plugin is especially useful for those who want to monitor their progress while farming NPCs or working towards specific goals, such as boss kill milestones or grinding for drops. 

## Features

- **Kill Tracking**: Automatically tracks the number of kills for each NPC you engage with.
- **Set Kill Limits**: Allows you to set a kill limit for specific NPCs and notifies you when the limit is reached.
- **Notifications**: Receive in-game notifications when you've reached your kill limit for an NPC.
- **Dynamic Overlay**: Displays the NPCs you're currently tracking along with their respective kill counts and limits.
- **Reset Options**: Easily reset kill counts or remove an NPC from tracking.

## Sending Log Files for Unknown Animations

The plugin logs any unknown death animations to help improve the accuracy and comprehensiveness of the plugin. These logs are saved to your system and can be sent to me for updates.


### Log File Locations:

- **Main Log File**: `~/.runelite/monstermonitor/your_player_name/monster_monitor_log.json`
- **Unknown Animations Log**: `~/.runelite/monstermonitor/your_player_name/unknown_animations.log`


### How to Send Log Files:

1. **Locate the Log Files**: Navigate to the directory listed above to find the log files.
2. **Send the Files**: Send the `unknown_animations.log` file (and optionally the `monster_monitor_log.json` file) to the plugin developer via monstermonitor@proton.me.
3. **Update the Plugin**: I will use these logs to update the plugin with new death animation IDs, improving its functionality.


## Usage

1. **Enable the Plugin**:
   - Once installed, go to the RuneLite settings and enable the **Monster Monitor** plugin.

2. **Track NPC Kills**:
   - The plugin will automatically begin tracking NPC kills as you play.

3. **Set Kill Limits**:
   - Right-click on an NPC in the plugin panel to set a kill limit. The plugin will notify you when the limit is reached.

4. **View Kill Counts**:
   - View your kill counts and limits in the overlay on the left side of the RuneLite client.

5. **Reset Kill Counts or Limits**:
   - Right-click on an NPC in the plugin panel to reset its kill count or remove it from tracking.

## Contribution

Contributions are welcome! If you find any bugs or have suggestions for improvements, please feel free to open an issue or submit a pull request.

### How to Contribute

1. Fork the repository.
2. Create a new branch (`git checkout -b feature-name`).
3. Make your changes.
4. Commit your changes (`git commit -am 'Add new feature'`).
5. Push to the branch (`git push origin feature-name`).
6. Open a Pull Request.

## Contact

For questions or support, open an issue on GitHub or email me @ monstermonitor@proton.me!


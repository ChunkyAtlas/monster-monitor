package com.monstermonitor;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.runelite.api.Client;
import net.runelite.api.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.*;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The MonsterMonitorLogger class is responsible for logging and managing data related to NPC kills.
 * It handles reading from and writing to log files specific to each player,
 * maintains the current state of the tracked NPCs, and handles NPC ignore/monitor states.
 */
public class MonsterMonitorLogger {
    private static final Logger logger = LoggerFactory.getLogger(MonsterMonitorLogger.class);
    private static final String BASE_LOG_DIR = System.getProperty("user.home") + "/.runelite/monstermonitor";
    private final Map<String, NpcData> npcLog = new LinkedHashMap<>(); // Use LinkedHashMap to maintain insertion order
    private String playerLogDir; // Directory to store logs specific to the player
    private String logFilePath; // Path to the main log file
    private final MonsterMonitorPlugin plugin;

    @Inject
    private Client client;

    @Inject
    private Gson gson;

    @Inject
    public MonsterMonitorLogger(MonsterMonitorPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initializes the logger with directories based on the player's name.
     * Creates necessary directories and files if they don't exist, and loads existing log data.
     */
    public void initialize() {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            return; // Exit early if the player is not yet initialized
        }

        String playerName = localPlayer.getName();
        playerLogDir = BASE_LOG_DIR + "/" + playerName;
        logFilePath = playerLogDir + "/monster_monitor_log.json";

        File directory = new File(playerLogDir);
        if (!directory.exists()) {
            directory.mkdirs(); // Create the directory if it doesn't exist
        }

        createEmptyFileIfNotExists(logFilePath);
        loadLog(); // Load existing data
    }

    /**
     * Helper method to create an empty file if it doesn't exist.
     *
     * @param filePath the path to the file that needs to be checked/created
     */
    private void createEmptyFileIfNotExists(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                logger.error("Failed to create new log file at {}", filePath, e);
            }
        }
    }

    /**
     * Loads the NPC log from the file, or creates a new file if it doesn't exist.
     * This method ensures that the NPC log is populated with data from previous sessions
     * and updates any missing fields to ensure compatibility.
     */
    public void loadLog() {
        if (logFilePath == null) {
            return;
        }

        File logFile = new File(logFilePath);
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
                saveLogAsync();  // Save the log (with initial empty data)
            } catch (IOException e) {
                logger.error("Failed to create new log file during load at {}", logFilePath, e);
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            Type type = new TypeToken<Map<String, NpcData>>() {}.getType();
            Map<String, NpcData> loadedLog = gson.fromJson(reader, type);
            if (loadedLog != null) {
                // Iterate over loaded entries and ensure they have all required fields
                loadedLog.forEach((npcName, npcData) -> {
                    // Ensure default values for missing fields
                    if (npcData.getKillLimit() == 0) {
                        npcData.setKillLimit(10); // Default kill limit (could be 0 if not set)
                    }
                    if (npcData.getKillCountForLimit() == 0) {
                        npcData.resetKillCountForLimit();
                    }
                    if (!npcData.isLimitSet()) {
                        npcData.setLimitSet(false);
                    }
                    if (!npcData.isIgnored()) {
                        npcData.setIgnored(false);
                    }
                    if (!npcData.isNotifyOnLimit()) {
                        npcData.setNotifyOnLimit(false);
                    }

                    // Update the in-memory log with the corrected data
                    npcLog.put(npcName, npcData);
                });

                // Save back the updated log to ensure that the new fields are persisted
                saveLogAsync();
            }
        } catch (IOException e) {
            logger.error("Failed to load log data from {}", logFilePath, e);
        }

        // Ensure the overlay is updated after loading data
        plugin.updateOverlayVisibility();
    }

    /**
     * Logs the death of an NPC by updating its kill count and saving the log.
     * Also reorders the log to ensure the last killed NPC is at the top.
     *
     * @param npcName the name of the NPC
     */
    public void logDeath(String npcName) {
        NpcData npcData = npcLog.getOrDefault(npcName, new NpcData(npcName));

        // Do not log kills for NPCs that are set to be ignored
        if (npcData.isIgnored()) {
            return;
        }

        npcData.incrementKillCount();
        npcLog.remove(npcName); // Remove the NPC if it exists to reinsert it at the top
        npcLog.put(npcName, npcData); // Insert it at the top

        saveLogAsync(); // Save the reordered log asynchronously
    }

    /**
     * Saves the current NPC log to the file asynchronously.
     * This method is called whenever NPC data is updated to ensure persistence.
     */
    public void saveLogAsync() {
        new Thread(this::saveLog).start();
    }

    /**
     * Synchronously saves the current NPC log to the file.
     * This method is intended to be run on a background thread.
     */
    public void saveLog() {
        if (logFilePath == null) {
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(logFilePath)))) {
            gson.toJson(npcLog, writer);
        } catch (IOException e) {
            logger.error("Failed to save log data to {}", logFilePath, e);
        }
    }

    /**
     * Retrieves the current NPC log.
     *
     * @return a map containing the NPC log data
     */
    public Map<String, NpcData> getNpcLog() {
        return npcLog;
    }

    /**
     * Updates the data for a specific NPC in the log and saves the log asynchronously.
     * If the NPC is ignored, it will not be tracked further.
     *
     * @param npcData the updated data for the NPC
     */
    public void updateNpcData(NpcData npcData) {
        npcLog.put(npcData.getNpcName(), npcData);
        saveLogAsync(); // Save the updated log asynchronously
        plugin.updateOverlayVisibility(); // Update overlay visibility whenever NPC data changes
    }
}

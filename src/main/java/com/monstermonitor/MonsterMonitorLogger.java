package com.monstermonitor;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.runelite.api.Client;
import net.runelite.api.Player;

import javax.inject.Inject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The MonsterMonitorLogger class is responsible for logging and managing data related to NPC kills.
 * It handles reading from and writing to log files specific to each player,
 * and maintains the current state of the tracked NPCs.
 */
public class MonsterMonitorLogger
{
    private static final String BASE_LOG_DIR = System.getProperty("user.home") + "/.runelite/monstermonitor";
    private final Map<String, NpcData> npcLog = new LinkedHashMap<>(); // Use LinkedHashMap to maintain insertion order
    private String playerLogDir; // Directory to store logs specific to the player
    private String logFilePath; // Path to the main log file

    @Inject
    private Client client;

    @Inject
    private Gson gson;

    /**
     * Initializes the logger with directories based on the player's name.
     * Creates necessary directories and files if they don't exist, and loads existing log data.
     */
    public void initialize()
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
        {
            return; // Exit early if the player is not yet initialized
        }

        String playerName = localPlayer.getName();
        playerLogDir = BASE_LOG_DIR + "/" + playerName;
        logFilePath = playerLogDir + "/monster_monitor_log.json";

        File directory = new File(playerLogDir);
        if (!directory.exists())
        {
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
    private void createEmptyFileIfNotExists(String filePath)
    {
        File file = new File(filePath);
        if (!file.exists())
        {
            try
            {
                file.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Loads the NPC log from the file, or creates a new file if it doesn't exist.
     * This method ensures that the NPC log is populated with data from previous sessions.
     */
    public void loadLog()
    {
        if (logFilePath == null)
        {
            return;
        }

        File logFile = new File(logFilePath);
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
                saveLog();  // Save the log (with initial empty data)
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        try (FileReader reader = new FileReader(logFile))
        {
            Type type = new TypeToken<Map<String, NpcData>>(){}.getType();
            Map<String, NpcData> loadedLog = gson.fromJson(reader, type);
            if (loadedLog != null)
            {
                npcLog.putAll(loadedLog); // Load the NPC log from the file
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Logs the death of an NPC by updating its kill count and saving the log.
     * Also reorders the log to ensure the last killed NPC is at the top.
     *
     * @param npcName the name of the NPC
     */
    public void logDeath(String npcName)
    {
        NpcData npcData = npcLog.getOrDefault(npcName, new NpcData(npcName));
        npcData.incrementKillCount();
        npcLog.remove(npcName); // Remove the NPC if it exists to reinsert it at the top
        npcLog.put(npcName, npcData); // Insert it at the top

        saveLog(); // Save the reordered log
    }

    /**
     * Saves the current NPC log to the file.
     * This method is called whenever NPC data is updated to ensure persistence.
     */
    public void saveLog()
    {
        if (logFilePath == null)
        {
            return;
        }

        try (FileWriter writer = new FileWriter(new File(logFilePath)))
        {
            gson.toJson(npcLog, writer);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the current NPC log.
     *
     * @return a map containing the NPC log data
     */
    public Map<String, NpcData> getNpcLog()
    {
        return npcLog;
    }

    /**
     * Updates the data for a specific NPC in the log and saves the log.
     *
     * @param npcData the updated data for the NPC
     */
    public void updateNpcData(NpcData npcData)
    {
        npcLog.put(npcData.getNpcName(), npcData);
        saveLog();
    }
}

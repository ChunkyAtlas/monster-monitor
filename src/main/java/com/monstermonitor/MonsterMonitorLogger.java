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
import java.util.HashMap;
import java.util.Map;

/**
 * The MonsterMonitorLogger class is responsible for logging and managing data related to NPC kills
 * and unknown death animations. It handles reading from and writing to log files specific to each player,
 * and maintains the current state of the tracked NPCs and unknown animations.
 */
public class MonsterMonitorLogger
{
    private static final String BASE_LOG_DIR = System.getProperty("user.home") + "/.runelite/monstermonitor";
    private final Map<String, Integer> unknownAnimations = new HashMap<>(); // Map to store unknown animations
    private final Map<String, NpcData> npcLog = new HashMap<>(); // Log to store NPC data
    private final Gson gson = new Gson();
    private String lastKilledNpcName;  // Track the last killed NPC's name
    private String playerLogDir; // Directory to store logs specific to the player
    private String logFilePath; // Path to the main log file
    private String unknownAnimationsFilePath; // Path to log unknown animations

    @Inject
    private Client client;

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
        unknownAnimationsFilePath = playerLogDir + "/unknown_death_animations.json";

        File directory = new File(playerLogDir);
        if (!directory.exists())
        {
            directory.mkdirs(); // Create the directory if it doesn't exist
        }

        // Ensure the unknown animations file is created, even if not needed right now
        createEmptyFileIfNotExists(unknownAnimationsFilePath);
        createEmptyFileIfNotExists(logFilePath);

        // Load existing data
        loadLog();
        loadUnknownAnimations();
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
     * Loads the unknown animations log from the file.
     * This method ensures that unknown animations encountered in previous sessions are preserved.
     */
    private void loadUnknownAnimations()
    {
        if (unknownAnimationsFilePath == null)
        {
            return;
        }

        File unknownAnimationsFile = new File(unknownAnimationsFilePath);
        if (!unknownAnimationsFile.exists())
        {
            return;
        }

        try (FileReader reader = new FileReader(unknownAnimationsFile))
        {
            Type type = new TypeToken<Map<String, Integer>>(){}.getType();
            Map<String, Integer> loadedUnknownAnimations = gson.fromJson(reader, type);
            if (loadedUnknownAnimations != null)
            {
                unknownAnimations.putAll(loadedUnknownAnimations);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Saves the unknown animations log to the file.
     * This method is called whenever an unknown animation is logged, ensuring the data is persisted.
     */
    private void saveUnknownAnimations()
    {
        if (unknownAnimationsFilePath == null)
        {
            return;
        }

        try
        {
            File file = new File(unknownAnimationsFilePath);
            if (!file.exists())
            {
                file.createNewFile();  // Explicitly create the file if it doesn't exist
            }

            try (FileWriter writer = new FileWriter(file))
            {
                gson.toJson(unknownAnimations, writer);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Logs unknown animations that are not recognized as death animations.
     * Also logs the NPC death in the main log file.
     *
     * @param npcName the name of the NPC
     * @param animationId the ID of the unknown animation
     */
    public void logUnknownAnimations(String npcName, int animationId)
    {
        if (animationId == -1)
        {
            return; // Do not log if the animation ID is -1
        }

        // Check if the animation is already recognized
        if (!DeathAnimationIDs.isDeathAnimation(animationId))
        {
            // Log in the unknown animations file
            unknownAnimations.put(npcName, animationId);
            saveUnknownAnimations(); // Save the unknown animations log

            // Also log the NPC death in the main log file
            logDeath(npcName, animationId); // This will log the kill like any other NPC
        }
    }

    /**
     * Retrieves the last logged unknown animation for a given NPC.
     *
     * @param npcName the name of the NPC
     * @return the animation ID of the last unknown animation, or -1 if none is found
     */
    public int getLastUnknownAnimations(String npcName)
    {
        return unknownAnimations.getOrDefault(npcName, -1);
    }

    /**
     * Logs the death of an NPC by updating its kill count and saving the log.
     * Also tracks the last killed NPC's name for reference.
     *
     * @param npcName the name of the NPC
     * @param animationId the ID of the death animation
     */
    public void logDeath(String npcName, int animationId)
    {
        NpcData npcData = npcLog.getOrDefault(npcName, new NpcData(npcName, animationId));
        npcData.incrementKillCount(); // Increment the kill count
        lastKilledNpcName = npcName;  // Update the last killed NPC's name
        npcLog.put(npcName, npcData); // Update the NPC log
        saveLog(); // Save the updated log to disk immediately
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
     * Retrieves the name of the last killed NPC.
     *
     * @return the last killed NPC's name
     */
    public String getLastKilledNpcName() {
        return lastKilledNpcName;
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

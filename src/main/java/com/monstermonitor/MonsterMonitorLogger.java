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

public class MonsterMonitorLogger
{
    private static final String BASE_LOG_DIR = System.getProperty("user.home") + "/.runelite/monstermonitor";
    private final Map<String, Integer> unknownAnimations = new HashMap<>(); // Map to store unknown animations
    private final Map<String, NpcData> npcLog = new HashMap<>(); // Log to store NPC data
    private final Gson gson = new Gson();
    private String playerLogDir; // Directory to store logs specific to the player
    private String logFilePath; // Path to the main log file
    private String unknownAnimationsFilePath; // Path to log unknown animations

    @Inject
    private Client client;

    // Initialize the logger with directories based on the player's name
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

    // Helper method to create an empty file if it doesn't exist
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

    // Load the log from the file, or create a new file if it doesn't exist
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

    public int getLastUnknownAnimations(String npcName)
    {
        return unknownAnimations.getOrDefault(npcName, -1);
    }

    public void logDeath(String npcName, int animationId)
    {
        NpcData npcData = npcLog.getOrDefault(npcName, new NpcData(npcName, animationId));
        npcData.incrementKillCount(); // Increment the kill count
        npcLog.put(npcName, npcData); // Update the NPC log
        saveLog(); // Save the updated log to disk immediately
    }

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

    public Map<String, NpcData> getNpcLog()
    {
        return npcLog;
    }

    public void updateNpcData(NpcData npcData)
    {
        npcLog.put(npcData.getNpcName(), npcData);
        saveLog();
    }
}

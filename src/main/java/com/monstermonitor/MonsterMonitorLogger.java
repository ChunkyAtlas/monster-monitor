package com.monstermonitor;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.runelite.api.Client;

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
    private final Map<String, NpcData> npcLog = new HashMap<>(); // Log to store NPC data
    private final Gson gson = new Gson();
    private String playerLogDir; // Directory to store logs specific to the player
    private String logFilePath; // Path to the main log file
    private String unknownLogFilePath; // Path to log unknown animations

    @Inject
    private Client client;

    // Initialize the logger with directories based on the player's name
    public void initialize()
    {
        String playerName = client.getLocalPlayer().getName();
        playerLogDir = BASE_LOG_DIR + "/" + playerName;
        logFilePath = playerLogDir + "/monster_monitor_log.json";
        unknownLogFilePath = playerLogDir + "/unknown_animations.log";

        File directory = new File(playerLogDir);
        if (!directory.exists())
        {
            directory.mkdirs(); // Create the directory if it doesn't exist
        }
    }

    // Load the log from the file, or create a new file if it doesn't exist
    public void loadLog()
    {
        initialize();
        File logFile = new File(logFilePath);
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
                addDummyData();  // Add dummy data to the log on first use
                saveLog();  // Save the log with dummy data
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

    // Add dummy data to the log file (for testing purposes)
    private void addDummyData()
    {
        NpcData dummyNpc = new NpcData("Man", 836);
        npcLog.put("Man", dummyNpc);
    }

    // Save the current state of the log to the file
    public void saveLog()
    {
        try (FileWriter writer = new FileWriter(new File(logFilePath)))
        {
            gson.toJson(npcLog, writer);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    // Log a death for the specified NPC and increment the kill count
    public void logDeath(String npcName, int animationId)
    {
        NpcData npcData = npcLog.getOrDefault(npcName, new NpcData(npcName, animationId));
        npcData.incrementKillCount();
        npcLog.put(npcName, npcData);
        saveLog();
    }

    // Log unknown animations to a separate file
    public void logUnknownAnimation(String npcName, int animationId)
    {
        File unknownLogFile = new File(unknownLogFilePath);
        if (!unknownLogFile.exists())
        {
            try
            {
                unknownLogFile.createNewFile();  // Create the file if it does not exist
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return;
            }
        }

        try (FileWriter writer = new FileWriter(unknownLogFile, true)) // Append mode
        {
            writer.write("Unknown death animation detected for NPC: " + npcName + " (Animation ID: " + animationId + ")\n");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    // Get the kill limit for a specific NPC
    public int getKillLimit(String npcName)
    {
        return npcLog.containsKey(npcName) ? npcLog.get(npcName).getKillLimit() : 0;
    }

    // Get the progress towards the kill limit for a specific NPC
    public int getKillCountForLimit(String npcName)
    {
        return npcLog.containsKey(npcName) ? npcLog.get(npcName).getKillCountForLimit() : 0;
    }

    // Return the entire NPC log
    public Map<String, NpcData> getNpcLog()
    {
        return npcLog;
    }

    // Update the NPC data in the log and save it
    public void updateNpcData(NpcData npcData)
    {
        npcLog.put(npcData.getNpcName(), npcData);
        saveLog();
    }
}

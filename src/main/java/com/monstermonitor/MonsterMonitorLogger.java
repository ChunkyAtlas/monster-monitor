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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The MonsterMonitorLogger class is responsible for logging and managing data related to NPC kills.
 * It handles reading from and writing to log files specific to each player,
 * maintains the current state of the tracked NPCs, and handles NPC ignore/monitor states.
 */
public class MonsterMonitorLogger {
	private static final Logger logger = LoggerFactory.getLogger(MonsterMonitorLogger.class);
	private static final String BASE_LOG_DIR = System.getProperty("user.home") + "/.runelite/monstermonitor";
	private final Map<String, NpcData> npcLog = Collections.synchronizedMap(new LinkedHashMap<>());
	private static final ExecutorService saveQueue = Executors.newSingleThreadExecutor();

	private String playerLogDir;
	private String logFilePath;
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
	 * Initializes the logger, creating directories and loading logs.
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

	private void createEmptyFileIfNotExists(String filePath) {
		File file = new File(filePath);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				logger.error("Failed to create log file at {}", filePath, e);
			}
		}
	}

	/**
	 * Loads the NPC log from the file, creating the file if it doesn't exist.
	 * Initializes missing fields for compatibility.
	 */
	public void loadLog() {
		if (logFilePath == null) {
			return;
		}

		File logFile = new File(logFilePath);

		synchronized (npcLog) {
			// Create file if it doesn't exist
			if (!logFile.exists()) {
				createEmptyFileIfNotExists(logFilePath);
			}

			try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
				Type type = new TypeToken<Map<String, NpcData>>() {}.getType();
				Map<String, NpcData> loadedLog = gson.fromJson(reader, type);

				if (loadedLog != null) {
					loadedLog.forEach((npcName, npcData) -> npcLog.put(npcName, ensureNpcDataCompatibility(npcData)));
				}
			} catch (IOException e) {
				logger.error("Failed to load log data from {}", logFilePath, e);
			}
		}

		plugin.updateOverlayVisibility();
	}

	/**
	 * Ensures the compatibility of NpcData by initializing missing fields.
	 *
	 * @param npcData The NpcData to validate.
	 * @return A validated and compatible NpcData object.
	 */
	private NpcData ensureNpcDataCompatibility(NpcData npcData) {
		if (npcData.getKillLimit() == 0) npcData.setKillLimit(10);
		if (npcData.getKillCountForLimit() == 0) npcData.resetKillCountForLimit();
		if (!npcData.isLimitSet()) npcData.setLimitSet(false);
		if (!npcData.isIgnored()) npcData.setIgnored(false);
		if (!npcData.isNotifyOnLimit()) npcData.setNotifyOnLimit(false);
		return npcData;
	}

	/**
	 * Logs the death of an NPC, updating its kill count and queuing a save operation.
	 *
	 * @param npcName the name of the NPC
	 */
	public void logDeath(String npcName) {
		synchronized (npcLog) {
			NpcData npcData = npcLog.computeIfAbsent(npcName, NpcData::new);

			if (npcData.isIgnored()) {
				return; // Skip ignored NPCs
			}

			npcData.incrementKillCount();
			npcLog.remove(npcName);
			npcLog.put(npcName, npcData);
		}
		saveLogAsync();
		plugin.updateOverlay();
	}

	/**
	 * Saves the log asynchronously using a sequential processing queue.
	 */
	public void saveLogAsync() {
		saveQueue.submit(this::saveLog);
	}

	/**
	 * Saves the NPC log to disk in a thread-safe manner.
	 */
	public void saveLog() {
		synchronized (npcLog) {
			if (logFilePath == null) {
				return;
			}

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath))) {
				gson.toJson(npcLog, writer);
			} catch (IOException e) {
				logger.error("Failed to save log data to {}", logFilePath, e);
			}
		}
	}

	/**
	 * Retrieves the current NPC log.
	 *
	 * @return a map containing the NPC log data
	 */
	public synchronized Map<String, NpcData> getNpcLog() {
		return new LinkedHashMap<>(npcLog);
	}

	/**
	 * Removes an NPC from the log by name.
	 *
	 * @param npcName the name of the NPC to remove
	 */
	public void removeNpcFromLog(String npcName) {
		synchronized (npcLog) {
			npcLog.remove(npcName);
		}
	}

	/**
	 * Updates an NPC's data in the log and queues a save operation.
	 *
	 * @param npcData the updated NPC data
	 */
	public void updateNpcData(NpcData npcData) {
		synchronized (npcLog) {
			npcLog.put(npcData.getNpcName(), npcData);
		}
		saveLogAsync();
		plugin.updateOverlayVisibility();
	}
}

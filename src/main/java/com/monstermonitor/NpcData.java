package com.monstermonitor;

public class NpcData
{
    private final String npcName; // Name of the NPC
    private int totalKillCount; // Total kills of this NPC
    private int killCountForLimit; // Progress towards the kill limit
    private int killLimit; // The set kill limit for this NPC
    private final int deathAnimationId; // Death animation ID of the NPC
    private boolean limitSet; // Flag to track if the limit is set
    private boolean notifyOnLimit; // Flag to determine if notifications should be sent when the kill limit is reached

    public NpcData(String npcName, int deathAnimationId)
    {
        this.npcName = npcName;
        this.deathAnimationId = deathAnimationId;
        this.totalKillCount = 0;
        this.killCountForLimit = 0;
        this.killLimit = 0; // No limit by default
        this.limitSet = false; // Default to not set
        this.notifyOnLimit = false; // Default to not notify
    }

    // Getters and setters for the class fields
    public String getNpcName() { return npcName; }
    public int getTotalKillCount() { return totalKillCount; }
    public int getKillCountForLimit() { return killCountForLimit; }
    public void setKillCountForLimit(int killCountForLimit) { this.killCountForLimit = killCountForLimit; }
    public int getKillLimit() { return killLimit; }

    public void setKillLimit(int killLimit)
    {
        this.killLimit = killLimit;
        this.killCountForLimit = 0; // Reset progress when a new limit is set
    }

    public void incrementKillCount()
    {
        this.totalKillCount++;
        this.killCountForLimit++;
    }

    public int getDeathAnimationId() { return deathAnimationId; }
    public boolean isLimitSet() { return limitSet; }
    public void setLimitSet(boolean limitSet) { this.limitSet = limitSet; }
    public boolean isNotifyOnLimit() { return notifyOnLimit; }
    public void setNotifyOnLimit(boolean notifyOnLimit) { this.notifyOnLimit = notifyOnLimit; }

    // Resets the total kill count and progress towards the limit
    public void resetKillCount()
    {
        this.totalKillCount = 0;
        this.killCountForLimit = 0;
    }

    // Resets only the progress towards the limit
    public void resetKillCountForLimit()
    {
        this.killCountForLimit = 0;
    }
}

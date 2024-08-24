package com.example;

import com.monstermonitor.MonsterMonitorPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ExamplePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(MonsterMonitorPlugin.class);
		RuneLite.main(args);
	}
}
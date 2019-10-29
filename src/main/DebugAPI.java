package main;

import org.bukkit.Bukkit;

public class DebugAPI {

	public static void send(String s) {
		Bukkit.getConsoleSender().sendMessage(s);
	}
	
}

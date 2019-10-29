package main;

import org.bukkit.plugin.java.JavaPlugin;

import shops.ShopHandler;

public class DungeonRealms extends JavaPlugin{

	public static DungeonRealms instance;
	
	private ShopHandler shopHandler;
	
	public void onEnable() {
		instance = this;
		
		shopHandler = new ShopHandler();
		shopHandler.onEnable();
	}
	
	public void onDisable() {
		shopHandler.onDisable();
		
		instance = null;
	}
	
}

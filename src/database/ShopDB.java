package database;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import main.GameAPI;

public class ShopDB {

	public static void checkFile() {
		File file = new File(GameAPI.getInstance().getDataFolder() + "/collection_bin");
		
		if (!file.exists()) {
			file.mkdirs();
		}
	}
	
	public static void deleteCollectionBinFile(UUID uuid) {
		Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(GameAPI.getInstance(), new Runnable() {
			public void run() {
				File file = new File(GameAPI.getInstance().getDataFolder() + "/collection_bin/" + uuid + ".yml");
				
				if (file.exists()) {
					file.delete();
				}
			}	
		}, 1L);
	}
	
	public static boolean hasItemsInBin(UUID uuid) {
		File file = new File(GameAPI.getInstance().getDataFolder() + "/collection_bin/" + uuid + ".yml");
		
		if (file.exists()) {
			return true;
		} else {
			return false;
		}
	}
	
	public static List<ItemStack> getItemsInBin(UUID uuid){
			List<ItemStack> list = new ArrayList<>();
				
				File file = new File(GameAPI.getInstance().getDataFolder() + "/collection_bin/" + uuid + ".yml");
				YamlConfiguration config = new YamlConfiguration();
				
				if (file.exists()) {
					
					try {
						config.load(file);
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					for (int i = 0; i < 54; i++) {
						
						if (config.get("" + i) != null) {
							list.add(config.getItemStack("" + i));
						}
						
					}
					
				}
		
		return list;
	}
	
	public static void savePlayerFile(UUID uuid, List<ItemStack> list) {
		File file = new File(GameAPI.getInstance().getDataFolder() + "/collection_bin/" + uuid + ".yml");
		YamlConfiguration config = new YamlConfiguration();
		
			for (int i = 0; i < list.size(); i++) {
				config.set("" + i, list.get(i));
			}
					
			try {
				config.save(file);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	
}

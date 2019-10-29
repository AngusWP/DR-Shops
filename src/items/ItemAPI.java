package items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.md_5.bungee.api.ChatColor;

public class ItemAPI {
	
	public static ItemStack createGemNote(int amount) {
		ItemStack item = createCustomItem(Material.PAPER, 1, ChatColor.GREEN + "Gem Note", 
				Arrays.asList(ChatColor.WHITE.toString() + ChatColor.BOLD + "VALUE: " + ChatColor.WHITE + amount + " Gem(s)",
						       ChatColor.GRAY + "Exchange at any bank for gems."));
		return item;
	}
	
	public static int getGemNotePrice(ItemStack item) {
		
        if (item != null && item.getType() != Material.AIR) { 
        	if (item.getType() == Material.PAPER) {
        		
        		if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
        			List<String> lore = item.getItemMeta().getLore();
        			
        			for (int i = 0; i < lore.size(); i++) {
        				
		                String line = ChatColor.stripColor(lore.get(i));
		                
        				if (line.contains("VALUE")) {
        					
        					String[] s = line.split(" ");
        					
        					try {
        		                return Integer.parseInt(s[1]);
        		        
        		            } catch (Exception e) {
        						e.printStackTrace();
        		                return 0;
        		            }
        				}
        			}
        			
        		}
        	}
        		
       }
        return 0;
    }
	
	public static boolean isGemNote(ItemStack item) {
		
		if (item == null) return false;
		
		if (item.getType() == Material.PAPER) {
			if (item.hasItemMeta()) {
				if (item.getItemMeta().hasLore()) {
					
					List<String> lore = item.getItemMeta().getLore();
					
					for (int i = 0; i < lore.size(); i++) {
		                String line = ChatColor.stripColor(lore.get(i));
		                
		                if (line.contains("VALUE")) {
		                	return true;
		                }
					}
				}
			}
		}
		
		return false;
	}
	
	public static ItemStack createCustomItem(Material mat, int amount, String name, List<String> lore) {
		ItemStack i = new ItemStack(mat);
		ItemMeta m = i.getItemMeta();
		m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		m.setDisplayName(name);
		m.setLore(lore);
		i.setItemMeta(m);
		i.setAmount(amount);
		return i;
	}
	
	public static void removePriceTag(ItemStack item) {
		
		if (item.getItemMeta() == null) return;
		if (item.getItemMeta().getLore() == null) return;
		
		ItemMeta meta = item.getItemMeta();
		
		List<String> new_lore = new ArrayList<String>();
		new_lore.addAll(meta.getLore());
		
		for (int i = 0; i < new_lore.size(); i++) {
			if (new_lore.get(i).contains(ChatColor.GREEN + "Price: ")) {
				new_lore.remove(i);
			}
		}
		
		meta.setLore(new_lore);
		item.setItemMeta(meta);
		
	}
	
}

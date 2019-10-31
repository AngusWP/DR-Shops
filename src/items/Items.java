package items;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatColor;

public class Items {

	public static ItemStack SHOP_BOOK = ItemAPI.createCustomItem(Material.BOOK, 1, ChatColor.LIGHT_PURPLE + 
			 "Character Journal", Arrays.asList(ChatColor.GRAY + "Sneak right click to create a shop."));
	
}

package inventory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryAPI {
	
	public static boolean isEmpty(Inventory inv) {
		
		for (ItemStack i : inv.getContents()) {
			if (i != null) {
				return false;
			}
		}
	
		return true;
	}
	
}

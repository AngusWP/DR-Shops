package shops;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Chest.Type;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;

import database.ShopDB;
import items.ItemAPI;
import items.Items;
import main.GameAPI;

public class ShopAPI {

	public static void tryBuyItem(UUID uuid, Inventory shop, int amount_of_item, int price, int input, int slot) {
		Player pl = Bukkit.getPlayer(uuid);
		price = (price * input);
		
		if (input < 1 || input > amount_of_item) {
			pl.sendMessage(ChatColor.RED.toString() + input +" is not between 1 and " + amount_of_item + ".");
			pl.sendMessage(ChatColor.RED + "Item Buying - " + ChatColor.BOLD + "CANCELLED.");
			return;
		}
		
		if (pl.getInventory().firstEmpty() == -1) {
			pl.sendMessage(ChatColor.RED + "Your inventory is full.");
			return;
		}
		
		int user_money_in_emeralds = 0;
		int user_money_in_notes = 0;
		
		for (ItemStack item : pl.getInventory().getContents()) {
			if (item != null) {
				
				if (item.getType() == Material.PAPER) {
					user_money_in_notes += ItemAPI.getGemNotePrice(item);
					// this will return 0 if there's no lore, so this won't just add loads of money if it's vanilla paper.
				}
				
				if (item.getType() == Material.EMERALD) {
					user_money_in_emeralds += item.getAmount();
				}
			}
		}
		
		int user_money = user_money_in_emeralds + user_money_in_notes;
		
		if (price > user_money) {	
			pl.sendMessage(ChatColor.RED + "You do not have enough gems to buy that.");
			pl.sendMessage(ChatColor.RED.toString() + ChatColor.BOLD + "COST: " + ChatColor.RED + price + "G");
			return;
		}
		
		for (int i = 0; i < pl.getInventory().getSize(); i++) {
			if (pl.getInventory().getItem(i) != null) {
				
				if (pl.getInventory().getItem(i).getType() == Material.EMERALD){
					pl.getInventory().setItem(i, new ItemStack(Material.AIR));
				}
				
				if (ItemAPI.isGemNote(pl.getInventory().getItem(i))) {
					pl.getInventory().setItem(i, new ItemStack(Material.AIR));
				}
			}
			
		}
		
		ItemStack saved_item = ItemAPI.createCustomItem(getStoredItem(uuid).getType(), amount_of_item - input, 
				getStoredItem(uuid).getItemMeta().getDisplayName(), getStoredItem(uuid).getItemMeta().getLore());	
		
		
		if (saved_item.getAmount() > 0) {
			shop.setItem(slot, saved_item);
		} else {
			shop.setItem(slot, new ItemStack(Material.AIR));
		}

		saved_item.setAmount(input);
		removeStoredItem(uuid);
		ItemAPI.removePriceTag(saved_item);
		
		pl.getInventory().addItem(saved_item);
		pl.getInventory().addItem(ItemAPI.createGemNote((user_money - price)));
		pl.sendMessage(ChatColor.GREEN + "Transaction succesful.");
		pl.sendMessage(ChatColor.RED.toString() + ChatColor.BOLD + "COST: " + ChatColor.RED + price + "G");
		
	
		UUID owner = getShopOwnerFromInventory(shop);
		ItemStack gem_note = ItemAPI.createGemNote(price);
		
		try {
			Player o = Bukkit.getPlayer(owner);
			
			if (o != null) {
				
				o.sendMessage(ChatColor.GREEN + "You have sold a listing!");
				
				if (o.getInventory().firstEmpty() == -1) {
					o.sendMessage(ChatColor.RED + "Due to you have a full inventory, your cheque has been sent to your collection bin.");
				} else {
					o.sendMessage(ChatColor.GREEN + "Check your inventory for your cheque.");
					o.getInventory().addItem(gem_note);
				}
				
			} else {
				
				List<ItemStack> list = new ArrayList<>();
				
				if (ShopDB.hasItemsInBin(owner)) {
					list.addAll(ShopDB.getItemsInBin(owner));
					list.add(gem_note);
					
					ShopDB.deleteCollectionBinFile(owner); // so delete and readd all items with the gem note.
				} else {
					list.add(gem_note);
				}
				
				ShopDB.savePlayerFile(owner, list);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static boolean isCollectionBin(Inventory inv) {
		if (ShopHandler.collection_bin.contains(inv)) {
			return true;
		} else {
			return false;
		}
	}
	
	public static void toggleCollectionBin(Inventory inv) {
		if (isCollectionBin(inv)) {
			ShopHandler.collection_bin.remove(inv);
		} else {
			ShopHandler.collection_bin.add(inv);
		}
	}
	
	public static void openShop(UUID one, UUID two) {
		Player opener = Bukkit.getPlayer(one);
		
		opener.openInventory(getShopInventory(two));
	}
	
	public static UUID getShopOwnerFromInventory(Inventory inv) {
		for (UUID i : ShopHandler.shop_inv.keySet()) {
			if (getShopInventory(i).equals(inv)) {
				return i;
			}
		}
		
		return null;
	}
	
	public static void removeItem(UUID uuid, Inventory inv, int slot) {
		
		ItemStack item = null;
		
		for (int i = 0; i < inv.getSize(); i++) {
			if (i == slot) {
				item = inv.getItem(i); // make sure we know the exact spot.
				break;
			}
		}
		
		ItemAPI.removePriceTag(item);
		getShopInventory(uuid).setItem(slot, new ItemStack(Material.AIR));
		Bukkit.getPlayer(uuid).getInventory().addItem(item);
	}
	
	public static boolean hasPrice(ItemStack item) {
		if (item.hasItemMeta()){
			if (item.getItemMeta().hasLore()) {
				
				for (int i = 0; i < item.getItemMeta().getLore().size(); i++) {
					if (item.getItemMeta().getLore().get(0).contains(ChatColor.GREEN + "Price: ")) {
						return true;
					}
				}
				
			}
		}
		
		return false;
		
	}
	
	public static boolean isBuyableItem(Inventory inv, ItemStack item) {
		if (hasPrice(item)) {
			return true;
		}
		
		return false;
	}
	
	public static void toggleBuyingItem(UUID uuid) {
		if (isBuyingItem(uuid)) {
			ShopHandler.buying.remove(uuid);
			return;
		}
		
		ShopHandler.buying.add(uuid);
	}
	
	public static void setStoredItem(UUID uuid, ItemStack item) {
		ShopHandler.store_item.put(uuid, item);
	}
	
	public static ItemStack getStoredItem(UUID uuid) {
		ItemStack item = null;
		
		if (hasStoredItem(uuid)) {
			return ShopHandler.store_item.get(uuid);
		}
		
		return item;
	}

	public static boolean hasStoredItem(UUID uuid) {
		if (ShopHandler.store_item.containsKey(uuid)) {
			return true;
		}
		
		return false;
	}
	
	public static void removeStoredItem(UUID uuid) {
		if (hasStoredItem(uuid)) {
			ShopHandler.store_item.remove(uuid);
		}
	}
	
	public static void toggleSettingItemPrice(UUID uuid) {
		if (isSettingItemPrice(uuid)) {
			ShopHandler.setting_price.remove(uuid);
			return;
		}
		
		ShopHandler.setting_price.add(uuid);
		Player p = Bukkit.getPlayer(uuid);
		p.sendMessage(ChatColor.GREEN + "Enter the " + ChatColor.BOLD + "GEM " + ChatColor.GREEN + "value of [" + 
				ChatColor.BOLD + "1x" + ChatColor.GREEN + "] this item.");
		
	}
	
	public static boolean isBuyingItem(UUID uuid) {
		if (ShopHandler.buying.contains(uuid)) {
			return true;
		}
		
		return false;
	}
	
	public static boolean isSettingItemPrice(UUID uuid) {
		if (ShopHandler.setting_price.contains(uuid)) {
			return true;
		}
			
		return false;
	}
	
	public static boolean isLocked(UUID uuid) {
		if (getShopInventory(uuid).contains(Material.GRAY_DYE)) {
			return true;
		}
		
		return false;
	}
	
	public static String getShopName(UUID uuid) {
		Hologram h = ShopHandler.shop_name_data.get(uuid);
		return ShopHandler.shop_name.get(h);
	}
	
	public static void deleteShopName(UUID uuid) {
		Hologram h = ShopHandler.shop_name_data.get(uuid);
		h.delete();
	}
	
	public static void toggleShopStatus(UUID uuid) {
		ItemStack new_button;
		
		if (getShopInventory(uuid).contains(Material.GRAY_DYE)) {
			new_button = ItemAPI.createCustomItem(Material.LIME_DYE, 1, ChatColor.RED + 
					"Click to" + ChatColor.BOLD + " CLOSE" + ChatColor.RED +" this shop.", 
					Arrays.asList(ChatColor.GRAY + "Allows modifying shop stock."));
			
		} else {
			new_button = ItemAPI.createCustomItem(Material.GRAY_DYE, 1, ChatColor.GREEN + "Click here to " +
        			ChatColor.BOLD + "OPEN" + ChatColor.GREEN + " your Shop", 
        			Arrays.asList(ChatColor.GRAY + "This will open your shop to the public."));
			

			List<HumanEntity> viewers = new ArrayList<>();
			viewers.addAll(getShopInventory(uuid).getViewers());
			
			for (HumanEntity ent : viewers) {
				ent.closeInventory();
			}
		}
		
		getShopInventory(uuid).setItem(17, new_button);
		
		String name = getShopName(uuid);
		deleteShopName(uuid);
		setShopName(uuid, name, getShopLocation(uuid)); 
		// this is to update the colour.
	}
	
	public static void createShopInventory(UUID uuid) {
		Player p = Bukkit.getPlayer(uuid);
		Inventory inv = Bukkit.createInventory(null, 18, p.getName() + "'s Shop");
        
        ItemStack black_pane = ItemAPI.createCustomItem(Material.BLACK_STAINED_GLASS_PANE, 1, " ", Arrays.asList(" "));
        
        inv.setItem(9, ItemAPI.createCustomItem(Material.NAME_TAG, 1, ChatColor.GREEN + "Rename Shop",
        		Arrays.asList(ChatColor.GRAY + "Click here to rename your shop.")));
        
        inv.setItem(10, black_pane);
        inv.setItem(11, black_pane);
        inv.setItem(12, black_pane);
        inv.setItem(13, black_pane);
        inv.setItem(14, black_pane);
        inv.setItem(15, black_pane);
        
        inv.setItem(16, ItemAPI.createCustomItem(Material.BARRIER, 1, ChatColor.RED + "Delete Shop", 
        		Arrays.asList(ChatColor.GRAY + "Click here to safely delete your items.",
        				ChatColor.GRAY + "Your items will be sent to your collection bin.")));
        
        inv.setItem(17, ItemAPI.createCustomItem(Material.GRAY_DYE, 1, ChatColor.GREEN + "Click here to " +
        			ChatColor.BOLD + "OPEN" + ChatColor.GREEN + " your Shop", 
        			Arrays.asList(ChatColor.GRAY + "This will open your shop to the public.")));

        ShopHandler.shop_inv.put(uuid, inv);
	}
	
	public static Inventory getShopInventory(UUID uuid) {
		Inventory inv = null;
		
		if (hasShop(uuid)) {
			return ShopHandler.shop_inv.get(uuid);
		}
		
		return inv;
	}
	
	public static UUID getShopOwner(Location loc) {
		UUID uuid = null;
		
		if (isShop(loc)) {
			uuid = ShopHandler.shop_loc.get(loc);
		}
		
		return uuid;
	}
	
	public static boolean isNamingShop(UUID uuid) {
		if (ShopHandler.naming_shop.contains(uuid)) {
			return true;
		}
		
		return false;
	}
	
	public static boolean isShop(Location loc) {
		if (ShopHandler.shop_loc.containsKey(loc)) {
			return true;
		}
		
		return false;
	}
	
	public static boolean isShopInventory(Inventory inv) {
		if (ShopHandler.shop_inv.containsValue(inv)) {
			return true;
		}
		
		return false;
	}
	
	public static List<Location> getShopLocation(UUID uuid) {
		List<Location> loc = new ArrayList<>();
		
		if (hasShop(uuid)) {
			loc.addAll(ShopHandler.shop.get(uuid));
		}
		
		return loc;
	}
	
	public static UUID getPlayerFromLocation(Location loc) {
		
		UUID uuid = null;
		
		for (Location l : ShopHandler.shop_loc.keySet()) {
			if (l.equals(loc)) {
				return ShopHandler.shop_loc.get(loc); 
			}
		}
		
		return uuid;
	}
	
	public static void createShop(UUID uuid, Location loc1, Location loc2) {
		
		ShopHandler.shop_loc.put(loc1, uuid);
		ShopHandler.shop_loc.put(loc2, uuid);
		
		ShopHandler.shop.put(uuid, Arrays.asList(loc1, loc2));
		
		loc1.getBlock().setType(Material.CHEST);
		loc2.getBlock().setType(Material.CHEST);
		
		Chest c1 = (Chest) loc1.getBlock().getBlockData();
		c1.setType(Type.LEFT);
		loc1.getBlock().setBlockData(c1);
		
		// these make sure the chest is a double chest, not two singles.
		
		Chest c2 = (Chest) loc2.getBlock().getBlockData();
		c2.setType(Type.RIGHT);
		loc2.getBlock().setBlockData(c2);
		
		createShopInventory(uuid);
	}
	
	public static boolean canPlaceShopHere(Location loc) {
		
		if (ShopHandler.shop_loc.containsKey(loc)) {
			return false;
		}
		
		if (ShopHandler.shop_loc.containsKey(loc.subtract(0, -1, 0))) {
			// another shop already placed below it.
			return false;
		}
		
		// if there is a block to the left or right, dont't let us create the shop.
		
		if (loc.subtract(1, 0, 0).getBlock().getType() != Material.AIR) {
			return false;
		}
		
		if (loc.add(2, 0, 0).getBlock().getType() != Material.AIR) { 
			return false;
		}
		
		return true;
	}
	
	public static void toggleNamingShop(UUID uuid) {
		if (ShopHandler.naming_shop.contains(uuid)) {
			ShopHandler.naming_shop.remove(uuid);
			
		} else {
			ShopHandler.naming_shop.add(uuid);
			
			Player pl = Bukkit.getPlayer(uuid);
			pl.sendMessage(ChatColor.YELLOW + "Please enter a " + ChatColor.BOLD + "SHOP NAME." + ChatColor.YELLOW + " [3-16 characters.]");
		}
	}
	
	public static boolean isValidInteract(PlayerInteractEvent e, Action action) {
		if (!e.getHand().equals(EquipmentSlot.HAND)) return false;
		if (e.getAction() != action) return false;
		if (e.getPlayer().getInventory().getItemInMainHand() == null) return false;			
		
		return true;	
	}
	
	public static boolean hasJournal(UUID uuid) {
		Player pl = Bukkit.getPlayer(uuid);
		
		if (pl.getInventory().contains(Items.SHOP_BOOK)) {
			return true;
		}
		
		return false;
	}
	
	public static boolean isJournal(ItemStack item) {
		if (item != null) {
			if (item.equals(Items.SHOP_BOOK)) {
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean isShopOwner(UUID uuid, Inventory inv) {
		if (hasShop(uuid)) {
			if (getShopInventory(uuid).equals(inv)) {
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean hasNamedShop(UUID uuid) {
		if (ShopHandler.shop_name_data.containsKey(uuid)) {
			return true;
		}
		
		return false;
	}
	
	public static void deleteShop(UUID uuid) {
		
		List<Location> list = getShopLocation(uuid);
		
		list.get(0).getBlock().setType(Material.AIR);
		list.get(1).getBlock().setType(Material.AIR);
		
		list.remove(0);
		Inventory inv = getShopInventory(uuid);
		
		List<ItemStack> items = new ArrayList<>();
		
		// the (inv.getSize() - 9) means that any size it is, it will work.
		for (int i = 0; i < (inv.getSize() - 9); i++) {
			if (inv.getItem(i) != null) {
				ItemAPI.removePriceTag(inv.getItem(i));
				items.add(inv.getItem(i));
			}
		}
		
		if (items.size() > 0) {
			ShopDB.savePlayerFile(uuid, items);
			// if there are items in the shop - save them.
		}
		
		ShopHandler.shop.remove(uuid);
		
		if (hasNamedShop(uuid)) {
			deleteShopName(uuid);	
			ShopHandler.shop_name.remove(ShopHandler.shop_name_data.get(uuid));
			ShopHandler.shop_name_data.remove(uuid);
		}
	
		ShopHandler.shop_inv.remove(uuid);
	}
	
	public static void setShopName(UUID uuid, String s, List<Location> list) {
		ChatColor prefix;
		
		Location l = new Location(Bukkit.getWorld("world"), 0, 0, 0);
		l.setX(list.get(0).getX());
		l.setY(list.get(0).getY());
		l.setZ(list.get(0).getZ());
		
		if (ShopAPI.isLocked(getPlayerFromLocation(l))) {
			prefix = ChatColor.RED;
		} else {
			prefix = ChatColor.GREEN;
		}
		
		l.add(1, 1.5, 0.5);
		
		Bukkit.getServer().getScheduler().runTaskLater(GameAPI.getInstance(), new Runnable(){
			public void run() {
				Hologram h = HologramsAPI.createHologram(GameAPI.getInstance(), l);
				h.appendTextLine(prefix + s);
				ShopHandler.shop_name.put(h, s);
				ShopHandler.shop_name_data.put(uuid, h);
				// this is because you can't create a hologram in an async method, so we have to do it in a scheduler to stop that.
			}
		}, 0L);
	
		
	}
	
	public static void setPrice(ItemStack item, int price) {
		ItemAPI.removePriceTag(item);
		ItemMeta meta = item.getItemMeta();
		
		if (meta.hasLore()) {
			
			List<String> lore = new ArrayList<String>();
			lore.addAll(meta.getLore());
			lore.add(ChatColor.GREEN + "Price: " + ChatColor.WHITE + price + "G " + ChatColor.GREEN + "each");
			meta.setLore(lore);
			
		} else {
			meta.setLore(Arrays.asList(ChatColor.GREEN + "Price: " + ChatColor.WHITE + price + "G " + ChatColor.GREEN + "each"));
		}
		
		item.setItemMeta(meta);
	}

    public static Integer getPrice(ItemStack item){
        if (item.hasItemMeta() && item.getItemMeta().hasLore()){
            for (int i = 0; i < item.getItemMeta().getLore().size(); i++){
                try {
                    String line = ChatColor.stripColor(item.getItemMeta().getLore().get(i));
                    if (line.contains("Price:")) {
                        return Integer.parseInt(line.split("Price: ")[1].split("G")[0]);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        return 0;
    }

	
	public static boolean hasShop(UUID uuid) {
		if (ShopHandler.shop.containsKey(uuid)) {
			return true;
		}
		
		return false;
	}
	
}

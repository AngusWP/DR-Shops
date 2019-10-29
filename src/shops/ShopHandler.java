package shops;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.gmail.filoghost.holographicdisplays.api.Hologram;

import database.ShopDB;
import gui.ShopGUI;
import inventory.InventoryAPI;
import items.Items;
import main.GameAPI;
import net.md_5.bungee.api.ChatColor;

@SuppressWarnings("deprecation")
public class ShopHandler implements Listener{

	public static HashMap<UUID, List<Location>> shop = new HashMap<>();
	public static HashMap<Location, UUID> shop_loc = new HashMap<Location, UUID>();
	public static HashMap<UUID, Inventory> shop_inv = new HashMap<UUID, Inventory>();
	public static HashMap<Hologram, String> shop_name = new HashMap<Hologram, String>();
	public static HashMap<UUID, Hologram> shop_name_data = new HashMap<UUID, Hologram>();
	public static HashMap<UUID, ItemStack> store_item = new HashMap<UUID, ItemStack>();
	public static List<Inventory> collection_bin = new ArrayList<>();
	public static HashMap<UUID, UUID> buying_from = new HashMap<>();
	public static HashMap<UUID, Integer> slot_of_item = new HashMap<>();
	
	public static List<UUID> naming_shop = new ArrayList<>();
	public static List<UUID> buying = new ArrayList<>();
	public static List<UUID> setting_price = new ArrayList<>();
	public static List<UUID> not_new_item = new ArrayList<>();
	
	public void onEnable() {
		Bukkit.getServer().getPluginManager().registerEvents(this, GameAPI.getInstance());
		ShopDB.checkFile();
	}
	
	public void onDisable() {
		List<UUID> list = new ArrayList<>();
		list.addAll(shop.keySet());
		// stop concurrent mod exception
		
		for (UUID uuid : list) {
			ShopAPI.deleteShop(uuid);
		}
	}
	
	@EventHandler
	public void onBinClick(InventoryClickEvent e) {
		if (ShopAPI.isCollectionBin(e.getView().getTopInventory())) {
			// make sure it is a collection bin
			e.setCancelled(true);
			
			Inventory bot = e.getView().getBottomInventory();
			
			if (e.getClickedInventory() == null) return;
			if (e.getClickedInventory().equals(bot)) return;
			
			if (e.getClickedInventory().equals(e.getView().getTopInventory())) {
				ItemStack item = e.getCurrentItem();
				
				if (item != null) {
					
					if (e.getWhoClicked().getInventory().firstEmpty() == -1) {
						e.getWhoClicked().sendMessage(ChatColor.RED + "Your inventory is full.");
						return;
					}
					
					e.getView().getTopInventory().setItem(e.getSlot(), new ItemStack(Material.AIR));
					e.getWhoClicked().getInventory().addItem(item);
				}
			}
		}
	}
	
	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		if (ShopAPI.isCollectionBin(e.getView().getTopInventory())) {
			
			Inventory inv = e.getView().getTopInventory();
			
			if (InventoryAPI.isEmpty(inv)) {
				ShopDB.deleteCollectionBinFile(e.getPlayer().getUniqueId());
			} else {
				List<ItemStack> list = new ArrayList<>();
				
				for (ItemStack item : inv.getContents()) {
					if (item != null) {
						list.add(item);
					}
				}

				ShopDB.savePlayerFile(e.getPlayer().getUniqueId(), list);
			}
			
			ShopAPI.toggleCollectionBin(e.getView().getTopInventory());
		}
	}
	
	@EventHandler
	public void onBinOpen(PlayerInteractEvent e) {
		if (!ShopAPI.isValidInteract(e, Action.RIGHT_CLICK_BLOCK)) return;
		if (e.getClickedBlock().getType() != Material.JUKEBOX) return;
		
		if (!ShopDB.hasItemsInBin(e.getPlayer().getUniqueId())) {
			e.getPlayer().sendMessage(ChatColor.GRAY + "You don't have any items in your collection bin.");
			return;
		}
		
		Inventory inv = Bukkit.createInventory(null, 54, "Collection Bin");
		
		List<ItemStack> list = ShopDB.getItemsInBin(e.getPlayer().getUniqueId());
		
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) != null) {
				inv.addItem(list.get(i));
			}
		}
		
		ShopAPI.toggleCollectionBin(inv);
		// we add them to a list to make sure we know its a collection bin. not a renamed chest.
		e.getPlayer().openInventory(inv);
	}
	
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if (ShopAPI.isShopInventory(e.getView().getTopInventory())) {
			if (e.getCurrentItem() != null) {
			
				if (e.getClickedInventory().equals(e.getView().getTopInventory())) e.setCancelled(true);
				
					if (ShopAPI.isShopOwner(e.getWhoClicked().getUniqueId(), e.getView().getTopInventory())) {
						ShopGUI.manageOwner(e.getWhoClicked().getUniqueId(), e.getCurrentItem(), e.getView().getTopInventory(), e);	
					} else {
						ShopGUI.manageBuyer(e.getWhoClicked().getUniqueId(), e.getCurrentItem(), e.getView().getTopInventory(), e);
						e.setCancelled(true);
						// just not matter what stop them from clicking anything.
					}

			} 
		}
	}
	
	@EventHandler
	public void onDrag(InventoryDragEvent e) {
		if (ShopAPI.isShopInventory(e.getInventory())) {
			e.setCancelled(true);
			((Player) e.getWhoClicked()).updateInventory();
		}
		
		if (ShopAPI.isCollectionBin(e.getInventory())) {
			e.setCancelled(true);
			((Player) e.getWhoClicked()).updateInventory();
		}
	}
	
	@EventHandler
	public void onClickDisables(InventoryClickEvent e) {
		if (ShopAPI.isShopInventory(e.getView().getTopInventory())) {
			if (e.getClick() == ClickType.SHIFT_RIGHT || e.getClick() == ClickType.SHIFT_LEFT || 
					e.getClick() == ClickType.DOUBLE_CLICK) {
				e.setCancelled(true);
			} 
		}
	}
	
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		
		UUID uuid = e.getPlayer().getUniqueId();
		
		if (ShopAPI.isSettingItemPrice(uuid)) {
			e.getPlayer().getInventory().addItem(ShopAPI.getStoredItem(e.getPlayer().getUniqueId()));
			ShopAPI.toggleSettingItemPrice(uuid);
		}
		
		if (ShopAPI.isBuyingItem(uuid)) {
			ShopAPI.toggleBuyingItem(uuid);
		}
		
		if (ShopAPI.isNamingShop(uuid)) {
			ShopAPI.toggleNamingShop(uuid);
			// we know they are naming their shop, so this will always remove.
		}
		
		if (buying_from.containsKey(e.getPlayer().getUniqueId())) {
			buying_from.remove(e.getPlayer().getUniqueId());
		}
		
		if (slot_of_item.containsKey(e.getPlayer().getUniqueId())) {
			slot_of_item.remove(e.getPlayer().getUniqueId());
		}
	}
	
	@EventHandler
	public void onShopOpen(PlayerInteractEvent e) {
		if (!ShopAPI.isValidInteract(e, Action.RIGHT_CLICK_BLOCK)) return;
		if (!(e.getClickedBlock().getType() == Material.CHEST)) return;
		
		if (ShopAPI.isShop(e.getClickedBlock().getLocation())) {
			
			e.setCancelled(true); // stop the player opening the default chest.
			
			if (!ShopAPI.hasNamedShop(ShopAPI.getShopOwner(e.getClickedBlock().getLocation()))) return;

			if (ShopAPI.isBuyingItem(e.getPlayer().getUniqueId())) return;
			if (ShopAPI.isSettingItemPrice(e.getPlayer().getUniqueId())) return;
			if (ShopAPI.isNamingShop(e.getPlayer().getUniqueId())) return;
			
			// if they are naming, setting price or buying an item they can't open another.
			
			if (ShopAPI.isLocked(ShopAPI.getShopOwner(e.getClickedBlock().getLocation()))) {
				if (!ShopAPI.getShopOwner(e.getClickedBlock().getLocation()).equals(e.getPlayer().getUniqueId())) {
					e.getPlayer().sendMessage(ChatColor.RED + "That shop is currently closed.");
					return;
				}
			}
			
			ShopAPI.openShop(e.getPlayer().getUniqueId(), ShopAPI.getShopOwner(e.getClickedBlock().getLocation()));
			e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.BLOCK_CHEST_OPEN, 1F, 1F);
		}
	}
	
	@EventHandler
	public void onShopInventoryClose(InventoryCloseEvent e) {
		if (ShopAPI.isShopInventory(e.getView().getTopInventory())) {
			Player pl = (Player) e.getPlayer();
			pl.playSound(pl.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1F, 1F);
		}
	}
	
	@EventHandler
	public void onOpenShop(PlayerInteractEvent e) {
		if (!ShopAPI.isValidInteract(e, Action.RIGHT_CLICK_BLOCK)) return;
		
		if (!ShopAPI.isJournal(e.getPlayer().getInventory().getItemInMainHand())) return;
		if (!e.getPlayer().isSneaking()) return;
		
		if (ShopDB.hasItemsInBin(e.getPlayer().getUniqueId())) {
			e.getPlayer().sendMessage(ChatColor.GRAY + "You can't open a shop when you have items in your collection bin.");
			return;
		}
		
		if (ShopAPI.hasShop(e.getPlayer().getUniqueId())) {
			e.getPlayer().sendMessage(ChatColor.GRAY + "You already have a shop open.");
			return;
		}
		
		if (!ShopAPI.canPlaceShopHere(e.getClickedBlock().getLocation())) { 
			e.getPlayer().sendMessage(ChatColor.GRAY + "You cannot place a shop here.");
			return;
		}
		
		Location loc1 = e.getClickedBlock().getLocation().add(0, 1, 0);
		Location loc2 = e.getClickedBlock().getLocation().add(1, 1, 0);
		
		ShopAPI.createShop(e.getPlayer().getUniqueId(), loc1, loc2);
		e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ITEM_SHIELD_BLOCK, 1F, 1F);
		ShopAPI.toggleNamingShop(e.getPlayer().getUniqueId());
	}
	
	@EventHandler
	public void onPickup(PlayerPickupItemEvent e) {
		if (ShopAPI.isSettingItemPrice(e.getPlayer().getUniqueId())){
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onDrop(PlayerDropItemEvent e) {
		
		if (ShopAPI.isSettingItemPrice(e.getPlayer().getUniqueId())) {
			e.setCancelled(true);
		}
		
		if (ShopAPI.isBuyingItem(e.getPlayer().getUniqueId())) {
			e.setCancelled(true);
		}
		
		if (e.getItemDrop().getItemStack().getItemMeta() != null) {
			if (ShopAPI.isJournal(e.getItemDrop().getItemStack())) {
				e.getPlayer().sendMessage(ChatColor.GRAY + "You cannot drop this item.");
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {
		
		if (ShopAPI.isBuyingItem(e.getPlayer().getUniqueId())) {
			e.setCancelled(true);
			ShopAPI.toggleBuyingItem(e.getPlayer().getUniqueId());
			
			ItemStack item_buying = ShopAPI.getStoredItem(e.getPlayer().getUniqueId());
			
			int amount_of_item = item_buying.getAmount();
			int price = ShopAPI.getPrice(item_buying);

			String msg = e.getMessage();
			
			try {
				int input = Integer.parseInt(msg);
				
				ShopAPI.tryBuyItem(e.getPlayer().getUniqueId(), ShopAPI.getShopInventory(buying_from.get(e.getPlayer().getUniqueId())),
						amount_of_item, price, input, slot_of_item.get(e.getPlayer().getUniqueId()));
				
			} catch (NumberFormatException ex) {
				e.getPlayer().sendMessage(ChatColor.RED + "'" + msg + "' is not a number.");
				e.getPlayer().sendMessage(ChatColor.RED + "Item Buying - " + ChatColor.BOLD + "CANCELLED");
			}
			
			
			buying_from.remove(e.getPlayer().getUniqueId());
			slot_of_item.remove(e.getPlayer().getUniqueId());
		}
		
		if (ShopAPI.isSettingItemPrice(e.getPlayer().getUniqueId())) {
			e.setCancelled(true);
			
			String amount = e.getMessage();
			
			try {
				int price = Integer.parseInt(amount);
			
				if (price < 1 || price > 9999999) {
					e.getPlayer().sendMessage(ChatColor.RED + "" + price + " is not between 1 and 9999999." );
					e.getPlayer().sendMessage(ChatColor.RED + "Item Pricing - " + ChatColor.BOLD + "CANCELLED");
					e.getPlayer().getInventory().addItem(ShopAPI.getStoredItem(e.getPlayer().getUniqueId()));
				} else {
					
					e.getPlayer().sendMessage(ChatColor.GREEN + "Price set. Right click item to edit.");
					e.getPlayer().sendMessage(ChatColor.YELLOW + "Left Click the item to remove it from your shop.");
					
					ShopAPI.setPrice(ShopAPI.getStoredItem(e.getPlayer().getUniqueId()), price);	
			
					if (not_new_item.contains(e.getPlayer().getUniqueId())) {
						for (int i = 0; i < (ShopAPI.getShopInventory(e.getPlayer().getUniqueId()).getSize() - 9); i++) {
							if (ShopAPI.getShopInventory(e.getPlayer().getUniqueId()).getItem(i).equals(ShopAPI.getStoredItem(e.getPlayer().getUniqueId()))) {
								ShopAPI.getShopInventory(e.getPlayer().getUniqueId()).setItem(i, new ItemStack(Material.AIR));
								break;
							}
						}
						
						// so, if they are rechanging the item's price, add them to this arraylist.
						// when they are added, once they change the price, remove first item that matches that description
						// (so it won't remove multiple if they have two items of the same stack) and then add the item saved
						// back into the player's shop.
						
						not_new_item.remove(e.getPlayer().getUniqueId());
					}
				
					ShopAPI.getShopInventory(e.getPlayer().getUniqueId()).addItem(ShopAPI.getStoredItem(e.getPlayer().getUniqueId()));
				}
				
				
			} catch (NumberFormatException ex) {
				e.getPlayer().sendMessage(ChatColor.RED + "'" + amount + "' is not a valid number.");
				e.getPlayer().sendMessage(ChatColor.RED + "Item Pricing - " + ChatColor.BOLD + "CANCELLED");
				e.getPlayer().getInventory().addItem(ShopAPI.getStoredItem(e.getPlayer().getUniqueId()));
			}
			
			ShopAPI.removeStoredItem(e.getPlayer().getUniqueId());	
			ShopAPI.toggleSettingItemPrice(e.getPlayer().getUniqueId());
		}
		
		if (ShopAPI.isNamingShop(e.getPlayer().getUniqueId())) {
			e.setCancelled(true);
			ShopAPI.toggleNamingShop(e.getPlayer().getUniqueId());
			// whatever the player types, an outcome will happen so may as well remove them here.
			
			String entered_name = e.getMessage();
			
			if (entered_name.length() > 16 || entered_name.length() < 3) {
				e.getPlayer().sendMessage(ChatColor.RED + "Shop name must be between 3 and 16 characters.");
				Bukkit.getServer().getScheduler().runTaskLater(GameAPI.getInstance(), new Runnable() {
					public void run() {
					if (ShopAPI.hasNamedShop(e.getPlayer().getUniqueId())) return;
					ShopAPI.deleteShop(e.getPlayer().getUniqueId());
					}
				}, 1L);
				return;
			}
			
			
			if (!ShopAPI.hasNamedShop(e.getPlayer().getUniqueId())) {
				e.getPlayer().sendMessage(ChatColor.YELLOW + "Shop name assigned.");
				e.getPlayer().sendMessage(" ");
				e.getPlayer().sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "YOU'VE CREATED A SHOP!");
				e.getPlayer().sendMessage(ChatColor.YELLOW + "To stock your shop, simply click on the items in your inventory.");	
			} else {
				e.getPlayer().sendMessage(ChatColor.YELLOW + "Updated name shop.");
				
				ShopAPI.deleteShopName(e.getPlayer().getUniqueId());
			}
			
			ShopAPI.setShopName(e.getPlayer().getUniqueId(), entered_name, ShopAPI.getShopLocation(e.getPlayer().getUniqueId()));
		}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		if (!ShopAPI.hasJournal(e.getPlayer().getUniqueId())) {
			e.getPlayer().getInventory().addItem(Items.SHOP_BOOK);
		}
	}
	
}

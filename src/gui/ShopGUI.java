package gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatColor;
import shops.ShopAPI;
import shops.ShopHandler;

public class ShopGUI {

	public static void manageBuyer(UUID uuid, ItemStack item, Inventory inv, InventoryClickEvent e) {
		Player pl = Bukkit.getPlayer(uuid);
		
		if (e.getView().getTopInventory().equals(inv)) {
			
			if (pl.getInventory().firstEmpty() == -1) {
				pl.sendMessage(ChatColor.RED + "Your inventory is full.");
				return;
			}
			
			ShopAPI.setStoredItem(uuid, item);
			ShopAPI.toggleBuyingItem(uuid);
			
			ShopHandler.slot_of_item.put(uuid, e.getSlot());
			ShopHandler.buying_from.put(uuid, ShopAPI.getShopOwnerFromInventory(inv));
			
			
			pl.closeInventory();
			pl.sendMessage(ChatColor.GREEN + "Enter the " + ChatColor.BOLD + "QUANTITY " +
			     ChatColor.GREEN + "you would like to purchase.");
			pl.sendMessage(ChatColor.GRAY + "MAX: " + item.getAmount() + "X (" + (item.getAmount() * ShopAPI.getPrice(item)) + "), "
					+ "OR " + ShopAPI.getPrice(item) +"G/each.");
		}
	}
	
	public static void manageOwner(UUID uuid, ItemStack item, Inventory inv, InventoryClickEvent e) {
		Player pl = Bukkit.getPlayer(uuid);
		
		if (item.getType().equals(Material.BLACK_STAINED_GLASS_PANE)) return;
		
		if (item.getType().equals(Material.NAME_TAG)) {
			pl.closeInventory();
			ShopAPI.toggleNamingShop(uuid);
			return;
		}
		
		if (item.getType().equals(Material.GRAY_DYE) || item.getType().equals(Material.LIME_DYE)) {
			pl.playSound(pl.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F);
			ShopAPI.toggleShopStatus(uuid);
			return;
		}
		
		if (item.getType().equals(Material.BARRIER)) {
			
				if (!ShopAPI.isLocked(uuid)) {
					pl.sendMessage(ChatColor.RED + "You must close your shop if you wish to delete it.");
					return;
				}
			
				ShopAPI.deleteShop(uuid);
				pl.playSound(pl.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1F, 1F);
				pl.sendMessage(ChatColor.GREEN + "Your shop was removed.");
				
				List<HumanEntity> viewers = new ArrayList<>();
				viewers.addAll(inv.getViewers());
				// the reason we have to write it like this is because otherwise it will create a 
				// concurrent modification exception as it technically could be changed in that exact moment.
				
				for (HumanEntity ent : viewers) {
					ent.closeInventory();
					// close the inventory of everyone viewing it.
				}
				
				return;
		}
		
		// down here the only remaining options are available items/blank spaces.
		
		if (item != null) {
			
			Inventory top = e.getView().getTopInventory();
			Inventory bot = e.getView().getBottomInventory();
			
			if (e.getClickedInventory().equals(top)) { 
				if (!ShopAPI.isLocked(uuid)) {
					pl.sendMessage(ChatColor.RED + "You must close your shop to edit it.");
					e.setCancelled(true);
					return;
				}
				
				if (ShopAPI.isBuyableItem(top, item)) {
					if (e.getClick() == ClickType.RIGHT) {
						ShopHandler.not_new_item.add(uuid);
						ShopAPI.setStoredItem(uuid, item);
						ShopAPI.toggleSettingItemPrice(uuid);
						pl.closeInventory();
					}
					
					
					
					if (e.getClick() == ClickType.LEFT) {
						ShopAPI.removeItem(uuid, top, e.getSlot());
					}
					
					return;
				}
			} 
			
			if (e.getClickedInventory().equals(bot)) {
				e.setCancelled(true);
				
				if (ShopAPI.isJournal(item)) return;
				
				if (!ShopAPI.isLocked(uuid)) {
					pl.sendMessage(ChatColor.RED + "You must close your shop to edit it.");
					return;
				}
				
				if (ShopAPI.getShopInventory(uuid).firstEmpty() == -1) {
					pl.sendMessage(ChatColor.RED + "Your shop is full, so you cannot place another item in there.");
					return;
				}
				
				ShopAPI.setStoredItem(uuid, item);
				pl.getInventory().setItem(e.getSlot(), new ItemStack(Material.AIR));
				ShopAPI.toggleSettingItemPrice(uuid);
				pl.closeInventory();
				
				
			}
		}
	
		
		
		}
	}

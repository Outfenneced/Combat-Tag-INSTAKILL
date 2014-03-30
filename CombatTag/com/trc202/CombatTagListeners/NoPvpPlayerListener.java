package com.trc202.CombatTagListeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.trc202.CombatTag.CombatTagInstakill;

public class NoPvpPlayerListener implements Listener {
	private final CombatTagInstakill plugin;

	public NoPvpEntityListener entityListener;

	public NoPvpPlayerListener(CombatTagInstakill instance) {
		plugin = instance;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent e) {
		Player quitPlr = e.getPlayer();
		if (quitPlr.isDead()) {
			plugin.entityListener.onPlayerDeath(quitPlr);
		} else if (plugin.inTagged(quitPlr.getName())) {
			//Player is likely in pvp
			if (plugin.isInCombat(quitPlr.getName())) {
				//Player has logged out before the pvp battle is considered over by the plugin
				if (plugin.isDebugEnabled()) {
					plugin.log.info("[CombatTag] " + quitPlr.getName() + " has logged out during pvp!");
					plugin.log.info("[CombatTag] " + quitPlr.getName() + " has been instakilled!");
				}
				alertPlayers(quitPlr);
				quitPlr.damage(1000L);
				plugin.removeTagged(quitPlr.getName());
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerKick(PlayerKickEvent event) {
		Player player = event.getPlayer();
		if (plugin.inTagged(player.getName())) {
			if (plugin.isInCombat(player.getName())) {
				if (plugin.settings.dropTagOnKick()) {
					if (plugin.isDebugEnabled()) {
						plugin.log.info("[CombatTag] Player tag dropped for being kicked.");
					}
					plugin.removeTagged(player.getName());
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (event.getMaterial() == Material.ENDER_PEARL) {
				if (plugin.isInCombat(event.getPlayer().getName())) {
					if (plugin.settings.blockEnderPearl()) {
						event.getPlayer().sendMessage(ChatColor.RED + "[CombatTag] You can't ender pearl while tagged.");
						event.setCancelled(true);
					}
				}
			}
		}
	}


	@EventHandler(priority = EventPriority.LOW)
	public void onTeleport(PlayerTeleportEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (plugin.settings.blockTeleport() == true && plugin.isInCombat(event.getPlayer().getName()) && plugin.ctIncompatible.notInArena(event.getPlayer())) {
			TeleportCause cause = event.getCause();
			if ((cause == TeleportCause.PLUGIN || cause == TeleportCause.COMMAND)) { 
				if(event.getPlayer().getWorld() != event.getTo().getWorld()){
					event.getPlayer().sendMessage(ChatColor.RED + "[CombatTag] You can't teleport across worlds while tagged.");
					event.setCancelled(true);
				} else if(event.getFrom().distance(event.getTo()) > 8){ //Allow through small teleports as they are inconsequential, but some plugins use these
					event.getPlayer().sendMessage(ChatColor.RED + "[CombatTag] You can't teleport while tagged.");
					event.setCancelled(true);
				}
			}
		}
	}

	private void alertPlayers(Player quitPlr) {
		for(Player player: plugin.getServer().getOnlinePlayers()){
			if(player.hasPermission("combattag.alert")){
				player.sendMessage(ChatColor.RED + "[CombatTag] " + quitPlr.getName() + " has PvPLogged!");
			}
		}

	}
}

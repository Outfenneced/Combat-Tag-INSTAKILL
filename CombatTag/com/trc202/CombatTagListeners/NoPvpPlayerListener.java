package com.trc202.CombatTagListeners;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
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
import org.bukkit.event.player.PlayerToggleFlightEvent;

import com.trc202.CombatTag.CombatTag;

public class NoPvpPlayerListener implements Listener {
	private final CombatTag plugin;

	public NoPvpEntityListener entityListener;

	public NoPvpPlayerListener(CombatTag instance) {
		plugin = instance;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent e) {
		Player quitPlr = e.getPlayer();
		if(quitPlr.hasPermission("combattag.ignore.pvplog")){ return; }
		if (quitPlr.isDead()) {
			plugin.entityListener.onPlayerDeath(quitPlr);
		} else if (plugin.inTagged(quitPlr.getUniqueId())) {
			//Player is likely in pvp
			if (plugin.isInCombat(quitPlr.getUniqueId())) {
				//Player has logged out before the pvp battle is considered over by the plugin
				if (plugin.isDebugEnabled()) {
					plugin.log.info("[CombatTag] " + quitPlr.getName() + " has logged out during pvp!");
					plugin.log.info("[CombatTag] " + quitPlr.getName() + " has been instakilled!");
				}
				alertPlayers(quitPlr);
				quitPlr.damage(1000L);
				plugin.removeTagged(quitPlr.getUniqueId());
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerKick(PlayerKickEvent event) {
		Player quitPlr = event.getPlayer();
		UUID playerUUID = quitPlr.getUniqueId();
		if (quitPlr.isDead() || quitPlr.getHealth() <= 0) {
			plugin.entityListener.onPlayerDeath(quitPlr);
			return;
		}
		if (plugin.isInCombat(playerUUID)) {
			//Player has logged out before the pvp battle is considered over by the plugin
			alertPlayers(quitPlr);
			if (plugin.isDebugEnabled()) {plugin.log.info("[CombatTag] " + quitPlr.getName() + " has been instakilled!");}
			quitPlr.damage(1000L);
			plugin.removeTagged(playerUUID);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (event.getMaterial() == Material.ENDER_PEARL) {
				if (plugin.isInCombat(event.getPlayer().getUniqueId())) {
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
		if (plugin.settings.blockTeleport() && plugin.isInCombat(event.getPlayer().getUniqueId()) && plugin.ctIncompatible.notInArena(event.getPlayer())) {
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
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerToggleFly(PlayerToggleFlightEvent event){
		Player player = event.getPlayer();
		if(plugin.settings.blockFly() && plugin.isInCombat(player.getUniqueId()) && event.isFlying()){
			player.sendMessage(ChatColor.RED + "[CombatTag] You can't fly while tagged!");
			player.setFlying(false);
			event.setCancelled(true);
		}
	}

	private void alertPlayers(Player quitPlr) {
		for(Player player: plugin.getServer().getOnlinePlayers()){
			if(player.hasPermission("combattag.alert")){
				Location loc = quitPlr.getLocation();
				player.sendMessage(ChatColor.RED + "[CombatTag] " + quitPlr.getName() + " has PvPLogged at: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "!");
			}
		}
	}
}

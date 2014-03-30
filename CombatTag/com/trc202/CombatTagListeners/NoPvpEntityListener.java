package com.trc202.CombatTagListeners;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import com.trc202.CombatTag.CombatTag;

public class NoPvpEntityListener implements Listener{

	CombatTag plugin;
	
	public NoPvpEntityListener(CombatTag combatTag){
		this.plugin = combatTag;
	}
	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityDamage(EntityDamageEvent EntityDamaged){
		if (EntityDamaged.isCancelled() || (EntityDamaged.getDamage() == 0)){return;}
		if (EntityDamaged instanceof EntityDamageByEntityEvent){
    		EntityDamageByEntityEvent e = (EntityDamageByEntityEvent)EntityDamaged;
    		Entity dmgr = e.getDamager();
    		if(dmgr instanceof Projectile)
    		{
    			dmgr = (Entity) ((Projectile)dmgr).getShooter();
    		}
    		if ((dmgr instanceof Player) && (e.getEntity() instanceof Player) && plugin.settings.playerTag()){//Check to see if the damager and damaged are players
    			Player damager = (Player) dmgr;
    			Player tagged = (Player) e.getEntity();
    			if(damager != tagged && damager != null){
    				for(String disallowedWorlds : plugin.settings.getDisallowedWorlds()){
    					if(damager.getWorld().getName().equalsIgnoreCase(disallowedWorlds)){
    						//Skip this tag the world they are in is not to be tracked by combat tag
    						return;
    					}
    				}
	    			onPlayerDamageByPlayer(damager,tagged);
    			}
    		} else if ((dmgr instanceof LivingEntity) && (e.getEntity() instanceof Player) && plugin.settings.mobTag()){
    			LivingEntity damager = (LivingEntity) dmgr;
    			Player tagged = (Player) e.getEntity();
    			if(damager != tagged && damager != null){
    				for(String disallowedWorlds : plugin.settings.getDisallowedWorlds()){
    					if(damager.getWorld().getName().equalsIgnoreCase(disallowedWorlds)){
    						//Skip this tag the world they are in is not to be tracked by combat tag
    						return;
    					}
    				}
	    			onPlayerDamageByMob(damager,tagged);
    			}
    		}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDeath(EntityDeathEvent event){
		//if Player died with a tag duration, cancel the timeout and remove the data container
		if(event.getEntity() instanceof Player){
			onPlayerDeath((Player) event.getEntity());
		}
	}
	
	public void onPlayerDeath(Player deadPlayer){
		plugin.removeTagged(deadPlayer.getName());
	}
	
	private void onPlayerDamageByPlayer(Player damager, Player damaged){
		
		if(plugin.ctIncompatible.WarArenaHook(damager) && plugin.ctIncompatible.WarArenaHook(damaged)){
			
			if(!damager.hasPermission("combattag.ignore")){	
				if(plugin.settings.blockCreativeTagging() && damager.getGameMode() == GameMode.CREATIVE){damager.sendMessage(ChatColor.RED + "[CombatTag] You can't tag players while in creative mode!");return;}
				
				if(plugin.settings.isSendMessageWhenTagged() && !plugin.isInCombat(damager.getName())){
					String tagMessage = plugin.settings.getTagMessageDamager();
					tagMessage = tagMessage.replace("[player]", "" + damaged.getName());
					damager.sendMessage(ChatColor.RED + "[CombatTag] " + tagMessage);
				}
				if(plugin.isDebugEnabled()){
					plugin.log.info("[CombatTag] " + damager.getName() + " tagged " + damaged.getName() + ", setting pvp timeout");
				}
				plugin.addTagged(damager);
			}
			if(!damaged.hasPermission("combattag.ignore") && !plugin.settings.onlyDamagerTagged()){	
				if(!plugin.isInCombat(damaged.getName())){
					if(plugin.settings.isSendMessageWhenTagged()){
						String tagMessage = plugin.settings.getTagMessageDamaged();
						tagMessage = tagMessage.replace("[player]", damager.getName());
						damaged.sendMessage(ChatColor.RED + "[CombatTag] " + tagMessage);
					}
				}
				plugin.addTagged(damaged);
			}
		}
	}
	
	private void onPlayerDamageByMob(LivingEntity damager, Player damaged) {
		if(damager == null){return;}
		if(plugin.ctIncompatible.WarArenaHook(damaged)){
			if(!damaged.hasPermission("combattag.ignoremob")){	
				if(!plugin.isInCombat(damaged.getName())){
					if(plugin.settings.isSendMessageWhenTagged()){
						String tagMessage = plugin.settings.getTagMessageDamaged();
						tagMessage = tagMessage.replace("[player]", damager.getType().name());
						damaged.sendMessage(ChatColor.RED + "[CombatTag] " + tagMessage);
					}
					if(plugin.isDebugEnabled()){
						plugin.log.info("[CombatTag] " + damager.getType().name() + " tagged " + damaged.getName() + ", setting pvp timeout");
					}
				}
				plugin.addTagged(damaged);
			}
		}
	}
}
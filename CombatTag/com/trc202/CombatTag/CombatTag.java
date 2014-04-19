package com.trc202.CombatTag;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import com.google.common.collect.ImmutableList;
import com.trc202.CombatTagListeners.CombatTagCommandPrevention;
import com.trc202.CombatTagListeners.NoPvpBlockListener;
import com.trc202.CombatTagListeners.NoPvpEntityListener;
import com.trc202.CombatTagListeners.NoPvpPlayerListener;
import com.trc202.helpers.Settings;
import com.trc202.helpers.SettingsHelper;
import com.trc202.helpers.SettingsLoader;

public class CombatTag extends JavaPlugin {
    private SettingsHelper settingsHelper;
    private File settingsFile;
    public Settings settings;
    public final Logger log = Logger.getLogger("Minecraft");
    private HashMap<UUID, Long> tagged;
    private static String mainDirectory = "plugins/CombatTagInstakill";
    private static final List<String> SUBCOMMANDS = ImmutableList.of("reload", "command");
    private static final List<String> COMMAND_SUBCOMMANDS = ImmutableList.of("add", "remove");

    
    public final CombatTagIncompatibles ctIncompatible = new CombatTagIncompatibles(this);
    private final NoPvpPlayerListener plrListener = new NoPvpPlayerListener(this);
    public final NoPvpEntityListener entityListener = new NoPvpEntityListener(this);
    private final NoPvpBlockListener blockListener = new NoPvpBlockListener(this);
    private final CombatTagCommandPrevention commandPreventer = new CombatTagCommandPrevention(this);

    public CombatTag() {
        settings = new Settings();
        new File(mainDirectory).mkdirs();
        settingsFile = new File(mainDirectory + File.separator + "settings.prop");
        settingsHelper = new SettingsHelper(settingsFile, "CombatTag");
    }
    
    @Override
    public void onDisable() {
        //Just in case...
        log.info("[CombatTag] Disabled");
    }

    @Override
    public void onEnable() {
        tagged = new HashMap<UUID, Long>();
        settings = new SettingsLoader().loadSettings(settingsHelper, this.getDescription().getVersion());
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(plrListener, this);
        pm.registerEvents(entityListener, this);
        pm.registerEvents(commandPreventer, this);
        pm.registerEvents(blockListener, this);
        log.info("[" + getDescription().getName() + "]" + " has loaded with a tag time of " + settings.getTagDuration() + " seconds");
    }

    public boolean addTagged(Player player){
    	if(player.isOnline()){
    		tagged.remove(player.getUniqueId());
    		tagged.put(player.getUniqueId(), PvPTimeout(getTagDuration()));
    		return true;
    	}
    	return false;
    }

    public long getRemainingTagTime(UUID uuid){
    	if(tagged.get(uuid) == null){return -1;}
    	return (tagged.get(uuid) - System.currentTimeMillis());
	}
    
    public SettingsHelper getSettingsHelper(){
    	return this.settingsHelper;
    }
    
    /**
     *
     * @return the system tag duration as set by the user
     */
    public int getTagDuration() {
        return settings.getTagDuration();
    }
    
    public boolean inTagged(UUID uuid){
    	return tagged.containsKey(uuid);
    }
    
    public boolean isDebugEnabled() {
        return settings.isDebugEnabled();
    }
    
    public boolean isInCombat(UUID uuid){
    	if(getRemainingTagTime(uuid) < 0){
    		tagged.remove(uuid);
    		return false;
    	} else {
    		return true;
    	}
    }
    
	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		if (command.getName().equalsIgnoreCase("ct") || (command.getName().equalsIgnoreCase("combattag"))) {
			if (args.length == 0) {
				if (sender instanceof Player) {
					if(isInCombat(((Player) sender).getUniqueId())) {
						String message = settings.getCommandMessageTagged();
						message = message.replace("[time]", "" + (getRemainingTagTime(((Player) sender).getUniqueId()) / 1000));
						sender.sendMessage(message);
					} else {
						tagged.remove(((Player) sender).getUniqueId());
						sender.sendMessage(settings.getCommandMessageNotTagged());
					}
				} else {
					log.info("[CombatTag] /ct can only be used by a player!");
				}
				return true;
			} else if (args[0].equalsIgnoreCase("reload")) {
				if (sender.hasPermission("combattag.reload")) {
					settings = new SettingsLoader().loadSettings(settingsHelper, this.getDescription().getVersion());
					if (sender instanceof Player) {
						sender.sendMessage(ChatColor.RED + "[CombatTag] Settings were reloaded!");
					} else {
						log.info("[CombatTag] Settings were reloaded!");
					}
				} else {
					if (sender instanceof Player) {
						sender.sendMessage(ChatColor.RED + "[CombatTag] You don't have the permission 'combattag.reload'!");
					}
				}
				return true;
			} else if (args[0].equalsIgnoreCase("command")) {
				if (sender.hasPermission("combattag.command")) {
					if (args.length > 2) {
						if (args[1].equalsIgnoreCase("add")) {
							if (args[2].length() == 0 || !args[2].startsWith("/")) {
								sender.sendMessage(ChatColor.RED + "[CombatTag] Correct Usage: /ct command add /<command>");
							} else {
								String disabledCommands = settingsHelper.getProperty("disabledCommands");
								if (!disabledCommands.contains(args[2])) {
									disabledCommands = disabledCommands.substring(0, disabledCommands.length() - 1) + "," + args[2] + "]";
									disabledCommands = disabledCommands.replace("[,", "[");
									disabledCommands = disabledCommands.replaceAll(",,", ",");
									settingsHelper.setProperty("disabledCommands", disabledCommands);
									settingsHelper.saveConfig();
									sender.sendMessage(ChatColor.RED + "[CombatTag] Added " + args[2] + " to combat blocked commands.");
									settings = new SettingsLoader().loadSettings(settingsHelper, this.getDescription().getVersion());
								} else {
									sender.sendMessage(ChatColor.RED + "[CombatTag] That command is already in the blocked commands list.");
								}
							}
						} else if (args[1].equalsIgnoreCase("remove")) {
							if (args[2].length() == 0 || !args[2].startsWith("/")) {
								sender.sendMessage(ChatColor.RED + "[CombatTag] Correct Usage: /ct command remove /<command>");
							} else {
								String disabledCommands = settingsHelper.getProperty("disabledCommands");
								if (disabledCommands.contains(args[2] + ",") || disabledCommands.contains(args[2] + "]")) {
									disabledCommands = disabledCommands.replace(args[2] + ",", "");
									disabledCommands = disabledCommands.replace(args[2] + "]", "]");
									disabledCommands = disabledCommands.replace(",]", "]");
									disabledCommands = disabledCommands.replaceAll(",,", ",");
									settingsHelper.setProperty("disabledCommands", disabledCommands);
									settingsHelper.saveConfig();
									sender.sendMessage(ChatColor.RED + "[CombatTag] Removed " + args[2] + " from combat blocked commands.");
									settings = new SettingsLoader().loadSettings(settingsHelper, this.getDescription().getVersion());
								} else {
									sender.sendMessage(ChatColor.RED + "[CombatTag] That command is not in the blocked commands list.");
								}
							}
						}
					} else {
						sender.sendMessage(ChatColor.RED + "[CombatTag] Correct Usage: /ct command <add/remove> /<command>");
					}
				}
				return true;
			} else {
				sender.sendMessage(ChatColor.RED + "[CombatTag] That is not a valid command!");
				return true;
			}
		}
		return false;
	}

    @Override
    public final List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    	if (args.length == 1) {
    		return StringUtil.copyPartialMatches(args[0], SUBCOMMANDS, new ArrayList<String>(SUBCOMMANDS.size()));
    	} else if (args.length == 2) {
    		System.out.println(args[1]);
    		if (args[0].equalsIgnoreCase("command")) {
    			return StringUtil.copyPartialMatches(args[1], COMMAND_SUBCOMMANDS, new ArrayList<String>(COMMAND_SUBCOMMANDS.size()));
    		}
    	}
    	return ImmutableList.of();
    }
    
    public long PvPTimeout(int seconds){
		return System.currentTimeMillis() + (seconds * 1000);
	}
    
    public long removeTagged(UUID uuid){
    	if(inTagged(uuid)){
    		return tagged.remove(uuid);
    	}
    	return -1;
    }
}

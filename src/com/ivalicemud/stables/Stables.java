package com.ivalicemud.stables;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Boat;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.EnderSignal;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Giant;
import org.bukkit.entity.Golem;
import org.bukkit.entity.Horse;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Monster;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Pig;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Squid;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Weather;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;


public class Stables extends JavaPlugin implements Listener
{    Connection conn;

private FileConfiguration horseConfig;
private File horseConfigFile;
static ArrayList<String> randomNames = new ArrayList<String>();

private FileConfiguration localConfig = null;
private File localConfigFile = null;
public File randomNameFile;
public YamlConfiguration randomNameConfig;
public static Economy economy = null;
static boolean flatfile;
static boolean setup;
static boolean outOfDate = false;
static String currentVersion = "";
public static Stables plugin;
ResultSet rs;
private WorldGuardPlugin worldguard;

    public Stables()
    {
    }
    
    public void convertConfig() {
    	
    	if ( getConfig().contains("randomNames")) //Remove randomNames from main config and create a new file, for cleanliness
    	{
    	   randomNameConfig.set("randomNames",getConfig().getStringList("randomNames"));
    	   getConfig().set("randomNames",null);
    	}
    		
    }
    

    public void onEnable()
    {
    	// Begin Setup
    	plugin = this;
        flatfile = false;
        setup = false;
        getServer().getPluginManager().registerEvents(new EventListener(), this);
        randomNameFile = new File(getDataFolder(), "randomNames.yml");
	    randomNameConfig = new YamlConfiguration();

        LoadConfiguration();
        
        SetupRecipes();
        if(!setup)
            OpenDatabase();
        if(flatfile)
        {
        	//setup Save Crap
        }
        setupEconomy();
        updateCheck();
        checkWorldGuard();
        
        if(outOfDate)
        {
            getServer().getLogger().info("Stables is currently out of date! You are using version " + plugin.getDescription().getVersion() + " - the newst version is " + currentVersion);
            getServer().getLogger().info("You can download the newest version at http://dev.bukkit.org/bukkit-plugins/stables/files/");
        }
        
        if (flatfile) // Add a timer to save the flatfile database
        {
        	getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
        	{ 
        		@Override
                public void run() {
                        saveHorseConfig();
                }
        	}	, 60L, getConfig().getInt("general.Save") * (20 * 60));
        }
    } 
     
    public void onDisable()
    {
    	CloseDatabase();
    	saveHorseConfig();
    }
    
    public void setConfig(String line, Object set) // Easy function to set Config options if they don't exist
    {
        if(!getConfig().contains(line))
            getConfig().set(line, set);
    }

    public void setLang(String line, String local ) 
    {
        if(!getlocalConfig().contains(line))
            getlocalConfig().set(line, local);
    }

    public void LoadConfiguration() // Create with defaults and/or load the config file
    {
        convertConfig();
        randomNames.clear();

        setConfig("general.Debug", false );
        setConfig("general.BlockAll",false);
        setConfig("general.PVPDamage", true);
        setConfig("general.EnviromentDamage",true);
        setConfig("general.OwnerDamage",false);
        setConfig("general.MobDamage",false);
        setConfig("general.Theft",false);
        setConfig("general.CheckUpdates",true);

        setConfig("general.MaxOwned.default",10);
        setConfig("general.ProtectUnclaimed",false);
        setConfig("general.Save",10);
        setConfig("general.showUnownedWarning",true);
        
        setConfig("MySQL.useSQLite", true );
        setConfig("MySQL.useMySQL", false );
        setConfig("MySQL.database", "YourDBName");
        setConfig("MySQL.host", "localhost");
        setConfig("MySQL.user", "root");
        setConfig("MySQL.password", "abcd1234");
        setConfig("MySQL.port", 0 );
        setConfig("MySQL.prefix", "stables_");
        	
        setConfig("horses.tame.AllowMaxNamed",false);
        setConfig("horses.allowFind",false);
        setConfig("horses.allowTP",false);
        setConfig("horses.allowSummon",false);
        
        setConfig("horses.AutoOwn",false);
        setConfig("horses.AutoSaddle",false);
        setConfig("horses.NoTagRename",false);
        
        setConfig("horses.lure.allow", true);
        setConfig("horses.lure.delay", 10);
        setConfig("horses.lure.disabled", "world_nether, world_the_end");
        setConfig("horses.lure.enabled", "world");
        setConfig("horses.lure.useEnabled",false);

        setConfig("lure.396.chance",50);
        setConfig("lure.396.type",1);
        setConfig("lure.396.health.max",30);
        setConfig("lure.396.health.min",15);

        /*
        setConfig("horses.lure.chance", 50);
        setConfig("horses.lure.item", 396);
        setConfig("horses.lure.health.max", 30);
        setConfig("horses.lure.health.min", 15);
        */
        
       	setConfig("stable.cost", 0);
       	setConfig("stable.useCommand", false);
       	setConfig("stable.timeout", 10);
       	setConfig("stable.disabled", "world_nether, world_the_end");
       	setConfig("stable.useEnabled", false);
       	setConfig("stable.enabled", "world");
            
        setConfig("recipe.usePerms",false);
        setConfig("recipe.hardMode",false);
        setConfig("recipe.saddle",true);
        setConfig("recipe.nametag",true);
        setConfig("recipe.armor.iron",true);
        setConfig("recipe.armor.gold",true);
        setConfig("recipe.armor.diamond",true);
        
        addRandomNames();
        saveConfig();
        getHorseConfig();
        getlocalConfig();
        setupLanguage();
        
    }
    
 
    
    public void reloadlocalConfig() {
            if (localConfigFile == null) {
        localConfigFile = new File(getDataFolder(), "language.yml");
        }
        localConfig = YamlConfiguration.loadConfiguration(localConfigFile);
       
    }
    
    public FileConfiguration getlocalConfig() {
        if (localConfig == null) {
            reloadlocalConfig();
        }
        return localConfig;
    }
    
    
    public void savelocalConfig() {
        if (localConfig == null || localConfigFile == null) {
            return;
        }
        try {
            getlocalConfig().save(localConfigFile);
        } catch (IOException ex) {
            error("Could not save config file to " + localConfigFile);
        }
    }
    
    public String getLang(String phrase) {
    	if ( getlocalConfig().contains(phrase) )
    		return getlocalConfig().getString(phrase);
    	else
    		return "Localization Error: " + phrase + " missing.";
    }
    
 
    public void SetupRecipes()
    {
        if(getConfig().getBoolean("recipe.saddle"))
        {
            ShapedRecipe Saddle = new ShapedRecipe(new ItemStack(Material.SADDLE));
            Saddle.shape(new String[] {
                "LLL", "LIL", "I I"
            });
            Saddle.setIngredient('L', Material.LEATHER);
            Saddle.setIngredient('I', Material.IRON_INGOT);
            getServer().addRecipe(Saddle);
            getServer().getLogger().info((new StringBuilder(String.valueOf(getLang("RECIPE_ADDED")))).append(" 'SADDLE'").toString());
        }
        if(getConfig().getBoolean("recipe.nametag"))
        {
            ShapedRecipe NameTag = new ShapedRecipe(new ItemStack(Material.NAME_TAG));
            NameTag.shape(new String[] {
                "  S", " P ", "P  "
            });
            NameTag.setIngredient('S', Material.STRING);
            NameTag.setIngredient('P', Material.PAPER);
            getServer().addRecipe(NameTag);
            getServer().getLogger().info((new StringBuilder(String.valueOf(getLang("RECIPE_ADDED")))).append(" 'NAME_TAG'").toString());
        }
        if(getConfig().getBoolean("recipe.armor.iron"))
        {
            ShapedRecipe ArmorIron = new ShapedRecipe(new ItemStack(Material.IRON_BARDING));
            ArmorIron.shape(new String[] {
                "  I", "ILI", "III"
            });
            ArmorIron.setIngredient('L', Material.WOOL, -1);
            if ( getConfig().getBoolean("recipe.hardMode") == true )
            	ArmorIron.setIngredient('I', Material.IRON_BLOCK);
            else
            ArmorIron.setIngredient('I', Material.IRON_INGOT);
            getServer().addRecipe(ArmorIron);
            getServer().getLogger().info((new StringBuilder(String.valueOf(getLang("RECIPE_ADDED")))).append(" 'IRON_BARDING'").toString());
        }
        if(getConfig().getBoolean("recipe.armor.gold"))
        {
            ShapedRecipe ArmorGold = new ShapedRecipe(new ItemStack(Material.GOLD_BARDING));
            ArmorGold.shape(new String[] {
                "  I", "ILI", "III"
            });
            ArmorGold.setIngredient('L', Material.WOOL, -1);
            if ( getConfig().getBoolean("recipe.hardMode") == true )
            	ArmorGold.setIngredient('I', Material.GOLD_BLOCK);
            else
            ArmorGold.setIngredient('I', Material.GOLD_INGOT);
            getServer().addRecipe(ArmorGold);
            getServer().getLogger().info((new StringBuilder(String.valueOf(getLang("RECIPE_ADDED")))).append(" 'GOLD_BARDING'").toString());
        }
        if(getConfig().getBoolean("recipe.armor.diamond"))
        {
            ShapedRecipe ArmorDiamond = new ShapedRecipe(new ItemStack(Material.DIAMOND_BARDING));
            ArmorDiamond.shape(new String[] {
                "  I", "ILI", "III"
            });
            ArmorDiamond.setIngredient('L', Material.WOOL, -1);
            if ( getConfig().getBoolean("recipe.hardMode") == true )
            	ArmorDiamond.setIngredient('I', Material.DIAMOND_BLOCK);
            else
            ArmorDiamond.setIngredient('I', Material.DIAMOND);
            getServer().addRecipe(ArmorDiamond);
            getServer().getLogger().info((new StringBuilder(String.valueOf(getLang("RECIPE_ADDED")))).append(" 'DIAMOND_BARDING'").toString());
        }
        
    }

    private void checkWorldGuard() // See if server is using WorldGuard - for Horse recovery protections
    {
    	Plugin plug = getServer().getPluginManager().getPlugin("WorldGuard");
    	 
        // WorldGuard may not be loaded
        if (plug == null || !(plug instanceof WorldGuardPlugin)) {
            worldguard = null;
        }
     
        worldguard = (WorldGuardPlugin) plug;
        
    }
    
    private void setupEconomy() // See if server is using Economy plugins, for stables costs
    {
    	 if (getServer().getPluginManager().getPlugin("Vault") == null) {
             return;
         }
         RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
         if (rsp == null) {
             return ;
         }
         economy = rsp.getProvider();
         return;
         
    }
    
    public String HorseName(String id, LivingEntity horse)
    {
        if(id == null)
            id = horse.getUniqueId().toString();
        String name = null;
        if(flatfile)
        {
            if(getHorseConfig().contains((new StringBuilder("horses.")).append(id).append(".named").toString()))
                name = getHorseConfig().getString((new StringBuilder("horses.")).append(id).append(".named").toString());
        } else
        {
            rs = queryDB((new StringBuilder("SELECT named FROM ")).append(getConfig().getString("MySQL.prefix")).append("horses WHERE uid='").append(id).append("'").toString());
            if(rs != null)
                name = getResultString(1);
        }
        if(name == null)
        {
            if(horse != null && horse.getCustomName() != null)
            {
                debug((new StringBuilder("Not found in database ")).append(id).toString());
                return horse.getCustomName();
            } else
            {
                return "Unknown";
            }
        } else
        {
            return name.replace("`", "'");
        }
    }

    public String HorseOwner(String id)
    {
        String owner;
        if(flatfile)
        {
            if(getHorseConfig().contains((new StringBuilder("horses.")).append(id).toString()))
                owner = getHorseConfig().getString((new StringBuilder("horses.")).append(id).append(".owner").toString());
            else
                return null;
        } else
        {
            rs = queryDB((new StringBuilder("SELECT owner FROM ")).append(getConfig().getString("MySQL.prefix")).append("horses WHERE uid='").append(id).append("'").toString());
            if(rs == null)
                return null;
            owner = getResultString(1);
        }
        return owner;
    }

    public void saveHorseConfig()
    {
        if(!flatfile)
            return;
        debug("Saving flatfile horse config ...");
        if(horseConfig == null || horseConfigFile == null)
            return;
        try
        {
            getHorseConfig().save(horseConfigFile);
        }
        catch(IOException ex)
        {
            error((new StringBuilder("Could not save config to ")).append(horseConfigFile).toString());
        }
    }

    public FileConfiguration getHorseConfig()
    {
        if(horseConfig == null)
            reloadHorseConfig();
        return horseConfig;
    }
    
    
    public void reloadHorseConfig() {
    	
        if (horseConfigFile == null) {
        horseConfigFile = new File(getDataFolder(),"horses.yml");
        }
        horseConfig = YamlConfiguration.loadConfiguration(horseConfigFile);

    }
    

    public void syntax( CommandSender s, String str )
    {
    	s.sendMessage(getLang("SYNTAX")+ " " + str);
    }
    public void error( Object msg )
    {
		Bukkit.getServer().getLogger().severe("Stables: " + msg.toString());
    }
    
    void local( CommandSender s, String str )
    {
    	s.sendMessage(getLang(str));
    }
    
    public void debug(Object msg )
    {
			if ( plugin.getConfig().getBoolean("general.Debug") == true )
			{
				Bukkit.getServer().getLogger().info("Stables DEBUG: " + msg.toString() );
			}
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String label, String args[])
    {
    	if(cmd.getName().equalsIgnoreCase("view"))
            if(!(sender instanceof Player))
            {
                local(sender,"NO_CONSOLE");
                return true;
            } else
            {
                Player p = (Player)sender;
                viewStables(p);
                return true;
            }
        if(cmd.getName().equalsIgnoreCase("recover"))
        {
            if(!(sender instanceof Player))
            {
                local(sender,"NO_CONSOLE");
                return true;
            } else
            {
                Player p = (Player)sender;
                recoverStables(p, args, 0);
                return true;
            }
    }
        if(cmd.getName().equalsIgnoreCase("rename"))
        {
        	if ( args.length < 1 )
        	{
        		syntax(sender,"/stables rename (horsename)");
        		return true;
        	}
        	
        	  if(!(sender instanceof Player))
              {
                  local(sender,"NO_CONSOLE");
                  return true;
              }
              Player p = (Player)sender;
              
              if(getConfig().getBoolean("horses.NoTagRename") == false)
              {
              	local(sender,"COMMAND_DISABLED");
              	return true;
              }
              

          	String uid = findHorse( args, 0, sender.getName() );
          	
        	if ( uid == null )
        	{
        		local(sender,"HORSE_UNKNOWN");
        		return true;
        	}
          	debug("Horse found.");
        	
        	List<Entity> entitylist = p.getNearbyEntities(20, 20, 20);
        	
        	for ( int t=0; t<entitylist.size();t++) {
        		if ( entitylist.get(t).getUniqueId().toString().equals(uid) )
        		{
                	String newName = getRandomName();
                	LivingEntity l = (LivingEntity) entitylist.get(t);
                	l.setCustomName(newName);
                	nameHorse(uid,newName);
                	
                	local(sender,"NEW_NAME");
                	return true;
        		}
        	}
        	
        	local(sender,"RENAME_NOT_FOUND");
        	return true;
        }
        if(cmd.getName().equalsIgnoreCase("ro"))
        {
            if(!(sender instanceof Player))
            {
                local(sender,"NO_CONSOLE");
                return true;
            }
            Player p = (Player)sender;
            if(!perm(p, "stables.remove"))
            {
                local(sender,"NO_PERM");
                return true;
            } else
            {
                p.setMetadata("stables.removeowner", new FixedMetadataValue(plugin, Boolean.valueOf(true)));
                local(p,"HIT_REMOVE");
                return true;
            }
        }
        if(cmd.getName().equalsIgnoreCase("spawnhorse"))
        {
            if(!(sender instanceof Player))
            {
                local(sender,"NO_CONSOLE");
                return true;
            }
            Player p = (Player)sender;
            if(!perm(p, "stables.spawn"))
            {
                local(sender,"NO_PERM");
                return true;
            }
            if(args.length == 0)
            {
                spawnHorse(p.getLocation(), false, false);
                p.sendMessage("Random horse spawned!");
                return true;
            }
            if(args.length == 1)
            {
                if(args[0].equals("zombie"))
                {
                    p.sendMessage("Zombie horse spawned.");
                    spawnHorse(p.getLocation(), true, false);
                    return true;
                }
                if(args[0].equals("skeleton"))
                {
                    p.sendMessage("Skeleton horse spawned.");
                    spawnHorse(p.getLocation(), false, true);
                    return true;
                } else
                {
                    return false;
                }
            } else
            {
                return false;
            }
        }
        if(cmd.getName().equalsIgnoreCase("stables"))
        {
            if(args.length == 0 || args.length >= 1 && args[0].equalsIgnoreCase("help"))
            {
                sender.sendMessage((new StringBuilder("Stables, version ")).append(plugin.getDescription().getVersion()).toString());
                sender.sendMessage("-----------------------------------");
                sender.sendMessage("AddRider - "+getLang("CMD_ADD"));
                sender.sendMessage("DelRider - "+getLang("CMD_DEL"));
                sender.sendMessage("List - "+getLang("CMD_LIST"));
                sender.sendMessage("Abandon - "+getLang("CMD_ABANDON"));
                if(getConfig().getBoolean("stable.useCommand"))
                {
                    sender.sendMessage("View - "+getLang("CMD_VIEW"));
                    sender.sendMessage("Store - "+getLang("CMD_STORE"));
                    sender.sendMessage("Recover - "+getLang("CMD_RECOVER"));
                }
                if ( getConfig().getBoolean("horses.NoTagRename"))
                	sender.sendMessage("Rename - "+getLang("CMD_RENAME"));
                if(perm((Player)sender,"stables.find") || getConfig().getBoolean("horses.allowFind"))
                    sender.sendMessage("Find - "+getLang("CMD_FIND"));
                if(perm((Player)sender,"stables.tp") || getConfig().getBoolean("horses.allowTP"))
                    sender.sendMessage("TP - "+getLang("CMD_TP"));
                if(perm((Player)sender,"stables.summon") || getConfig().getBoolean("horses.allowSummon"))
                    sender.sendMessage("Summon - "+getLang("CMD_SUMMON"));
                if(!(sender instanceof Player) || (sender instanceof Player) && perm((Player)sender, "stables.info"))
                    sender.sendMessage("check - "+getLang("CMD_CHECK"));
                if(!(sender instanceof Player) || (sender instanceof Player) && perm((Player)sender, "stables.remove"))
                    sender.sendMessage("removeowner - "+getLang("CMD_RO"));
                if(!(sender instanceof Player) || (sender instanceof Player) && perm((Player)sender, "stables.name"))
                    sender.sendMessage("name - "+getLang("CMD_NAME"));
                if(!(sender instanceof Player) || (sender instanceof Player) && perm((Player)sender, "stables.list"))
                    sender.sendMessage("listall - "+getLang("CMD_LISTALL"));
                if(!(sender instanceof Player) || (sender instanceof Player) && perm((Player)sender, "stables.clear"))
                    sender.sendMessage("clearhorses - "+getLang("CMD_CLEAR"));
                if(!(sender instanceof Player) || (sender instanceof Player) && perm((Player)sender, "stables.admin"))
                    sender.sendMessage("reload - "+getLang("CMD_RELOAD"));
                if(!(sender instanceof Player) || (sender instanceof Player) && perm((Player)sender, "stables.admin"))
                    sender.sendMessage("save - "+getLang("CMD_SAVE"));
                if(!(sender instanceof Player) || (sender instanceof Player) && perm((Player)sender, "stables.admin"))
                    sender.sendMessage("config - "+getLang("CMD_CONFIG"));
                if(!(sender instanceof Player) || (sender instanceof Player) && perm((Player)sender, "stables.admin"))
                    sender.sendMessage("convert - "+getLang("CMD_CONVERT"));
                return true;
            }
            if(args.length >= 1 && args[0].equalsIgnoreCase("version"))
            {
                sender.sendMessage((new StringBuilder("Stables, by raum266 - version ")).append(plugin.getDescription().getVersion()).toString());
                return true;
            }
            if(args.length >= 1 && args[0].equalsIgnoreCase("report"))
            {
                sender.sendMessage("Stables Config:");
                sender.sendMessage((new StringBuilder("Debug Mode: ")).append(getConfig().getBoolean("general.Debug")).toString());
                sender.sendMessage((new StringBuilder("Block All: ")).append(getConfig().getBoolean("general.BlockAll")).toString());
                sender.sendMessage((new StringBuilder("Block PVP: ")).append(getConfig().getBoolean("general.PVPDamage")).toString());
                sender.sendMessage((new StringBuilder("Block Environment: ")).append(getConfig().getBoolean("general.EnviromentDamage")).toString());
                sender.sendMessage((new StringBuilder("Block Owner: ")).append(getConfig().getBoolean("general.OwnerDamage")).toString());
                sender.sendMessage((new StringBuilder("Block Mob: ")).append(getConfig().getBoolean("general.MobDamage")).toString());
                sender.sendMessage((new StringBuilder("Allow Theft: ")).append(getConfig().getBoolean("general.Theft")).toString());
                sender.sendMessage((new StringBuilder("Save Time: ")).append(getConfig().getInt("general.Save")).toString());
                return true;
            }
            if(args.length >= 1 && (args[0].equalsIgnoreCase("check") || args[0].equalsIgnoreCase("info")))
            {
                if(!(sender instanceof Player))
                {
                    local(sender,"NO_CONSOLE");
                    return true;
                }
                Player p = (Player)sender;
                if(!perm(p, "stables.info"))
                {
                    local(sender,"NO_PERM");
                    return true;
                } else
                {
                    p.setMetadata("stables.checkinfo", new FixedMetadataValue(plugin, Boolean.valueOf(true)));
                    local(p,"CHECK_HIT");
                    return true;
                }
            }
            if(args.length >= 1 && args[0].equals("name"))
            {
            	  if(!(sender instanceof Player))
                  {
                      local(sender,"NO_CONSOLE");
                      return true;
                  }
            
            	Player p = (Player)sender;
                if(!perm(p, "stables.name"))
                {
                    local(sender,"NO_PERM");
                    return true;
                }
                
                if ( args.length < 4)
                {
                	syntax(p,"/stables name (exact player name) \"(old name)\" \"(new name)\" - The quotes are required!");
                	return true;
                }
                
                String pname = args[1];
            	int Start = 2;
    			String a = "";

        		while ( Start < args.length )
        		{
        			a = a + " " + args[Start];
        			Start++;
        		}
        		a = a.trim();
                String[] names = a.split("\"");
                
                if ( names.length != 4 )
                {
                 	syntax(p,"/stables name (exact player name) \"(old name)\" \"(new name)\" - The quotes are required!");
                	return true;
                }
                String[] oldname = names[1].split(" ");
                String newname = names[3];
                
                
                String uid = findHorse( oldname, 0, pname );
                
               	if ( uid == null )
              	{
              		local(sender,"HORSE_UNKNOWN");
              		return true;
              	}
               	
               	List<Entity> entitylist = p.getNearbyEntities(20, 20, 20);
            	
            	for ( int t=0; t<entitylist.size();t++) {
            		if ( entitylist.get(t).getUniqueId().toString().equals(uid) )
            		{
                    	LivingEntity l = (LivingEntity) entitylist.get(t);
                    	l.setCustomName(newname);
                    	nameHorse(uid,newname);
                    	local(sender,"NEW_NAME");
                    	return true;
            		}
            	}
            	
            	local(sender,"RENAME_NOT_FOUND");
               	
            return true;
            }
            if(args.length >= 1 && args[0].equalsIgnoreCase("list"))
            {
                String owner = sender.getName();
                sender.sendMessage(owner+ " " + getLang("LIST_OWNED") );
                if(flatfile)
                {
                    for(String horse : getHorseConfig().getConfigurationSection("owners").getKeys(false) )
                    {

                        debug((new StringBuilder("Checking Horse: ")).append(horse).toString());
                        if(getHorseConfig().getString((new StringBuilder("owners.")).append(horse).toString()).equals(owner))
                            sender.sendMessage((new StringBuilder()).append(HorseName(horse, null)).toString());
                    }

                    return true;
                }
                rs = queryDB((new StringBuilder("SELECT uid, tamed, named, x, y, z FROM ")).append(getConfig().getString("MySQL.prefix")).append("horses WHERE owner='").append(owner).append("'").toString());
                try
                {
                    while(rs.next()) 
                        sender.sendMessage((new StringBuilder("Name: ")).append(rs.getString(3).replace("`", "'")).toString());
                }
                catch(SQLException e)
                {
                    error("SQL Error - List");
                    debug((new StringBuilder()).append(e.getStackTrace()).toString());
                }
                return true;
            }
            if(args.length >= 1 && args[0].equalsIgnoreCase("abandon"))
            {
            	if ( args.length < 2 )
            	{
            		syntax(sender,"/stables abandon (horsename)");
            		return true;
            	}
            	
            	  if(!(sender instanceof Player))
                  {
                      local(sender,"NO_CONSOLE");
                      return true;
                  }
                  Player p = (Player)sender;
                  
                String uid = findHorse( args, 1, sender.getName() );
              	if ( uid == null )
              	{
              		local(sender,"HORSE_UNKNOWN");
              		return true;
              	}
              	
              	List<Entity> entitylist = p.getLocation().getWorld().getEntities();
            	
            	for ( int t=0; t<entitylist.size();t++) {
            		if ( entitylist.get(t).getUniqueId().toString().equals(uid) )
            		{
            			LivingEntity l = (LivingEntity)entitylist.get(t);
            			l.setCustomName(null);
            			removeHorse(uid);
            			local(sender,"HORSE_ABANDON");
            			return true;
            		}
            	}
            	removeHorse(uid);
    			
    			local(sender,"HORSE_ABANDON_NOT_FOUND");
    			return true;
            }
            if(args.length >= 1 && ( args[0].equalsIgnoreCase("teleport") || args[0].equalsIgnoreCase("tp") ) )
            {
            
            
            	if ( args.length < 2 )
            	{
            		syntax(sender,"/stables tp (horsename)");
            		return true;
            	}
            	
                if(!(sender instanceof Player))
                {
                    local(sender,"NO_CONSOLE");
                    return true;
                }
                Player p = (Player)sender;
                
                if( !getConfig().getBoolean("horses.allowTP") && !perm(p,"stables.tp"))
                {
                	local(sender,"COMMAND_DISABLED");
                	return true;
                }
            
            	
            	String uid = findHorse( args, 1, sender.getName() );
            	if ( uid == null )
            	{
            		local(sender,"HORSE_UNKNOWN");
            		return true;
            	}
            	
            	
            	debug("Horse found - checking for location ...");
            	Location loc = getHorseLocation (uid);
            	if ( loc == null )
            	{
            		local(sender,"HORSE_NOT_FOUND");
            		return true;
            	}
            	
            	p.teleport(loc);
            	
            	local(sender,"TP_FOUND");
            	return true;
            	
            }
            if(args.length >= 1 && args[0].equalsIgnoreCase("summon"))
            {

            	if ( args.length < 2 )
            	{
            		syntax(sender,"/stables summon (horsename)");
            		return true;
            	}
            	
            	  if(!(sender instanceof Player))
                  {
                      local(sender,"NO_CONSOLE");
                      return true;
                  }
                  Player p = (Player)sender;
                  
                  if(!getConfig().getBoolean("horses.allowSummon") && !perm(p,"stables.summon"))
                  {
                  	local(sender,"COMMAND_DISABLED");
                  	return true;
                  }
                  

              	String uid = findHorse( args, 1, sender.getName() );
            	if ( uid == null )
            	{
            		local(sender,"HORSE_UNKNOWN");
            		return true;
            	}
            	
            	
            	debug("Horse found - searching for location ...");
            	
            	List<Entity> entitylist = p.getLocation().getWorld().getEntities();
            	
            	for ( int t=0; t<entitylist.size();t++) {
            		if ( entitylist.get(t).getUniqueId().toString().equals(uid) )
            		{
            			entitylist.get(t).teleport(p.getLocation());
            			local(sender,"SUMMON_HORSE");
            			saveLocation( (Horse) entitylist.get(t) );
            			return true;
            		}
            	}

            	local(sender,"HORSE_NOT_FOUND");
            		return true;
            	
            	
            }
            if(args.length >= 1 && args[0].equalsIgnoreCase("dismount"))
            {
            	if(!(sender instanceof Player))
                {
                    local(sender,"NO_CONSOLE");
                    return true;
                }
                Player p = (Player)sender;
                
                if( !perm((Player)sender,"stables.dismount") )
                {
                	local(sender,"NO_PERM");
                	return true;
                }
                
                if (args.length != 2 )
                {
                	syntax(sender,"/stables dismount (player)");
                return true;
                }
                
            	Player target = (Bukkit.getServer().getPlayer(args[1]));
	            if (target == null) {
	               sender.sendMessage(args[0].toString() + " is not here!");
	               return true;
	            }
	            if ( target.getVehicle() != null )
	            {
	            	debug("Removing player from vehicle");
	            	
	            	target.getVehicle().getPassenger().leaveVehicle();
	            }
	            
	            sender.sendMessage("Done!");
            	return true;
            }
            if(args.length >= 1 && args[0].equalsIgnoreCase("find"))
            {
                if(!(sender instanceof Player))
                {
                    local(sender,"NO_CONSOLE");
                    return true;
                }
                Player p = (Player)sender;
                
                if( !getConfig().getBoolean("horses.allowFind") && !perm((Player)sender,"stables.find") )
                {
                	local(sender,"COMMAND_DISABLED");
                	return true;
                }
                

            	if ( args.length < 2 )
            	{
            		syntax(sender,"/stables find (horsename)");
            		return true;
            	}
            	
            	String uid = findHorse( args, 1, sender.getName() );
            	if ( uid == null )
            	{
            		local(sender,"HORSE_UNKNOWN");
            		return true;
            	}
            	
            	
            	debug("Horse found - checking for location ...");
            	Location loc = getHorseLocation (uid);
            	if ( loc == null )
            	{
            		local(sender,"HORSE_NOT_FOUND");
            		return true;
            	}
            	if ( loc.getWorld() !=  p.getLocation().getWorld() )
            	{
            		local(sender,"HORSE_WRONG_WORLD");
            		return true;
            	}
            	p.setCompassTarget(loc);
            	local(sender,"COMPASS_LOCKED");
            	
            	return true;
            	
            }
            if(args.length >= 1 && args[0].equalsIgnoreCase("listall"))
            {
                if((sender instanceof Player) && !perm((Player)sender, "stables.info"))
                {
                    local(sender,"NO_PERM");
                    return true;
                }
                if(args.length == 1)
                {
                    local(sender,"LIST_NOARG");
                    return true;
                }
                String owner;
                if(getServer().getPlayerExact(args[1]) == null)
                {
                    OfflinePlayer player = getServer().getOfflinePlayer(args[1]);
                    if(!player.hasPlayedBefore())
                    {
                        local(sender,"UNKNOWN_OWNER");
                        return true;
                    }
                    owner = player.getName();
                } else
                {
                    owner = getServer().getPlayerExact(args[1].toString()).getName();
                }
                
                sender.sendMessage((new StringBuilder(String.valueOf(owner))).append(" ").append(getLang("LIST_OWNED")).append(":").toString());
                if(flatfile)
                {
                    for(String horse : getHorseConfig().getConfigurationSection("owners").getKeys(false) )
                    {
                        
                        debug((new StringBuilder("Checking Horse: ")).append(horse).toString());
                        if(getHorseConfig().getString((new StringBuilder("owners.")).append(horse).toString()).equals(owner))
                            sender.sendMessage((new StringBuilder()).append(HorseName(horse, null)).toString());
                    }

                    return true;
                }
                rs = queryDB((new StringBuilder("SELECT uid, tamed, named, x, y, z FROM ")).append(getConfig().getString("MySQL.prefix")).append("horses WHERE owner='").append(owner).append("'").toString());
                try
                {
                    while(rs.next()) 
                        sender.sendMessage((new StringBuilder("Name: ")).append(rs.getString(3).replace("`", "'")).toString());
                }
                catch(SQLException e)
                {
                    error("SQL Error - ListAll");
                    debug((new StringBuilder()).append(e.getStackTrace()).toString());
                }
                return true;
            }
            if(args.length >= 1 && args[0].equalsIgnoreCase("clearhorses"))
            {
                if((sender instanceof Player) && !perm((Player)sender, "stables.clear"))
                {
                    local(sender,"NO_PERM");
                    return true;
                }
                if(args.length == 1)
                {
                    local(sender,"REMOVE_NOARG");
                    return true;
                }
                if(getServer().getPlayerExact(args[1]) == null)
                {
                    local(sender,"UNKNOWN_OWNER");
                    return true;
                }
                String owner = getServer().getPlayerExact(args[1]).getName();
                if(!flatfile)
                {
                    writeDB((new StringBuilder("DELETE FROM ")).append(getConfig().getString("MySQL.prefix")).append("owners WHERE name='").append(owner).append("'").toString());
                    writeDB((new StringBuilder("DELETE FROM ")).append(getConfig().getString("MySQL.prefix")).append("horses WHERE owner='").append(owner).append("'").toString());
                    writeDB((new StringBuilder("DELETE FROM ")).append(getConfig().getString("MySQL.prefix")).append("riders WHERE owner='").append(owner).append("'").toString());
                    writeDB((new StringBuilder("DELETE FROM ")).append(getConfig().getString("MySQL.prefix")).append("stable WHERE owner='").append(owner).append("'").toString());
                }
                sender.sendMessage("Horses cleared.");
                return true;
            }
            if(args.length >= 1 && args[0].equalsIgnoreCase("addrider"))
            {
                if(!(sender instanceof Player))
                {
                    local(sender,"NO_CONSOLE");
                    return true;
                }
                Player p = (Player)sender;
                if(args.length == 1)
                {
                    p.sendMessage("Who do you want to add as a rider?");
                    return true;
                } else
                {
                    p.sendMessage("Punch the horse you want to add the rider to.");
                    p.setMetadata("stables.addrider", new FixedMetadataValue(plugin, args[1].toLowerCase()));
                    return true;
                }
            }
            if(args.length >= 1 && args[0].equalsIgnoreCase("delrider"))
            {
                if(!(sender instanceof Player))
                {
                    local(sender,"NO_CONSOLE");
                    return true;
                }
                Player p = (Player)sender;
                if(args.length == 1)
                {
                    p.sendMessage("Who do you want to delete as a rider?");
                    return true;
                } else
                {
                    p.sendMessage("Punch the horse you want to delete the rider from.");
                    p.setMetadata("stables.delrider", new FixedMetadataValue(plugin, args[1].toLowerCase()));
                    return true;
                }
            }
            if(args.length >= 1 && args[0].equalsIgnoreCase("removechest"))
                if(!(sender instanceof Player))
                {
                    local(sender,"NO_CONSOLE");
                    return true;
                } else
                {
                    Player p = (Player)sender;
                    p.sendMessage("Hit the horse you wish to remove the chest of.");
                    p.sendMessage("WARNING: Anything remaining in the chest will be DESTROYED!");
                    p.setMetadata("stables.removechest", new FixedMetadataValue(plugin, Boolean.valueOf(true)));
                    return true;
                }
            if(args.length >= 1 && args[0].equalsIgnoreCase("store"))
            {
                if(!(sender instanceof Player))
                {
                    local(sender,"NO_CONSOLE");
                    return true;
                }
                if(!getConfig().getBoolean("stable.useCommand"))
                {
                    sender.sendMessage("You are unable to do that.");
                    return true;
                } else
                {
                    commandStore((Player)sender);
                    return true;
                }
            }
            if(args.length >= 1 && args[0].equalsIgnoreCase("view"))
            {
                if(!(sender instanceof Player))
                {
                    local(sender,"NO_CONSOLE");
                    return true;
                }
                viewStables((Player)sender);
                return true;
            }
            if(args.length >= 1 && args[0].equalsIgnoreCase("recover"))
                if(!(sender instanceof Player))
                {
                    local(sender,"NO_CONSOLE");
                    return true;
                } else
                {
                   
                    recoverStables((Player)sender, args, 1);
                    return true;
                }
            if(args.length >= 1 && (args[0].equalsIgnoreCase("ro") || args[0].equalsIgnoreCase("removeowner")))
            {
                if(!(sender instanceof Player))
                {
                    local(sender,"NO_CONSOLE");
                    return true;
                }
                Player p = (Player)sender;
                if(!perm(p, "stables.remove"))
                {
                    local(sender,"NO_PERM");
                    return true;
                } else
                {
                    p.sendMessage("Punch the horse you want to remove the owner of.");
                    p.setMetadata("stables.removeowner", new FixedMetadataValue(plugin, Boolean.valueOf(true)));
                    return true;
                }
            }
            if(args.length >= 1 && args[0].equalsIgnoreCase("reload"))
                if(!sender.hasPermission("stables.admin"))
                {
                    local(sender,"NO_PERM");
                    return true;
                } else
                {
                    reloadConfig();
                    reloadlocalConfig();
                        
                    sender.sendMessage("Stables configuration reloaded.");
                    return true;
                }
            if(args.length >= 1 && args[0].equalsIgnoreCase("save"))
                if(!sender.hasPermission("stables.admin"))
                {
                    local(sender,"NO_PERM");
                    return true;
                } else
                {
                    saveHorseConfig();
                    sender.sendMessage("Saved.");
                    return true;
                }
            if(args.length >= 1 && args[0].equalsIgnoreCase("convert"))
                if(!sender.hasPermission("stables.admin"))
                {
                    local(sender,"NO_PERM");
                    return true;
                } else
                {
                    ConvertDatabase();
                    sender.sendMessage("Database converted.");
                    return true;
                }
            if(args.length >= 1 && args[0].equalsIgnoreCase("config"))
                if(!sender.hasPermission("stables.admin"))
                {
                    local(sender,"NO_PERM");
                    return true;
                } else
                {
                    changeConfig(sender, args);
                    return true;
                }
        }
        return false;
    }

    public void viewStables(Player p)
    {
        int num = 0;
        p.sendMessage("You have the following horses in the stables:");
        if(flatfile)
        {
            p.sendMessage("The stables are closed!");
            return;
        }
        rs = queryDB((new StringBuilder("SELECT name FROM ")).append(getConfig().getString("MySQL.prefix")).append("stable WHERE owner='").append(p.getName()).append("'").toString());
        try
        {
            while(rs.next()) 
            {
            	String name = rs.getString(1).replaceAll("`", "\"");
                num++;
                p.sendMessage(num + ") Name: " + name);
            }
            p.sendMessage("Use /stables recover # to retrieve a horse.");
            return;
        }
        catch(SQLException e)
        {
        	e.printStackTrace();
            error("SQL Error - View");
            
        }
    }

    public void recoverStables(Player p, String args[], int arg)
    {
    	ArrayList<String> a = new ArrayList<String>();
    	
        if(flatfile)
        {
            p.sendMessage("The horse stables are currently closed.");
            return;
        }

        if(disabledWorld("stable.useEnabled","stable.disabled", p.getWorld().getName()))
        {
            p.sendMessage("You are unable to do that here!");
            return;
        }

        if(!enabledWorld("stable.useEnabled","stable.enabled", p.getWorld().getName()))
        {
            p.sendMessage("You are unable to do that here.");
            return;
        }

		if (worldguard != null) {
			debug("Checking worldguard ....");
			RegionManager r = worldguard.getRegionManager(p.getWorld());

			ApplicableRegionSet set = r.getApplicableRegions(p.getLocation());
			if (r.size() != 0) {

				if (!set.allows(DefaultFlag.MOB_SPAWNING)) {
					debug("WG: No mob spawning here!");
					local(p,"RECOVER_WG");
					return;
				}
			}

		}
		
		if ( p.getWorld().getAllowAnimals() == false || p.getWorld().getAllowMonsters() == false )
		{
			debug("World Settings - No mobs");
			local(p,"RECOVER_WG");
			return;
		}
        
        if(!getConfig().getBoolean("stable.useCommand") && !atStable(p.getLocation(), Integer.valueOf(5)))
        {
            p.sendMessage("You are not close enough to the stables for that.");
            return;
        }
        
        if ( args != null )
        while ( arg+1 <= args.length )
        {
        	a.add(args[arg]);
        	arg++;
        }
        	
        if( args == null || a.size() == 0 ) 
        {
        	int num = 0;
            rs = queryDB((new StringBuilder("SELECT name FROM ")).append(getConfig().getString("MySQL.prefix")).append("stable WHERE owner='").append(p.getName()).append("'").toString());

            try
            {
        		
            		
            	p.sendMessage("You have the following horses in your stables:\n");
                while(rs.next())
                {
                	num++;
                	p.sendMessage(num + ") "+rs.getString("name").replaceAll("`","'"));
                	
                }
                if ( num != 0 )
                {
            	p.sendMessage("\nWhich horse did you want to recover?");
            	p.sendMessage("Type "+ChatColor.DARK_AQUA+"/stables recover (name)");
                }
                else
                {
                	p.sendMessage("None!");
                }
                	
            	return;
            }
            catch(SQLException e)
            {
                p.sendMessage("You do not have any horses in your stables!");
                return;
            }
        }


        String horseName = "";
        arg = 0;
        
    	while ( arg < a.size() )
		{
			horseName = horseName + " " + a.get(arg).replaceAll(";","-");
			arg++;
		}
    	        
        String query = "SELECT uid, health, type, chested, bred, variant, temper, tamed, saddled, armoritem, name, str FROM "+getConfig().getString("MySQL.prefix")+"stable WHERE owner='"+p.getName()+"' AND name LIKE '"+horseName.replaceAll("'", "`").trim()+"%'";
        debug(query);
        rs = queryDB(query);
        try
        {
            while(rs.next()) 
            {
                	String name = rs.getString(11).replaceAll("`","'");
                    p.sendMessage("The stable master wanders off to the stalls, then returns with " + name );
                    HorseModifier hm = HorseModifier.spawn(p.getLocation());
                    Horse h = (Horse)hm.getHorse();
                    String chest = rs.getString(4);
                    String bred = rs.getString(5);
                    String tamed = rs.getString(8);
                    String saddle = rs.getString(9);
                    debug((new StringBuilder()).append(chest).toString());
                    debug((new StringBuilder()).append(bred).toString());
                    debug((new StringBuilder()).append(tamed).toString());
                    debug((new StringBuilder()).append(saddle).toString());
                    hm.setChested(chest.equals("1"));
                    hm.setBred(bred.equals("1"));
                    hm.setTamed(tamed.equals("1"));
                    hm.setSaddled(saddle.equals("1"));
                    Double health = Double.valueOf(rs.getDouble(2));
                    hm.setType(HorseModifier.HorseType.fromId(rs.getInt(3)));
                    hm.setVariant(HorseModifier.HorseVariant.fromId(rs.getInt(6)));
                    hm.setTemper(rs.getInt(7));
                    if(rs.getInt(10) != 0)
                    {
                        ItemStack armor = new ItemStack(rs.getInt(10), 1);
                        hm.setArmorItem(armor);
                    }
                    hm.getHorse().setCustomName(name);
                    h.setJumpStrength(rs.getDouble(12));
                    h.setMaxHealth(health.doubleValue());
                    h.setHealth(health.doubleValue());
                    String newID = hm.getHorse().getUniqueId().toString();
                    String oldID = rs.getString(1);
                   
                        writeDB((new StringBuilder("UPDATE ")).append(getConfig().getString("MySQL.prefix")).append("horses SET uid = '").append(newID).append("' WHERE uid='").append(oldID).append("';").toString());
                        writeDB((new StringBuilder("UPDATE ")).append(getConfig().getString("MySQL.prefix")).append("riders SET uid = '").append(newID).append("' WHERE uid='").append(oldID).append("';").toString());
                        writeDB((new StringBuilder("UPDATE ")).append(getConfig().getString("MySQL.prefix")).append("owners SET horse = '").append(newID).append("' WHERE horse='").append(oldID).append("';").toString());
                        writeDB((new StringBuilder("DELETE FROM ")).append(getConfig().getString("MySQL.prefix")).append("stable WHERE uid='").append(oldID).append("';").toString());
                  return;
            }

            recoverStables( p, null, 0);
            //p.sendMessage("That is an invalid horse. Use /stables recover # - The number is found via /stables view");
            return;
        }
        catch(SQLException e)
        {
            error("SQL Error - Recover");
            e.printStackTrace();
            return;
        }
    }

    public boolean perm(Player p, String pe)
    {
        if(pe.contains("punish"))
            return p.hasPermission(pe);
        return p.hasPermission(pe) || p.hasPermission("stables.admin");
    }

    public boolean isHorse(Entity entity)
    {
        if(entity instanceof Horse)
            return true;
        if(!(entity instanceof LivingEntity))
            return false;
        if((entity instanceof Sheep) || (entity instanceof Cow) || (entity instanceof Minecart) || (entity instanceof Boat) || (entity instanceof MushroomCow) || (entity instanceof Pig) || (entity instanceof Wolf) || (entity instanceof Bat) || (entity instanceof Chicken) || (entity instanceof Blaze) || (entity instanceof CaveSpider) || (entity instanceof Creeper) || (entity instanceof EnderDragon) || (entity instanceof Enderman) || (entity instanceof Ghast) || (entity instanceof Giant) || (entity instanceof Golem) || (entity instanceof HumanEntity) || (entity instanceof IronGolem) || (entity instanceof MagmaCube) || (entity instanceof Monster) || (entity instanceof Ocelot) || (entity instanceof PigZombie) || (entity instanceof Player) || (entity instanceof Silverfish) || (entity instanceof Skeleton) || (entity instanceof Slime) || (entity instanceof Snowman) || (entity instanceof Spider) || (entity instanceof Squid) || (entity instanceof Villager) || (entity instanceof Witch) || (entity instanceof Wither) || (entity instanceof Zombie) || (entity instanceof Item) || (entity instanceof ExperienceOrb) || (entity instanceof Painting) || (entity instanceof Arrow) || (entity instanceof Snowball) || (entity instanceof Fireball) || (entity instanceof SmallFireball) || (entity instanceof EnderPearl) || (entity instanceof EnderSignal) || (entity instanceof ThrownExpBottle) || (entity instanceof ItemFrame) || (entity instanceof WitherSkull) || (entity instanceof TNTPrimed) || (entity instanceof FallingBlock) || (entity instanceof Firework) || (entity instanceof Boat) || (entity instanceof Minecart) || (entity instanceof EnderCrystal) || (entity instanceof Egg) || (entity instanceof Weather) || (entity instanceof Player))
        {
            return false;
        } else
        {
            debug((new StringBuilder("Entity: ")).append(entity.getClass()).toString());
            return true;
        }
    }

    public void changeConfig(CommandSender sender, String args[])
    {
        boolean found;

        {
            found = false;
            if(args.length == 1)
            {
                sender.sendMessage("Alter what config option?");
                sender.sendMessage("Debug");
                sender.sendMessage("Block");
                sender.sendMessage("Lure");
                sender.sendMessage("Virtual");
                sender.sendMessage("Recipe");
                sender.sendMessage("Name");
                return;
            }
            if(args.length >= 2 && args[1].equalsIgnoreCase("debug"))
            {
                getConfig().set("general.Debug", Boolean.valueOf(!getConfig().getBoolean("general.Debug")));
                sender.sendMessage((new StringBuilder("Toggled: Debug message is now ")).append(getConfig().getBoolean("general.Debug")).toString());
                found = true;
                return;
            }
            if(args.length >= 2 && args[1].equalsIgnoreCase("Name"))
            {
            	sender.sendMessage("This is only editable directly in the config for now.");
            	return;
            }
            
            if(args.length >= 2 && args[1].equalsIgnoreCase("Lure"))
            {
                if(args.length != 4)
                {
                    syntax(sender,"/stables config lure (option) (setting)");
                    sender.sendMessage("Valid lure options are: allow, chance, item, delay, disabled, min, max");
                    return;
                }
                String option = args[2].toLowerCase();
                String set = args[3];

                switch(option) 
                {
                default: found = false;
                    break;

                case "chance": 
                    break;
                case "max":
                	break;
                case "min": 
                        break;
                case "item": 
                        break;
                case "allow": 
                        break;
                case "delay": 
                        break;
                case "disabled": 
                    break;
                }
                if ( !found )
                {
                syntax(sender,"/stables config lure (option) (setting)");
                sender.sendMessage("Valid lure options are: allow, chance, item, delay, disabled, min, max");
                return;
                }
                saveConfig();
                return;
            }
            if(args.length >= 2 && args[1].equalsIgnoreCase("Virtual"))
            {
                if(args.length != 4)
                {
                	syntax(sender,"/stables config virtual (option) (setting)");
                    sender.sendMessage("Valid Virtual Stable options are: cost, command, timeout, disabled");
                    return;
                }
                String option = args[2];
                String s1 = args[3];
            } else
            if(args.length >= 2 && args[1].equalsIgnoreCase("Recipe"))
            {
                if(args.length != 3)
                {
                	syntax(sender,"/stables config recipe (option)");
                    sender.sendMessage("Valid recipe options are: saddle, nametag, ironarmor, goldarmor, diamondarmor, usePerms");
                    return;
                }
                String s = args[2];
            } else
            if(args.length >= 2 && args[1].equalsIgnoreCase("block"))
            {
                if(args.length != 3)
                {
                	syntax(sender,"/stables config block (type)");
                    sender.sendMessage("Valid block types are: all, pvp, environment, owner, mob");
                    return;
                }
                String type = args[2];
                if(type.equals("all"))
                    getConfig().set("general.BlockAll", Boolean.valueOf(!getConfig().getBoolean("general.BlockAll")));
                else
                if(type.equals("pvp"))
                    getConfig().set("general.PVPDamage", Boolean.valueOf(!getConfig().getBoolean("general.PVPDamage")));
                else
                if(type.equals("environment"))
                    getConfig().set("general.EnvironmentDamage", Boolean.valueOf(!getConfig().getBoolean("general.EnvironmentDamage")));
                else
                if(type.equals("owner"))
                    getConfig().set("general.OwnerDamage", Boolean.valueOf(!getConfig().getBoolean("general.OwnerDamage")));
                else
                if(type.equals("mob"))
                {
                    getConfig().set("general.MobDamage", Boolean.valueOf(!getConfig().getBoolean("general.MobDamage")));
                } else
                {
                	syntax(sender,"/stables config block (type)");
                    sender.sendMessage("Valid block types are: all, pvp, environment, owner, mob");
                    return;
                }
                sender.sendMessage("Damage config is now:");
                sender.sendMessage((new StringBuilder("Block ALL: ")).append(getConfig().getBoolean("general.BlockAll")).toString());
                sender.sendMessage((new StringBuilder("Block PVP: ")).append(getConfig().getBoolean("general.PVPDamage")).toString());
                sender.sendMessage((new StringBuilder("Block Mob: ")).append(getConfig().getBoolean("general.MobDamage")).toString());
                sender.sendMessage((new StringBuilder("Block Owner: ")).append(getConfig().getBoolean("general.OwnerDamage")).toString());
                sender.sendMessage((new StringBuilder("Block Enviroment: ")).append(getConfig().getBoolean("general.EnvironmentDamage")).toString());
                found = true;
            } else
            if(args.length >= 2 && args[1].equals("max"))
            {
                if(args.length != 4)
                {
                	syntax(sender,"is /stables config max (#) (amount)");
                    return;
                }
                String perm = args[2].toString();
                String s2 = args[3].toString();
            }
        }
        if(!found)
        {
            sender.sendMessage("That is an invalid option.");
            return;
        } else
        {
            saveConfig();
            reloadConfig();
            return;
        }
    }

    public boolean atStable(Location loc, Integer radius)
    {
        World world = loc.getWorld();
        for(int y = 1; y > -radius.intValue(); y--)
        {
            for(int x = 1; x > -radius.intValue(); x--)
            {
                for(int z = 1; z > -radius.intValue(); z--)
                {
                    Block scan = world.getBlockAt((int)loc.getX() + x, (int)loc.getY() + y, (int)loc.getZ() + z);
                    if(scan.getType() == Material.WALL_SIGN)
                    {
                        Sign sign = (Sign)scan.getState();
                        String stablesign = ChatColor.stripColor(sign.getLine(0));
                        if(stablesign.equals("[Stables]"))
                        {
                            debug("Stables found nearby...");
                            return true;
                        }
                    }
                }

            }

        }

        return false;
    }

    public void removeHorse(String id)
    {
        if(flatfile)
        {
            plugin.getHorseConfig().set((new StringBuilder("horses.")).append(id).toString(), null);
            plugin.getHorseConfig().set((new StringBuilder("riders.")).append(id).toString(), null);
            plugin.getHorseConfig().set((new StringBuilder("owners.")).append(id).toString(), null);
        } else
        {
            plugin.writeDB((new StringBuilder("DELETE FROM ")).append(getConfig().getString("MySQL.prefix")).append("horses WHERE uid='").append(id).append("'").toString());
            plugin.writeDB((new StringBuilder("DELETE FROM ")).append(getConfig().getString("MySQL.prefix")).append("riders WHERE uid='").append(id).append("'").toString());
            plugin.writeDB((new StringBuilder("DELETE FROM ")).append(getConfig().getString("MySQL.prefix")).append("stable WHERE uid='").append(id).append("'").toString());
            plugin.writeDB((new StringBuilder("DELETE FROM ")).append(getConfig().getString("MySQL.prefix")).append("owners WHERE horse='").append(id).append("'").toString());
        }
    }

    public boolean disabledWorld(String config, String list, String world)
    {
    	if ( getConfig().getBoolean(config) == true )
    		return false;
    	
        String worlds[] = getConfig().getString(list).split(",");
        String as[];
        int j = (as = worlds).length;
        for(int i = 0; i < j; i++)
        {
            String check = as[i];
            debug((new StringBuilder("Checking '")).append(check.trim()).append("' vs '").append(world).append("'").toString());
            if(world.equalsIgnoreCase(check.trim()))
                return true;
        }

        return false;
    }

    public boolean enabledWorld(String check, String list, String world)
    {
    	if ( getConfig().getBoolean(check) == false )
    		return true;
    	
        String worlds[] = getConfig().getString(list).split(",");
        String as[];
        int j = (as = worlds).length;
        for(int i = 0; i < j; i++)
        {
            String check2 = as[i];
            debug((new StringBuilder("Enabled Checking '")).append(check2.trim()).append("' vs '").append(world).append("'").toString());
            if(world.equalsIgnoreCase(check2.trim()))
                return true;
        }

        return false;
    }
    
    public void stableHorse(LivingEntity horse, String owner)
    {
        HorseModifier h = new HorseModifier(horse);
        Horse he = (Horse)horse;
        
        if(disabledWorld("stable.useEnabled","stable.disabled", horse.getWorld().getName()))
        {
            local(getServer().getPlayer(owner),"DISABLED_WORLD");
            return;
        }

        if(!enabledWorld("stable.useEnabled","stable.enabled", horse.getWorld().getName()))
        {
            local(getServer().getPlayer(owner),"DISABLED_WORLD");
            return;
        }

        
        if(!getServer().getPlayer(owner).hasPermission("stables.free") && economy != null && getConfig().getDouble("stable.cost") > 0.0D)
        {
            double cost = getConfig().getInt("stable.cost");
            if(economy.getBalance(owner) < cost)
            {
                getServer().getPlayer(owner).sendMessage(getLang("TOO_POOR") + cost + ".");
                return;
            }
            getServer().getPlayer(owner).sendMessage(getLang("FEE_COLLECT") + cost + ".");
            economy.withdrawPlayer(owner, cost);
        }
        local(getServer().getPlayer(owner),"MASTER_STORE");
        
        owner = HorseOwner(horse.getUniqueId().toString());
        //Test
        String id = horse.getUniqueId().toString();
        String name = horse.getCustomName().replaceAll("'", "`");
        int type = h.getType().getId();
        int var = h.getVariant().getId();
        int armor = h.getArmorItem().getTypeId();
        int temper = h.getTemper();
        double str = he.getJumpStrength();
        double health = h.getHorse().getMaxHealth();
        int tame = 0;
        int saddle = 0;
        int chest = 0;
        int bred = 0;
        if(h.isTamed())
            tame = 1;
        if(h.isChested())
            chest = 1;
        if(h.isBred())
            bred = 1;
        if(he.getInventory().getSaddle() != null)
            saddle = 1;
        if(flatfile)
        {
            return;
        } else
        {
            writeDB((new StringBuilder("INSERT INTO ")).append(getConfig().getString("MySQL.prefix")).append("stable (name, owner, uid,health,type,chested,bred,variant,temper,tamed,saddled,armoritem, str) VALUES( '").append(name).append("', '").append(owner).append("', '").append(id).append("',").append(health).append(", ").append(type).append(", ").append(chest).append(", ").append(bred).append(",").append(var).append(",").append(temper).append(", ").append(tame).append(", ").append(saddle).append(", ").append(armor).append(",").append(str).append("  )").toString());
            horse.remove();
            return;
        }
    }

    boolean canRide(LivingEntity e, Player p)
    {
        if(perm(p, "stables.ride"))
            return true;
        if(isRider(p.getName().toLowerCase(), e.getUniqueId().toString()))
            return true;
        return isOwner(p.getName(), e.getUniqueId().toString());
    }

    boolean canTame(Player player)
    {
        int num = 100;
        int owned = 0;
        boolean notame = false;
        if(flatfile)
        {
            if(!getHorseConfig().contains("owners"))
            {
                debug("Empty file - asuming no horses.");
                return true;
            }
            for(String horses : getHorseConfig().getConfigurationSection("owners").getKeys(false)) 
            {
                String owner = getHorseConfig().getString((new StringBuilder("owners.")).append(horses).toString());
                debug((new StringBuilder("checking ")).append(horses).toString());
                if(owner.equals(player.getName()))
                    owned++;
            }

        } else
        {
            String query = (new StringBuilder("SELECT * FROM ")).append(getConfig().getString("MySQL.prefix")).append("horses WHERE owner='").append(player.getName()).append("'").toString();
            debug((new StringBuilder()).append(query).toString());
            rs = queryDB(query);
            try
            {
                while(rs.next()) 
                    owned++;
            }
            catch(SQLException e)
            {
                error("SQL Error - canTame");
                e.printStackTrace();
            }
        }
        if(owned == 0)
        {
            debug((new StringBuilder(String.valueOf(player.getName()))).append(" owns 0 horses.").toString());
            return true;
        }
        debug((new StringBuilder(String.valueOf(player.getName()))).append(" owns ").append(owned).append(" horses.").toString());
        for(; num > 0; num--)
        {
            if(!player.hasPermission((new StringBuilder("stables.max.")).append(num).toString()))
                continue;
            debug((new StringBuilder("Found VIP permission : ")).append(num).toString());
            if(num >= 1 && owned >= num)
                notame = true;
            break;
        }

        if(num <= 0)
        {
            debug("No special perms found - using default MaxOwned");
            if(getConfig().getInt("general.MaxOwned.default") >= 1 && owned >= getConfig().getInt("general.MaxOwned.default"))
                notame = true;
        }
        if(notame)
        {
            local(player,"TOO_MANY_HORSES");
            return false;
        } else
        {
            return true;
        }
    }
    public void ConvertDatabase()
    {
        File f = new File("plugins\\stables\\horses.yml");
        if(!f.exists())
        {
            debug("nothing to convert.");
            return;
        }

        for(Iterator<String> iterator = getHorseConfig().getConfigurationSection("owners").getKeys(false).iterator(); iterator.hasNext();)
        {
            String owners = (String)iterator.next();
            String horses;
            for(Iterator<String> iterator2 = getHorseConfig().getConfigurationSection((new StringBuilder("owners.")).append(owners).append(".horse").toString()).getKeys(false).iterator(); iterator2.hasNext(); writeDB((new StringBuilder("INSERT INTO owners (name, horse) VALUES('")).append(owners).append("','").append(horses).append("')").toString()))
                horses = (String)iterator2.next();

        }

        String horse;
        String owner;
        long tamed;
        String name;
        for(Iterator<String> iterator1 = getHorseConfig().getConfigurationSection("horses").getKeys(false).iterator(); iterator1.hasNext(); writeDB((new StringBuilder("INSERT INTO horses (uid, owner, tamed, named, x, y, z) VALUES( '")).append(horse).append("', '").append(owner).append("', ").append(tamed).append(", '").append(name).append("', 0, 0, 0 )").toString()))
        {
            horse = (String)iterator1.next();
            if(getHorseConfig().isSet((new StringBuilder("horses.")).append(horse).append(".rider").toString()))
            {
                String rider;
                for(Iterator<String> iterator3 = getHorseConfig().getConfigurationSection((new StringBuilder("horses.")).append(horse).append(".rider").toString()).getKeys(false).iterator(); iterator3.hasNext(); writeDB((new StringBuilder("INSERT INTO riders (uid, name) VALUES('")).append(horse).append("','").append(rider).append("')").toString()))
                    rider = (String)iterator3.next();

                getHorseConfig().set((new StringBuilder("horses.")).append(horse).append(".rider").toString(), null);
            }
            owner = getHorseConfig().getString((new StringBuilder("horses.")).append(horse).append(".owner").toString());
            tamed = getHorseConfig().getLong((new StringBuilder("horses.")).append(horse).append(".tamed").toString());
            name = getHorseConfig().getString((new StringBuilder("horses.")).append(horse).append(".name").toString());
        }

        f.renameTo(new File("plugins\\stables\\OLD-horses.yml"));
        debug("Database converted.");
        
    }

    Location getHorseLocation ( String uid )
    {
    Double x = 0.0, y = 0.0, z = 0.0;
	String world = null;
    	if(flatfile)
    	{
    		if ( !getHorseConfig().contains("horses."+uid+".x") )
    			return null;
    		x = getHorseConfig().getDouble("horses."+uid+".x");
    		y = getHorseConfig().getDouble("horses."+uid+".y");
    		z = getHorseConfig().getDouble("horses."+uid+".z");
    		world = getHorseConfig().getString("horses."+uid+".world");
    	}
    	else
    	{
    		String query = "SELECT x, y, z, world FROM " + getConfig().getString("MySQL.prefix") + "horses WHERE uid='"+uid+"'";
    		debug(query);
    		rs = queryDB(query);
    		try {
				rs.next();
				x = rs.getDouble("x");
				y = rs.getDouble("y");
				z = rs.getDouble("z");
				world = rs.getString("world");
			} catch (SQLException e) {
				debug("Unable to find location.");
				return null;
			}
    	}

    	if ( world == null )
    		return null;
    	
    	Location loc = new Location( getServer().getWorld(world), x, y, z); 
    	return loc;
    }
    
    String findHorse( String args[], int Start, String owner )
    {
    	String name = "";
		
		while ( Start < args.length )
		{
			name = name + " " + args[Start];
			Start++;
		}
		
		name = name.trim();
    	if(flatfile)
        {
    		debug("Flatfile searching....");
            for(String horse : getHorseConfig().getConfigurationSection("horses").getKeys(false) )
            {
                if(getHorseConfig().getString("horses."+horse+".owner").equals( owner ) )
                {
                	if ( getHorseConfig().getString("horses."+horse+".named").toLowerCase().matches(name.toLowerCase()) )
                		return horse;
                }
                 
            }
            return null;
        }
    	debug("SQL Searching ...");
    	name = name.replace("'","`");
    	name = name.replace(";","");
    	String query = "SELECT * from " + getConfig().getString("MySQL.prefix") + "horses WHERE UPPER(owner)=UPPER('"+owner+"') AND named LIKE '"+name+"%'";
    	debug(query);
    	rs = queryDB(query);
    	if ( rs == null ) return null;
    	try {
    		debug("Found query ..");
			rs.next();
			return rs.getString("uid");
			
		} catch (SQLException e) {
			debug(query);
			error("findHorse");
			return null;
		}
    	
    	
    	
    }
    
    public void OpenDatabase()
    {     String url = "";
   
    if(setup && flatfile)
        return;
    
    if(getConfig().getBoolean("MySQL.useMySQL"))
    {
        flatfile = false;
        String user = getConfig().getString("MySQL.user");
        String pass = getConfig().getString("MySQL.password");
        String host = getConfig().getString("MySQL.host");
        String db = getConfig().getString("MySQL.database");
        String port = getConfig().getString("MySQL.port");
        if(port.equals("0"))
            url = (new StringBuilder("jdbc:mysql://")).append(host).append("/").append(db).toString();
        else
            url = (new StringBuilder("jdbc:mysql://")).append(host).append(":").append(port).append("/").append(db).toString();
        try
        {
            conn = DriverManager.getConnection(url, user, pass);
            getServer().getLogger().info("Stables loading with MySQL.");
        }
        catch(SQLException e)
        {
            error("Unable to open database with MySQL - Check your database information.");
            debug((new StringBuilder()).append(e.getStackTrace()).toString());
        }
    } else
    if(getConfig().getBoolean("MySQL.useSQLite"))
    {
        flatfile = false;
        String sDriverName = "org.sqlite.JDBC";
        try
        {
            Class.forName(sDriverName);
        }
        catch(ClassNotFoundException e1)
        {
            error("Unable to load SqlDrivers - Converting to Flatfile!");
            debug((new StringBuilder()).append(e1.getStackTrace()).toString());
            flatfile = true;
            return;
        }
        url = (new StringBuilder("jdbc:sqlite:")).append(new File(getDataFolder(), "stables.db")).toString();
        try
        {
            conn = DriverManager.getConnection(url);
            getServer().getLogger().info("Stables loading with SQLite.");
        }
        catch(SQLException e)
        {
            error("Unable to open database with SQLite");
            debug((new StringBuilder()).append(e.getStackTrace()).toString());
        }
    } else
    {
        flatfile = true;
        setup = true;
        getServer().getLogger().info("Flatfile Database being used : Virtual Stables (Storage) have been disabled.");
        return;
    }
       
      
    if ( setup == true )
    	return;

    setup = true;

            writeDB((new StringBuilder("CREATE TABLE IF NOT EXISTS ")).append(getConfig().getString("MySQL.prefix")).append("horses ( id double PRIMARY KEY, uid text, owner text, tamed long, named text, x double, y double, z double, world text ) ").toString());
            writeDB((new StringBuilder("CREATE TABLE IF NOT EXISTS ")).append(getConfig().getString("MySQL.prefix")).append("riders ( id double PRIMARY KEY, uid text, name text, owner text, horse_id integer ) ").toString());
            writeDB((new StringBuilder("CREATE TABLE IF NOT EXISTS ")).append(getConfig().getString("MySQL.prefix")).append("owners ( id double PRIMARY KEY, name text, horse text, horse_id integer )").toString());
            writeDB((new StringBuilder("CREATE TABLE IF NOT EXISTS ")).append(getConfig().getString("MySQL.prefix")).append("stable ( id double PRIMARY KEY, uid text, name text, owner text, health integer, type integer, chested boolean, bred boolean, variant integer, temper integer, tamed boolean, saddled boolean, armoritem integer)").toString());
            AddCol((new StringBuilder(String.valueOf(getConfig().getString("MySQL.prefix")))).append("riders").toString(), "horse_id", "integer");
            AddCol((new StringBuilder(String.valueOf(getConfig().getString("MySQL.prefix")))).append("owners").toString(), "horse_id", "integer");
            AddCol((new StringBuilder(String.valueOf(getConfig().getString("MySQL.prefix")))).append("stable").toString(), "name", "text");
            AddCol((new StringBuilder(String.valueOf(getConfig().getString("MySQL.prefix")))).append("stable").toString(), "owner", "text");
            AddCol((new StringBuilder(String.valueOf(getConfig().getString("MySQL.prefix")))).append("stable").toString(), "str", "double");
            AddCol(getConfig().getString("MySQL.prefix") + "horses", "world", "text");
            
            		
            if(getConfig().getBoolean("MySQL.useMySQL"))
            {
                writeDB((new StringBuilder("ALTER TABLE ")).append(getConfig().getString("MySQL.prefix")).append("horses  CHANGE  `id`  `id` DOUBLE NOT NULL AUTO_INCREMENT").toString());
                writeDB((new StringBuilder("ALTER TABLE ")).append(getConfig().getString("MySQL.prefix")).append("riders  CHANGE  `id`  `id` DOUBLE NOT NULL AUTO_INCREMENT").toString());
                writeDB((new StringBuilder("ALTER TABLE ")).append(getConfig().getString("MySQL.prefix")).append("owners  CHANGE  `id`  `id` DOUBLE NOT NULL AUTO_INCREMENT").toString());
                writeDB((new StringBuilder("ALTER TABLE ")).append(getConfig().getString("MySQL.prefix")).append("stable  CHANGE  `id`  `id` DOUBLE NOT NULL AUTO_INCREMENT").toString());
                writeDB((new StringBuilder("ALTER TABLE ")).append(getConfig().getString("MySQL.prefix")).append("horses  CHANGE  `tamed`  `tamed` LONG").toString());
            }
    }

    public void writeDB(String query)
    {
        try
        {
            if(conn == null)
                OpenDatabase();
            Statement statement = conn.createStatement();
            statement.setQueryTimeout(10);
            statement.executeUpdate(query);
        }
        catch(SQLException e)
        {
            error("writeDB error");
        }
    }

    public ResultSet queryDB(String query)
    {
    	  try
          {
        if(rs != null)
            rs.close();
        
        if(conn == null || conn.isClosed() )
            OpenDatabase();
        
        Statement statement = conn.createStatement();
        statement.setQueryTimeout(30);
        rs = statement.executeQuery(query);
        return rs;
          }
    	  catch (SQLException e)
    	  {
   	        if(rs != null) try { rs.close(); } catch (SQLException e1) { error("queryDB error, ResultSet"); }
    	    if(conn != null) try { conn.close(); } catch (SQLException e2) { error("queryDB error, Connection");}
    	    
    	    return null;
    	  }
    }

    public void CloseDatabase()
    {
        try
        {
            if(conn != null)
            {
                rs.close();
                conn.close();
            }
        }
        catch(SQLException e)
        {
            error("closeDatabase() error.");
        }
    }
    
    public void commandStore(final Player p)
    {
        if(p == null)
            return;
        if(flatfile)
        {
            local(p,"FLATFILE_STABLES");
            return;
        } else
        {
            local(p,"HIT_STORE");
            p.setMetadata("stables.store", new FixedMetadataValue(plugin, Boolean.valueOf(true)));
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            	public void run() {
            		if ( p.hasMetadata("stables.store"))
            		{
            		p.removeMetadata("stables.store",plugin);
            		p.sendMessage("Stable storage time out.");
            		}
            	}}, plugin.getConfig().getInt("stable.timeout") * 20);
            return;
        }
        
    }

    
    public void AddCol(String table, String col, String type)
    {
        if(flatfile)
            return;
        try
        {
            if(rs != null)
                rs.close();
            if(conn == null)
            {
                debug("queryDB: Database not open");
                OpenDatabase();
            }
            Statement statement = conn.createStatement();
            statement.setQueryTimeout(30);
            rs = statement.executeQuery((new StringBuilder("SELECT COUNT(")).append(col).append(") FROM ").append(table).toString());
            return;
        }
        catch(SQLException e)
        {
            writeDB((new StringBuilder("ALTER TABLE ")).append(table).append(" ADD COLUMN ").append(col).append(" ").append(type).toString());
        }
        debug("Adding colum to table ....");
    }
    
    
    public String getResultString(int num) {
    	try {
			while(rs.next()) {
				return rs.getString(num);
			}
		} catch (SQLException e) {
			debug("SQL Error");
			return null;
			//e.printStackTrace();
		}
    	return null;
    }
    

    public void updateCheck()
    {
    	/*
        boolean found = false;
        if(!getConfig().getBoolean("general.CheckUpdates"))
            return;
        String readurl = "http://dev.bukkit.org/server-mods/stables/files.rss";
        try
        {
            URL url = new URL(readurl);
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            while((str = br.readLine()) != null) 
            {
                if(str.contains("<title>") && !found)
                {
                    found = true;
                    continue;
                }
                if(!str.contains("<title>") || !found)
                    continue;
                String line = str.replace("<title>", "");
                str = line.replace("</title>", "");
                str = str.replace("Stables", "");
                currentVersion = str.trim();
                if(!currentVersion.equals(plugin.getDescription().getVersion()))
                    outOfDate = true;
                break;
            }
            br.close();
        }
        catch(IOException ioexception) { }
        */
    }

    public void lureHorse(String name)
    {
        Player player = getServer().getPlayerExact(name);
        if(player == null)
        {
            debug("Player no longer found");
            return;
        }
        
        int itemId = ((MetadataValue) player.getMetadata("stables.luring").get(0)).asInt();
        debug("Luring horse with itemId" + itemId);
        player.removeMetadata("stables.luring", plugin);
        Random generator = new Random();
        int randomNum = generator.nextInt(100) + 1;

        if(disabledWorld("horses.lure.useEnabled","horses.lure.disabled", player.getWorld().getName()))
        {
            local(player,"DISABLED_WORLD");
            return;
        }

        if(!enabledWorld("horses.lure.useEnabled","horses.lure.enabled", player.getWorld().getName()))
        {
            local(player,"DISABLED_WORLD");
            return;
        }

        
            debug("Num: " + randomNum + " - Chance = " + getConfig().getInt("lure."+itemId+".chance"));
        	
        if(randomNum > getConfig().getInt("lure."+itemId+".chance"))
        {
            local(player,"LURE_FAIL");
            return;
        }
        
        HorseModifier hm = HorseModifier.spawn(player.getLocation());
        int horseType = getConfig().getInt("lure."+itemId+".type");
        
        if ( getConfig().getInt("lure."+itemId+".type") == -1 )
        	horseType = generator.nextInt(3);
        else if ( getConfig().getInt("lure."+itemId+".type") == -2 )
        	horseType = generator.nextInt(5);
        
        
        hm.setType(HorseModifier.HorseType.fromId(horseType));
        
        if ( horseType == 0 )
        {
        
            int var = generator.nextInt(31);
            HorseModifier.HorseVariant vars[] = {
                HorseModifier.HorseVariant.WHITE, HorseModifier.HorseVariant.CREAMY, HorseModifier.HorseVariant.CHESTNUT, HorseModifier.HorseVariant.BROWN, HorseModifier.HorseVariant.BLACK, HorseModifier.HorseVariant.GRAY, HorseModifier.HorseVariant.DARK_BROWN, HorseModifier.HorseVariant.WHITE_WHITE, HorseModifier.HorseVariant.CREAMY_WHITE, HorseModifier.HorseVariant.CHESTNUT_WHITE, 
                HorseModifier.HorseVariant.BROWN_WHITE, HorseModifier.HorseVariant.BLACK_WHITE, HorseModifier.HorseVariant.GRAY_WHITE, HorseModifier.HorseVariant.DARK_BROWN_WHITE, HorseModifier.HorseVariant.WHITE_WHITE_FIELD, HorseModifier.HorseVariant.CREAMY_WHITE_FIELD, HorseModifier.HorseVariant.CHESTNUT_WHITE_FIELD, HorseModifier.HorseVariant.BROWN_WHITE_FIELD, HorseModifier.HorseVariant.BLACK_WHITE_FIELD, HorseModifier.HorseVariant.GRAY_WHITE_FIELD, 
                HorseModifier.HorseVariant.DARK_BROWN_WHITE_FIELD, HorseModifier.HorseVariant.WHITE_WHITE_DOTS, HorseModifier.HorseVariant.CREAMY_WHITE_DOTS, HorseModifier.HorseVariant.CHESTNUT_WHITE_DOTS, HorseModifier.HorseVariant.BROWN_WHITE_DOTS, HorseModifier.HorseVariant.BLACK_WHITE_DOTS, HorseModifier.HorseVariant.GRAY_WHITE_DOTS, HorseModifier.HorseVariant.DARK_BROWN_WHITE_DOTS, HorseModifier.HorseVariant.WHITE_BLACK_DOTS, HorseModifier.HorseVariant.CREAMY_BLACK_DOTS, 
                HorseModifier.HorseVariant.CHESTNUT_BLACK_DOTS, HorseModifier.HorseVariant.BROWN_BLACK_DOTS, HorseModifier.HorseVariant.BLACK_BLACK_DOTS, HorseModifier.HorseVariant.GRAY_BLACK_DOTS, HorseModifier.HorseVariant.DARK_BROWN_BLACK_DOTS
            };
                hm.setVariant(vars[var]);
        	
        }

        hm.setTamed(false);
        int MaxHealth = getConfig().getInt("lure."+itemId+".health.max");
        int MinHealth = getConfig().getInt("lure."+itemId+".health.min");
        Double health = Double.valueOf(generator.nextInt(MaxHealth - MinHealth) + MinHealth);
        hm.getHorse().setHealth(health.doubleValue());
        hm.getHorse().setMaxHealth(health.doubleValue());
    }

    public void spawnHorse(Location loc, boolean z, boolean s)
    {
        Random generator = new Random();
        int var = generator.nextInt(31);
        int type = generator.nextInt(3);
        if(z)
            type = 3;
        if(s)
            type = 4;
        HorseModifier.HorseVariant vars[] = {
            HorseModifier.HorseVariant.WHITE, HorseModifier.HorseVariant.CREAMY, HorseModifier.HorseVariant.CHESTNUT, HorseModifier.HorseVariant.BROWN, HorseModifier.HorseVariant.BLACK, HorseModifier.HorseVariant.GRAY, HorseModifier.HorseVariant.DARK_BROWN, HorseModifier.HorseVariant.WHITE_WHITE, HorseModifier.HorseVariant.CREAMY_WHITE, HorseModifier.HorseVariant.CHESTNUT_WHITE, 
            HorseModifier.HorseVariant.BROWN_WHITE, HorseModifier.HorseVariant.BLACK_WHITE, HorseModifier.HorseVariant.GRAY_WHITE, HorseModifier.HorseVariant.DARK_BROWN_WHITE, HorseModifier.HorseVariant.WHITE_WHITE_FIELD, HorseModifier.HorseVariant.CREAMY_WHITE_FIELD, HorseModifier.HorseVariant.CHESTNUT_WHITE_FIELD, HorseModifier.HorseVariant.BROWN_WHITE_FIELD, HorseModifier.HorseVariant.BLACK_WHITE_FIELD, HorseModifier.HorseVariant.GRAY_WHITE_FIELD, 
            HorseModifier.HorseVariant.DARK_BROWN_WHITE_FIELD, HorseModifier.HorseVariant.WHITE_WHITE_DOTS, HorseModifier.HorseVariant.CREAMY_WHITE_DOTS, HorseModifier.HorseVariant.CHESTNUT_WHITE_DOTS, HorseModifier.HorseVariant.BROWN_WHITE_DOTS, HorseModifier.HorseVariant.BLACK_WHITE_DOTS, HorseModifier.HorseVariant.GRAY_WHITE_DOTS, HorseModifier.HorseVariant.DARK_BROWN_WHITE_DOTS, HorseModifier.HorseVariant.WHITE_BLACK_DOTS, HorseModifier.HorseVariant.CREAMY_BLACK_DOTS, 
            HorseModifier.HorseVariant.CHESTNUT_BLACK_DOTS, HorseModifier.HorseVariant.BROWN_BLACK_DOTS, HorseModifier.HorseVariant.BLACK_BLACK_DOTS, HorseModifier.HorseVariant.GRAY_BLACK_DOTS, HorseModifier.HorseVariant.DARK_BROWN_BLACK_DOTS
        };
        HorseModifier hm = HorseModifier.spawn(loc);
        hm.setType(HorseModifier.HorseType.fromId(type));
        if(type == 0)
            hm.setVariant(vars[var]);
        hm.setTamed(true);
        hm.setSaddled(true);
        int MaxHealth = getConfig().getInt("horses.lure.health.max");
        int MinHealth = getConfig().getInt("horses.lure.health.min");
        Double health = Double.valueOf(generator.nextInt(MaxHealth - MinHealth) + MinHealth);
        hm.getHorse().setMaxHealth(health.doubleValue());
    }

    public boolean addHorse(Player p, Entity e, boolean auto)
    {
    	if ( !isHorse(e) )
    	{
    		local(p,"ADD_ERROR");
    		return false;
    	}

    	if(!canTame(p) )
        {
            return false;
        }
          
    	String horseName = "";
    	
    	if ( !auto )
    		horseName = p.getItemInHand().getItemMeta().getDisplayName().replace("'", "`");
    	else
    	{
    		LivingEntity l = (LivingEntity) e;
    		if ( l.getCustomName() != null )
    			horseName = l.getCustomName();
    		else
    			horseName = getRandomName();
    	}
    	
        if(Stables.flatfile)
        {
            debug("Flatfile new horse");
            getHorseConfig().set((new StringBuilder("riders.")).append(e.getUniqueId()).toString(), null);
            getHorseConfig().set((new StringBuilder("owners.")).append(e.getUniqueId()).toString(), p.getName());
            getHorseConfig().set((new StringBuilder("horses.")).append(e.getUniqueId()).append(".owner").toString(), p.getName());
            getHorseConfig().set((new StringBuilder("horses.")).append(e.getUniqueId()).append(".tamed").toString(), Long.valueOf(System.currentTimeMillis()));
            getHorseConfig().set((new StringBuilder("horses.")).append(e.getUniqueId()).append(".named").toString(), horseName);
        } else
        {
        	
            writeDB((new StringBuilder("DELETE FROM ")).append(getConfig().getString("MySQL.prefix")).append("riders WHERE uid='").append(e.getUniqueId().toString()).append("'").toString());
            writeDB((new StringBuilder("INSERT INTO ")).append(getConfig().getString("MySQL.prefix")).append("owners (name,horse) VALUES('").append(p.getName()).append("', '").append(e.getUniqueId().toString()).append("')").toString());
            writeDB((new StringBuilder("INSERT INTO ")).append(getConfig().getString("MySQL.prefix")).append("horses (uid, owner, tamed, named, x, y, z) VALUES( '").append(e.getUniqueId().toString()).append("', '").append(p.getName()).append("', ").append(System.currentTimeMillis()).append(", '").append(horseName).append("', 0, 0, 0 )").toString());
        }
        
        
        saveLocation( (Horse) e );
        if ( auto )
        {
        	LivingEntity l = (LivingEntity) e;
        	l.setCustomName(horseName);
        	local(p,"ADD_AUTO");
        }
        else
    	local(p,"NEW_STEED");
        
        if ( getConfig().getBoolean("horses.AutoSaddle") == true )
        {
        	debug("Adding a saddle!");
        	HorseModifier hm = new HorseModifier((LivingEntity) e);
        	hm.setSaddled(true);

        }
        
        return true;
    }
    
    public String getRandomName()
    {
        Random generator = new Random();
        int num = generator.nextInt(randomNames.size());
        String name = "Biscuit";
        try {
        name = randomNames.get(num);
        } catch (NullPointerException e) {
        	name = "Biscuit";
        }
    	
    	return name;
    }
    
    public boolean isRider(String name, String horse)
    {
    		if(flatfile)
                return getHorseConfig().contains((new StringBuilder("riders.")).append(horse).append(".").append(name).toString());
    	else
    	{
      String query = "SELECT id FROM "+getConfig().getString("MySQL.prefix")+"riders WHERE uid='" + horse + "' AND name='" + name + "'";
      debug(query);
        try {
        	  rs = queryDB(query);
        	  rs.next();
        	  if ( rs.getInt("id") >= 0 )
        		  return true;

        	  //debug("not a rider.");
        	  return false;
		} catch (SQLException e) {
			//e.printStackTrace();
			return false;
		}
    	}
    }

    public boolean isOwner(String name, String horse)
    {
		if (flatfile) {
            return name.equalsIgnoreCase(getHorseConfig().getString("horses."+horse+".owner").toString());

		} else {
			try {
				rs = queryDB((new StringBuilder("SELECT id FROM "
						+ getConfig().getString("MySQL.prefix")
						+ "riders WHERE uid='")).append(horse)
						.append("' AND owner='").append(name).append("'")
						.toString());
				rs.next();
				if (rs.getInt("id") >= 0)
					return true;

				return false;
			} catch (SQLException e) {
				return false;
			}
		}
    }
    
    void nameHorse( String id, String name)
    {
        if ( Stables.flatfile )
        {
        plugin.getHorseConfig().set("horses."+id + ".named", name);
        }
        else
        {
            plugin.writeDB("UPDATE " + plugin.getConfig().getString("MySQL.prefix") + "horses SET named='"+name+"' WHERE uid='"+id+"'");
        }

    }
    
    void saveLocation( Horse h )
    {
	if (plugin.HorseOwner(h.getUniqueId().toString()) != null) {
		debug("Saving horse location : "+h.getUniqueId());
		
		int x = h.getLocation().getBlockX();
		int y = h.getLocation().getBlockY();
		int z = h.getLocation().getBlockZ();

		if (Stables.flatfile) {
			plugin.getHorseConfig().set("horses." + h.getUniqueId() + ".x",	x);
			plugin.getHorseConfig().set("horses." + h.getUniqueId() + ".y",	y);
			plugin.getHorseConfig().set("horses." + h.getUniqueId() + ".z",	z);
			plugin.getHorseConfig().set("horses." + h.getUniqueId() + ".world", h.getLocation().getWorld().getName());
		} else {
			String query = "UPDATE "
					+ getConfig().getString("MySQL.prefix")
					+ "horses SET x=" + x + ", y=" + y + ", z=" + z +", world='"+h.getLocation().getWorld().getName()+"'"
					+ " WHERE uid='" + h.getUniqueId() + "';";
			plugin.writeDB(query);
			debug(query);
		}
	}
    }
    
    
    private void setupLanguage() {
    	
    	// English
    	setLang("SYNTAX","Syntax is: ");
    	setLang("ADD_HIT","Punch the horse you want to add the rider to.");
    	setLang("ADD_NOARG","Who do you want to add as a rider?");
    	setLang("CONFIG_ERROR","Could not save config to");
    	setLang("CONFIG_RELOAD","Stables configuration reloaded.");
    	setLang("CONFIG_SAVE","Horses saved.");
    	setLang("DEL_HIT","Punch the horse you want to delete the rider from.");
    	setLang("DEL_NOARG","Who do you want to delete as a rider?");
    	setLang("HIT_FREE","You set this beast free.");
    	setLang("HIT_MAX","You already own too many horses! You cannot tame this beast.");
    	setLang("HIT_NEW","Enjoy your new steed!");
    	setLang("HIT_REMOVE","Punch the horse you want to remove the owner from.");
    	setLang("LIST_NOARG","Who do you wish to list the horses of?");
    	setLang("LIST_OWNED","owns the following horses:");
    	setLang("NO_CONSOLE","This command cannot be run from the console.");
    	setLang("NO_PERM","You do not have permission for that.");
    	setLang("NOT_OWNER","That is not even your horse!");
    	setLang("PERM_NOCLEAR","That is not your horse! You cannot set it free!");
    	setLang("PERM_NORIDE","You have not been given permission to ride that horse!");
    	setLang("PERM_NOTHEFT","That is not your horse! That belongs to ");
    	setLang("RECIPE_ADDED","Recipe added:");
    	setLang("REMOVE_NOARG","Who do you wish to remove the horses of?");
    	setLang("REMOVE_NOHORSE","That player owns no horses.");
    	setLang("RIDER_ADD","Rider added!");
    	setLang("RIDER_DEL","Rider removed.");
    	setLang("RO_HIT","Punch the horse you want to remove the owner of.");
    	setLang("UNKNOWN_OWNER","That owner is unknown.");
    	
    	setLang("SUMMON_HORSE","You summon your steed to your location.");
    	setLang("CHECK_HIT","Punch the horse you want to check the info of.");
    	setLang("LIST_NOHORSE", "That player owns no horses.");
    	setLang("HORSE_UNKNOWN","A horse by that name was not located.");
    	setLang("HORSE_NOT_FOUND","Your steed could not be located.");
    	setLang("TP_FOUND","You teleport to your steed's last known location.");
    	setLang("COMMAND_DISABLED","A mystical force prevents you from doing this.");
    	setLang("HORSE_WRONG_WORLD","Your steed was not found in this world.");
    	setLang("COMPASS_LOCKED","Your compass has locked in to your steed's last location.");
    	setLang("DISABLED_WORLD","You are unable to do that here!");
    	setLang("TOO_POOR","You are unable to afford the stable master's fee of $");
    	setLang("FEE_COLLECT","The stable master collects his fee of $");
    	setLang("MASTER_STORE","The stable master leads your horse into a stall.");
    	setLang("TOO_MANY_HORSES","You already own too many horses! You cannot tame this beast.");
    	setLang("FLATFILE_STABLES","The horse stables are currently closed.");
    	setLang("HIT_STORE","Hit the horse you wish to store.");
    	setLang("LURE_FAIL","You failed to lure any horses out.");
    	setLang("EXIT_NOT_TAME","This horse has not yet been named, and is not claimed by you. Use a name tag to claim it for your own!");
    	setLang("PUNISH_BREED","Your ability to breed horses has been revoked.");
    	setLang("PUNISH_NAME","Your ability to name horses has been revoked.");
    	setLang("NOT_RIDER","has not given you permission to ride that horse!");
    	setLang("SET_FREE","You set this beast free.");
    	setLang("NEW_STEED","Enjoy your new steed!");
    	setLang("REMOVE_CHEST","You have removed the chest from your steed.");
    	setLang("NO_CHESTS","The stable master cannot be held responsible for a horse's inventory, and refuses to stable your steed at this time. You may use /stables removechest instead.");
    	setLang("ALREADY_LURE","Shh! You're already trying to lure out a horse!");
    	setLang("START_LURE","You begin trying to lure out a horse ...");
    	setLang("RECIPE_PERM","You do not have the knowledge to craft that item!");
    	setLang("HORSE_ABANDON","You abandon your steed.");
    	setLang("HORSE_ABANDON_NOT_FOUND","You abandon your steed. Note: The physical horse was not located. As such, it may remain 'named', but is no longer claimed by you.");
    	setLang("CMD_NAME","Change the name of (player)'s horse to (new name)");
    	setLang("CMD_ADD","Add (rider) to your horse");
    	setLang("CMD_DEL","Remove (rider) from your horse");
    	setLang("CMD_LIST","List all of your own horses");
    	setLang("CMD_ABANDON","Free (horse) from your ownership");
    	setLang("CMD_VIEW","Show all horses in your virtual stables");
    	setLang("CMD_STORE","Store a horse in your virtual stables");
    	setLang("CMD_RECOVER","Recover horse # from your virtual stables. Requires #, NOT NAME");
    	setLang("CMD_FIND","Point a compass to your horse's last location");
    	setLang("CMD_SUMMON","Summon your horse to your location");
    	setLang("CMD_TP","Teleport to your horse's last location");
    	setLang("CMD_CHECK","View a horse's information & owner");
    	setLang("CMD_RO","Remove a horse's owner");
    	setLang("CMD_LISTALL","View all of (player)'s horses");
    	setLang("CMD_CLEAR","Remove ALL horses owned by (player)");
    	setLang("CMD_RELOAD","Reload the config file - will not change database options");
    	setLang("CMD_SAVE","Force a save of the horse database");
    	setLang("CMD_CONFIG","Alter config options");
    	setLang("CMD_CONVERT","Convert Flatfile YAML config to SQL");
    	setLang("CMD_RENAME","Rename a horse from a random list of names");
    	setLang("ADD_ERROR","That is not a horse! You cannot claim it!");
    	setLang("ADD_AUTO","You have claimed this steed as your own!");
    	setLang("NEW_NAME","You have given your steed a new name!");
    	setLang("RENAME_NOT_FOUND","Your horse couldn't be found near by - are you too far away?");
    	setLang("RECOVER_WG","This area is protected! The stablemaster will not deliver here!");
    	 savelocalConfig();
    }
    
    public void addRandomNames()
    {
    	
    	if ( !getConfig().isConfigurationSection("randomNames") )
    	{
    		List<String> n = new ArrayList<String>();
    
    n.add("Ace");
    n.add("Agatha");
    n.add("Airheart");
    n.add("Amberlocks");
    n.add("Ambrosia");
    n.add("Amethyst Star");
    n.add("Apple Bloom");
    n.add("Apple Brown Betty");
    n.add("Apple Bumpkin");
    n.add("Apple Cinnamon");
    n.add("Apple Dumpling");
    n.add("Apple Fritter");
    n.add("Apple Pie");
    n.add("Apple Rose");
    n.add("Apple Tart");
    n.add("Applejack");
    n.add("Archer");
    n.add("Arctic Lily");
    n.add("Atlas");
    n.add("Aura");
    n.add("Autumn Gem");
    n.add("Ballad");
    n.add("Baritone");
    n.add("Beauty Brass");
    n.add("Bee Bop");
    n.add("Belle Star");
    n.add("Berry Frost");
    n.add("Berry Splash");
    n.add("Big Shot");
    n.add("Big Wig");
    n.add("Black Marble");
    n.add("Black Stone");
    n.add("Blaze");
    n.add("Blossomforth");
    n.add("Blue Belle");
    n.add("Blue Bonnet");
    n.add("Bluebell");
    n.add("Bonnie");
    n.add("Boo");
    n.add("Bottlecap");
    n.add("Brown Sugar");
    n.add("Buddy");
    n.add("Bumpkin");
    n.add("Caesar");
    n.add("Calamity Mane");
    n.add("Cappuccino");
    n.add("Caramel");
    n.add("Caramel Apple");
    n.add("Castle");
    n.add("Charcoal");
    n.add("Charm");
    n.add("Cheerilee");
    n.add("Cheery");
    n.add("Cherry Berry");
    n.add("Chocolate");
    n.add("Cinnabelle");
    n.add("Cinnamon Swirl");
    n.add("Classy Clover");
    n.add("Clip Clop");
    n.add("Cloudchaser");
    n.add("Cloudy");
    n.add("Clover");
    n.add("Cobalt");
    n.add("Coconut");
    n.add("Concerto");
    n.add("Cornflower");
    n.add("Cosmic");
    n.add("Cotton ");
    n.add("Cream Puff");
    n.add("Creme Brulee");
    n.add("Crescent Moon");
    n.add("Dainty");
    n.add("Daisy");
    n.add("Derpy");
    n.add("Dinky Doo");
    n.add("Dipsy");
    n.add("Dosie Dough");
    n.add("Dr. Hooves");
    n.add("Drizzle");
    n.add("Dry Wheat");
    n.add("Dust Devil");
    n.add("Earl Grey");
    n.add("Electric Blue");
    n.add("Eliza");
    n.add("Emerald Beacon");
    n.add("Esmeralda");
    n.add("Evening Star");
    n.add("Fancy Pants");
    n.add("Felix");
    n.add("Fiddlesticks");
    n.add("Fire Streak");
    n.add("Flank Sinatra");
    n.add("Flash");
    n.add("Fleetfoot");
    n.add("Flitter");
    n.add("Flounder");
    n.add("Flurry");
    n.add("Ginger");
    n.add("Gingerbread");
    n.add("Giselle");
    n.add("Gizmo");
    n.add("Golden Glory");
    n.add("Golden Harvest");
    n.add("Goldilocks");
    n.add("Graceful Falls");
    n.add("Granny Pie");
    n.add("Graphite");
    n.add("Harry Trotter");
    n.add("Hay Fever");
    n.add("Haymish");
    n.add("Hercules");
    n.add("Hoity Toity");
    n.add("Honeycomb");
    n.add("Hope");
    n.add("Hot Wheels");
    n.add("Ivory");
    n.add("Jangles");
    n.add("Junebug");
    n.add("Knit Knot");
    n.add("Lance");
    n.add("Laurette");
    n.add("Lavender Skies");
    n.add("Lavenderhoof");
    n.add("Lemon Chiffon");
    n.add("Liberty Belle");
    n.add("Lickety Split");
    n.add("Lightning Bolt");
    n.add("Lightning Streak");
    n.add("Lily Valley");
    n.add("Lincoln");
    n.add("Long Jump");
    n.add("Lotus Blossom");
    n.add("Lucky Clover");
    n.add("Majesty");
    n.add("Marigold");
    n.add("Masquerade");
    n.add("Meadow Song");
    n.add("Melody");
    n.add("Merry May");
    n.add("Milky Way");
    n.add("Millie");
    n.add("Mochaccino");
    n.add("Moondancer");
    n.add("Nightingale");
    n.add("Nixie");
    n.add("Ocean Breeze");
    n.add("Opal Bloom");
    n.add("Orange Blossom");
    n.add("Orchid Dew");
    n.add("Orion");
    n.add("Paisley Pastel");
    n.add("Pampered Pearl");
    n.add("Paradise");
    n.add("Peachy Cream");
    n.add("Peachy Pie");
    n.add("Peachy Sweet");
    n.add("Peppermint Crunch");
    n.add("Persnickety");
    n.add("Petunia");
    n.add("Pigpen");
    n.add("Pink Lady");
    n.add("Pipsqueak");
    n.add("Pixie");
    n.add("Pound Cake");
    n.add("Primrose");
    n.add("Princess");
    n.add("Pristine");
    n.add("Professor Bill Neigh");
    n.add("Pumpkin");
    n.add("Purple Haze");
    n.add("Quake");
    n.add("Quicksilver");
    n.add("Ragtime");
    n.add("Rain Dance");
    n.add("Rainbow");
    n.add("Rainbowshine");
    n.add("Raindrops");
    n.add("Rapidfire");
    n.add("Raven");
    n.add("Red Rose");
    n.add("Riverdance");
    n.add("Rose");
    n.add("Rose Quartz");
    n.add("Rosewing");
    n.add("Roxie");
    n.add("Rumble");
    n.add("Sandstorm");
    n.add("Sapphire Rose");
    n.add("Seasong");
    n.add("Serenity");
    n.add("Shamrock");
    n.add("Shining Star");
    n.add("Shoeshine");
    n.add("Shortround");
    n.add("Sightseer");
    n.add("Silver Lining");
    n.add("Silverspeed");
    n.add("Silverwing");
    n.add("Sky");
    n.add("Slipstream");
    n.add("Smart Cookie");
    n.add("Smokestack");
    n.add("Snails");
    n.add("Snips");
    n.add("Snowflake");
    n.add("Spitfire");
    n.add("Spring Flower");
    n.add("Spring Skies");
    n.add("Squeaky Clean");
    n.add("Star Bright");
    n.add("Star Gazer");
    n.add("Starburst");
    n.add("Stardancer");
    n.add("Starlight");
    n.add("Steamer");
    n.add("Stella");
    n.add("Storm");
    n.add("Stormfeather");
    n.add("Strawberry Cream");
    n.add("Strike");
    n.add("Sugar Plum");
    n.add("Sugarberry");
    n.add("Sun Streak");
    n.add("Sunburst");
    n.add("Sunlight");
    n.add("Sunny");
    n.add("Sunset ");
    n.add("Sunstone");
    n.add("Surf");
    n.add("Surprise");
    n.add("Sweet Dreams");
    n.add("Sweet Tart");
    n.add("Sweet Tooth");
    n.add("Sweetie Belle");
    n.add("Symphony");
    n.add("Thorn");
    n.add("Thornhoof");
    n.add("Tiger Lily");
    n.add("Toastie");
    n.add("Toffee");
    n.add("Treasure");
    n.add("Twilight Sky");
    n.add("Twilight Sparkle");
    n.add("Twinkleshine");
    n.add("Twist");
    n.add("Unicorn King");
    n.add("Vera");
    n.add("Vigilance");
    n.add("Whiplash");
    n.add("Wild Fire");
    n.add("Wildwood Flower");
    n.add("Wisp");
    n.add("Yo-Yo");
    n.add("Zodiac");
    
	
    randomNameConfig.set("randomNames",n); 
    try {
		randomNameConfig.save(randomNameFile);
	} catch (IOException e) {
		debug("Error saving flatfile");
	}
    randomNames = (ArrayList<String>) randomNameConfig.getStringList("randomNames");

    
	
    	}
    	
    	
    


    }
}

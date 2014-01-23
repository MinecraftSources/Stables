package com.ivalicemud.stables;


import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import com.ivalicemud.stables.HorseModifier.HorseType;
import com.ivalicemud.stables.HorseModifier.HorseVariant;



public class Stables extends JavaPlugin implements Listener
{
	 
	Connection conn;
	static Map<String, String> lang = null;//new HashMap<>();
	static Map<String, String> localenUS = new HashMap<>();
	static Map<String, String> localdeDE = new HashMap<>();
	
	private FileConfiguration horseConfig = null;
	private File horseConfigFile = null;
	
    public static Stables plugin;
    ResultSet rs;
    
    public Stables()
    {
    }
    
    public void convertConfig() {
    	
    }
    
    public void LoadConfiguration() 
    {
        	
    		convertConfig();
        	if (!getConfig().contains("general.Debug")) this.getConfig().set("general.Debug",false);
        	if (!getConfig().contains("general.BlockAll")) this.getConfig().set("general.BlockAll",false);
        	if (!getConfig().contains("general.PVPDamage")) this.getConfig().set("general.PVPDamage",true);
        	if (!getConfig().contains("general.EnvironmentDamage")) this.getConfig().set("general.EnvironmentDamage",true);
        	if (!getConfig().contains("general.OwnerDamage")) this.getConfig().set("general.OwnerDamage",false);
        	if (!getConfig().contains("general.MobDamage")) this.getConfig().set("general.MobDamage",false);
        	if (!getConfig().contains("general.Theft")) this.getConfig().set("general.Theft",false);
        	
        	if (!getConfig().contains("general.MaxOwned.default")) this.getConfig().set("general.MaxOwned.default",3);
        	if (!getConfig().contains("general.MaxOwned.1")) this.getConfig().set("general.MaxOwned.1",10);
        	if (!getConfig().contains("general.MaxOwned.2")) this.getConfig().set("general.MaxOwned.2",20);
        	if (!getConfig().contains("general.MaxOwned.3")) this.getConfig().set("general.MaxOwned.3",30);
        	if (!getConfig().contains("general.Language")) this.getConfig().set("general.Language", "enUS");
        	if (!getConfig().contains("general.ProtectUnclaimed")) this.getConfig().set("general.ProtectUnclaimed",false);
        	
        	if (!getConfig().contains("horses.tame.AllowMaxNamed")) this.getConfig().set("horses.tame.AllowMaxNamed",false);
        	
        	
        	if (!getConfig().contains("horses.lure.allow")) this.getConfig().set("horses.lure.allow",true);
        	if (!getConfig().contains("horses.lure.chance")) this.getConfig().set("horses.lure.chance",50);
        	if (!getConfig().contains("horses.lure.item")) this.getConfig().set("horses.lure.item",396);
        	if (!getConfig().contains("horses.lure.delay")) this.getConfig().set("horses.lure.delay",10);
        	if (!getConfig().contains("horses.lure.disabled")) this.getConfig().set("horses.lure.disabled","Nether, The End");
        	if (!getConfig().contains("horses.lure.health.max")) this.getConfig().set("horses.lure.health.max",30);
        	if (!getConfig().contains("horses.lure.health.min")) this.getConfig().set("horses.lure.health.min",15);
        	
        	if (!getConfig().contains("recipe.saddle")) this.getConfig().set("recipe.saddle",true);
        	if (!getConfig().contains("recipe.nametag")) this.getConfig().set("recipe.nametag",true);
        	if (!getConfig().contains("recipe.armor.iron")) this.getConfig().set("recipe.armor.iron",true);
        	if (!getConfig().contains("recipe.armor.gold")) this.getConfig().set("recipe.armor.gold",true);
        	if (!getConfig().contains("recipe.armor.diamond")) this.getConfig().set("recipe.armor.diamond",true);
        	if (!getConfig().contains("recipe.hay")) this.getConfig().set("recipe.hay",true);
        	saveConfig();
        	
        	getHorseConfig();
        	
        	
    }

    public void onEnable()
    {
        plugin = this;

        getServer().getPluginManager().registerEvents(new EventListener(), this);
	     
        LoadConfiguration();
        OpenDatabase();
        ConvertDatabase();
        GetLanguage();
        SetupRecipes();
      }
    
    public void onDisable()
    {
    	CloseDatabase();
    	//saveHorseConfig();
    }
 
    public String l(String phrase) {
    	return lang.get(phrase);
    }
    
    public void GetLanguage()
    {
    	SetupLanguage();
    	switch( getConfig().getString("general.Language") )
    	{
    	default: getServer().getLogger().severe("Language in 'general.Language' not found! Defaulting to English!");
    			lang = localenUS;
    			break;
    	case "enUS": lang = localenUS; break;
    	case "deDE": lang = localdeDE; break;
    	}
    }
    
 
    public void SetupRecipes() 
    {
    	org.bukkit.Server server = getServer();
    	
    	if ( getConfig().getBoolean("recipe.saddle") == true ) {
    		
            ShapedRecipe Saddle = new ShapedRecipe(new ItemStack(Material.SADDLE));
            Saddle.shape(new String[] {
                "LLL", "LIL", "I I"
            });
            Saddle.setIngredient('L', Material.LEATHER);
            Saddle.setIngredient('I', Material.IRON_INGOT);
            server.addRecipe(Saddle);
            getServer().getLogger().info( l("RECIPE_ADDED") + " 'SADDLE'");
    	}
    	
  	if ( getConfig().getBoolean("recipe.nametag") == true ) {
    		
            ShapedRecipe NameTag = new ShapedRecipe(new ItemStack(Material.NAME_TAG));
            NameTag.shape(new String[] {
                "  S", " P ", "P  "
            });
            NameTag.setIngredient('S', Material.STRING);
            NameTag.setIngredient('P', Material.PAPER);
            server.addRecipe(NameTag);
            getServer().getLogger().info(l("RECIPE_ADDED") + " 'NAME_TAG'");
    	}
  	
  	if ( getConfig().getBoolean("recipe.armor.iron") == true ) {
		
        ShapedRecipe ArmorIron = new ShapedRecipe(new ItemStack(Material.IRON_BARDING));
        ArmorIron.shape(new String[] {
                "  I", "ILI", "III"
        });
        //ArmorIron.setIngredient('L'); //Material.WOOL);
        ArmorIron.setIngredient('L', Material.WOOL, -1);
        ArmorIron.setIngredient('I', Material.IRON_INGOT);
        server.addRecipe(ArmorIron);
        getServer().getLogger().info(l("RECIPE_ADDED") + " 'IRON_BARDING'");
	}
 	if ( getConfig().getBoolean("recipe.armor.gold") == true ) {
		
        ShapedRecipe ArmorGold = new ShapedRecipe(new ItemStack(Material.GOLD_BARDING));
        ArmorGold.shape(new String[] {
                "  I", "ILI", "III"
        });
        ArmorGold.setIngredient('L', Material.WOOL, -1);;
        ArmorGold.setIngredient('I', Material.GOLD_INGOT);
        server.addRecipe(ArmorGold);
        getServer().getLogger().info(l("RECIPE_ADDED") + " 'GOLD_BARDING'");

	}
 	if ( getConfig().getBoolean("recipe.armor.diamond") == true ) {
		
        ShapedRecipe ArmorDiamond = new ShapedRecipe(new ItemStack(Material.DIAMOND_BARDING));
        ArmorDiamond.shape(new String[] {
                "  I", "ILI", "III"
        });
        ArmorDiamond.setIngredient('L', Material.WOOL, -1);
        ArmorDiamond.setIngredient('I', Material.DIAMOND);
        server.addRecipe(ArmorDiamond);
        getServer().getLogger().info(l("RECIPE_ADDED") + " 'DIAMOND_BARDING'");

	}
if ( getConfig().getBoolean("recipe.hay") == true ) {
		
		getServer().addRecipe(new ShapelessRecipe(new ItemStack(Material.WHEAT,9)).addIngredient(1, Material.HAY_BLOCK));
        getServer().getLogger().info(l("RECIPE_ADDED") + " 'WHEAT_FROM_HAY'");

	}
    }
    
    public String HorseName(String id, LivingEntity horse) throws SQLException
    {
    	if ( id == null )
    		id = horse.getUniqueId().toString();
    	
    	rs = queryDB("SELECT named FROM horses WHERE uid='"+id+"'");

    	String name = getResultString(1); 
    	debug(name+"");
    	if ( name == null )
    	{
    		
    		if ( horse != null && horse.getCustomName() != null)
    		{
    			debug("Not found in database " + id);
    			return horse.getCustomName();
    		}
    		return "Unknown";
    	}
    	
    	return name;
    }
   
    public String HorseOwner(String id)
    {
    	rs = queryDB("SELECT owner FROM horses WHERE uid='"+id+"'");

    	String owner = getResultString(1); 
    	debug(""+owner);
    	return owner;
    	
    }
    
   
    
    public FileConfiguration getHorseConfig() {
        if (horseConfig == null) {
            this.reloadHorseConfig();
        }
        return horseConfig;
    }
    
    
    public void reloadHorseConfig() {
    	
        if (horseConfigFile == null) {
        horseConfigFile = new File(getDataFolder(),"horses.yml");
        }
        horseConfig = YamlConfiguration.loadConfiguration(horseConfigFile);

    }
    

    public void error(String msg )
    {
		Bukkit.getServer().getLogger().severe("Stables: " + msg);
    }
    
    public void debug(String msg )
    {
			if ( plugin.getConfig().getBoolean("general.Debug") == true )
			{
				Bukkit.getServer().getLogger().info("Stables DEBUG: " + msg);
			}
    }
    
        
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    	if (cmd.getName().equalsIgnoreCase("ro")) {
    		if (!(sender instanceof Player)) {
    			sender.sendMessage(l("NO_CONSOLE"));
    		} else {
    			Player p = (Player) sender;
    			p.setMetadata("stables.removeowner", new FixedMetadataValue(plugin,true));
    			p.sendMessage(l("HIT_REMOVE"));
    		}
    		return true;
    	}
    	if (cmd.getName().equalsIgnoreCase("stables")) { 
    		if ( args.length == 0 ) 
    		{
    		sender.sendMessage("Stables, version "+plugin.getDescription().getVersion());
    		sender.sendMessage("-----------------------------------");
    		sender.sendMessage("AddRider - Add (rider) to your horse.");
    		sender.sendMessage("DelRider - Remove (rider) from your horse.");
    		if ( !sender.hasPermission("stables.admin")) return true;
    		sender.sendMessage("check");
    		sender.sendMessage("removeowner");
    		sender.sendMessage("listhorses");
    		sender.sendMessage("check");
    		sender.sendMessage("clearhorses");
    		sender.sendMessage("reload");
    		sender.sendMessage("save");
    		sender.sendMessage("config");

    		return true;
    		}
    		
    		if ( args.length >= 1 && args[0].equalsIgnoreCase("version") ) {
    			sender.sendMessage("Stables, by raum266 - version "+plugin.getDescription().getVersion());
    			return true;
    		}

    		if ( args.length >= 1 && args[0].equalsIgnoreCase("report") ) {
    			sender.sendMessage("Stables Config:");
    			sender.sendMessage("Debug Mode: " + getConfig().getBoolean("general.Debug"));
    			sender.sendMessage("Block All: " + getConfig().getBoolean("general.BlockAll"));
    			sender.sendMessage("Block PVP: " + getConfig().getBoolean("general.PVPDamage"));
    			sender.sendMessage("Block Environment: " + getConfig().getBoolean("general.EnviromentDamage"));
    			sender.sendMessage("Block Owner: " + getConfig().getBoolean("general.OwnerDamage"));
    			sender.sendMessage("Block Mob: " + getConfig().getBoolean("general.MobDamage"));
    			sender.sendMessage("Allow Theft: " + getConfig().getBoolean("general.Theft"));
    			sender.sendMessage("Save Time: " + getConfig().getInt("general.Save"));
    			sender.sendMessage("Max Owned Horses: " + getConfig().getInt("general.MaxOwned"));
    			sender.sendMessage("Recipes Enabled:");
    			sender.sendMessage("saddle: " + getConfig().getBoolean("recipe.saddle"));
    			sender.sendMessage("name tags: " + getConfig().getBoolean("recipe.nametag"));
    			sender.sendMessage("iron armor: " + getConfig().getBoolean("recipe.armor.iron"));
    			sender.sendMessage("gold armor: " + getConfig().getBoolean("recipe.armor.gold"));
    			sender.sendMessage("diamond armor: " + getConfig().getBoolean("recipe.armor.diamond"));
    			sender.sendMessage("hay to wheat: " + getConfig().getBoolean("recipe.hay"));
    			return true;
    		}

    		
    		if ( args.length >= 1 && args[0].equalsIgnoreCase("check") ) {
    			if (!(sender instanceof Player)) {
        			sender.sendMessage(l("NO_CONSOLE"));
        			return true;
        		} else {
        			Player p = (Player) sender;
        			
    			if ( !sender.hasPermission("stables.admin"))
    			{
    				sender.sendMessage(l("NO_PERM"));
    				return true;
    			}
    			
    			p.setMetadata("stables.checkinfo", new FixedMetadataValue(plugin,true));
    			p.sendMessage(l("CHECK_HIT"));
    			return true;
        		}
    		}
    		if ( args.length >= 1 && args[0].equalsIgnoreCase("listhorses") ) {
        		
    			
    			if ( !sender.hasPermission("stables.admin"))
    			{
    				sender.sendMessage(l("NO_PERM"));
    				return true;
    			}
    			
    			if ( args.length == 1 )
    			{
    				sender.sendMessage(l("LIST_NOARG"));
    				return true;
    			}

    			String owner;
    			
				if ( getServer().getPlayerExact(args[1]) == null )
    			{
					
					
					OfflinePlayer player = getServer().getOfflinePlayer(args[1]);
					
					if ( !player.hasPlayedBefore() )
					{
						sender.sendMessage(l("UNKNOWN_OWNER"));
						return true;

					}
					owner = player.getName();
    			}
				else
				owner = getServer().getPlayerExact(args[1].toString()).getName();
				
    			sender.sendMessage(owner + " "+l("LIST_OWNED") +":");
    			
    			rs = queryDB("SELECT uid, tamed, named, x, y, z FROM horses WHERE owner='"+owner+"'");
    			
    			try {
					while(rs.next()) {
						sender.sendMessage("ID: " + rs.getString(1) + " : Named: " + rs.getString(3));
					}
				} catch (SQLException e) {
					debug("SQL Error");
					//e.printStackTrace();
				}
    			/*
    			if ( !getConfig().isSet("owners."+owner) ) 
    			{
    				sender.sendMessage("None.");
    				return true;
    			}
               	for(String horse : getHorseConfig().getConfigurationSection("owners."+ owner+".horse").getKeys(false))
               	{
               		sender.sendMessage("ID: " + horse + " ("+HorseName(horse,null)+")");
               		
            		continue;
               	} */
    			return true;
        		
    		}

    		if ( args.length >= 1 && args[0].equalsIgnoreCase("clearhorses") ) {
    		
        			
    			if ( !sender.hasPermission("stables.admin"))
    			{
    				sender.sendMessage(l("NO_PERM"));
    				return true;
    			}
    			
    			if (args.length == 1 )
    			{
    				sender.sendMessage(l("REMOVE_NOARG"));
    				return true;
    			}

    			if ( getServer().getPlayerExact(args[1]) == null )
    			{
					sender.sendMessage(l("UNKNOWN_OWNER"));
					return true;
    			}
    			
    			String owner = getServer().getPlayerExact(args[1]).getName();
    			
    			
           		writeDB("DELETE FROM owners WHERE name='"+owner+"'");
           		writeDB("DELETE FROM horses WHERE owner='"+owner+"'");
           		writeDB("DELETE FROM riders WHERE owner='"+owner+"'");
               	
               	sender.sendMessage("Horses cleared.");
    			return true;
        		
    		}
    		
    		if ( args.length >= 1 && args[0].equalsIgnoreCase("addrider") ) {
    			if (!(sender instanceof Player)) {
        			sender.sendMessage(l("NO_CONSOLE"));
        			return true;
        		} else {
        			Player p = (Player) sender;
        			
        		if ( args.length == 1 )
        		{
        			p.sendMessage("Who do you want to add as a rider?");
        		return true;
        		}
        		
    			p.sendMessage("Punch the horse you want to add the rider to.");
    			p.setMetadata("stables.addrider", new FixedMetadataValue(plugin,args[1].toLowerCase()));
    			
    			return true;
        		}
    		}
    		
    		if ( args.length >= 1 && args[0].equalsIgnoreCase("delrider") ) {
    			if (!(sender instanceof Player)) {
        			sender.sendMessage(l("NO_CONSOLE"));
        			return true;
        		} else {
        			Player p = (Player) sender;
        			
        		if ( args.length == 1 )
        		{
        			p.sendMessage("Who do you want to delete as a rider?");
        		return true;
        		}
        		
    			p.sendMessage("Punch the horse you want to delete the rider from.");
    			p.setMetadata("stables.delrider", new FixedMetadataValue(plugin,args[1].toLowerCase()));
    			return true;
        		}
    		}
    		
    		if ( args.length >= 1 && args[0].equalsIgnoreCase("ro") ) {
    			if (!(sender instanceof Player)) {
        			sender.sendMessage(l("NO_CONSOLE"));
        			return true;
        		} else {
        			Player p = (Player) sender;
        			
    			if ( !sender.hasPermission("stables.admin"))
    			{
    				sender.sendMessage(l("NO_PERM"));
    				return true;
    			}
    			

    			p.sendMessage("Punch the horse you want to remove the owner of.");
    			p.setMetadata("stables.removeowner", new FixedMetadataValue(plugin,true));
    			return true;
        		}
    		}
    		
    		if ( args.length >= 1 && args[0].equalsIgnoreCase("reload") ) {
    			if ( !sender.hasPermission("stables.admin"))
    			{
    				sender.sendMessage(l("NO_PERM"));
    				return true;
    			}
    			
    			this.reloadConfig();
    			sender.sendMessage("Stables configuration reloaded.");
    			return true;
    		}

    		
    		if ( args.length >= 1 && args[0].equalsIgnoreCase("config") ) {
    			if ( !sender.hasPermission("stables.admin"))
    			{
    				sender.sendMessage(l("NO_PERM"));
    				return true;
    			}
    			
    			sender.sendMessage("This command will change things in the config. It is not currently active.");
    			return true;
    		}
}
    	return false;
    }
    
   
    boolean canTame( Player player ) {
    	
    	int num = 100;
    	int owned = 0;
    	boolean notame = false;
    	
    	rs = queryDB("SELECT COUNT(*) FROM horses WHERE owner='"+player.getName()+"'");
    	try {
			while(rs.next()) {
				 owned += 1;
				
			}
		} catch (SQLException e) {
			debug("SQL Error");
			//e.printStackTrace();
		}

	      
    	if ( owned == 0 )
    	{
    		debug(player.getName() + " owns 0 horses.");
    		return true;
    	}
    	
       	
       	debug(player.getName() + " owns " + owned + " horses.");
       	
		while (num > 0) {
			
			int max = 0;
			
				if (player.hasPermission("stables.max."+num) ) {
					if ( getConfig().contains("general.MaxOwned."+num) )
					{
					max = getConfig().getInt("general.MaxOwned."+num);
					debug("Max listed: "+max);
					debug("Found VIP permission : " + num);
					if (  max >= 1 && owned >= max)
						
					notame = true;
					break;
					}
				}
			num -= 1;
		}
		if ( num <= 0 )
		{
			debug("No special perms found - using default MaxOwned");
			if ( getConfig().getInt("general.MaxOwned.default") >= 1 && 
					owned >= getConfig().getInt("general.MaxOwned.default") )
				notame = true;
		}
       	
       	if ( notame == true )
       	{
       		player.sendMessage("You already own too many horses! You cannot tame this beast.");
       		return false;
       	}
       
       	return true;
    	         		
    }
    
    public void ConvertDatabase() {
    	
    	File f = new File("plugins\\stables\\horses.yml");
    	if(!f.exists())
    	{
    		debug("nothing to convert.");
    		return;
    	}
    		
    	for(String owners : getHorseConfig().getConfigurationSection("owners").getKeys(false)) // Convert Owners
       	{
        	
			for(String horses : getHorseConfig().getConfigurationSection("owners."+owners+".horse").getKeys(false))
        	{
        		writeDB("INSERT INTO owners (name, horse) VALUES('"+owners+"','"+ horses +"')");
//        		getHorseConfig().set("owners."+owners+".horse."+horses, null);
        	}
//			getHorseConfig().set("owners."+owners+".horse", null);
//        	getHorseConfig().set("owners."+owners, null);
       	}

    	// Convert Riders
    	
    	
    	for(String horse : getHorseConfig().getConfigurationSection("horses").getKeys(false)) // Convert Horses
       	{
        	
    		if ( getHorseConfig().isSet("horses."+horse+".rider"))
    		{
    			for(String rider : getHorseConfig().getConfigurationSection("horses."+horse+".rider").getKeys(false)) // Convert Riders
    			{
    				writeDB("INSERT INTO riders (uid, name) VALUES('"+horse+"','"+ rider +"')");
//    				getHorseConfig().set("horses."+horse+".rider."+rider, null);
    			}
    		getHorseConfig().set("horses."+horse+".rider", null);
    		}

    		String owner = getHorseConfig().getString("horses."+horse+".owner");
    		long tamed = getHorseConfig().getLong("horses."+horse+".tamed");
    		String name = getHorseConfig().getString("horses."+horse+".name");
    		
    		writeDB("INSERT INTO horses (uid, owner, tamed, named, x, y, z) VALUES( '"+horse+"', '"+ owner  +"', "+ tamed +", '"+ name +"', 0, 0, 0 )");
//   		getHorseConfig().set("horses."+horse+".name",null);
//    		getHorseConfig().set("horses."+horse+".owner",null);
  //  		getHorseConfig().set("horses."+horse+".tamed",null);
//    		getHorseConfig().set("horses."+horse,null);
  
       	}

    	f.renameTo(new File("plugins\\stables\\OLD-horses.yml"));
    	debug("Database converted.");
    	
    }
    
    public void OpenDatabase() {
   	
	  String url = "jdbc:sqlite:plugins\\stables\\stables.db";
	 
	  
	  try{
	    conn = DriverManager.getConnection(url);

	    
	    writeDB("CREATE TABLE IF NOT EXISTS horses ( id double PRIMARY KEY, uid string, owner string, tamed timestamp, named string, x double, y double, z double ) ");
	    writeDB("CREATE TABLE IF NOT EXISTS riders ( id double PRIMARY KEY, uid string, name string, owner string ) ");
	    writeDB("CREATE TABLE IF NOT EXISTS owners ( id double PRIMARY KEY, name string, horse string )");
	  
	  } catch(Exception e){
	    error("Unable to open database.");
	  }

    }

    public void writeDB( String query ) {
		try {
			if ( conn == null )
				OpenDatabase();
			
			Statement statement = conn.createStatement();
			statement.setQueryTimeout(10);
			statement.executeUpdate(query);
		} catch (SQLException e) {
			error("writeDB error");
			//debug("writeDB error: "+ query);
			//e.printStackTrace();
		}
		
		return;
		
    }
    
	public ResultSet queryDB(String query) {
    	
    	 
		try {
			if ( rs != null )
				rs.close();

			if ( conn == null )
				OpenDatabase();

			Statement statement = conn.createStatement();
			statement.setQueryTimeout(30); 
		    rs = statement.executeQuery(query);
		      
		      return rs;
		} catch (SQLException e) {
			error("queryDB error");
			return null;
		}
	      
	  /*    
	      while(rs.next())
	      {
	        // read the result set
	        System.out.println("name = " + rs.getString("name"));
	        System.out.println("id = " + rs.getInt("id"));
	      }
	      */

    }
    public void CloseDatabase() {
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
          // connection close failed.
         error("closeDatabase() error.");
        }
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
    
    public void lureHorse(String name)
    {
    	
    	
    	
    	Player player = getServer().getPlayerExact(name);
    	if ( player == null ) {
    		debug("Player no longer found");
    		return;
    	}
    		
    	
    	debug("Luring a horse ...");
    	player.removeMetadata("stables.luring",plugin);
    	
    	Random generator = new Random();
    	int randomNum =  generator.nextInt(100) + 1;
    	String world = getConfig().getString("horses.lure.disabled");
    	debug("Checking world: " + player.getWorld().getName() + " vs list: " + world);
    	if ( world.contains( player.getWorld().getName() )) {
   			player.sendMessage("You fail to locate any horses.");
   			return;
   		}
    	
    	if ( randomNum > getConfig().getInt("horses.lure.chance")) {
    		player.sendMessage("You fail to lure any horses.");
    		return;
    	}
    	
    	int type = generator.nextInt(3);
    	
    	if ( generator.nextInt(100) > 80 )
    		type = 0;
    	
    	int var = generator.nextInt(31);
    			HorseVariant vars[] = { HorseVariant.WHITE, HorseVariant.CREAMY, HorseVariant.CHESTNUT, HorseVariant.BROWN, HorseVariant.BLACK, HorseVariant.GRAY, HorseVariant.DARK_BROWN, HorseVariant.WHITE_WHITE, HorseVariant.CREAMY_WHITE, HorseVariant.CHESTNUT_WHITE, HorseVariant.BROWN_WHITE, HorseVariant.BLACK_WHITE, HorseVariant.GRAY_WHITE, HorseVariant.DARK_BROWN_WHITE, HorseVariant.WHITE_WHITE_FIELD, HorseVariant.CREAMY_WHITE_FIELD, HorseVariant.CHESTNUT_WHITE_FIELD, HorseVariant.BROWN_WHITE_FIELD, HorseVariant.BLACK_WHITE_FIELD, HorseVariant.GRAY_WHITE_FIELD, HorseVariant.DARK_BROWN_WHITE_FIELD, HorseVariant.WHITE_WHITE_DOTS, HorseVariant.CREAMY_WHITE_DOTS, HorseVariant.CHESTNUT_WHITE_DOTS, HorseVariant.BROWN_WHITE_DOTS, HorseVariant.BLACK_WHITE_DOTS, HorseVariant.GRAY_WHITE_DOTS, HorseVariant.DARK_BROWN_WHITE_DOTS, HorseVariant.WHITE_BLACK_DOTS, HorseVariant.CREAMY_BLACK_DOTS, HorseVariant.CHESTNUT_BLACK_DOTS, HorseVariant.BROWN_BLACK_DOTS, HorseVariant.BLACK_BLACK_DOTS, HorseVariant.GRAY_BLACK_DOTS, HorseVariant.DARK_BROWN_BLACK_DOTS  };
    					  
    	debug("Type: "+type+" Var: "+var);
    	HorseModifier hm = HorseModifier.spawn(player.getLocation());
    	hm.setType(HorseType.fromId(type));

    	if ( type == 0 )
    		hm.setVariant(vars[var]);
    	hm.setTamed(false);
    	
    	int MaxHealth = getConfig().getInt("horses.lure.health.max");
    	int MinHealth = getConfig().getInt("horses.lure.health.min");
    	Double health = (double) (generator.nextInt(MaxHealth-MinHealth)+MinHealth);
    	
    	hm.getHorse().setMaxHealth(health);
   
    }
    
    public boolean isRider(String name, String horse) throws SQLException {
    	rs = queryDB("SELECT id FROM riders WHERE uid='"+horse+"' AND name='"+name+"'");
    	while(rs.next()) {
			return true;
		}
    	return false;
    }
    
    public boolean isOwner(String name, String horse) throws SQLException {
    	rs = queryDB("SELECT id FROM riders WHERE uid='"+horse+"' AND owner='"+name+"'");
    	while(rs.next()) {
			return true;
		}
    	return false;
    }
    private void SetupLanguage() {
    	
    	// English
    	
    	localenUS.put("ADD_HIT","Punch the horse you want to add the rider to.");
    	localenUS.put("ADD_NOARG","Who do you want to add as a rider?");
    	localenUS.put("CONFIG_ERROR","Could not save config to");
    	localenUS.put("CONFIG_RELOAD","Stables configuration reloaded.");
    	localenUS.put("CONFIG_SAVE","Horses saved.");
    	localenUS.put("DEL_HIT","Punch the horse you want to delete the rider from.");
    	localenUS.put("DEL_NOARG","Who do you want to delete as a rider?");
    	localenUS.put("HIT_FREE","You set this beast free.");
    	localenUS.put("HIT_MAX","You already own too many horses! You cannot tame this beast.");
    	localenUS.put("HIT_NEW","Enjoy your new steed!");
    	localenUS.put("HIT_REMOVE","Punch the horse you want to remove the owner from.");
    	localenUS.put("LIST_NOARG","Who do you wish to list the horses of?");
    	localenUS.put("LIST_OWNED","owns the following horses:");
    	localenUS.put("NO_CONSOLE","This command cannot be run from the console.");
    	localenUS.put("NO_PERM","You do not have permission for that.");
    	localenUS.put("NOT_OWNER","That is not even your horse!");
    	localenUS.put("PERM_NOCLEAR","That is not your horse! You cannot set it free!");
    	localenUS.put("PERM_NORIDE","You have not been given permission to ride that horse!");
    	localenUS.put("PERM_NOTHEFT","That is not your horse! That belongs to ");
    	localenUS.put("RECIPE_ADDED","Recipe added:");
    	localenUS.put("REMOVE_NOARG","Who do you wish to remove the horses of?");
    	localenUS.put("REMOVE_NOHORSE","That player owns no horses.");
    	localenUS.put("RIDER_ADD","Rider added!");
    	localenUS.put("RIDER_DEL","Rider removed.");
    	localenUS.put("RO_HIT","Punch the horse you want to remove the owner of.");
    	localenUS.put("UNKNOWN_OWNER","That owner is unknown.");
    	localenUS.put("CHECK_HIT","Punch the horse you want to check the info of.");
    	localenUS.put("LIST_NOHORSE", "That player owns no horses.");
    	
    	//German
    	localdeDE.put("ADD_HIT", "Schlag das Pferd um den Reiter hinzu zu fügen.");
    	localdeDE.put("ADD_NOARG", "Wen möchten sie als Reiter/in hinzufügen?");
    	localdeDE.put("CHECK_HIT", "Schlagen sie das Pferd um die Informationen von diesem einzusehen.");
    	localdeDE.put("CONFIG_ERROR", "Konnte die Config nicht sichern bei");
    	localdeDE.put("CONFIG_RELOAD", "Stables Konfiguration wurde erfolgreich neu geladen.");
    	localdeDE.put("CONFIG_SAVE", "Pferde wurden gesichert.");
    	localdeDE.put("DEL_HIT", "Schlag das Pferd um den Reiter zu entfernen.");
    	localdeDE.put("DEL_NOARG", "Wen möchtest du als Reiter wirklich entfernen?");
    	localdeDE.put("HIT_FREE", "Du stellst das Pferd frei.");
    	localdeDE.put("HIT_MAX", "Du besitzt schon zu viele Pferde! Du kannst dieses nicht zähmen.");
    	localdeDE.put("HIT_NEW", "Viel Spaß mit deinem neuem Pferd!");
    	localdeDE.put("HIT_REMOVE", "Schlag das Pferd um den Besitzer davon zu entfernen.");
    	localdeDE.put("LIST_NOARG", "Wem möchtest du die Pferde auflisten?");
    	localdeDE.put("LIST_NOHORSE", "Dieser Spieler besitzt keine Pferde.");
    	localdeDE.put("LIST_OWNED", "Du besitzt folgende Pferde:");
    	localdeDE.put("NO_CONSOLE", "Dieser Kommando kann nicht mit der Konsole gestartet werden.");
    	localdeDE.put("NO_PERM", "Keine_berechtigung");
    	localdeDE.put("NOT_OWNER", "Das ist nicht dein Pferd!");
    	localdeDE.put("PERM_NOCLEAR", "Das ist nicht dein Pferd! Du kannst den Besitzer/Reiter nicht entfernen!");
    	localdeDE.put("PERM_NORIDE", "Du hast keine Berechtigung um das Pferd zu Reiten.");
    	localdeDE.put("PERM_NOTHEFT", "Das ist nicht dein Pferd! Das gehört");
    	localdeDE.put("RECIPE_ADDED", "Rezept hinzugefügt:");
    	localdeDE.put("REMOVE_NOARG", "Wem möchtest du die Pferde entfernen?");
    	localdeDE.put("REMOVE_NOHORSE", "Dieser Spieler besitzt kein Pferd.");
    	localdeDE.put("RIDER_ADD", "Reiter/in hinzugefügt!");
    	localdeDE.put("RIDER_DEL", "Reiter/in entfernt.");
    	localdeDE.put("RO_HIT", "Schlag das Pferd um den Besitzer zu entfernen.");
    	localdeDE.put("UNKNOWN_OWNER", "Der Besitzer ist Unbekannt.");
    }
}

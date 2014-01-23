package com.ivalicemud.stables;
import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_6_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_6_R1.entity.CraftHanging;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;


import com.avaje.ebean.EbeanServer;


public class EventListener implements Listener, Plugin {

	Stables plugin = Stables.plugin;
	
	   public void debug(String msg )
	    {
				if ( plugin.getConfig().getBoolean("general.Debug") == true )
				{
				Bukkit.getServer().getLogger().info("Stables DEBUG: " + msg);
				}
	    }

    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event ) {
    	if ( !(event.getEntity() instanceof Horse) ) return;
    	
    	LivingEntity e = (LivingEntity) event.getEntity();
    	
    	
  		plugin.writeDB("DELETE FROM horses WHERE uid='"+ e.getUniqueId()+"'");
  		plugin.writeDB("DELETE FROM riders WHERE uid='"+ e.getUniqueId()+"'");
  		plugin.writeDB("DELETE FROM owners WHERE horse='"+ e.getUniqueId()+"'");

  		return;
    }
    
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) throws SQLException {
    	
    	if ( !(event.getRightClicked() instanceof Horse) )
    		{
    		debug("Not a horse: " + event.getRightClicked().getClass());
        	if ( (event.getRightClicked() instanceof Hanging) ) {
        	
        		debug("hanging");
        		Hanging h = (Hanging) event.getRightClicked();
        		if ( h.getType() == EntityType.PAINTING || h.getType() == EntityType.ITEM_FRAME) return;
        	
        	
        		debug(""+h.getUniqueId());
        	
    		
        		return;
        		}
        	//if (( event.getRightClicked() instanceof Fence ) ) {
        	
        	//}
    		}
    	
    	String owner = "";
    	boolean owned = false;
    		
    	LivingEntity e = (LivingEntity) event.getRightClicked();
    	debug("Horse being right clicked: " + plugin.HorseName(null,e));

    	owner = plugin.HorseOwner(e.getUniqueId().toString());
    	if ( owner != null ) owned = true;
    	
    	/*
    	
    		if ( plugin.getHorseConfig().contains("horses."+e.getUniqueId()+".owner") )
    		{
    		owner = plugin.getHorseConfig().getString("horses."+e.getUniqueId()+".owner");
    		owned = true;
    		}
    		else
    		{
    			plugin.getHorseConfig().set("horses."+e.getUniqueId(),null);
        		e.setCustomName(null);
    		}
    	
    		if ( owner.length() <= 1 )
    		{
    			plugin.getHorseConfig().set("horses."+e.getUniqueId(),null);
        		e.setCustomName(null);
    		}
    	*/
    			
    		if (!event.getPlayer().hasPermission("stables.admin") && 
    				owned == true && 
    				!(event.getPlayer().getItemInHand().getType() == Material.NAME_TAG) &&
        			!owner.equals(event.getPlayer().getName() )) //Not Owner - Can we ride it?
        	{
        		if ( !plugin.isRider(event.getPlayer().getName().toLowerCase(),e.getUniqueId().toString()) )
        			//!plugin.getHorseConfig().contains("horses."+e.getUniqueId()+".rider."+event.getPlayer().getName().toLowerCase()) )
        		{
        			event.getPlayer().sendMessage("You have not been given permission to ride that horse!");
        			event.setCancelled(true);
        			return;
        		}
        	}
    		else
    	if (!event.getPlayer().hasPermission("stables.admin") )
    		if ( owned && !!plugin.isRider(event.getPlayer().getName().toLowerCase(),e.getUniqueId().toString()) 
    				&& plugin.getConfig().getBoolean("general.Theft") == false )
    		{
    			event.getPlayer().sendMessage("That is not your horse! That belongs to " + owner + "!");
    			event.setCancelled(true);
    			return;
    		}
    	
    	
    	if ( owned == true && 
    			event.getPlayer().getItemInHand().getType() == Material.NAME_TAG &&
        		!event.getPlayer().getItemInHand().getItemMeta().hasDisplayName() ) 	
    	{
    		if ( !owner.equals(event.getPlayer().getName() ) )
    		{
    			event.getPlayer().sendMessage("That is not your horse! You cannot set it free!");
    			event.setCancelled(true);
    			return;
    		}
    		
    		event.getPlayer().sendMessage("You set this beast free.");
    		
    		plugin.writeDB("DELETE FROM owners WHERE horse='"+event.getRightClicked().getUniqueId()+"'");
    		plugin.writeDB("DELETE FROM horses WHERE uid='"+event.getRightClicked().getUniqueId()+"'");
    		plugin.writeDB("DELETE FROM riders WHERE uid='"+event.getRightClicked().getUniqueId()+"'");
   
    		e.setCustomName(null);
    		event.setCancelled(true);
    		return;
    	}
    	
    
    		
        if ( owned == false && 
        		event.getPlayer().getItemInHand().getType() == Material.NAME_TAG &&
        		event.getPlayer().getItemInHand().getItemMeta().hasDisplayName() ) 
        {
        	
      	if ( !plugin.canTame(event.getPlayer()) ) 
      	{
      		if ( !plugin.getConfig().getBoolean("horses.tame.AllowMaxNamed") == true )
      			event.setCancelled(true);
      		return;
      		
      	}
        		
       	
        	event.getPlayer().sendMessage("Enjoy your new steed!");
      		plugin.writeDB("DELETE FROM riders WHERE uid='"+event.getRightClicked().getUniqueId()+"'");
      		plugin.writeDB("INSERT INTO owners (name,horse) VALUES('"+ event.getPlayer().getName()+"', '"+event.getRightClicked().getUniqueId()+"')");
      		
      		plugin.writeDB("INSERT INTO horses (uid, owner, tamed, named, x, y, z) VALUES( '"+event.getRightClicked().getUniqueId()+"', '"+  event.getPlayer().getName()  +"', "+ System.currentTimeMillis()  +", '"+ event.getPlayer().getItemInHand().getItemMeta().getDisplayName() +"', 0, 0, 0 )");
      		
    
        	return;
        }
        
        
    }
    
    @EventHandler//(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) throws SQLException {
 	if ( !(event.getEntity() instanceof Horse)) return;
    	
    	boolean owned = false;
    	String owner = "";
    	
    	
    	LivingEntity e = (LivingEntity) event.getEntity();

      	owner = plugin.HorseOwner(e.getUniqueId().toString());
    	if ( owner != null ) owned = true;
    		
    	
    	if ( owned == false ) return;
    	debug("Horse beign damaged:" + plugin.HorseName(null,e));
    	DamageCause cause = event.getCause();
    	Entity damager = null;
    	

    	if (cause.equals(DamageCause.DROWNING) ||
    			cause.equals(DamageCause.BLOCK_EXPLOSION) ||
    			cause.equals(DamageCause.CONTACT) ||
    			cause.equals(DamageCause.FALL) ||
    			cause.equals(DamageCause.FALLING_BLOCK) ||
    			cause.equals(DamageCause.FIRE) ||
    			cause.equals(DamageCause.FIRE_TICK) ||
    			cause.equals(DamageCause.LAVA) ||
    			cause.equals(DamageCause.SUFFOCATION)   			     			) 
    	{
    		if ( plugin.getConfig().getBoolean("general.EnvironmentDamage") )
        	{
        		debug("Ennviroment onEntityDamage: Cancelled");
        		event.setCancelled(true);
        		return;
        	}	
    	}
    	else if (cause.equals(DamageCause.CUSTOM)){//PROJECTILE)) {
   
   
				if (damager instanceof Player) {
					
			    	 Player p = (Player) damager;
			    	 debug("Player damage from "+ p.getName() );
			    	 debug("Owner is "+ owner);
			    	 
			    	if ( plugin.getConfig().getBoolean("general.PVPDamage") && !owner.equals(p.getName()) )
			    	{
			    		debug("Fireball onEntityDamage: Cancelled PVP");
			    		event.setCancelled(true);
			    		return;
			    	}
			    	else if ( plugin.getConfig().getBoolean("general.OwnerDamage") && owner.equals(p.getName()) )
			    	{
			    		debug("Fireball onEntityDamage: Cancelled Owner");
			    		event.setCancelled(true);
			    		return;
			    		
			    	}
			    
					
				} else  {
					if ( plugin.getConfig().getBoolean("general.MobDamage") )
						debug("Fireball onEntityDamage: Cancelled Mob");
						event.setCancelled(true);
				}
					
		 
    	}
			else if (cause.equals(DamageCause.ENTITY_ATTACK))
    	{ //Handled elsewhere
				debug("Entity Damage");
        		return;
        	
    	}
    	
    	
    	
    }
    

    @EventHandler//(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) throws SQLException {
    	
    	DamageCause cause = event.getCause();
    	
    	if ( !(event.getEntity() instanceof Horse)) return;
    	
    	
  		Horse t = (Horse) event.getEntity();
  	 	String owner = "";
    	boolean owned = false;
    	
    	LivingEntity e = (LivingEntity) event.getEntity();
    	
    	
    	if ( event.getEntity() instanceof Sheep  ||
    			event.getEntity() instanceof Cow  ||
    			event.getEntity() instanceof MushroomCow  ||
    			event.getEntity() instanceof Pig  ||
    			event.getEntity() instanceof Wolf  ||
    			event.getEntity() instanceof Bat  ||
       			event.getEntity() instanceof Chicken  ) return;
      	
    	
    		owner = plugin.HorseOwner(e.getUniqueId().toString());
    		if ( owner != null ) owned = true;
    	
    		if ( (event.getDamager() instanceof Player) && 
        			event.getDamager().hasMetadata("stables.checkinfo"))    	
        	{
    			Player p = (Player) event.getDamager();
    			p.sendMessage("UID: "+e.getUniqueId());
    			p.sendMessage("That horse is owned by "+owner);
    			p.removeMetadata("stables.checkinfo", plugin);
        		event.setCancelled(true);
        		return;
        	}
  		
    	if ( (event.getDamager() instanceof Player) && 
    			event.getDamager().hasMetadata("stables.removeowner"))    	
    	{
    		Player p = (Player) event.getDamager();
    	
    		if ( owned == false ) { 
    			p.sendMessage("That horse is not yet owned."); 
    		}
    		else
    		{
    			p.sendMessage("That horse no longer has an owner.");
    			
    			plugin.writeDB("DELETE FROM owners WHERE horse='"+t.getUniqueId()+"'");
           		plugin.writeDB("DELETE FROM horses WHERE uid='"+t.getUniqueId()+"'");
           		plugin.writeDB("DELETE FROM riders WHERE uid='"+t.getUniqueId()+"'");
           		
        	
        		e.setCustomName(null);
    		}

    		p.removeMetadata("stables.removeowner", plugin);
    		event.setCancelled(true);
    		return;
    	}
    	
    	if ( owned == false ) return;
    	
    	if ( (event.getDamager() instanceof Player) && 
      			event.getDamager().hasMetadata("stables.addrider"))    	
      	{
      		Player p = (Player) event.getDamager();
      	
      		if ( !owner.equals(p.getName()) )
      		{
      			p.sendMessage("That is not even your horse!");
      			event.setCancelled(true);
      			return;
      		}
      		String name = event.getDamager().getMetadata("stables.addrider").get(0).asString();
      		plugin.writeDB("INSERT INTO riders (uid, name, owner) VALUES('"+e.getUniqueId()+"','"+name+"','"+p.getName()+"')");
   			p.sendMessage("Rider added!");
      		p.removeMetadata("stables.addrider", plugin);
      		event.setCancelled(true);
      		return;
      	}

      	
      	if ( (event.getDamager() instanceof Player) && 
      			event.getDamager().hasMetadata("stables.delrider"))    	
      	{
      		Player p = (Player) event.getDamager();
      	
      		if ( !owner.equals(p.getName()) )
      		{
      			p.sendMessage("That is not even your horse!");
      			event.setCancelled(true);
      			return;
      		}
      		String name = event.getDamager().getMetadata("stables.delrider").get(0).asString();
      		plugin.writeDB("DELETE FROM riders WHERE uid='"+t.getUniqueId()+"' AND name='"+name+"'");
      		p.sendMessage("Rider removed!");
      		p.removeMetadata("stables.delrider", plugin);
      		event.setCancelled(true);
      		return;
      	}
      	
    	debug("Horse being damaged:" + plugin.HorseName(null,e));
    	
    	if ( plugin.getConfig().getBoolean("general.BlockAll") )
    	{
    		debug("Cancelling damage - Blocking All Damage");
    		event.setCancelled(true);
    		return;
    	}
    	
    	else if (cause.equals(DamageCause.PROJECTILE)) {
    		debug("Projectile Damage");
  		  Entity damager = event.getDamager();
  		   debug("Damager: " + damager);
  		  if (damager instanceof Fireball) {
  				Fireball arrow = (Fireball) damager;
  				if (arrow.getShooter() instanceof Player) {
  				
		     Player p = (Player) arrow.getShooter();		    
		    	if ( plugin.getConfig().getBoolean("general.PVPDamage") && !owner.equals(p.getName()) )
		    	{
		    		debug("Fireball onEntityDamage: Cancelled PVP");
		    		event.setCancelled(true);
		    		return;
		    	}
		    	else if ( plugin.getConfig().getBoolean("general.OwnerDamage") && owner.equals(p.getName()) )
		    	{
		    		debug("Fireball onEntityDamage: Cancelled Owner");
		    		event.setCancelled(true);
		    		return;
		    		
		    	}
		    } else  {
				if ( plugin.getConfig().getBoolean("general.MobDamage") )
					debug("Fireball onEntityDamage: Cancelled Mob");
					event.setCancelled(true);
			}
				
    		  }
  		  
  		  if (damager instanceof Arrow) {
  			  debug("Arrow info!");
  				Arrow arrow = (Arrow) damager;
  			if (arrow.getShooter() instanceof Player) {
  				debug("Player shot arrow");
  				Player p = (Player) arrow.getShooter();		 
  				
		    	if ( plugin.getConfig().getBoolean("general.PVPDamage") && !owner.equals(p.getName()) )
		    	{
		    		debug("Fireball onEntityDamage: Cancelled PVP");
		    		event.setCancelled(true);
		    		return;
		    	}
		    	else if ( plugin.getConfig().getBoolean("general.OwnerDamage") && owner.equals(p.getName()) )
		    	{
		    		debug("Fireball onEntityDamage: Cancelled Owner");
		    		event.setCancelled(true);
		    		return;
		    		
		    	}
		    } else  {
				if ( plugin.getConfig().getBoolean("general.MobDamage") )
					debug("Fireball onEntityDamage: Cancelled Mob");
					event.setCancelled(true);
			}
				
    		  }
	}
  	

    	else if ( plugin.getConfig().getBoolean("general.MobDamage") &&	!(event.getDamager() instanceof Player) )
    	{
    		debug("Cancelling damage - Mob");
    		event.setCancelled(true);
    		return;
    	}	
    	
    	else if ( event.getDamager() instanceof Player )
    	{
    	 Player p = (Player) event.getDamager();
    	
    	 
    	if ( plugin.getConfig().getBoolean("general.PVPDamage") && !owner.equals(p.getName()) )
    	{
    		debug("Cancelling damage - PVP");
    		event.setCancelled(true);
    		return;
    	}
    	
    	
    	else if ( plugin.getConfig().getBoolean("general.OwnerDamage") && owner.equals(p.getName()) )
    	{
    		debug("Cancelling damage - Owner Damage");
    		event.setCancelled(true);
    		return;
    		
    	}
    	}
    	
    	debug("No damage issues - letting damage go through. "+event.getDamage());
    	event.setCancelled(false);

   
    }
   
    
    @EventHandler
    public void onPlayerInteractBlock(PlayerInteractEvent event){
    	
    	int itemId = event.getPlayer().getItemInHand().getTypeId();
    	
    	if ( event.getPlayer().isSneaking() == false ) {
        	//	player.sendMessage("You're not sneaking.");
        		return;
        	}
    	
    	if ( itemId != plugin.getConfig().getInt("horses.lure.item") )
    		return;

    	if ( event.getPlayer().hasMetadata("stables.luring"))
    	{
    		event.getPlayer().sendMessage("Shh! You're already trying to lure out a horse!");
    		return;
    	}
    	int amt = event.getPlayer().getItemInHand().getAmount() - 1;
    	event.getPlayer().getItemInHand().setAmount(amt);
    	debug("Amt remainnig: "+amt);
    	
    	if ( amt == 0 )
    	{
    	ItemStack itemstack = new ItemStack(  plugin.getConfig().getInt("horses.lure.item") );
    	event.getPlayer().getInventory().removeItem(itemstack);//.remove(itemstack);
    	debug("Removing item stack...");
    	}
    	
    	event.getPlayer().sendMessage("You begin trying to lure out a horse ...");
    	
    	int stime = plugin.getConfig().getInt("horses.lure.delay") * 20;
    	
    	event.getPlayer().setMetadata("stables.luring", new FixedMetadataValue(plugin,true));
    	
    	debug("Trying ot lure .. scheduling task for " +stime +" ticks");
    	final String name = event.getPlayer().getName();
    	
    	Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
    	    public void run() {
    	    	debug("Running scheduled task ...");
    	    	plugin.lureHorse( name );
    	    }}, stime);
    }
  

    
    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event){
    	debug(""+event.getRecipe());
    	
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event ) {
    	
    	
    	

    
    }
    
    
    
    
    
    
    
    
	@Override
	public List<String> onTabComplete(CommandSender arg0, Command arg1,
			String arg2, String[] arg3) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2,
			String[] arg3) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public FileConfiguration getConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getDataFolder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EbeanServer getDatabase() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChunkGenerator getDefaultWorldGenerator(String arg0, String arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PluginDescriptionFile getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Logger getLogger() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PluginLoader getPluginLoader() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getResource(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Server getServer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isNaggable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onDisable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onEnable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLoad() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reloadConfig() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveConfig() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveDefaultConfig() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveResource(String arg0, boolean arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setNaggable(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	
	
}

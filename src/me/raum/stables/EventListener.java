package me.raum.stables;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
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
	   public void onVehicleExit(VehicleExitEvent event)
	    {
	        if(!plugin.isHorse(event.getVehicle()))
	            return;
	        
		Horse h = (Horse) event.getVehicle();

		if (plugin.HorseOwner(h.getUniqueId().toString()) != null) {
			plugin.saveLocation( h );
		} else if (h.isTamed() && (event.getExited() instanceof Player))
	        {
	            Player p = (Player)event.getExited();
	            String name = plugin.HorseName(h.getUniqueId().toString(), null);
	            if ( plugin.getConfig().getBoolean("horses.AutoOwn") == true )
	            {
	            	
	                plugin.addHorse( p, (Entity) h, true);
	                	return;
                        
	            }
	            else
	            if(name.equalsIgnoreCase("Unknown") && plugin.getConfig().getBoolean("general.showUnownedWarning"))
	            {
	                plugin.local(p,"EXIT_NOT_TAME");
	                return;
	            }
	        }
	    }
	    
	    @EventHandler
	    public void onUnleash(EntityUnleashEvent event)
	    {
	    	debug("Unleash: " + event.getEntity().toString() + " - " + event.getReason());
	    }
	    
	    @EventHandler
	    public void onEntityDeath(EntityDeathEvent event)
	    {
	        if(!plugin.isHorse(event.getEntity()))
	        {
	            return;
	        } else
	        {
	            LivingEntity e = event.getEntity();
	            plugin.removeHorse(e.getUniqueId().toString());
	            return;
	        }
	    }
    
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
    {
 
        if(!plugin.isHorse(event.getRightClicked()))
        {
            debug((new StringBuilder("Not a horse: ")).append(event.getRightClicked().getClass()).toString());
            if(event.getRightClicked() instanceof Hanging)
            {
                debug("hanging");
                Hanging h = (Hanging)event.getRightClicked();
                if(h.getType() == EntityType.PAINTING || h.getType() == EntityType.ITEM_FRAME)
                {
                    return;
                } else
                {
                    debug((new StringBuilder()).append(h.getUniqueId()).toString());
                    return;
                }
            } else
            {
                return;
            }
        }
        String owner = "";
        boolean owned = false;
        LivingEntity e = (LivingEntity)event.getRightClicked();
        debug((new StringBuilder("Horse being right clicked: ")).append(plugin.HorseName(null, e)).toString());
        if(!event.getPlayer().hasPermission("stables.admin") && event.getPlayer().hasPermission("stables.punish.breed") && (event.getPlayer().getItemInHand().getTypeId() == 322 || event.getPlayer().getItemInHand().getTypeId() == 396))
        {
            plugin.local(event.getPlayer(),"PUNISH_BREED");
            event.setCancelled(true);
            return;
        }
        owner = plugin.HorseOwner(e.getUniqueId().toString());
        if(owner != null)
            owned = true;
        if(owned && event.getPlayer().getItemInHand().getType() != Material.NAME_TAG && !owner.equals(event.getPlayer().getName()) && !plugin.canRide(e, event.getPlayer()))
        {
            event.getPlayer().sendMessage(owner + " " + plugin.getLang("NOT_RIDER"));
            event.setCancelled(true);
            return;
        }
        if(owned && event.getPlayer().getItemInHand().getType() == Material.NAME_TAG && !event.getPlayer().getItemInHand().getItemMeta().hasDisplayName())
            if(!owner.equals(event.getPlayer().getName()))
            {
                plugin.local(event.getPlayer(),"NOT_OWNER");
                event.setCancelled(true);
                return;
            } else
            {
                plugin.local(event.getPlayer(),"SET_FREE");
                plugin.removeHorse(e.getUniqueId().toString());
                e.setCustomName(null);
                event.setCancelled(true);
                return;
            }
        if(owned && event.getPlayer().getItemInHand().getType() == Material.NAME_TAG && event.getPlayer().getItemInHand().getItemMeta().hasDisplayName() && !owner.equals(event.getPlayer().getName()))
        {
            plugin.local(event.getPlayer(),"NOT_OWNER");
            event.setCancelled(true);
            return;
        }

        if(owned && event.getPlayer().getItemInHand().getType() == Material.NAME_TAG && event.getPlayer().getItemInHand().getItemMeta().hasDisplayName() && owner.equals(event.getPlayer().getName()))
        {
    
        	
        	String horseName = event.getPlayer().getItemInHand().getItemMeta().getDisplayName().replace("'", "`");
        	
        	plugin.nameHorse(event.getRightClicked().getUniqueId().toString(),horseName);
            return;
        }

        if(!owned && event.getPlayer().getItemInHand().getType() == Material.NAME_TAG && event.getPlayer().getItemInHand().getItemMeta().hasDisplayName())
        {
            String name = event.getPlayer().getItemInHand().getItemMeta().getDisplayName();
            if(name.contains(";") || name.contains("\\"))
            {
                event.getPlayer().sendMessage("That is an invalid name tag. Please rename the tag before trying to claim this horse.");
                event.setCancelled(true);
                return;
            }
            if(event.getPlayer().hasPermission("stables.punish.name") && !event.getPlayer().hasPermission("stables.admin"))
            {
                plugin.local(event.getPlayer(),"PUNISH_NAME");
                event.setCancelled(true);
                return;
            }
       
           	if ( event.isCancelled() == true )
        	{
        		debug("Event cancelled outside of Stables .. Cancelling here.");
        		return;
        	}
           	
            if (!plugin.addHorse(event.getPlayer(),event.getRightClicked(), false) )
                event.setCancelled(true);
                
            return;
        } else
        {
            return;
        }
    }
    
    
    @EventHandler
    public void onSplash( PotionSplashEvent event )
    {
    	if ( event.getPotion() == null || event.getPotion().getShooter() == null )
    		return;
    	
    	LivingEntity l = event.getPotion().getShooter();
    	Player p = null ;
    	boolean found = false;
    	
    	if ( l instanceof Player && !plugin.getConfig().getBoolean("general.PVPDamage"))
    		return;

    	if ( !(l instanceof Player) && !plugin.getConfig().getBoolean("general.MobDamage"))
    		return;

    	if ( l instanceof Player)
    		p = (Player) l;
    	
    	 
    	for ( LivingEntity a : event.getAffectedEntities() )
    	{
    		String owner = null;
    		if ( !(a instanceof Horse) ) 
    			continue;
    		owner = plugin.HorseOwner(a.getUniqueId().toString());
    		
   		 if ( plugin.getConfig().getBoolean("general.ProtectUnclaimed") == true && owner == null )
    			 owner = "unclaimed";
    		 
    		if ( owner == null )
    			continue;
    		
    		debug("Named horses ... checking splash info ...");
    		
    		if ( p == null && plugin.getConfig().getBoolean("general.MobDamage") )
    		{
    			debug("Blocking from mob damage");
    			found = true;
    		}

    		if ( p != null && plugin.getConfig().getBoolean("general.PVPDamage")  )
    		{
    			if ( !owner.equals(p.getName()) )
    			{
    			debug("Blocking from pvp damage");
    			found = true;
    			}
    		}

    		if ( p != null && owner.equals(p.getName()) && plugin.getConfig().getBoolean("general.OwnerDamage")  )
    		{
    			debug("Blocking from owner damage");
    			found = true;
    		}

    		
    		if ( found ) 
    			break;
    		
    		continue;
    	}
    	
    	if ( found )
    	{
    	debug("Blocking damage ...");
    	event.setCancelled(true);
    	}
    }
    
    @EventHandler//(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event)
    {
        if(!(event.getEntity() instanceof Horse))
            return;
        boolean owned = false;
        String owner = "";
        LivingEntity e = (LivingEntity)event.getEntity();
        owner = plugin.HorseOwner(e.getUniqueId().toString());
        
  		 if ( plugin.getConfig().getBoolean("general.ProtectUnclaimed") == true && owner == null )
			 owner = "unclaimed";

        if(owner != null)
            owned = true;
        if(!owned)
            return;
        debug((new StringBuilder("Horse beign damaged:")).append(plugin.HorseName(null, e)).toString());
        org.bukkit.event.entity.EntityDamageEvent.DamageCause cause = event.getCause();
        Entity damager = null;
        
       if(cause.equals(org.bukkit.event.entity.EntityDamageEvent.DamageCause.DROWNING) || cause.equals(org.bukkit.event.entity.EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) || cause.equals(org.bukkit.event.entity.EntityDamageEvent.DamageCause.CONTACT) || cause.equals(org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL) || cause.equals(org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALLING_BLOCK) || cause.equals(org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE) || cause.equals(org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE_TICK) || cause.equals(org.bukkit.event.entity.EntityDamageEvent.DamageCause.LAVA) || cause.equals(org.bukkit.event.entity.EntityDamageEvent.DamageCause.SUFFOCATION))
        {
            if(plugin.getConfig().getBoolean("general.EnvironmentDamage"))
            {
                debug("Ennviroment onEntityDamage: Cancelled");
                event.setCancelled(true);
                return;
            }
        } else
        if(cause.equals(org.bukkit.event.entity.EntityDamageEvent.DamageCause.CUSTOM))
        {
            if(damager instanceof Player)
            {
                Player p = (Player)damager;
                debug((new StringBuilder("Player damage from ")).append(p.getName()).toString());
                debug((new StringBuilder("Owner is ")).append(owner).toString());
                if(plugin.getConfig().getBoolean("general.PVPDamage") && !owner.equals(p.getName()))
                {
                    debug("Fireball onEntityDamage: Cancelled PVP");
                    event.setCancelled(true);
                    return;
                }
                if(plugin.getConfig().getBoolean("general.OwnerDamage") && owner.equals(p.getName()))
                {
                    debug("Fireball onEntityDamage: Cancelled Owner");
                    event.setCancelled(true);
                    return;
                }
            } else
            {
                if(plugin.getConfig().getBoolean("general.MobDamage"))
                    debug("Fireball onEntityDamage: Cancelled Mob");
                event.setCancelled(true);
            }
        } else
        if(cause.equals(org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK))
        {
            debug("Entity Damage");
            return;
        }
    }

    @EventHandler//(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
    {
        if(!(event.getEntity() instanceof Horse))
            return;
        org.bukkit.event.entity.EntityDamageEvent.DamageCause cause = event.getCause();
        Horse t = (Horse)event.getEntity();
        String owner = "";
        boolean owned = false;
        LivingEntity e = (LivingEntity)event.getEntity();
        owner = plugin.HorseOwner(e.getUniqueId().toString());
        
  		 if ( plugin.getConfig().getBoolean("general.ProtectUnclaimed") == true && owner == null )
			 owner = "unclaimed";

  		 
        if(owner != null)
            owned = true;
        if((event.getDamager() instanceof Player) && event.getDamager().hasMetadata("stables.checkinfo"))
        {
            Player p = (Player)event.getDamager();
            event.setCancelled(true);
            p.removeMetadata("stables.checkinfo", plugin);
            p.sendMessage((new StringBuilder("UID: ")).append(e.getUniqueId()).toString());
            p.sendMessage((new StringBuilder("That horse is owned by ")).append(owner).toString());
            p.sendMessage((new StringBuilder("Jump Strength:")).append(t.getJumpStrength()).toString());
            p.sendMessage((new StringBuilder("Health: ")).append(t.getHealth()).append("/").append(t.getMaxHealth()).toString());
            return;
        }
        if((event.getDamager() instanceof Player) && event.getDamager().hasMetadata("stables.removeowner"))
        {
            Player p = (Player)event.getDamager();
            if(!owned)
            {
                p.sendMessage("That horse is not yet owned.");
            } else
            {
                p.sendMessage("That horse no longer has an owner.");
                plugin.removeHorse(e.getUniqueId().toString());
                e.setCustomName(null);
            }
            p.removeMetadata("stables.removeowner", plugin);
            event.setCancelled(true);
            return;
        }
        if(!owned)
            return;
        if((event.getDamager() instanceof Player) && event.getDamager().hasMetadata("stables.addrider"))
        {
            Player p = (Player)event.getDamager();
            if(!owner.equals(p.getName()) && !plugin.perm(p,"stables.addrider"))
            {
            	plugin.local(p, "NOT_OWNER");
                event.setCancelled(true);
                return;
            }
            String name = ((MetadataValue)event.getDamager().getMetadata("stables.addrider").get(0)).asString();
            if(Stables.flatfile)
                plugin.getHorseConfig().set((new StringBuilder("riders.")).append(e.getUniqueId()).append(".").append(name).toString(), Boolean.valueOf(true));
            else
                plugin.writeDB((new StringBuilder("INSERT INTO ")).append(plugin.getConfig().getString("MySQL.prefix")).append("riders (uid, name, owner) VALUES('").append(e.getUniqueId()).append("','").append(name).append("','").append(p.getName()).append("')").toString());
            plugin.local(p, "RIDER_ADD");
            p.removeMetadata("stables.addrider", plugin);
            event.setCancelled(true);
            return;
        }
        if((event.getDamager() instanceof Player) && event.getDamager().hasMetadata("stables.removechest"))
        {
            Player p = (Player)event.getDamager();
            if(!owner.equals(p.getName()))
            {
            	plugin.local(p, "NOT_OWNER");
                event.setCancelled(true);
                return;
            } else
            {
                plugin.local(p,"REMOVE_CHEST");
                p.removeMetadata("stables.removechest", plugin);
                HorseModifier hm = new HorseModifier(t);
                hm.setChested(false);
                event.setCancelled(true);
                return;
            }
        }
        if((event.getDamager() instanceof Player) && event.getDamager().hasMetadata("stables.store"))
        {
            Player p = (Player)event.getDamager();
            
            if(!owner.equals(p.getName()) && !plugin.perm(p,"stables.store.others"))
            {
            	plugin.local(p, "NOT_OWNER");
                event.setCancelled(true);
                return;
            }
            HorseModifier hm = new HorseModifier(e);
            if(hm.isChested())
            {
                plugin.local(p,"NO_CHESTS");
                event.setCancelled(true);
                return;
            } else
            {
                plugin.stableHorse(e, p.getName());
                p.removeMetadata("stables.store", plugin);
                event.setCancelled(true);
                return;
            }
        }
        if((event.getDamager() instanceof Player) && event.getDamager().hasMetadata("stables.delrider"))
        {
            Player p = (Player)event.getDamager();
            if(!owner.equals(p.getName()))
            {
            	plugin.local(p, "NOT_OWNER");
                event.setCancelled(true);
                return;
            }
            String name = ((MetadataValue)event.getDamager().getMetadata("stables.delrider").get(0)).asString();
            if(Stables.flatfile)
                plugin.getHorseConfig().set((new StringBuilder("riders.")).append(e.getUniqueId()).append(".").append(name).toString(), null);
            else
                plugin.writeDB((new StringBuilder("DELETE FROM ")).append(plugin.getConfig().getString("MySQL.prefix")).append("riders WHERE uid='").append(t.getUniqueId()).append("' AND name='").append(name).append("'").toString());
            plugin.local(p, "RIDER_DEL");

            p.removeMetadata("stables.delrider", plugin);
            event.setCancelled(true);
            return;
        }
        debug((new StringBuilder("Horse being damaged:")).append(plugin.HorseName(null, e)).toString());
        if(plugin.getConfig().getBoolean("general.BlockAll"))
        {
            debug("Cancelling damage - Blocking All Damage");
            event.setCancelled(true);
            return;
        }
     
        if(cause.equals(org.bukkit.event.entity.EntityDamageEvent.DamageCause.PROJECTILE))
        {
            debug("Projectile Damage");
            Entity damager = event.getDamager();
            debug((new StringBuilder("Damager: ")).append(damager).toString());
            if(damager instanceof Fireball)
            {
                Fireball arrow = (Fireball)damager;
                if(arrow.getShooter() instanceof Player)
                {
                    Player p = (Player)arrow.getShooter();
                    if(plugin.getConfig().getBoolean("general.PVPDamage") && !owner.equals(p.getName()))
                    {
                        debug("Fireball onEntityDamage: Cancelled PVP");
                        event.setCancelled(true);
                        return;
                    }
                    if(plugin.getConfig().getBoolean("general.OwnerDamage") && owner.equals(p.getName()))
                    {
                        debug("Fireball onEntityDamage: Cancelled Owner");
                        event.setCancelled(true);
                        return;
                    }
                } else
                {
                    if(plugin.getConfig().getBoolean("general.MobDamage"))
                        debug("Fireball onEntityDamage: Cancelled Mob");
                    event.setCancelled(true);
                }
            }
            if(damager instanceof Arrow)
            {
                debug("Arrow info!");
                Arrow arrow = (Arrow)damager;
                if(arrow.getShooter() instanceof Player)
                {
                    debug("Player shot arrow");
                    Player p = (Player)arrow.getShooter();
                    if(plugin.getConfig().getBoolean("general.PVPDamage") && !owner.equals(p.getName()))
                    {
                        debug("Fireball onEntityDamage: Cancelled PVP");
                        event.setCancelled(true);
                        return;
                    }
                    if(plugin.getConfig().getBoolean("general.OwnerDamage") && owner.equals(p.getName()))
                    {
                        debug("Fireball onEntityDamage: Cancelled Owner");
                        event.setCancelled(true);
                        return;
                    }
                } else
                {
                    if(plugin.getConfig().getBoolean("general.MobDamage"))
                        debug("Fireball onEntityDamage: Cancelled Mob");
                    event.setCancelled(true);
                }
            }
        } else
        {
            if(plugin.getConfig().getBoolean("general.MobDamage") && !(event.getDamager() instanceof Player))
            {
                debug("Cancelling damage - Mob");
                event.setCancelled(true);
                return;
            }
            if(event.getDamager() instanceof Player)
            {
                Player p = (Player)event.getDamager();
                if(plugin.getConfig().getBoolean("general.PVPDamage") && !owner.equals(p.getName()))
                {
                    debug("Cancelling damage - PVP");
                    event.setCancelled(true);
                    return;
                }
                if(plugin.getConfig().getBoolean("general.OwnerDamage") && owner.equals(p.getName()))
                {
                    debug("Cancelling damage - Owner Damage");
                    event.setCancelled(true);
                    return;
                }
            }
        }
        debug((new StringBuilder("No damage issues - letting damage go through. ")).append(event.getDamage()).toString());
        event.setCancelled(false);
    }

    
    @EventHandler
    public void onPlayerInteractBlock(PlayerInteractEvent event)
    {
    	
    	if ( plugin.getConfig().getBoolean("horses.lure.allow") == false )
    		return;
    	
        if(event.getPlayer().hasMetadata("stables.luring"))
        {
           plugin.local( event.getPlayer(),"ALREADY_LURE");
            return;
        }
        
        if(!event.getPlayer().isSneaking())
            return;
        
        int itemId = event.getPlayer().getItemInHand().getTypeId();
   
        if( !plugin.getConfig().contains("lure."+itemId+".chance"))  
            return;
    
        int amt = event.getPlayer().getItemInHand().getAmount() - 1;
        event.getPlayer().getItemInHand().setAmount(amt);
        debug((new StringBuilder("Amt remainnig: ")).append(amt).toString());
        if(amt == 0)
        {
            ItemStack itemstack = new ItemStack(plugin.getConfig().getInt("horses.lure.item"));
            event.getPlayer().getInventory().removeItem(new ItemStack[] {
                itemstack
            });
            debug("Removing item stack...");
        }
        
        plugin.local(event.getPlayer(),"START_LURE");
        int stime = plugin.getConfig().getInt("horses.lure.delay") * 20;
        event.getPlayer().setMetadata("stables.luring", new FixedMetadataValue(plugin, itemId));
        final String name = event.getPlayer().getName();

    	Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
    	    public void run() {
    	    	plugin.lureHorse( name );
    	    }}, stime);
    }
  

    
    @EventHandler
    public void onCraftItem(CraftItemEvent event)
    {
        if(!plugin.getConfig().getBoolean("recipe.usePerms"))
            return;
        int id = event.getRecipe().getResult().getType().getId();
        String perm = "";
        Player p = (Player)event.getView().getPlayer();
        switch(id)
        {
        default:
            return;

        case 421: 
            perm = "stables.recipe.nametag";
            break;

        case 417: 
            perm = "stables.recipe.armor.iron";
            break;

        case 418: 
            perm = "stables.recipe.armor.gold";
            break;

        case 419: 
            perm = "stables.recipe.armor.diamond";
            break;

        case 329: 
            perm = "stables.recipe.saddle";
            break;
        }
        debug((new StringBuilder("Checking perm ")).append(perm).toString());
        if(!plugin.perm(p, perm))
        {
           plugin.local(p,"RECIPE_PERM");
            event.setCancelled(true);
            return;
        } else
        {
            debug("Pass! Crafting");
            return;
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if(event.getClickedBlock() == null)
            return;
        Player p = event.getPlayer();
        if(event.getClickedBlock().getType() == Material.WALL_SIGN || event.getClickedBlock().getType() == Material.SIGN_POST)
        {
            debug("Checking signs!");
            Sign sign = (Sign)event.getClickedBlock().getState();
            String stablesign = ChatColor.stripColor(sign.getLine(0));
            if(stablesign.equals("[Stables]"))
                plugin.commandStore(p);
            return;
        } else
        {
            return;
        }
    }
    
    @EventHandler
    public void onSignChange(SignChangeEvent event)
    {
        String stableSign = event.getLine(0);
        if(!stableSign.equalsIgnoreCase("[stables]"))
            return;
        Block block = event.getBlock();
        Player p = event.getPlayer();
        boolean typeWallSign = block.getTypeId() == Material.WALL_SIGN.getId();
        boolean typeSignPost = block.getTypeId() == Material.SIGN_POST.getId();
        if(typeWallSign || typeSignPost)
        {
            Sign sign = (Sign)block.getState();
            if(!plugin.perm(p, "stables.sign"))
            {
                p.sendMessage(plugin.getLang("NO_PERM"));
                event.setLine(0, p.getName());
                event.setLine(1, "is");
                event.setLine(2, "naughty.");
                event.setLine(3, "");
                sign.update(true);
                return;
            } else
            {
                p.sendMessage("Stables created!");
                event.setLine(0, "&9[&0Stables&9]");
                event.setLine(0, event.getLine(0).replaceAll("&([0-9A-Fa-f])", "\247$1"));
                event.setLine(1, "");
                event.setLine(2, "Use /recover");
                event.setLine(3, "to retrieve!");
                sign.update(true);
                return;
            }
        } else
        {
            return;
        }
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

package me.HAklowner.SecureChests;


import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

public class SecureChestsPlayerListener extends PlayerListener{

	public SecureChests plugin;

	public SecureChestsPlayerListener(SecureChests instance) {
		plugin = instance;
	}
	
	public void onPlayerInteract(PlayerInteractEvent event) {
		
		//make sure we are dealing with a block and not clicking on air or an entity
        if (!(event.getAction().equals(Action.LEFT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)))
            return;
        
        Boolean dchest = false;
		Block b = event.getClickedBlock();
		if(b.getTypeId() == 54) { //make sure block click is a chest.
			Player player = event.getPlayer();
			Location chestloc = b.getLocation();
			
			Location ccN = b.getLocation();
			Location ccE = b.getLocation();
			Location ccS = b.getLocation();
			Location ccW = b.getLocation();
			
			ccN = ccN.subtract(0,0,1);
			ccE = ccE.subtract(1,0,0);
			ccS = ccS.add(0,0,1);
			ccW = ccW.add(1,0,0);
			
			if (ccN.getBlock().getTypeId() == 54) {
				chestloc = chestloc.subtract(0, 0, 1);
				dchest = true;
			} else if (ccE.getBlock().getTypeId() == 54) {
				chestloc = chestloc.subtract(1, 0, 0);
				dchest = true;
			} else if (ccS.getBlock().getTypeId() == 54) {
				dchest = true;
			} else if (ccW.getBlock().getTypeId() == 54) {
				dchest = true;
			}
			
			//create the YAML string location
			String yamlloc = chestloc.getWorld().getName() + "." + chestloc.getBlockX() + "_" + chestloc.getBlockY() + "_" + chestloc.getBlockZ();
			
			//get name of chest owner
			String lockname = plugin.getStorageConfig().getString(yamlloc.concat(".owner"));
			
			//get the current /sc command status:
			// 0/null=none
			// 1= lock
			// 2= unlock
			// 3= add to chest access list
			// 4= remove from chest access list
			
			Integer cmdstatus = plugin.scCmd.get(player);
			if (cmdstatus == null)
				cmdstatus = 0;
			
			//is if it is owned or not
			if(lockname == null) {
				//if not owned and your in /lock mode
				if (cmdstatus == 1) {
					if (dchest)
						player.sendMessage("[Secure Chests] Double Chest locked.");
					else
						player.sendMessage("[Secure Chests] Single Chest locked.");
					plugin.getStorageConfig().set(yamlloc.concat(".owner"),player.getName());
					plugin.scCmd.remove(player);
					plugin.saveStorageConfig();
				}
			} else {
			//It is owned by someone check if it is yours!
				if (player.getName().equals(lockname)) { // it's yours!
					if(cmdstatus == 2) {//check to see if they want to unlock this chest.
						plugin.getStorageConfig().set(yamlloc, null);
						plugin.scCmd.remove(player);
						player.sendMessage("[Secure Chests] chest unlocked");
						plugin.saveStorageConfig();
						
					} else if(cmdstatus == 3) { //check to see if they want to add a name to the access list.
						plugin.scCmd.remove(player);
						String checkName = plugin.scAList.get(player);
						if (!plugin.getStorageConfig().getBoolean(yamlloc+".access."+checkName)){
							player.sendMessage("[Secure Chests] Adding "+checkName+" to access list.");
							plugin.getStorageConfig().set(yamlloc+".access."+checkName, true);
							plugin.saveStorageConfig();
							plugin.scAList.remove(player);
						} else {
							player.sendMessage("[Secure Chests] Player "+checkName+" already in access list.");
						}
					} else if(cmdstatus == 4) { //They want to Remove a name from the access list
						plugin.scCmd.remove(player);
						String checkName = plugin.scAList.get(player);
						if (!plugin.getStorageConfig().getBoolean(yamlloc+".access."+checkName)){
							player.sendMessage("[Secure Chests] Player "+checkName+" Not found in list.");
						} else {
							player.sendMessage("[Secure Chests] Player "+checkName+" Removed from list.");
							plugin.getStorageConfig().set(yamlloc+".access."+checkName, null);
							plugin.saveStorageConfig();
							plugin.scAList.remove(player);
						}
					} else if(cmdstatus == 5) { //check to see if they want to add a name to the deny list.
						plugin.scCmd.remove(player);
						String checkName = plugin.scAList.get(player);
						if (plugin.getStorageConfig().getBoolean(yamlloc+".access."+player.getName()) || plugin.getStorageConfig().get(yamlloc+".access."+player.getName()) == null){
							player.sendMessage("[Secure Chests] Adding "+checkName+" to deny list.");
							plugin.getStorageConfig().set(yamlloc+".access."+checkName, false);
							plugin.saveStorageConfig();
							plugin.scAList.remove(player);
						} else {
							player.sendMessage("[Secure Chests] Player "+checkName+" already in deny list.");
						} 
					} else {// no commands to be executed just open chest.
						player.sendMessage("[Secure Chests] You own this Chest");
						return;
					}
				} else { // chest owned by someone else
					
					//check access list for your name
					
					if (plugin.getStorageConfig().getBoolean(yamlloc+".access."+player.getName())){ //You are on the list!
						player.sendMessage("[Secure Chests] You have access to " + lockname + "'s chest." );
						return; //allow you to open it!
					} else if (plugin.getStorageConfig().get(yamlloc+".access."+player.getName()) != null) { //You are on the deny list! ohh no!
						event.setCancelled(true);
					} else if (plugin.getAListConfig().getBoolean(lockname+"." + player.getName())){ // you are on the global allow list! yay!
						player.sendMessage("[Secure Chests] You have acces to " + lockname + "'s chest." );
						return;
					}
					
					if(player.hasPermission("securechests.bypass.open")) { //check for admin bypass
						player.sendMessage("[Secure Chests] bypassing lock owned by player: " + lockname);
						event.setCancelled(false);
						return;
					} else { //no bypass owned by someone else deny entry
						player.sendMessage("[Secure Chests] Can not open chest, owned by:  " + lockname);
						if(!(player.hasPermission("securechests.bypass.break") && event.getAction() == Action.LEFT_CLICK_BLOCK)) {
							event.setCancelled(true);
						}
					}
				}
			}
		} //end check for chest block
	}//end onPlayerInteract();
	
	public void onPlayerQuit(PlayerQuitEvent event) {
		if (plugin.scAList.get(event.getPlayer()) != null) {
			plugin.scAList.remove(event.getPlayer());
		}
		if (plugin.scCmd.get(event.getPlayer()) != null) {
			plugin.scCmd.remove(event.getPlayer());
		}
	}
}

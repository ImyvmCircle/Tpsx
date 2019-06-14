package com.imyvm.tpsx;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.connorlinfoot.actionbarapi.ActionBarAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.minecraft.server.v1_14_R1.MathHelper;
import net.minecraft.server.v1_14_R1.MinecraftServer;

public final class Tpsx extends JavaPlugin implements Listener {
	@SuppressWarnings("deprecation")
	static final MinecraftServer server = MinecraftServer.getServer();
	static final NumberFormat formatter = new DecimalFormat("#0.00");
	static final Map<UUID, Boolean> map = new HashMap<>();

	@Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("tpsx")) {
		    if (!(sender instanceof Player)){
		        sender.sendMessage(getTpsinfo());
		        return false;
            }
			Player player = (Player) sender;
			if (map.containsKey(player.getUniqueId())){
				map.remove(player.getUniqueId());
				sender.sendMessage("tpsx off");
				return true;
			}
			map.put(player.getUniqueId(), true);
            sender.sendMessage("tpsx on");
			// double mspt = MathHelper.average(server.lastTickLengths) * 1.0E-6D;
			return true;
		}
		return false;
	}

    @EventHandler
	public void playermove(PlayerMoveEvent event){
	    Player player = event.getPlayer();
	    if (!map.get(player.getUniqueId())){
	        return;
        }
        ActionBarAPI.sendActionBar(player,getTpsinfo(), 100);
    }

	private String getTpsinfo(){
		double mspt = MathHelper.a(server.f) * 1.0E-6D;
		double tps = Math.min(1000.0 / mspt, 20.0);

		String mspt_color = mspt <= 40 ? "§a" : (mspt >= 60 ? "§c" : "§e");
		String tps_color = tps < 20 ? "§c" : "§a";

		return ("TPS: " + tps_color + formatter.format(tps) + "§r, MSPT: " + mspt_color + formatter.format(mspt) + "§r");
	}
}

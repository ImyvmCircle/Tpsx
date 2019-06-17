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
import org.bukkit.plugin.java.JavaPlugin;

import net.minecraft.server.v1_14_R1.MathHelper;
import net.minecraft.server.v1_14_R1.MinecraftServer;

public class Tpsx extends JavaPlugin {
    @SuppressWarnings("deprecation")
    static final MinecraftServer server = MinecraftServer.getServer();
    static final NumberFormat formatter = new DecimalFormat("#0.00");
    static final Map<UUID, Player> players = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
        	@Override
        	public void run() {
        		sendTpsInfo();
        	}
        }, 0, 20);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("tpsx")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(getTpsInfo());
                return true;
            }

            Player player = (Player) sender;
            if (players.containsKey(player.getUniqueId())) {
            	players.remove(player.getUniqueId());
            	ActionBarAPI.sendActionBar(player, "", 0);
                sender.sendMessage("tpsx off");
            }
            else {
            	players.put(player.getUniqueId(), player);
            	sender.sendMessage("tpsx on");
            }
            return true;
        }
        return false;
    }
    
    public void sendTpsInfo() {
        String msg = getTpsInfo();
        for (Player player : players.values()) {
        	ActionBarAPI.sendActionBar(player, msg);
        }
    }

    private String getTpsInfo() {
        // double mspt = MathHelper.average(server.lastTickLengths) * 1.0E-6D;
        double mspt = MathHelper.a(server.f) * 1.0E-6D;
        double tps = Math.min(1000.0 / mspt, 20.0);

        String mspt_color = mspt <= 40 ? "§a" : (mspt >= 60 ? "§c" : "§e");
        String tps_color = tps < 20 ? "§c" : "§a";

        return ("TPS: " + tps_color + formatter.format(tps) + "§r, MSPT: " + mspt_color + formatter.format(mspt) + "§r");
    }
}

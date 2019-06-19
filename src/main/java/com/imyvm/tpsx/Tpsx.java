package com.imyvm.tpsx;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.google.common.collect.Iterables;

import com.connorlinfoot.actionbarapi.ActionBarAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.minecraft.server.v1_14_R1.IChatBaseComponent;
import net.minecraft.server.v1_14_R1.MathHelper;
import net.minecraft.server.v1_14_R1.MinecraftServer;
import net.minecraft.server.v1_14_R1.PacketPlayOutPlayerListHeaderFooter;
import net.minecraft.server.v1_14_R1.PlayerConnection;

public class Tpsx extends JavaPlugin {
    private static List<String> allowToggle = Arrays.asList("bar", "tab", "off");
    @SuppressWarnings("deprecation")
    private static MinecraftServer server = MinecraftServer.getServer();
    private static NumberFormat formatter = new DecimalFormat("#0.00");
    private Map<UUID, Player> barPlayers = new HashMap<>();
    private Map<UUID, Player> tabPlayers = new HashMap<>();

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

            if (args.length != 2 || !args[0].equals("toggle") || !allowToggle.stream().anyMatch(str -> str.equals(args[1]))) {
                return false;
            }

            Player player = (Player) sender;
            switchTo(player, args[1]);

            return true;
        }
        return false;
    }

    private void switchTo(Player player, String target) {
        barPlayers.remove(player.getUniqueId());
        tabPlayers.remove(player.getUniqueId());

        setPlayerListFooter(player, "");
        ActionBarAPI.sendActionBar(player, "");

        switch (target) {
            case "bar":
                barPlayers.put(player.getUniqueId(), player);
                break;

            case "tab":
                tabPlayers.put(player.getUniqueId(), player);
                break;
        }
    }

    private void updatePermission() {
        for (Player player : Iterables.concat(barPlayers.values(), tabPlayers.values())) {
            if (!player.hasPermission("tpsx.view")) {
                switchTo(player, "off");
            }
        }
    }

    private static void setPlayerListFooter(Player player, String footer) {
        CraftPlayer craftPlayer = (CraftPlayer) player;
        PlayerConnection connection = craftPlayer.getHandle().playerConnection;

        PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter();

        packet.header = IChatBaseComponent.ChatSerializer.a("{\"text\": \"\"}");
        packet.footer = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + footer + "\"}");

        connection.sendPacket(packet);
    }

    public void sendTpsInfo() {
        updatePermission();

        String msg = getTpsInfo();
        barPlayers.values().forEach(player -> ActionBarAPI.sendActionBar(player, msg));
        tabPlayers.values().forEach(player -> setPlayerListFooter(player, msg));
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

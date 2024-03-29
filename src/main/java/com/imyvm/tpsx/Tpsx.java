package com.imyvm.tpsx;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;

public class Tpsx extends JavaPlugin {
    private static final List<String> allowToggle = Arrays.asList("bar", "tab", "disable");
    @SuppressWarnings("deprecation")
    private static final MinecraftServer server = MinecraftServer.getServer();
    private static final NumberFormat formatter = new DecimalFormat("#0.0");
    private Map<UUID, Player> barPlayers = new HashMap<>();
    private Map<UUID, Player> tabPlayers = new HashMap<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::sendTpsInfo, 0, 20);

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onLeave(PlayerQuitEvent event) {
                barPlayers.remove(event.getPlayer().getUniqueId());
                tabPlayers.remove(event.getPlayer().getUniqueId());
            }
        }, this);

        this.getCommand("tpsx").setTabCompleter(this);
        setUsageFromConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("tpsx")) {
            if (!(sender instanceof Player) && args.length == 0) {
                sender.sendMessage(getTpsInfo());
                return true;
            }

            if (args.length == 2 && args[0].equals("toggle")) {
                return subCommandToggle(sender, args[1]);
            }
            if (args.length == 1 && args[0].equals("reload")) {
                return subCommandReload(sender);
            }

            return false;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> result = new ArrayList<>();

        if (args.length == 1) {
            result.addAll(filterStartsWith(Collections.singletonList("toggle"), args[0]));
            if ((sender instanceof Player) && sender.hasPermission("tpsx.manage") && "reload".startsWith(args[0])) {
                result.add("reload");
            }
        }
        else if (args.length == 2 && args[0].equals("toggle")) {
            result.addAll(filterStartsWith(allowToggle, args[1]));
        }

        return result;
    }

    private boolean subCommandToggle(CommandSender sender, String target) {
        if (!(sender instanceof Player)) {
            sendMessageFromConfig(sender, "toggle.message.from_server");
            return true;
        }

        if (allowToggle.stream().noneMatch(str -> str.equals(target))) {
            return false;
        }
        switchTo((Player)sender, target);

        return true;
    }

    private boolean subCommandReload(CommandSender sender) {
        if ((sender instanceof Player) && !sender.hasPermission("tpsx.manage")) {
            sender.sendMessage("§cI'm sorry, but you do not have permission to perform this command. Please contact the server administrators if you believe that this is in error.§r");
            return true;
        }

        this.reloadConfig();
        setUsageFromConfig();
        sendMessageFromConfig(sender, "reload.message.success");

        return true;
    }

    private void switchTo(Player player, String target) {
        barPlayers.remove(player.getUniqueId());
        tabPlayers.remove(player.getUniqueId());

        setPlayerListFooter(player, "");
        sendActionBar(player, "");

        switch (target) {
            case "bar":
                barPlayers.put(player.getUniqueId(), player);
                break;

            case "tab":
                tabPlayers.put(player.getUniqueId(), player);
                break;
        }

        sendMessageFromConfig(player, "toggle.message." + target);
    }

    private void updatePermission() {
        List<Player> toRemove = new ArrayList<>();
        for (Player player : Iterables.concat(barPlayers.values(), tabPlayers.values())) {
            if (!player.hasPermission("tpsx.view")) {
                toRemove.add(player);
            }
        }
        for (Player player : toRemove) {
            switchTo(player, "disable");
        }
    }

    private void setUsageFromConfig() {
        String usage = this.getConfig().getString("usage");
        if (usage != null) {
            this.getCommand("tpsx").setUsage(usage);
        }
    }

    private List<String> filterStartsWith(List<String> origin, String prefix) {
        return origin.stream().filter(str -> str.startsWith(prefix)).collect(Collectors.toList());
    }

    private void setPlayerListFooter(Player player, String footer) {
        player.setPlayerListFooter(footer);
    }

    private void sendMessageFromConfig(CommandSender sender, String path) {
        String message = this.getConfig().getString(path);
        if (message != null)
            sender.sendMessage(message);
    }

    public void sendTpsInfo() {
        updatePermission();

        String msg = getTpsInfo();
        barPlayers.values().forEach(player -> sendActionBar(player, msg));
        tabPlayers.values().forEach(player -> setPlayerListFooter(player, msg));
    }

    private String getTpsInfo() {
        double mspt = MathHelper.a(server.n) * 1.0E-6D;
        double tps = Math.min(1000.0 / mspt, 20.0);

        String mspt_color = mspt <= 40 ? "§a" : (mspt >= 60 ? "§c" : "§e");
        String tps_color = tps < 20 ? "§c" : "§a";

        return ("TPS: " + tps_color + formatter.format(tps) + "§r MSPT: " + mspt_color + formatter.format(mspt) + "§r");
    }

    private void sendActionBar(Player player, String msg){
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }
}

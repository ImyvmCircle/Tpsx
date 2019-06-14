package com.imyvm.tpsx;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import net.minecraft.server.v1_14_R1.MathHelper;
import net.minecraft.server.v1_14_R1.MinecraftServer;

public final class Tpsx extends JavaPlugin {
	@SuppressWarnings("deprecation")
	static final MinecraftServer server = MinecraftServer.getServer();
	static final NumberFormat formatter = new DecimalFormat("#0.00");

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("tpsx")) {
			// double mspt = MathHelper.average(server.lastTickLengths) * 1.0E-6D;
			double mspt = MathHelper.a(server.f) * 1.0E-6D;
			double tps = Math.min(1000.0 / mspt, 20.0);

			String mspt_color = mspt <= 40 ? "§a" : (mspt >= 60 ? "§c" : "§e");
			String tps_color = tps < 20 ? "§c" : "§a";

			sender.sendMessage("TPS: " + tps_color + formatter.format(tps) + "§r, MSPT: " + mspt_color + formatter.format(mspt) + "§r");

			return true;
		}

		return false;
	}
}

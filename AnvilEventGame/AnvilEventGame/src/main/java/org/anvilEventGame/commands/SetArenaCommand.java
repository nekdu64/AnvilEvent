package org.anvilEventGame.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.anvilEventGame.AnvilEventGame;
import org.anvilEventGame.game.MyMiniGame;

import java.util.ArrayList;
import java.util.List;

public class SetArenaCommand implements CommandExecutor {

    private final AnvilEventGame plugin;

    public SetArenaCommand(AnvilEventGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSeuls les joueurs peuvent utiliser cette commande.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUtilisation: /anvil setarena pos1|pos2, /anvil reload, /anvil resetarena");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("anvil.reload")) {
                player.sendMessage("§cVous n'avez pas la permission (§eanvil.reload§c).");
                return true;
            }
            plugin.reloadConfig();
            player.sendMessage("§aConfiguration rechargée !");
            return true;
        }

        if (args[0].equalsIgnoreCase("resetarena")) {
            if (!player.hasPermission("anvil.resetarena")) {
                player.sendMessage("§cVous n'avez pas la permission (§eanvil.resetarena§c).");
                return true;
            }

            // 📍 Restauration simple : reload les blocs directement depuis les coords
            Location pos1 = getLoc("arena.pos1");
            Location pos2 = getLoc("arena.pos2");

            if (pos1 == null || pos2 == null) {
                player.sendMessage("§cLes positions ne sont pas définies !");
                return true;
            }
            plugin.resetarena(pos1, pos2);

            player.sendMessage("§aArène Anvil restaurée avec succès !");
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("setarena")) {
            if (!player.hasPermission("anvil.setarena")) {
                player.sendMessage("§cVous n'avez pas la permission (§eanvil.setarena§c).");
                return true;
            }

            Location loc = player.getLocation();
            if (args[1].equalsIgnoreCase("pos1")) {
                plugin.getConfig().set("arena.pos1.world", loc.getWorld().getName());
                plugin.getConfig().set("arena.pos1.x", loc.getX());
                plugin.getConfig().set("arena.pos1.y", loc.getY());
                plugin.getConfig().set("arena.pos1.z", loc.getZ());
                plugin.saveConfig();
                player.sendMessage("§aPosition 1 de l'arène enregistrée !");
                return true;
            } else if (args[1].equalsIgnoreCase("pos2")) {
                plugin.getConfig().set("arena.pos2.world", loc.getWorld().getName());
                plugin.getConfig().set("arena.pos2.x", loc.getX());
                plugin.getConfig().set("arena.pos2.y", loc.getY());
                plugin.getConfig().set("arena.pos2.z", loc.getZ());
                plugin.saveConfig();
                player.sendMessage("§aPosition 2 de l'arène enregistrée !");
                return true;
            }
        }

        player.sendMessage("§cUtilisation: /anvil setarena pos1|pos2, /anvil reload, /anvil resetarena");
        return true;
    }

    private Location getLoc(String path) {
        String world = plugin.getConfig().getString(path + ".world");
        if (world == null) return null;
        double x = plugin.getConfig().getDouble(path + ".x");
        double y = plugin.getConfig().getDouble(path + ".y");
        double z = plugin.getConfig().getDouble(path + ".z");
        return new Location(Bukkit.getWorld(world), x, y, z);
    }
}

package org.anvilEventGame.Util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class EliminationMessage {
    private static final List<String> eliminationMessages = Arrays.asList(
            "§c%player% s'est fait écrabouiller !",
            "§c%player% a goûté à la gravité...",
            "§c%player% a été puni par le ciel !",
            "§c%player% a rencontré une enclume... violemment.",
            "§c%player% ne verra plus jamais les étoiles.",
            "§c%player% s'est fait enclumer.",
            "§c%player% a fait une overdose de fer."
    );

    private static final List<String> top3Messages = Arrays.asList(
            "§6%player% termine à la §e3ᵉ place§6, pas mal !",
            "§e%player% décroche la §e2ᵉ place§e, si proche de la victoire...",
            "§7%player% sort du podium, mais avec classe !"
    );

    public void broadcastElimination(Player player, int remaining) {
        String message;

        if (remaining == 2) {
            message = top3Messages.get(0); // 3e place
        } else if (remaining == 1) {
            message = top3Messages.get(1); // 2e place
        } else {
            message = eliminationMessages.get(new Random().nextInt(eliminationMessages.size()));
        }

        Bukkit.broadcastMessage(message.replace("%player%", player.getName()));
    }
}

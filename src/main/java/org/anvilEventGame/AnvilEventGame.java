package org.anvilEventGame;

import org.apache.maven.model.PluginContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.simpleEventManager.api.EventGame;
import org.anvilEventGame.commands.SetArenaCommand;
import org.anvilEventGame.game.MyMiniGame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class AnvilEventGame extends JavaPlugin implements EventGame {

    private MyMiniGame game;

    @Override
    public void onEnable() {
        this.getCommand("anvil").setExecutor(new SetArenaCommand(this));
        getLogger().info("AnvilEvent enabled!");

        // Sauvegarde le fichier config.yml depuis le JAR si non présent
        saveDefaultConfig();

        // Exemple de lecture depuis le config.ym
    }

    // Mon Manager (SEM) a besoin de ça
    @Override
    public void start(List<Player> players) {
        this.game = new MyMiniGame(players, this);
        game.start(getMode());
    }

    @Override
    public void stop() {
        if (game != null) {
            game.stop();
            game = null; // On check comme ca qu'un event est bien fini
        }
    }

    @Override
    public boolean hasWinner() {
        return game != null && !game.running;
    }

    @Override
    public List<Player> getWinners() {
        return game != null ? game.getWinners() : List.of();
    }

    @Override
    public void Removeplayer(Player player) {
        if (game != null) {
            game.eliminate(player);
        }
    }

    @Override
    public String getEventName() {
        return "anvil";
    }

    @Override
    public String getEventDescription() {
        return "Event anvil, regardez en haut, ne vous faites pas applatir.";
    }

    private String mode;

    @Override
    public void setMode(String mode) {
        this.mode = (mode != null) ? mode.toLowerCase() : pickRandomMode();
    }

    private String pickRandomMode() {
        List<String> availableModes = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            String name = getConfig().getString("AnvilConfig.Config" + i + ".Name");
            if (name != null && !"NotUse".equalsIgnoreCase(name)) {
                availableModes.add(name);
            }
        }
        if (availableModes.isEmpty()) return "default";
        return availableModes.get(new Random().nextInt(availableModes.size()));
    }


    @Override
    public String getMode() {
        return mode;
    }


    public void resetarena(Location pos1, Location pos2) {

        // 📍 Restauration simple : reload les blocs directement depuis les coords


        for (int x = Math.min(pos1.getBlockX(), pos2.getBlockX()); x <= Math.max(pos1.getBlockX(), pos2.getBlockX()); x++) {
            for (int y = Math.min(pos1.getBlockY(), pos2.getBlockY()); y <= Math.max(pos1.getBlockY(), pos2.getBlockY()); y++) {
                for (int z = Math.min(pos1.getBlockZ(), pos2.getBlockZ()); z <= Math.max(pos1.getBlockZ(), pos2.getBlockZ()); z++) {
                    Location blockLoc = new Location(pos1.getWorld(), x, y, z);
                    if (blockLoc.getBlock().getType().toString().contains("ANVIL")) {
                        blockLoc.getBlock().setType(Material.AIR); // 🧊 Tu enleve les anvil
                    }
                    if (blockLoc.getBlock().getType() == Material.GOLD_BLOCK) {
                        blockLoc.getBlock().setType(Material.OAK_SLAB); // Tu enleve les blocks d'or
                    }
                }
            }
        }
    }
}
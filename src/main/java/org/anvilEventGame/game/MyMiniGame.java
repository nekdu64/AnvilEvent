package org.anvilEventGame.game;

import org.anvilEventGame.Util.DelayUtil;
import org.anvilEventGame.Util.EliminationMessage;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.simpleEventManager.SimpleEventManager;
import org.simpleEventManager.utils.EventUtils;
import org.anvilEventGame.AnvilEventGame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static java.lang.Math.round;
import static org.bukkit.Bukkit.getOnlinePlayers;

public class MyMiniGame implements Listener {

    private final DelayUtil delayUtil;
    private final List<Player> players;
    private final List<Player> ranking = new ArrayList<>();
    private final List<BlockState> originalBlocks = new ArrayList<>();
    private final AnvilEventGame plugin;
    private int countdownTaskId = -1;
    public Player winner;
    public boolean running = false;
    private boolean Finale = false;
    private int startPlayerCount;
    private BossBar bossBar;



    public MyMiniGame(List<Player> players, AnvilEventGame plugin) {
        this.players = new ArrayList<>(players);
        this.plugin = plugin;
        this.startPlayerCount = players.size();
        this.delayUtil = new DelayUtil(plugin);
    }


    public void start(String Forceconfig) { // Start l'event

        //reference a mon manager
        SimpleEventManager sem = (SimpleEventManager) Bukkit.getPluginManager().getPlugin("SimpleEventManager");
        // Pour tp les joueurs au spawn de l'event grace au spawn set dans le manager avec /event setspawn anvil
        Location loc = EventUtils.getEventSpawnLocation(sem, plugin.getEventName());
        Bukkit.getPluginManager().registerEvents(MyMiniGame.this, plugin);
        for (Player player : players) {
            player.teleport(loc);
        }
        // Enregistre pour reset la map
        Location pos1 = getLoc("arena.pos1");
        Location pos2 = getLoc("arena.pos2");


        originalBlocks.clear();
        for (int x = Math.min(pos1.getBlockX(), pos2.getBlockX()); x <= Math.max(pos1.getBlockX(), pos2.getBlockX()); x++) {
            for (int y = Math.min(pos1.getBlockY(), pos2.getBlockY()); y <= Math.max(pos1.getBlockY(), pos2.getBlockY()); y++) {
                for (int z = Math.min(pos1.getBlockZ(), pos2.getBlockZ()); z <= Math.max(pos1.getBlockZ(), pos2.getBlockZ()); z++) {
                    Location blockLoc = new Location(pos1.getWorld(), x, y, z);
                    originalBlocks.add(blockLoc.getBlock().getState());
                }
            }
        }

         // Important pour que tout se reset bien (l'instance de l'event + les recompenses ect)

        // Créer la bossbar - HUD
        if(!Finale) {
            bossBar = Bukkit.createBossBar("§eJoueurs restants : " + players.size(), BarColor.BLUE, BarStyle.SEGMENTED_10);
            bossBar.setProgress(1.0);
            for (Player player : players) {
                bossBar.addPlayer(player);
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
        for (Player player : players) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        // Vérifie si la config existe dans AnvilConfig, sinon sélectionne une config aléatoire
        ConfigurationSection listConfig = plugin.getConfig().getConfigurationSection("AnvilConfig");
        running = true;

        String configChoisie = null;
        if (listConfig != null) {
            for (String key : listConfig.getKeys(false)) {
                if (key.equalsIgnoreCase(Forceconfig)) {
                    configChoisie = key; // utiliser le vrai nom avec la bonne casse
                    break;
                }
            }
        }
        if (configChoisie == null) {
            configChoisie = plugin.Randomconfig();
        }

        for (Player player : players) {
            player.sendTitle("§c§lANVIL", "§e"+configChoisie, 10, 100, 20);
        }
        String finalConfigChoisie = configChoisie;
        //Choix de la config et affichage
        delayUtil.delay(100, () -> {
            Finale = false;
            // Start Countdown -
            countdownTaskId = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                int countdown = 10;
                PotionEffect Resistance = new PotionEffect(PotionEffectType.RESISTANCE,50000,4,true,false);
                @Override
                public void run() {
                    if (countdown == 0) {
                        Bukkit.broadcastMessage("§a§lGO !");
                        for (Player player : players) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1f);
                            player.sendTitle("§a§lGO !", "", 10, 20, 20);
                            player.sendPotionEffectChange(player , Resistance);
                        }
                        Bukkit.getScheduler().cancelTask(countdownTaskId);
                        Départanvil("AnvilConfig."+finalConfigChoisie); //  LACHER LES ENCLUMES
                    }
                    if (countdown <= 3 && countdown > 0) {
                        for (Player player : players) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 0.5f);
                            player.sendTitle("§e" + countdown, "", 10, 20, 20);

                        }
                    }
                    Bukkit.broadcastMessage("§eDébut dans §l" + countdown + "s...");
                    countdown--;
                }
            }, 0L, 20L).getTaskId();
        });
    }

    public void normalstop() {

        Location pos1 = getLoc("arena.pos1");
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTask(countdownTaskId);
        String WorldEvent = pos1.getWorld().getName();
        Bukkit.getWorld(WorldEvent).setGameRule(GameRule.DO_ENTITY_DROPS, true);
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        delayUtil.delay(220, () -> {
            running = false;
            resetArena();
        });
    }




    private void Départanvil(String configChoisie) {

        String Config = configChoisie + ".Anvil";
        List<List<Integer>> AnvilList = (List<List<Integer>>) plugin.getConfig().getList(Config);
        int TimeMultiVagueTick = plugin.getConfig().getInt(configChoisie + ".TimeMultiVagueTick");
        int TimeVagueTick = plugin.getConfig().getInt(configChoisie + ".TimeVagueTick");
        String NameConfig = plugin.getConfig().getString(configChoisie + ".Name");
        String WorldEven = plugin.getConfig().getString("arena.pos1.world");
        Bukkit.getWorld(WorldEven).setGameRule(GameRule.DO_ENTITY_DROPS, false);
        int totalDelay = 0;
        int delayAfterTitle = plugin.getConfig().getInt(configChoisie + ".TimeAfterTitle");
        ; // ticks entre l'affichage du titre et le début de la 1ère vague


        for (int i = 0; i < AnvilList.size(); i++) {
            int pourcentage = AnvilList.get(i).get(0);
            int nbVague = AnvilList.get(i).get(1);
            int vagueIndex = i + 1;

            // Affiche le titre avec un léger délai
            delayUtil.delay(totalDelay + TimeVagueTick, () -> {
                for (Player player : players) {
                    player.sendTitle("§cVague:" + vagueIndex, "§e" + nbVague + " X " + pourcentage + "%", 10, 60, 20);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 0.5f);
                }
            });

            // Lance les vagues espacées dans le temps après l'affichage du titre
            for (int j = 0; j < nbVague; j++) {
                int delayThisWave = totalDelay + TimeVagueTick + delayAfterTitle + j * TimeMultiVagueTick;

                delayUtil.delay(delayThisWave, () -> {
                    LacherDenclume(pourcentage);
                });
            }

            // Ajoute le temps total pour passer à la prochaine vague
            totalDelay += TimeVagueTick + delayAfterTitle + (nbVague - 1) * TimeMultiVagueTick;
        }
    }

    private void LacherDenclume(int pourcentage) {
        Location pos1 = getLoc("arena.pos1");
        Location pos2 = getLoc("arena.pos2");
        for (Player player : players) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 0.5f);
        }

        World world = pos1.getWorld();
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());

        for (int x = Math.min(pos1.getBlockX(), pos2.getBlockX()) + 1; x + 1 <= Math.max(pos1.getBlockX(), pos2.getBlockX()); x++) {
            for (int z = Math.min(pos1.getBlockZ(), pos2.getBlockZ()) + 1; z + 1 <= Math.max(pos1.getBlockZ(), pos2.getBlockZ()); z++) {
                int randomNumber = (int) (Math.random() * 100); // entre 0 et 99
                if (randomNumber < pourcentage) {
                    Location spawnLoc = new Location(world, x + 0.5, maxY + 1, z + 0.5); // Centrage dans le bloc
                    FallingBlock fallingAnvil = world.spawnFallingBlock(spawnLoc, Material.ANVIL.createBlockData());
                    fallingAnvil.setDropItem(false); // Évite de drop une enclume au sol
                    fallingAnvil.setHurtEntities(true); // Facultatif : les entités prennent des dégâts
                    fallingAnvil.setDamagePerBlock(2.0f); // Facultatif : dégâts en fonction de la hauteur
                }
            }
        }
    }

    private void updateBossBar() {
        if (bossBar == null) return;

        double progress = Math.max(0.0, (double) players.size() / startPlayerCount);
        bossBar.setProgress(progress);
        bossBar.setTitle("§eJoueurs restants : " + players.size());
    }


    public void eliminate(Player player) {
        if (!players.contains(player)) return;
        if (!running) return;

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.5f);

        players.remove(player);
        ranking.addFirst(player);

        player.setGameMode(GameMode.SPECTATOR);
        SimpleEventManager sem = (SimpleEventManager) Bukkit.getPluginManager().getPlugin("SimpleEventManager");
        player.teleport(EventUtils.getEventSpawnLocation(sem, plugin.getEventName()));

        updateBossBar();

        delayUtil.delay(1, () -> {
            int remaining = players.size();
            if (remaining == 1) {

                EliminationMessage eliminationMessage = new EliminationMessage();
                eliminationMessage.broadcastElimination(player,remaining);
                endgame(players.getFirst());

            } else if (remaining <= 0 && !Finale) {
                Bukkit.broadcastMessage("§c[Anvil] Égalité ! IL FAUT UN VAINQUEUR --> FINALE");
                Player last = ranking.get(0);         // Dernier éliminé
                Player beforeLast = ranking.get(1);   // Avant-dernier éliminé
                Finale(beforeLast, last);

            } else {
                EliminationMessage eliminationMessage = new EliminationMessage();
                eliminationMessage.broadcastElimination(player,remaining);
            }
        });

    }
    private void Finale(Player player1, Player player2) {
        if (Finale) return;
        Finale = true;
        delayUtil.cancelAll();
        players.add(player1);
        players.add(player2);
        ranking.remove(player1);
        ranking.remove(player2);
        updateBossBar();
        KillAnvil(player1);
        delayUtil.delay(20, () -> {
            KillAnvil(player1);
        });
        for (Player allPlayer : Bukkit.getOnlinePlayers()) {
            if (allPlayer.getWorld().equals(player1.getWorld())) {
                allPlayer.sendTitle("§c§lFINALE", "§c" + player1.getName() + " §eVS§c " + player2.getName(), 10, 80, 20);
            }
        }
        start("Finale");

    }


    private void endgame(Player lastPlayer) {

        if (lastPlayer != null && !ranking.contains(lastPlayer) && lastPlayer.isOnline()) {
            delayUtil.cancelAll(); // Stop toutes les tâches programmées
            winner = lastPlayer;
            List<BlockState> originalBlocksCopy = originalBlocks;
            for (BlockState state : originalBlocksCopy) {
                if (state.getType().toString().contains("SLAB")) {
                    state.getBlock().setType(Material.GOLD_BLOCK);
                }
            }
            lastPlayer.teleport(lastPlayer.getLocation().add(0, 0.5, 0));
            // Limite les feux d'artifice à 3
            for (int i = 0; i < 3; i++) {
                delayUtil.delay(i*15, () -> {
                    KillAnvil(lastPlayer);
                    spawnFireworks(lastPlayer);
                });
            }

            ranking.add(0, winner);
            Bukkit.broadcastMessage("§6[Anvil] Le gagnant est : §e" + winner.getName());
        } else {
            Bukkit.broadcastMessage("§c[Anvil] Erreur : dernier joueur vivant introuvable ou déjà éliminé.");
        }

        // Termine proprement le jeu après 7,5 secondes
        normalstop();


    }

    public void spawnFireworks(Player player) {
        Location loc = player.getLocation().add(0, 1, 0); // légèrement au-dessus du joueur

        for (int i = 0; i < 3; i++) {

            Firework firework = (Firework) player.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
            FireworkMeta meta = firework.getFireworkMeta();

            FireworkEffect effect = FireworkEffect.builder()
                    .withColor(Color.RED)
                    .withColor(Color.YELLOW)
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .flicker(true)
                    .trail(true)
                    .build();

            meta.addEffect(effect);
            meta.setPower(1);
            firework.setFireworkMeta(meta);
        }
    }
    public List<Player> getWinners() {
        return new ArrayList<>(ranking);
    }

    private void resetArena() {
        for (BlockState state : originalBlocks) {
            state.update(true, false);
        }
    }

    private Location getLoc(String path) {
        String world = plugin.getConfig().getString(path + ".world");
        double x = plugin.getConfig().getDouble(path + ".x");
        double y = plugin.getConfig().getDouble(path + ".y");
        double z = plugin.getConfig().getDouble(path + ".z");
        return new Location(Bukkit.getWorld(world), x, y, z);
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!players.contains(player)) return;
        if (!running) return;
        // pas le droit de poser de blocs
        event.setCancelled(true);
    }
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!players.contains(player)) return;
        if (!running) return;
        // pas le droit de casser de blocs
        event.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if(event.getDamageSource().getCausingEntity() instanceof Player) {
            event.setCancelled(true);
            return;
        }
        if (!players.contains(player)) return;
        if (!running) return;
        eliminate(player);
        event.setCancelled(true);
    }

    public boolean isFinished() {
        return !running;
    }
    public void KillAnvil(Player player) {
        for (Entity entity : player.getWorld().getEntities()) {
            if (entity instanceof FallingBlock fallingBlock) {
                if (fallingBlock.getBlockData().getMaterial() == Material.ANVIL ||
                        fallingBlock.getBlockData().getMaterial() == Material.CHIPPED_ANVIL ||
                        fallingBlock.getBlockData().getMaterial() == Material.DAMAGED_ANVIL) {
                    fallingBlock.remove();

                }
            }
        }
    }
    public List<Player> getPlayers() {
        return players;
    }
    public void stop() {

        Location pos1 = getLoc("arena.pos1");
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTask(countdownTaskId);
        String WorldEvent = pos1.getWorld().getName();
        Bukkit.getWorld(WorldEvent).setGameRule(GameRule.DO_ENTITY_DROPS, true);
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        running = false;
        resetArena();
    }
}

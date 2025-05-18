package org.anvilEventGame.game;

import org.anvilEventGame.DelayUtil;
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
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.simpleEventManager.SimpleEventManager;
import org.simpleEventManager.utils.EventUtils;
import org.anvilEventGame.AnvilEventGame;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.round;

public class MyMiniGame implements Listener {

    private final DelayUtil delayUtil;
    private final List<Player> players;
    private final List<Player> ranking = new ArrayList<>();
    private final List<BlockState> originalBlocks = new ArrayList<>();
    private final AnvilEventGame plugin;
    public boolean isFinished;
    private int countdownTaskId = -1;
    private Player winner;
    private boolean running = false;
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

        running = true; // Important pour que tout se reset bien (l'instance de l'event + les recompenses ect)

        // Créer la bossbar - HUD
        bossBar = Bukkit.createBossBar("§eJoueurs restants : " + players.size(), BarColor.BLUE, BarStyle.SEGMENTED_10);
        bossBar.setProgress(1.0);
        for (Player player : players) {
            bossBar.addPlayer(player);
        }
        int Configchoisie = Forceconfig.isEmpty() ? Randomconfig() : getConfigNumberByName(Forceconfig);
        //Choix de la config et affichage
        delayUtil.delay(100, () -> {

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
                        Bukkit.getPluginManager().registerEvents(MyMiniGame.this, plugin);
                        Bukkit.getScheduler().cancelTask(countdownTaskId);
                        Départanvil(Configchoisie); //  LACHER LES ENCLUMES
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

    public void stop() {
        Location pos1 = getLoc("arena.pos1");
        Location pos2 = getLoc("arena.pos2");
        resetArena();
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTask(countdownTaskId);
        String WorldEvent = pos1.getWorld().getName();
        Bukkit.getWorld(WorldEvent).setGameRule(GameRule.DO_ENTITY_DROPS, true);
        delayUtil.cancelAll(); //Stop toute les taches actives
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        running = false; // Important encore une fois comme ca on est sur que l'event est bien fini
    }


    public int getConfigNumberByName(String name) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("AnvilConfig");
        if (section == null) {
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (player.isOp()) {
                    player.sendMessage("Config "+name+" n'existe pas");
                }
            });
        }

        for (String key : section.getKeys(false)) {
            String configName = plugin.getConfig().getString("AnvilConfig." + key + ".Name");
            if (configName != null && configName.equalsIgnoreCase(name)) {
                try {
                    return Integer.parseInt(key.replace("Config", ""));
                } catch (NumberFormatException e) {
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        if (player.isOp()) {
                            player.sendMessage("Config "+name+" n'existe pas, choix aléatoire d'une config à la place");
                        }
                    });
                    return Randomconfig();
                }
            }
        }
        return 0; // Not found
    }

    private int Randomconfig() {

        int x = 0; //nombres de config actives
        ArrayList<String> NameConfig = new ArrayList<>();
        for (int i = 1; i < 11; i++) {
            String Name = plugin.getConfig().getString("AnvilConfig.Config" + i + ".Name");
            if (!Name.equals("NotUse")) {
                NameConfig.add(Name);
                x++;
            }
        }
        int randomNumber = (int) (Math.random() * x);//obtient un nombre entre 1 et nombre de config active
        String Configchoisie = NameConfig.get(randomNumber);
        // Affichage du type choisi
        for (Player player : players) {
            player.sendTitle("§cANVIL", "Mode : " + Configchoisie, 10, 120, 20);
        }

        return randomNumber + 1;
    }


    private void Départanvil(int Configchoisie) {

        //Les taches sont plkanifiers a l'avance, tout est lancé d'ofzzzfice
        String Config = "AnvilConfig.Config" + Configchoisie + ".Anvil";
        List<List<Integer>> AnvilList = (List<List<Integer>>) plugin.getConfig().getList(Config);
        int TimeMultiVagueTick = plugin.getConfig().getInt("AnvilConfig.Config" + Configchoisie + ".TimeMultiVagueTick");
        int TimeVagueTick = plugin.getConfig().getInt("AnvilConfig.Config" + Configchoisie + ".TimeVagueTick");
        String WorldEven = plugin.getConfig().getString("arena.pos1.world");
        Bukkit.getWorld(WorldEven).setGameRule(GameRule.DO_ENTITY_DROPS, false);
        int totalDelay = 0;
        int delayAfterTitle = plugin.getConfig().getInt("AnvilConfig.Config" + Configchoisie + ".TimeAfterTitle");
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

        for (int x = Math.min(pos1.getBlockX(), pos2.getBlockX()) + 1; x + 1 <= Math.max(pos1.getBlockX(), pos2.getBlockX()); x++) {
            for (int z = Math.min(pos1.getBlockZ(), pos2.getBlockZ()) + 1; z + 1 <= Math.max(pos1.getBlockZ(), pos2.getBlockZ()); z++) {
                Location blockLoc = new Location(pos1.getWorld(), x, Math.max(pos1.getBlockY(), pos2.getBlockY()), z);
                int randomNumber = (int) (Math.random() * 100);//obtient un nombre entre 1 et 100
                if (randomNumber < pourcentage) {
                    blockLoc.getBlock().setType(Material.ANVIL);

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


    private void eliminate(Player player) {
        if (!players.contains(player)) return;
        if (!running) return;

        players.remove(player);
        ranking.add(player); // ← Ajouté en fin de classement

        player.setGameMode(GameMode.SPECTATOR);
        SimpleEventManager sem = (SimpleEventManager) Bukkit.getPluginManager().getPlugin("SimpleEventManager");
        player.teleport(EventUtils.getEventSpawnLocation(sem, plugin.getEventName()));

        updateBossBar();

        int remaining = players.size();

        if (remaining == 1) {

        endgame(players.get(0));

        } else if (remaining <= 0) {
            Bukkit.broadcastMessage("§c[Anvil] Égalité ! IL FAUT UN VAINQUEUR --> FINALE");
            Finale(ranking.getLast(),ranking.get(ranking.size()-1));

        } else {
            Bukkit.broadcastMessage("§a" + player.getName() + " s'est fait enclumer. Plus que §e" + remaining + " joueurs en vie.");
        }
    }
    private void Finale(Player player1, Player player2) {
        delayUtil.cancelAll();
        players.add(player1);
        players.add(player2);
        start(plugin.getConfig().getString("Finaleconfig"));

    }


    private void endgame(Player lastPlayer) {

        if (lastPlayer != null && !ranking.contains(lastPlayer) && lastPlayer.isOnline()) {
            delayUtil.cancelAll(); // Stop toutes les tâches programmées
            KillAnvil(lastPlayer);
            winner = lastPlayer;

            for (BlockState state : originalBlocks) {
                if (state.getType().toString().contains("SLAB")) {
                    state.getBlock().setType(Material.GOLD_BLOCK);
                }
            }

            // Limite les feux d'artifice à 3
            for (int i = 0; i < 3; i++) {
                spawnFireworks(lastPlayer);
            }

            ranking.add(0, winner);
            Bukkit.broadcastMessage("§6[Anvil] Le gagnant est : §e" + winner.getName());
        } else {
            Bukkit.broadcastMessage("§c[Anvil] Erreur : dernier joueur vivant introuvable ou déjà éliminé.");
        }

        // Termine proprement le jeu après 7,5 secondes
        delayUtil.delay(150, () -> {
            running = false;
        });
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
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!players.contains(player)) return;
        if (!running) return;
        eliminate(player);
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
}

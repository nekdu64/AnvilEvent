package org.anvilEventGame.Util;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;


public class DelayUtil {

    private final Plugin plugin;
    private final List<Integer> taskIds = new ArrayList<>();

    public DelayUtil(Plugin plugin) {
        this.plugin = plugin;
    }

    public void delay(int ticks, Runnable task) {
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, task, ticks).getTaskId();
        taskIds.add(taskId);
    }

    public void cancelAll() {
        for (int taskId : taskIds) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        taskIds.clear();
    }
}

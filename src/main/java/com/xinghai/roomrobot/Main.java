package com.xinghai.roomrobot;

import java.io.File;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class Main extends JavaPlugin {

    private FileConfiguration Config = null;
    private File configFile = null;
    private boolean isFolia = false;

    @Override
    public void onEnable() {
        getLogger().info("正在加载 扫地机器人插件...");
        
        // 检测是否为Folia服务器
        detectServerType();

        // 加载或创建配置文件
        this.configFile = new File(this.getDataFolder(), "config.yml");
        this.Config = YamlConfiguration.loadConfiguration(this.configFile);

        // 初始化配置文件
        if (!this.configFile.exists()) {
            try {
                this.configFile.createNewFile();
                // 设置默认值
                Config.addDefault("SleepTime", 60);
                Config.addDefault("Prefix", "[扫地机器人] ");
                Config.addDefault("Message", true);
                Config.options().copyDefaults(true);
                Config.save(configFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        
        // 注册命令
        this.getCommand("roomrobot").setExecutor(this);

        // 使用合适的调度器
        scheduleCleanTask();
        getLogger().info("扫地机器人插件加载完成！");
    }

    private void detectServerType() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            isFolia = true;
            getLogger().info("检测到 Folia 服务器，将使用 Folia 调度器");
        } catch (ClassNotFoundException e) {
            isFolia = false;
            getLogger().info("检测到 Bukkit 服务器，将使用 Bukkit 调度器");
        }
    }

    private void scheduleCleanTask() {
        // 从配置文件中读取SleepTime
        int sleepTime = Config.getInt("SleepTime") * 20;
        
        if (isFolia) {
            // 使用Folia的全局区域调度器
            getServer().getGlobalRegionScheduler().runAtFixedRate(this, task -> {
                executeCleanTask();
            }, sleepTime, sleepTime);
        } else {
            // 使用Bukkit的传统调度器
            new Clean().runTaskTimer(this, sleepTime, sleepTime);
        }
    }

    private void executeCleanTask() {
        CommandSender console = Bukkit.getConsoleSender();
        Bukkit.getServer().dispatchCommand(console, "kill @e[type=minecraft:item]");
        String prefix = Config.getString("Prefix");
        Boolean message = Config.getBoolean("Message");
        if (message) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(prefix+"§7扫地机器人正在全力清扫...§r");
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("roomrobot")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadConfigFile();
                String prefix = Config.getString("Prefix");
                sender.sendMessage(prefix+"§a扫地机器人配置已重载！");
                return true;
            }
            if (args.length > 0 && args[0].equalsIgnoreCase("version")) {
                String prefix = Config.getString("Prefix");
                sender.sendMessage(prefix+"§a当前插件版本：1.3");
                sender.sendMessage(prefix+"§a插件作者：@YeyingXingchen");
                sender.sendMessage(prefix+"§a插件官方群：1018502028");
                return true;
            }
        }
        return false;
    }

    private void reloadConfigFile() {
        if (this.configFile == null) {
            this.configFile = new File(this.getDataFolder(), "config.yml");
        }

        this.Config = YamlConfiguration.loadConfiguration(this.configFile);

        // 如果配置文件不存在，重新创建并设置默认值
        if (!this.configFile.exists()) {
            try {
                this.configFile.createNewFile();
                Config.addDefault("SleepTime", 60);
                Config.addDefault("Prefix", "[扫地机器人] ");
                Config.addDefault("Message", true);
                Config.options().copyDefaults(true);
                Config.save(configFile);
            } catch (IOException e) {
                getLogger().severe("无法创建配置文件: " + e.getMessage());
            }
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("正在卸载 扫地机器人插件...");
        try {
            Config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        getLogger().info("卸载成功！感谢使用！");
    }

    private class Clean extends BukkitRunnable {
        @Override
        public void run() {
            executeCleanTask();
        }
    }
}
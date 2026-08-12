package com.foliarace.plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.List;

final class FoliaRaceCommand implements TabExecutor {
    private final FoliaRacePlugin plugin;

    FoliaRaceCommand(FoliaRacePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String action = args.length == 0 ? "status" : args[0].toLowerCase(java.util.Locale.ROOT);
        switch (action) {
            case "status" -> {
                sender.sendMessage(ChatColor.AQUA + "FoliaRace " + ChatColor.WHITE + plugin.statusLine());
                return true;
            }
            case "start" -> {
                sender.sendMessage(ChatColor.GREEN + plugin.startSession(args.length > 1 ? args[1] : "manual"));
                return true;
            }
            case "stop" -> {
                sender.sendMessage(ChatColor.YELLOW + plugin.stopSession());
                return true;
            }
            case "flush" -> {
                sender.sendMessage(ChatColor.GREEN + plugin.flushReport());
                return true;
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Usage: /foliarace <status|start|stop|flush>");
                return false;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("status", "start", "stop", "flush").stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase(java.util.Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }
}

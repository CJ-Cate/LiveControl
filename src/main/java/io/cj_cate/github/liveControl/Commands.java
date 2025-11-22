package io.cj_cate.github.liveControl;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Commands implements CommandExecutor, TabCompleter {

    private void SendMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.getPrefix() + message));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l/livecontrol §7(or §6/lc§7)");
        sender.sendMessage("§e/lc §7or §e/lc check§7: See the remaining time");
        sender.sendMessage("§e/lc open [positive integer, blank for infinite]");
        sender.sendMessage("§e/lc close§7: Stop the ability to join the server");
        sender.sendMessage("§e/lc extend <int minutes> [notify=1] [force]");
        sender.sendMessage("§e/lc freeze [notify=true]");
        sender.sendMessage("§e/lc warn [sound=true] [title=false]");
        sender.sendMessage("§e/lc kickall§7: If server is closed, forcefully kick all non-bypassed players");
        sender.sendMessage("§e/lc bypass <add/remove> <name> [temporary=0]");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // Special Cases (user commands)
        if(args.length == 0 || args[0].equalsIgnoreCase("check")) {
            SendMessage(sender, Util.formatTime(Main.getTimeRemaining()));
            return true;
        }
        if(args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }
        // Admin commands only from here and below
        if (sender instanceof Player p && !p.hasPermission("livecontrol.admin")) {
            p.sendMessage("You do not have permission to use this command.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "debug" -> {
                SendMessage(sender, "isWhitelistEnforced: " + Main.getMain().getServer().isWhitelistEnforced());
                SendMessage(sender, "hasWhitelist: " + Main.getMain().getServer().hasWhitelist());
            }
            case "open" -> {
                Main.getMain().getServer().setWhitelistEnforced(false);
                SendMessage(sender, "Opened the server; whitelist disabled.");
            }
            case "close" -> {
                Main.getMain().getServer().setWhitelistEnforced(true);
                SendMessage(sender, "The server is closed; whitelisted enabled.");
            }
            case "kickall" -> {
                // Kick all players who are not on the whitelist
                Util.kickAllNonWhitelistedPlayers();
            }
            case "freeze" -> {
                SendMessage(sender, "The clock has been frozen.");
                Main.setFrozen(true);
            }
            case "unfreeze" -> {
                SendMessage(sender, "The clock has been unfrozen.");
                Main.setFrozen(false);
            }
            case "warn" -> {
                Util.broadcast("The remaining time on the server is: " + Util.formatTime(Main.getTimeRemaining()));
                SendMessage(sender, "Warning sent");
            }
            case "extend" -> {
                int extension_time = 0;
                try {
                    extension_time = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    SendMessage(sender, "Invalid argument of '" + args[1] + "'.");
                    return true; // break
                }
                // Minutes to milliseconds
                extension_time = extension_time*60_000;

                if(Main.getMain().getServer().hasWhitelist()) {
                    Main.getMain().getServer().setWhitelist(false);
                    SendMessage(sender, "Whitelist disabled to allow people to join");
                }
                // If net time is under zero (which would happen if its closed, or we subtract too much) then set it to zero
                if(Main.getTimeRemaining() - extension_time < 0) {
                    Main.setClosingTime(System.currentTimeMillis());
                    SendMessage(sender, "Negative time detected, setting closing time to Now.");
                } else {
                    Main.changeClosingTime(extension_time);
                }

                SendMessage(sender, "The server has been extended by " + Util.formatTime(extension_time));
                SendMessage(sender, "Total time remaining: " + Util.formatTime(Main.getTimeRemaining()));
            }
            default -> {
                // could use SendMessage but i like this nya~
                if(sender instanceof Player p) {
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4Meow, not a command :3"));
                } else {
                    Main.log("&4LC: Unknown command");
                }
            }
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Main subcommands
            List<String> subcommands = Arrays.asList("help", "check", "open", "close", "extend", "freeze", "warn", "kickall", "bypass");
            completions = subcommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String subcommand = args[0].toLowerCase();

            switch (subcommand) {
                case "open":
                    // Suggest time in minutes or "0" for indefinite
                    completions = Arrays.asList("0", "30", "60", "120", "180");
                    break;
                case "extend":
                    // Suggest time values
                    completions = Arrays.asList("15", "30", "60", "-15", "-30");
                    break;
                case "freeze":
                    // Suggest notify boolean
                    completions = Arrays.asList("true", "false", "1", "0");
                    break;
                case "warn":
                    // Suggest title boolean
                    completions = Arrays.asList("true", "false", "1", "0");
                    break;
                case "bypass":
                    // Suggest add/remove
                    completions = Arrays.asList("add", "remove");
                    break;
            }

            completions = completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 3) {
            String subcommand = args[0].toLowerCase();

            switch (subcommand) {
                case "extend":
                    // Suggest force boolean
                    completions = Arrays.asList("true", "false", "1", "0");
                    break;
                case "warn":
                    // Suggest sound boolean
                    completions = Arrays.asList("true", "false", "1", "0");
                    break;
                case "bypass":
                    // Suggest online player names
                    completions = sender.getServer().getOnlinePlayers().stream()
                            .map(p -> p.getName())
                            .collect(Collectors.toList());
                    break;
            }

            completions = completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 4) {
            String subcommand = args[0].toLowerCase();

            if (subcommand.equals("bypass")) {
                // Suggest temporary boolean
                completions = Arrays.asList("true", "false", "1", "0");
                completions = completions.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return completions;
    }
}
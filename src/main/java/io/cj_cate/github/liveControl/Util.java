package io.cj_cate.github.liveControl;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Util {

    // Format time to human readable
    public static String formatTime(long milliseconds) {
        long totalSeconds = milliseconds / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return hours + " hours:" + minutes + " minutes:" + seconds + " seconds";
    }

    public static void kickAllNonWhitelistedPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isWhitelisted()) {
                player.kickPlayer("Thanks for playing, see you soon! :)");
            }
        }
    }

    public static void broadcast(String... message) {
        StringBuilder messageBuilder = new StringBuilder();
        for (String s : message) {
            messageBuilder.append(s).append("\n");
        }
        String finalMessage = messageBuilder.toString();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Main.getPrefix() + finalMessage);
        }
    }

    // Magic numbers are for fadeIn, stay, fadeOut (in ticks)
    public static void sendSubtitleToAll(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle("", ChatColor.RED + message, 10, 70, 20);
        }
    }
}

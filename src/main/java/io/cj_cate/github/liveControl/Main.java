package io.cj_cate.github.liveControl;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Stack;
import java.util.function.Consumer;

/**
 * This plugin is not meant to be an exact timer to control the flower of players. It is intended to be
 * a "mostly accurate" timer (within ten seconds) that controls when people can join, with some eye candy
 * and handy functionality built it. It is built on the back of vanilla whitelist, so it will probably not
 * work with any other
 */

public final class Main extends JavaPlugin {

    private static Main mainn;
    public static Main getMain() { return mainn; }

    private static boolean isFrozen = false;
    public static void setFrozen(boolean frozen) { isFrozen = frozen; }
    private int reminder_counter = 0;
    private int reminder_cycle = 12; //TODO: change to read from config like (value * 6)
    private int offset = 0;

    // The time that the server will close in unix time millis.
    private static long closingTime = 0; // set in onEnable
    public static long getClosingTime() { return closingTime; }
    public static void changeClosingTime(long millisecond_change) { closingTime += millisecond_change; }
    public static void setClosingTime(long millisecond_time) { closingTime = millisecond_time; }
    public static long getTimeRemaining() { return closingTime - System.currentTimeMillis(); }

    // Used to schedule events that may need to be overwritten.
    private static Stack<ScheduledTask> responseStack = new Stack<>();
    public static void clearResponseStack() { responseStack.clear(); }

    public static String getPrefix() {
        return "§8§l[§6§lLC§8§l] §7§l| §e";
    }


    // Debug log
    public static void log(String message) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&d" + message));
    }

    // Get config
    public static <T> Object config(String path) {
        Object obj = mainn.getConfig().get(path);
        return (obj == null) ? null : (T) obj;
    }

    @Override
    public void onLoad() {
        mainn = this;
    }

    @Override
    public void onEnable() {
        log("Livecontrol is starting!");

        this.getServer().setWhitelist(false);

        closingTime = System.currentTimeMillis() + (1000 * 2 * 60); //TODO: change to read from config
        Commands cmds = new Commands();
        getCommand("livecontrol").setTabCompleter(cmds);
        getCommand("livecontrol").setExecutor(cmds);
        getCommand("lc").setExecutor(cmds);
        getCommand("lc").setTabCompleter(cmds);



        // This will run every ten seconds while the server is live
        Bukkit.getScheduler().runTaskTimer(this, () -> {

            // If the clock is "frozen", then:
            // - add ten seconds to the closing time
            // - add ten seconds to the offset
            // - every 2 minutes (12 cycles) remind the online players that the time is frozen
            if (isFrozen) {
                changeClosingTime(+10_000);
                offset += 10_000;

                reminder_counter++;
                if(reminder_counter % reminder_cycle == 0) {
                    reminder_counter = 0;
                    for(Player p : Bukkit.getOnlinePlayers()) {
                        p.sendMessage(ChatColor.AQUA + "" + ChatColor.ITALIC + "The clock is frozen");
                    }
                }
            } else {
                // If the clock is not frozen, then lets check on the reponse stack to do things like:
                // - close the server
                // - issue warnings about the server closing
                if(!responseStack.isEmpty() && System.currentTimeMillis() > (responseStack.peek().scheduledTime + offset)) {
                    ScheduledTask task = responseStack.pop();
                    task.function.run();
                }
            }

            // This will proc when it is time for the server to close.
            // When the time is past adjusted closing, the stack is empty, and whitelist is off.
            // when time is added, the stack is emptied and this is reset.
            if(System.currentTimeMillis() > closingTime && responseStack.isEmpty() && !Main.getMain().getServer().hasWhitelist()) {
                Main.log("LC Debug: The server has procced closing time");

                // begin final countdown
                Main.getMain().getServer().setWhitelist(true);
                long closing_grace_minutes = 5 * 60_000; //TODO: read from config
                // These are calculated by closing_grace_minutes * (1 - fraction)
                long fifth = (long) (closing_grace_minutes * 0.2) * 60_000;
                long half =  (long) (closing_grace_minutes * 0.5) * 60_000;

                Util.broadcast(
                    "The server is closing! Whitelist has been turned ON!",
                    "In " + closing_grace_minutes + " minutes you will be kicked!",
                    "Start funneling out please :)"
                );

                // Reset offset so that it effectively only counts time when its closing time
                offset = 0; // freeze feature

                // Server closed goodbye everyone! (added in reverse order because of stack FILO)
                responseStack.add(new ScheduledTask(System.currentTimeMillis() + offset + closing_grace_minutes,
                        () -> {
                            Main.log("LC Debug: Server has reached closing time");
                            Util.broadcast("Goodbye everyone!");
                            Util.kickAllNonWhitelistedPlayers();
                        }));
                responseStack.add(new ScheduledTask(System.currentTimeMillis() + offset + (closing_grace_minutes - fifth),
                        () -> {
                            Main.log("LC Debug: server a fifth of the way to closing");
                            Util.sendSubtitleToAll("The server closes in " + fifth + " minutes!");
                            Util.broadcast("The server closes in " + fifth + " minutes!");
                            Util.broadcast("You will be kicked with no further warning!");
                        }));
                responseStack.add(new ScheduledTask(System.currentTimeMillis() + offset + (closing_grace_minutes - half) ,
                        () -> {
                            Main.log("LC Debug: server half way to closing");
                            Util.sendSubtitleToAll("The server closes in " + half + " minutes!");
                            Util.broadcast("The server closes in " + half + " minutes!");
                        }));

            }
        }, 0L, 200L); // check every ten seconds

    }

    @Override
    public void onDisable() {
        log("Livecontrol is stopping!");
    }
}

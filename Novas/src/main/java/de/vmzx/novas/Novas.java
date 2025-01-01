package de.vmzx.novas;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public final class Novas extends JavaPlugin implements TabExecutor, TabCompleter, Listener {

    private final Map<String, Integer> novaBalances = new HashMap<>();
    private final String prefix = ChatColor.translateAlternateColorCodes('&', "&5&lNova&b&lCraft &8» &f");

    // On enable ist glaub klar wenn das plugin enabled wird wird das enabled halt nh lul
    @Override
    public void onEnable() {
        getCommand("novas").setExecutor(this);
        getCommand("novas").setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new NovasPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI support enabled.");
        }

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String uuid = player.getUniqueId().toString();
                novaBalances.put(uuid, novaBalances.getOrDefault(uuid, 0) + 1);
                player.sendMessage(prefix + " Du hast 1 Nova erhalten! Deine aktuellen Novas: " + novaBalances.get(uuid));
            }
        }, 0L, 72000L);
    }

    @Override
    public void onDisable() {
        novaBalances.clear();
    }
    // unsere einfache zusammen gewürfelte on Command mit allen commands
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("novas")) {
            if (args.length == 0) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    String uuid = player.getUniqueId().toString();
                    int balance = novaBalances.getOrDefault(uuid, 0);
                    player.sendMessage(prefix + " Du hast " + balance + " Nova's.");
                } else {
                    sender.sendMessage(prefix + " Dieser Befehl ist nur für Spieler verfügbar.");
                }
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(prefix + " Nutzung: /novas add/remove <Spieler> <Betrag>");
                return true;
            }

            String action = args[0].toLowerCase();
            String targetPlayer = args[1];
            int amount;

            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(prefix + " Der Betrag muss eine gültige Zahl sein.");
                return true;
            }

            Player player = Bukkit.getPlayer(targetPlayer);
            if (player == null) {
                sender.sendMessage(prefix + " Der Spieler ist nicht online.");
                return true;
            }

            String targetUUID = player.getUniqueId().toString();

            switch (action) {
                case "add":
                    if (!sender.hasPermission("Novas.add")) {
                        sender.sendMessage(prefix + " Du hast keine Berechtigung für diesen Befehl.");
                        return true;
                    }
                    novaBalances.put(targetUUID, novaBalances.getOrDefault(targetUUID, 0) + amount);
                    sender.sendMessage(prefix + " " + amount + " Nova's wurden zu " + player.getName() + " hinzugefügt.");
                    break;

                case "remove":
                    if (!sender.hasPermission("Novas.remove")) {
                        sender.sendMessage(prefix + " Du hast keine Berechtigung für diesen Befehl.");
                        return true;
                    }
                    int currentBalance = novaBalances.getOrDefault(targetUUID, 0);
                    if (currentBalance < amount) {
                        sender.sendMessage(prefix + " Der Spieler hat nicht genug Nova's.");
                        return true;
                    }
                    novaBalances.put(targetUUID, currentBalance - amount);
                    sender.sendMessage(prefix + " " + amount + " Nova's wurden von " + player.getName() + " entfernt.");
                    break;

                default:
                    sender.sendMessage(prefix + " Ungültige Aktion. Nutzung: /novas add/remove <Spieler> <Betrag>");
                    break;
            }

            return true;
        }

        return false;
    }
    // Der Tabcompleter das wichtig! lulzhaha
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("novas")) {
            if (args.length == 1) {
                return List.of("add", "remove").stream()
                        .filter(sub -> sub.startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args.length == 2) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return null;
    }
    // Hier haben wir unseren Blockbreak even wenn wir einen block braken wird random was getryed ob wir was kriegen oder nicht
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        Material blockType = event.getBlock().getType();

        if (blockType == Material.DIAMOND_ORE || blockType == Material.EMERALD_ORE || blockType == Material.GOLD_ORE) {
            if (new Random().nextInt(100) < 25) {
                novaBalances.put(uuid, novaBalances.getOrDefault(uuid, 0) + 1);
                player.sendMessage(prefix + " Du hast eine Nova durch das Abbauen von " + blockType.name() + " erhalten!");
            }
        }
    }

    public int getBalance(String uuid) {
        return novaBalances.getOrDefault(uuid, 0);
    }
}
// hier machen wir unseren placeholder in placeholder api das geile
class NovasPlaceholderExpansion extends me.clip.placeholderapi.expansion.PlaceholderExpansion {

    private final Novas plugin;

    public NovasPlaceholderExpansion(Novas plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "novas";
    }

    @Override
    public String getAuthor() {
        return "VMZX";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }

        if (params.equalsIgnoreCase("balance")) {
            return String.valueOf(plugin.getBalance(player.getUniqueId().toString()));
        }

        return null;
    }
}

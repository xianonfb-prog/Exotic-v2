package com.exotic.plugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler implements CommandExecutor, TabCompleter {

    private final ExoticPlugin plugin;

    public CommandHandler(ExoticPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /exotic <trial|cancel|complete|give|list> ...", NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "trial" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /exotic trial <swordId> <player>", NamedTextColor.RED));
                    return true;
                }
                ExoticItem item = ExoticItem.byId(args[1]);
                if (item == null) {
                    sender.sendMessage(Component.text("Unknown item id: " + args[1], NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found or offline: " + args[2], NamedTextColor.RED));
                    return true;
                }
                boolean started = plugin.trials().start(target, item);
                if (started) {
                    sender.sendMessage(Component.text("Started " + item.styledName() + " trial for " + target.getName(), NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text(target.getName() + " already has an active trial.", NamedTextColor.RED));
                }
                return true;
            }
            case "cancel" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /exotic cancel <player>", NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found or offline: " + args[1], NamedTextColor.RED));
                    return true;
                }
                plugin.trials().cancel(target);
                sender.sendMessage(Component.text("Cancelled active trial for " + target.getName(), NamedTextColor.YELLOW));
                return true;
            }
            case "complete" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /exotic complete <player>", NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found or offline: " + args[1], NamedTextColor.RED));
                    return true;
                }
                plugin.trials().forceComplete(target);
                sender.sendMessage(Component.text("Force-completed active trial for " + target.getName(), NamedTextColor.GREEN));
                return true;
            }
            case "give" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /exotic give <swordId> <player>", NamedTextColor.RED));
                    return true;
                }
                ExoticItem type = ExoticItem.byId(args[1]);
                if (type == null) {
                    sender.sendMessage(Component.text("Unknown item id: " + args[1], NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found or offline: " + args[2], NamedTextColor.RED));
                    return true;
                }
                ItemStack built = type.build();
                SwordUtil.bindToOwner(built, target.getUniqueId());
                target.getInventory().addItem(built);
                sender.sendMessage(Component.text("Gave " + type.styledName() + " to " + target.getName(), NamedTextColor.GREEN));
                return true;
            }
            case "list" -> {
                sender.sendMessage(Component.text("Exotic items:", NamedTextColor.GOLD));
                for (SwordType st : SwordType.values()) {
                    sender.sendMessage(Component.text(" - " + st.id() + ": " + st.displayName(), NamedTextColor.GRAY));
                }
                for (TomeType tt : TomeType.values()) {
                    sender.sendMessage(Component.text(" - " + tt.id() + ": " + tt.displayName(), NamedTextColor.GRAY));
                }
                return true;
            }
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand. Usage: /exotic <trial|cancel|complete|give|list>", NamedTextColor.RED));
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(List.of("trial", "cancel", "complete", "give", "list"));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("trial") || args[0].equalsIgnoreCase("give"))) {
            for (SwordType type : SwordType.values()) options.add(type.id());
            for (TomeType type : TomeType.values()) options.add(type.id());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("complete"))) {
            Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("trial") || args[0].equalsIgnoreCase("give"))) {
            Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
        }
        String current = args[args.length - 1].toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(current)).collect(Collectors.toList());
    }
}

package live.fewer.lpc;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class FewerLPC extends JavaPlugin implements Listener {

	LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
	LegacyComponentSerializer rgbSerializer = LegacyComponentSerializer.legacyAmpersand().toBuilder().hexColors().build();
	private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

	private LuckPerms luckPerms;
	
	@Override
	public void onEnable() {
		// Load an instance of 'LuckPerms' using the services manager.
		this.luckPerms = getServer().getServicesManager().load(LuckPerms.class);

		saveDefaultConfig();
		getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
		if (args.length == 1 && "reload".equals(args[0])) {
			reloadConfig();

			sender.sendMessage(colorize("&aLPC has been reloaded."));
			return true;
		}

		return false;
	}

	@Override
	public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
		if (args.length == 1)
			return Collections.singletonList("reload");

		return new ArrayList<>();
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onChat(AsyncChatEvent event) {
		Component message = event.message();
		final Player player = event.getPlayer();

//		// Get a LuckPerms cached metadata for the player.
		final CachedMetaData metaData = this.luckPerms.getPlayerAdapter(Player.class).getMetaData(player);
		final String group = metaData.getPrimaryGroup();

		String format = getConfig().getString(getConfig().getString("group-formats." + group) != null ? "group-formats." + group : "chat-format")
				.replace("{prefix}", metaData.getPrefix() != null ? metaData.getPrefix() : "")
				.replace("{suffix}", metaData.getSuffix() != null ? metaData.getSuffix() : "")
				.replace("{prefixes}", metaData.getPrefixes().keySet().stream().map(key -> metaData.getPrefixes().get(key)).collect(Collectors.joining()))
				.replace("{suffixes}", metaData.getSuffixes().keySet().stream().map(key -> metaData.getSuffixes().get(key)).collect(Collectors.joining()))
				.replace("{world}", player.getWorld().getName())
				.replace("{name}", player.getName())
//				.replace("{displayname}", player.getDisplayName())
				.replace("{username-color}", metaData.getMetaValue("username-color") != null ? metaData.getMetaValue("username-color") : "")
				.replace("{message-color}", metaData.getMetaValue("message-color") != null ? metaData.getMetaValue("message-color") : "");


//		System.out.println();
//		System.out.println(2);
////		System.out.println(format);
//		System.out.println(PlaceholderAPI.setPlaceholders(player, "%fewerlocalchat_vault_prefix%"));

		String formatWithPAPI;
		if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			formatWithPAPI = PlaceholderAPI.setPlaceholders(player, format);
		} else {
			formatWithPAPI = format;
		}

		String hexAdjusted = adjustHexToModernFormat(formatWithPAPI);

		boolean hasColorPerms = player.hasPermission("lpc.colorcodes");
		boolean hasRgbPerms = hasColorPerms && player.hasPermission("lpc.rgbcodes");

		System.out.println(hexAdjusted);

		Component coloredMessage = message;
		if (hasRgbPerms) coloredMessage = rgbSerializer.deserialize(serializer.serialize(message));
		else if (hasColorPerms) coloredMessage = serializer.deserialize(serializer.serialize(message));

		ChatRenderer componentFormat = (source, sourceDisplayName, inputMsg, ignoreAudience) -> {
			return rgbSerializer.deserialize(hexAdjusted)
					.replaceText(replacer -> {
						replacer.matchLiteral("{displayname}");
						replacer.replacement(sourceDisplayName);
					})
					.replaceText(replacer -> {
						replacer.matchLiteral("{message}");
						replacer.replacement(inputMsg);
					});
		};

		event.renderer(componentFormat);
		event.message(coloredMessage);
	}

	private String adjustHexToModernFormat(String formatWithPAPI) {
		return formatWithPAPI.replaceAll(
				"[&§]x[&§]([0-9a-zA-Z])[&§]([0-9a-zA-Z])[&§]([0-9a-zA-Z])[&§]([0-9a-zA-Z])[&§]([0-9a-zA-Z])[&§]([0-9a-zA-Z])",
				"&#$1$2$3$4$5$6");
	}

	private String colorize(final String message) {
		return ChatColor.translateAlternateColorCodes('&', message);
	}

	private String translateHexColorCodes(final String message) {
		final char colorChar = ChatColor.COLOR_CHAR;

		final Matcher matcher = HEX_PATTERN.matcher(message);
		final StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);

		while (matcher.find()) {
			final String group = matcher.group(1);

			matcher.appendReplacement(buffer, colorChar + "x"
					+ colorChar + group.charAt(0) + colorChar + group.charAt(1)
					+ colorChar + group.charAt(2) + colorChar + group.charAt(3)
					+ colorChar + group.charAt(4) + colorChar + group.charAt(5));
		}

		return matcher.appendTail(buffer).toString();
	}
}
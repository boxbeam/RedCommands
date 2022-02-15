package redempt.redlib.commandmanager;

import org.bukkit.plugin.Plugin;
import redempt.redlib.commandmanager.processing.CommandProcessUtils;
import redempt.redlib.misc.FormatUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents a list of messages loaded from a file with defaults
 * @author Redempt
 *
 */
public class Messages {
	
	private static Map<Plugin, Messages> all = new HashMap<>();
	private static Pattern placeholderPattern = Pattern.compile("%\\w+%");
	
	/**
	 * @return The default pattern to match placeholders with
	 */
	public static Pattern getDefaultPlaceholderPattern() {
		return placeholderPattern;
	}
	
	/**
	 * Loads messages from a file and writes missing defaults
	 * @param defaults The InputStream for default messages. Use {@link Plugin#getResource(String)} for this.
	 * @param path The path of the file in the plugin folder to load messages from
	 * @param placeholderPattern The regex pattern to match placeholders
	 * @return The Messages instance with messages loaded.
	 */
	public static Messages load(InputStream defaults, Path path, Pattern placeholderPattern) {
		try {
			Map<String, String> messages = Files.exists(path) ? parse(Files.readAllLines(path)) : new LinkedHashMap<>();
			BufferedReader reader = new BufferedReader(new InputStreamReader(defaults));
			List<String> lines = new ArrayList<>();
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
			Map<String, String> defaultMap = parse(lines);
			boolean[] missing = {false};
			defaultMap.forEach((k, v) -> {
				if (!messages.containsKey(k)) {
					messages.put(k, v);
					missing[0] = true;
				}
			});
			if (missing[0]) {
				write(messages, path);
			}
			Map<String, Message> messageMap = new HashMap<>();
			for (String key : defaultMap.keySet()) {
				messageMap.put(key, new Message(messages.get(key), defaultMap.get(key), placeholderPattern));
			}
			return new Messages(null, messageMap);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Loads messages from a file and writes missing defaults
	 * @param defaults The InputStream for default messages. Use {@link Plugin#getResource(String)} for this.
	 * @param path The path of the file in the plugin folder to load messages from
	 * @return The Messages instance with messages loaded.
	 */
	public static Messages load(InputStream defaults, Path path) {
		return load(defaults, path, placeholderPattern);
	}
	
	/**
	 * Loads messages from a file and writes missing defaults
	 * @param plugin The plugin loading the messages
	 * @param defaults The InputStream for default messages. Use {@link Plugin#getResource(String)} for this.
	 * @param filename The name of the file in the plugin folder to load messages from
	 * @param placeholderPattern The regex pattern to match placeholders
	 * @return The Messages instance with messages loaded.
	 */
	public static Messages load(Plugin plugin, InputStream defaults, String filename, Pattern placeholderPattern) {
		Path path = plugin.getDataFolder().toPath().resolve(filename);
		Messages messages = load(defaults, path, placeholderPattern);
		messages.plugin = plugin;
		all.put(plugin, messages);
		return messages;
	}
	
	/**
	 * Loads messages from a file and writes missing defaults
	 * @param plugin The plugin loading the messages
	 * @param defaults The InputStream for default messages. Use {@link Plugin#getResource(String)} for this.
	 * @param filename The name of the file in the plugin folder to load messages from
	 * @return The Messages instance with messages loaded.
	 */
	public static Messages load(Plugin plugin, InputStream defaults, String filename) {
		return load(plugin, defaults, filename, placeholderPattern);
	}
	
	/**
	 * Gets the Messages which have been loaded for a specific plugin, if they exist
	 * @param plugin The plugin to get the Messages for
	 * @return The Messages object, or null
	 */
	public static Messages getLoaded(Plugin plugin) {
		return all.get(plugin);
	}
	
	/**
	 * Loads messages from a file, messages.txt, and writes missing defaults
	 * @param plugin The plugin loading the messages
	 * @param defaults The InputStream for default messages. Use {@link Plugin#getResource(String)} for this.
	 * @param placeholderPattern The regex pattern to match placeholders
	 * @return The Messages instance with messages loaded.
	 */
	public static Messages load(Plugin plugin, InputStream defaults, Pattern placeholderPattern) {
		return load(plugin, defaults, "messages.txt", placeholderPattern);
	}
	
	/**
	 * Loads messages from a file, messages.txt, and writes missing defaults
	 * @param plugin The plugin loading the messages
	 * @param defaults The InputStream for default messages. Use {@link Plugin#getResource(String)} for this.
	 * @return The Messages instance with messages loaded.
	 */
	public static Messages load(Plugin plugin, InputStream defaults) {
		return load(plugin, defaults, "messages.txt", placeholderPattern);
	}
	
	/**
	 * Loads messages from a file, messages.txt, and writes missing defaults loaded from the plugin resource called messages.txt
	 * @param plugin The plugin loading the messages
	 * @param placeholderPattern The regex pattern to match placeholders
	 * @return The Messages instance with messages loaded.
	 */
	public static Messages load(Plugin plugin, Pattern placeholderPattern) {
		return load(plugin, plugin.getResource("messages.txt"), "messages.txt", placeholderPattern);
	}
	
	/**
	 * Loads messages from a file, messages.txt, and writes missing defaults loaded from the plugin resource called messages.txt
	 * @param plugin The plugin loading the messages
	 * @return The Messages instance with messages loaded.
	 */
	public static Messages load(Plugin plugin) {
		return load(plugin, plugin.getResource("messages.txt"), "messages.txt", placeholderPattern);
	}
	
	/**
	 * Determines which plugin is calling this method, finds its loaded messages, and returns the message with the given name.
	 * @param message The name of the message
	 * @return The message, which has been formatted according to the formatter of the Messages object
	 * @throws IllegalStateException if your plugin has not loaded any messages
	 */
	public static String msg(String message) {
		Plugin plugin = CommandProcessUtils.getCallingPlugin();
		Messages msgs = all.get(plugin);
		if (msgs == null) {
			throw new IllegalStateException("Your plugin has not loaded any messages, or this method is being called from the wrong plugin");
		}
		return msgs.get(message);
	}
	
	/**
	 * Determines which plugin is calling this method, finds its loaded messages, and returns the message with the given name, replacing placeholders
	 * @param message The name of the message
	 * @param placeholderValues Values for the placeholders in the message
	 * @return The message, which has been formatted according to the formatter of the Messages object and has placeholders replaced
	 * @throws IllegalStateException if your plugin has not loaded any messages
	 */
	public static String msgReplace(String message, String... placeholderValues) {
		Plugin plugin = CommandProcessUtils.getCallingPlugin();
		Messages msgs = all.get(plugin);
		if (msgs == null) {
			throw new IllegalStateException("Your plugin has not loaded any messages, or this method is being called from the wrong plugin");
		}
		return msgs.getAndReplace(message, placeholderValues);
	}
	
	private static Map<String, String> parse(List<String> input) {
		Map<String, String> map = new LinkedHashMap<>();
		for (String s : input) {
			if (s == null) {
				break;
			}
			int index = s.indexOf(':');
			map.put(s.substring(0, index), s.substring(index + 1).trim());
		}
		return map;
	}
	
	private static void write(Map<String, String> map, java.nio.file.Path file) {
		List<String> lines = map.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.toList());
		try {
			if (!Files.exists(file.getParent())) {
				Files.createDirectories(file.getParent());
			}
			Files.write(file, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Plugin plugin;
	private Map<String, Message> messages;
	private UnaryOperator<String> formatter = FormatUtils::color;
	
	private Messages(Plugin plugin, Map<String, Message> messages) {
		this.messages = messages;
		this.plugin = plugin;
		applyFormat();
	}
	
	private void applyFormat() {
		messages.values().forEach(m -> {
			m.setFormatted(formatter.apply(m.getValue()));
		});
	}
	
	/**
	 * Sets the function which will be used to format message strings before they are returned
	 * @param formatter The function to format messages
	 * @return Itself
	 */
	public Messages setFormatter(UnaryOperator<String> formatter) {
		this.formatter = formatter;
		applyFormat();
		return this;
	}
	
	/**
	 * @return The plugin these messages belong to
	 */
	public Plugin getPlugin() {
		return plugin;
	}
	
	/**
	 * Gets a formatted message by name
	 * @param msg The name of the message
	 * @return The message, which has been formatted according to the formatter function of this Messages
	 */
	public String get(String msg) {
		Message message = messages.get(msg);
		if (message == null) {
			throw new IllegalArgumentException("Message '" + msg + "' does not have an assigned or default value!");
		}
		return message.getFormattedValue();
	}
	
	/**
	 * Gets a formatted message by name, replacing placeholders in it
	 * @param msg The name of the message
	 * @param placeholderValues The values for the placeholders in the message, in the order they appear in the default value
	 * @return The formatted message with its placeholders replaced
	 */
	public String getAndReplace(String msg, String... placeholderValues) {
		Message message = getMessage(msg);
		List<String> placeholders = message.getPlaceholders();
		if (placeholderValues.length != placeholders.size()) {
			throw new IllegalArgumentException("Expected exactly " + placeholders.size() + " placeholder values, got " + placeholderValues.length);
		}
		String val = message.getFormattedValue();
		for (int i = 0; i < placeholders.size(); i++) {
			val = val.replace(placeholders.get(i), placeholderValues[i]);
		}
		return val;
	}
	
	/**
	 * Gets the raw Message object by name
	 * @param msg The name of the message object
	 * @return The Message, or null
	 */
	public Message getMessage(String msg) {
		return messages.get(msg);
	}
	
}

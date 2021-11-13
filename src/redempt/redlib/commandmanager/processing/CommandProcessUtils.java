package redempt.redlib.commandmanager.processing;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import redempt.redlib.commandmanager.ArgType;
import redempt.redlib.commandmanager.Command;
import redempt.redlib.commandmanager.CommandParser;
import redempt.redlib.commandmanager.Messages;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

public class CommandProcessUtils {
	
	private static Messages globalMessages;
	
	static {
		Path path = Paths.get("plugins/RedLib/command-format.txt");
		globalMessages = Messages.load(CommandParser.class.getClassLoader().getResourceAsStream("command-format.txt"), path);
	}
	
	public static Messages getCommandManagerMessages() {
		return globalMessages;
	}
	
	public static String msg(String message) {
		return globalMessages.get(message);
	}
	
	public static Plugin getCallingPlugin() {
		Exception ex = new Exception();
		try {
			Class<?> clazz = Class.forName(ex.getStackTrace()[2].getClassName());
			Plugin plugin = JavaPlugin.getProvidingPlugin(clazz);
			return plugin.isEnabled() ? plugin : Bukkit.getPluginManager().getPlugin(plugin.getName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static SimpleCommandMap getCommandMap() {
		try {
			Field field = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
			field.setAccessible(true);
			return (SimpleCommandMap) field.get(Bukkit.getPluginManager());
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static Map<String, org.bukkit.command.Command> getKnownCommands(SimpleCommandMap commandMap) {
		try {
			Class<?> clazz = commandMap.getClass();
			while (!clazz.getSimpleName().equals("SimpleCommandMap")) {
				clazz = clazz.getSuperclass();
			}
			Field mapField = clazz.getDeclaredField("knownCommands");
			mapField.setAccessible(true);
			return (Map<String, org.bukkit.command.Command>) mapField.get(commandMap);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static List<ArgType<?>> getBaseArgTypes() {
		List<ArgType<?>> types = new ArrayList<>();
		types.add(new ArgType<>("int", (Function<String, Integer>) Integer::parseInt).constraint(msg("numberOutsideRange"), (c, v) -> {
			String[] split = c.split(",");
			int min = split[0].length() == 0 ? Integer.MIN_VALUE : Integer.parseInt(split[0]);
			int max = split[1].length() == 0 ? Integer.MAX_VALUE : Integer.parseInt(split[1]);
			return v >= min && v <= max;
		}));
		types.add(new ArgType<>("double", Double::parseDouble).constraint(msg("numberOutsideRange"), (c, v) -> {
			String[] split = c.split(",");
			double min = split[0].length() == 0 ? Double.MIN_VALUE : Double.parseDouble(split[0]);
			double max = split[1].length() == 0 ? Double.MIN_VALUE : Double.parseDouble(split[1]);
			return v >= min && v <= max;
		}));
		types.add(new ArgType<>("float", Float::parseFloat).constraint(msg("numberOutsideRange"), (c, v) -> {
			String[] split = c.split(",");
			float min = split[0].length() == 0 ? Float.MIN_VALUE : Float.parseFloat(split[0]);
			float max = split[1].length() == 0 ? Float.MIN_VALUE : Float.parseFloat(split[1]);
			return v >= min && v <= max;
		}));
		types.add(new ArgType<>("long", (Function<String, Long>) Long::parseLong).constraint(msg("numberOutsideRange"), (c, v) -> {
			String[] split = c.split(",");
			long min = split[0].length() == 0 ? Long.MIN_VALUE : Long.parseLong(split[0]);
			long max = split[1].length() == 0 ? Long.MIN_VALUE : Long.parseLong(split[1]);
			return v >= min && v <= max;
		}));
		types.add(new ArgType<>("string", s -> s));
		types.add(new ArgType<>("boolean", s -> {
			switch (s.toLowerCase()) {
				case "true":
					return true;
				case "false":
					return false;
				default:
					return null;
			}
		}).tabStream(c -> Stream.of("true", "false")));
		types.add(new ArgType<Player>("player", (Function<String, Player>) Bukkit::getPlayerExact).tabStream(c -> Bukkit.getOnlinePlayers().stream().map(Player::getName)));
		return types;
	}
	
	public static List<String> splitArgsForTab(String[] args) {
		List<String> argList = new ArrayList<>();
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.length() > 0 && arg.charAt(0) == '"' && i + 1 < args.length) {
				String next = args[i + 1];
				if (next.length() > 0 && next.charAt(next.length() - 1) == '"') {
					argList.add(arg + " " + next);
					i++;
					continue;
				}
			}
			argList.add(args[i]);
		}
		return argList;
	}
	
	public static Object[] combine(Object[] first, Object[] second) {
		Object[] newArr = new Object[first.length + second.length];
		System.arraycopy(first, 0, newArr, 0, first.length);
		System.arraycopy(second, 0, newArr, first.length, second.length);
		return newArr;
	}
	
	public static String getConversionFailMessage(CommandParameter carg, String arg) {
		return msg("invalidArgument").replace("%arg%", carg.getNameAndConstraint()).replace("%value%", arg);
	}
	
	public static Result<Object, String> convertArg(Command command, CommandParameter carg, String arg, Object[] output, int offset, CommandSender sender) {
		ArgType<?> type = carg.getType();
		Object prev = null;
		int pos = carg.getPosition() + (offset - 1);
		if (type.getParent() != null && pos > 0) {
			prev = output[pos];
		}
		try {
			Object obj = carg.getType().convert(sender, prev, arg, carg.getConstraint());
			if (obj != null) {
				return Result.success(command, obj);
			}
			String error = getConversionFailMessage(carg, arg);
			if (carg.getType().getFailedConstraintMessage() == null) {
				return Result.failure(command, error);
			}
			obj = carg.getType().convert(sender, prev, arg, null);
			if (obj != null) {
				return Result.failure(command, error + "\n" + carg.getType().getFailedConstraintMessage());
			}
			return Result.failure(command, error);
		} catch (Exception e) {
			return Result.failure(command, getConversionFailMessage(carg, arg));
		}
	}
	
	public static Result<String[], Boolean[]> splitArgs(String input) {
		List<String> args = new ArrayList<>();
		List<Boolean> quoted = new ArrayList<>();
		StringBuilder combine = new StringBuilder();
		boolean quotes = false;
		char[] chars = input.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			if (c == '\\' && i + 1 < chars.length) {
				combine.append(chars[i + 1]);
				i++;
				continue;
			}
			if (c == '"') {
				quotes = !quotes;
				if (combine.length() > 0) {
					args.add(combine.toString());
					quoted.add(!quotes);
					combine = new StringBuilder();
				}
				continue;
			}
			if (c == ' ' && !quotes) {
				if (combine.length() > 0) {
					args.add(combine.toString());
					quoted.add(false);
					combine = new StringBuilder();
				}
				continue;
			}
			combine.append(c);
		}
		if (combine.length() > 0) {
			args.add(combine.toString());
			quoted.add(false);
		}
		return Result.result(null, args.toArray(new String[args.size()]), quoted.toArray(new Boolean[quoted.size()]));
	}
	
}

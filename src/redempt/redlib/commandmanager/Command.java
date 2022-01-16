package redempt.redlib.commandmanager;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import redempt.redlib.commandmanager.CommandCollection.MergedBaseCommand;
import redempt.redlib.commandmanager.exceptions.CommandHookException;
import redempt.redlib.commandmanager.processing.CommandArgument;
import redempt.redlib.commandmanager.processing.CommandParameter;
import redempt.redlib.commandmanager.processing.CommandProcessUtils;
import redempt.redlib.commandmanager.processing.CommandFlag;
import redempt.redlib.commandmanager.processing.Result;
import redempt.redlib.commandmanager.processing.UnregisterListener;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a command which can be registered
 *
 * @author Redempt
 */
public class Command {
	
	private static List<ArgType<?>> types = CommandProcessUtils.getBaseArgTypes();
	private static SimpleCommandMap commandMap = CommandProcessUtils.getCommandMap();
	private static Map<String, org.bukkit.command.Command> knownCommands = CommandProcessUtils.getKnownCommands(commandMap);
	
	protected List<Command> children = new ArrayList<>();
	protected Plugin plugin;
	private CommandArgument[] args;
	private CommandFlag[] flags;
	private Map<Character, CommandFlag> charBoolFlags = new HashMap<>();
	private ContextProvider<?>[] contextProviders;
	private ContextProvider<?>[] asserters;
	protected String[] names;
	private String permission;
	private SenderType type;
	protected String hook;
	private Method methodHook;
	protected String help;
	private Object listener;
	private boolean noTab = false;
	protected boolean topLevel = false;
	protected Command parent = null;
	private boolean hideSub = false;
	private boolean noHelp = false;
	private boolean postArg = false;
	private boolean hasPostArgChild = false;
	
	protected Command() {}
	
	protected Command(String[] names, CommandArgument[] args, CommandFlag[] flags, ContextProvider<?>[] providers,
	                  ContextProvider<?>[] asserters, String help, String permission, SenderType type, String hook,
	                  List<Command> children, boolean hideSub, boolean noTab, boolean noHelp, boolean postArg) {
		this.names = names;
		this.args = args;
		this.flags = flags;
		for (CommandFlag flag : flags) {
			if (!flag.getType().getName().equals("boolean")) {
				continue;
			}
			for (String name : flag.getNames()) {
				if (name.length() == 2) {
					char c = name.charAt(1);
					charBoolFlags.put(c, flag);
				}
			}
		}
		if (charBoolFlags.size() == 0) {
			charBoolFlags = null;
		}
		this.contextProviders = providers;
		this.asserters = asserters;
		this.permission = permission;
		this.type = type;
		this.hook = hook;
		this.help = help == null ? null : help.replace("\n", "\n" + CommandProcessUtils.msg("helpTextColor"));
		this.hideSub = hideSub;
		this.noTab = noTab;
		this.noHelp = noHelp;
		this.postArg = postArg;
		this.children = children;
		for (Command command : children) {
			command.parent = this;
			if (command.args.length == 0 || args.length == 0) {
				continue;
			}
			CommandArgument first = command.args[0];
			if (first.getType().getParent() == null) {
				continue;
			}
			String firstName = args[args.length - 1].getType().getName();
			String secondName = first.getType().getParent().getName();
			if (!firstName.equals(secondName)) {
				throw new IllegalStateException("Argument " + first.getName() + " for command " + command.getName()
						+ " does not have an argument of type " + first.getType().getParent().getName() + " before it");
			}
		}
		hasPostArgChild = children.stream().anyMatch(c -> c.postArg);
	}
	
	/**
	 * Shows the help to a CommandSender
	 *
	 * @param sender The sender to show the help to
	 * @return True if the help was shown to the user, false if the usage was shown instead
	 */
	public boolean showHelp(CommandSender sender) {
		String title = CommandProcessUtils.msg("helpTitle").replace("%cmdname%", names[0]);
		List<String> lines = new ArrayList<>();
		Collections.addAll(lines, getHelpRecursive(sender, 0).trim().split("\n"));
		if (parent != null) {
			parent.children.stream().filter(c -> c != this && c.nameMatches(names[0])).forEach(c -> {
				Collections.addAll(lines, c.getHelpRecursive(sender, 0).trim().split("\n"));
			});
		}
		lines.removeIf(s -> s.length() == 0);
		if (lines.size() > 0) {
			sender.sendMessage(title);
			lines.forEach(sender::sendMessage);
		} else {
			sender.sendMessage(CommandProcessUtils.msg("showUsage").replace("%usage%", getFullName()));
		}
		return lines.size() > 0;
	}
	
	protected String getHelpRecursive(CommandSender sender, int level) {
		if (permission != null && !sender.hasPermission(permission)) {
			return "";
		}
		StringBuilder help = new StringBuilder();
		help.append(this.help == null ? "" : CommandProcessUtils.msg("helpEntry").replace("%cmdname%", getFullName()).replace("%help%", this.help) + "\n");
		if (hideSub && level != 0) {
			if (help.length() == 0) {
				return CommandProcessUtils.msg("helpEntry").replace("%cmdname%", getFullName()).replace("%help%", "[Hidden subcommands]") + "\n";
			}
			return help.toString();
		}
		for (Command command : children) {
			help.append(command.getHelpRecursive(sender, level + 1));
		}
		return help.toString();
	}
	
	/**
	 * @return The expanded name of the command, plus arguments
	 */
	public String getFullName() {
		Stack<String> stack = new Stack<>();
		Command current = this;
		boolean lastPostArg = true;
		while (current != null && !(current instanceof MergedBaseCommand)) {
			if (lastPostArg) {
				stack.add(current.getArgsDisplay());
			}
			stack.add(current.getName());
			lastPostArg = current.postArg;
			current = current.parent;
		}
		StringBuilder builder = new StringBuilder();
		while (!stack.isEmpty()) {
			builder.append(stack.pop()).append(' ');
		}
		while (builder.length() > 0 && builder.charAt(builder.length() - 1) == ' ') {
			builder.deleteCharAt(builder.length() - 1);
		}
		return "/" + builder;
	}
	
	/**
	 * @return The display of the arguments for this command
	 */
	public String getArgsDisplay() {
		String display = flags.length > 0 ? String.join(" ", Arrays.stream(flags).map(CommandFlag::toString).collect(Collectors.toList())) + " " : "";
		display += String.join(" ", Arrays.stream(args).map(CommandArgument::toString).collect(Collectors.toList()));
		return display;
	}
	
	/**
	 * @return The name of the command concatenated with its parents' names
	 */
	public String getExpandedName() {
		String name = names[0];
		if (parent != null && !(parent instanceof MergedBaseCommand)) {
			name = parent.getExpandedName() + " " + name;
			return name;
		}
		return "/" + name;
	}
	
	private Result<Object[], String> processArgs(String[] argArray, Boolean[] quoted, List<Object> prepend, CommandSender sender) {
		Object[] output = new Object[args.length + flags.length + 1 + prepend.size()];
		System.arraycopy(prepend.toArray(), 0, output, 1, prepend.size());
		int offset = prepend.size() + 1;
		output[0] = sender;
		List<String> args = new ArrayList<>();
		List<Boolean> quotedList = new ArrayList<>();
		Collections.addAll(args, argArray);
		Collections.addAll(quotedList, quoted);
		String err;
		
		err = processFlags(args, output, quotedList, sender);
		if (err != null) {
			return Result.failure(this, err);
		}
		
		List<CommandArgument> commandArgs = new ArrayList<>();
		Collections.addAll(commandArgs, this.args);
		err = convertArgs(commandArgs, args, quotedList, output, offset, sender);
		if (err != null) {
			return Result.failure(this, err);
		}
		return Result.success(this, output);
	}
	
	private String getWrongArgumentCountMessage(Command command, int args, int optionals) {
		if (optionals == 0) {
			return CommandProcessUtils.msg("wrongArgumentCount")
					.replace("%args%", this.args.length + "")
					.replace("%count%", args + "");
		} else {
			return CommandProcessUtils.msg("wrongArgumentCount")
					.replace("%args%", (this.args.length - optionals) + "-" + this.args.length)
					.replace("%count%", args + "");
		}
	}
	
	private String convertArgs(List<CommandArgument> commandArgs, List<String> args, List<Boolean> quoted, Object[] output, int offset, CommandSender sender) {
		if (commandArgs.size() == 0) {
			if (args.size() > 0) {
				return getWrongArgumentCountMessage(this, args.size(), 0);
			}
			return null;
		}
		int diff = commandArgs.size() - args.size();
		int optionals = (int) commandArgs.stream().filter(CommandArgument::isOptional).count();
		if (optionals < diff || (args.size() > commandArgs.size() && !lastArgTakesAll())) {
			return getWrongArgumentCountMessage(this, args.size(), optionals);
		}
		int argPos = 0;
		for (int i = 0; i < args.size(); i++) {
			CommandArgument carg = commandArgs.get(argPos);
			String arg = args.get(i);
			if (carg.takesAll()) {
				Result<Object, String> result = processTakeAllArg(carg, args, quoted, i, output, offset, sender);
				if (result.getMessage() != null) {
					return result.getMessage();
				}
				output[carg.getPosition() + offset] = result.getValue();
				return null;
			}
			if (!carg.isOptional() || diff == 0) {
				Result<Object, String> convertResult = CommandProcessUtils.convertArg(this, carg, arg, output, offset, sender);
				if (convertResult.getMessage() != null) {
					return convertResult.getMessage();
				}
				output[carg.getPosition() + offset] = convertResult.getValue();
				argPos++;
				continue;
			}
			Result<Object, String> convertResult = CommandProcessUtils.convertArg(this, carg, arg, output, offset, sender);
			if (convertResult.getValue() == null || diff >= optionals) {
				if (carg.isContextDefault() && !(sender instanceof Player)) {
					return CommandProcessUtils.msg("contextDefaultFromConsole").replace("%arg%", carg.getName());
				}
				diff--;
				optionals--;
				output[carg.getPosition() + offset] = carg.getDefaultValue(sender);
				commandArgs.remove(argPos);
				i--;
				continue;
			}
			output[carg.getPosition() + offset] = convertResult.getValue();
			argPos++;
		}
		for (int i = argPos; i < commandArgs.size(); i++) {
			CommandArgument carg = commandArgs.get(i);
			if (carg.isContextDefault() && !(sender instanceof Player)) {
				return CommandProcessUtils.msg("contextDefaultFromConsole").replace("%arg%", carg.getName());
			}
			output[carg.getPosition() + offset] = carg.getDefaultValue(sender);
			diff--;
		}
		if (diff != 0) {
			return CommandProcessUtils.msg("ambiguousOptional");
		}
		return null;
	}
	
	private Result<Object, String> processTakeAllArg(CommandArgument arg, List<String> args, List<Boolean> quoted, int start, Object[] output, int offset, CommandSender sender) {
		if (start >= args.size()) {
			if (!arg.isOptional()) {
				return Result.failure(this, CommandProcessUtils.msg("needArgument").replace("%arg%", arg.getName()));
			}
			if (arg.isContextDefault() && !(sender instanceof Player)) {
				return Result.failure(this, CommandProcessUtils.msg("contextDefaultFromConsole").replace("%arg%", arg.getName()));
			}
		}
		if (arg.consumes()) {
			if (start >= args.size()) {
				return Result.success(this, arg.getDefaultValue(sender));
			}
			StringBuilder builder = new StringBuilder();
			for (int i = start; i < args.size(); i++) {
				if (quoted.get(i)) {
					builder.append('"').append(args.get(i)).append('"');
				} else {
					builder.append(args.get(i));
				}
				if (i != args.size() - 1) {
					builder.append(' ');
				}
			}
			return CommandProcessUtils.convertArg(this, arg, builder.toString(), output, offset, sender);
		}
		Class<?> clazz = methodHook.getParameterTypes()[arg.getPosition() + offset];
		if (!clazz.isArray()) {
			throw new IllegalStateException("Expected type parameter #" + (arg.getPosition() + 2) + " for method hook " + methodHook.getName() + " to be an array");
		}
		clazz = clazz.getComponentType();
		if (start >= args.size()) {
			Object arr = Array.newInstance(clazz, 1);
			Array.set(arr, 0, arg.getDefaultValue(sender));
			return Result.success(this, arr);
		}
		Object arr = Array.newInstance(clazz, args.size() - start);
		for (int i = start; i < args.size(); i++) {
			Result<Object, String> convert = CommandProcessUtils.convertArg(this, arg, args.get(i), output, offset, sender);
			if (convert.getMessage() != null) {
				return convert;
			}
			Array.set(arr, i - start, convert.getValue());
		}
		return Result.success(this, arr);
	}
	
	private String processFlags(List<String> args, Object[] output, List<Boolean> quoted, CommandSender sender) {
		for (int i = 0; i < args.size(); i++) {
			String arg = args.get(i);
			if (!arg.startsWith("-") || quoted.get(i)) {
				continue;
			}
			CommandFlag flag = Arrays.stream(flags).filter(f -> f.nameMatches(arg)).findFirst().orElse(null);
			if (flag == null) {
				if (charBoolFlags == null) {
					continue;
				}
				List<CommandFlag> flags = arg.chars().skip(1).mapToObj(c -> (char) c).map(charBoolFlags::get).collect(Collectors.toList());
				if (flags.stream().anyMatch(Objects::isNull)) {
					continue;
				}
				flags.forEach(f -> output[f.getPosition() + 1] = true);
				args.remove(i);
				quoted.remove(i);
				i--;
				continue;
			}
			if (flag.getType().getName().equals("boolean")) {
				output[flag.getPosition() + 1] = true;
				args.remove(i);
				quoted.remove(i);
				i--;
				continue;
			}
			if (i == args.size() - 1) {
				return CommandProcessUtils.msg("needFlagValue").replace("%flag%", flag.getName());
			}
			String next = args.get(i + 1);
			Result<Object, String> result = CommandProcessUtils.convertArg(this, flag, next, null, 0, sender);
			if (result.getMessage() != null) {
				return result.getMessage();
			}
			output[flag.getPosition() + 1] = result.getValue();
			args.subList(i, i + 2).clear();
			quoted.subList(i, i + 1).clear();
			i--;
		}
		for (CommandFlag flag : flags) {
			if (output[flag.getPosition() + 1] != null) {
				continue;
			}
			if (flag.getType().getName().equals("boolean")) {
				output[flag.getPosition() + 1] = false;
				continue;
			}
			if (flag.isContextDefault() && !(sender instanceof Player)) {
				return CommandProcessUtils.msg("contextDefaultFlagFromConsole").replace("%flag%", flag.getName());
			}
			output[flag.getPosition() + 1] = flag.getDefaultValue(sender);
		}
		return null;
	}
	
	private Result<Object[], String> getContext(CommandSender sender) {
		if (!(sender instanceof Player)) {
			return Result.failure(this, CommandProcessUtils.msg("playerOnly"));
		}
		Object[] output = new Object[contextProviders.length];
		for (int i = 0; i < output.length; i++) {
			Object obj = contextProviders[i].provide((Player) sender);
			if (obj == null) {
				String error = contextProviders[i].getErrorMessage();
				return Result.failure(this, error);
			}
			output[i] = obj;
		}
		return Result.success(this, output);
	}
	
	private Result<Boolean, String> assertAll(CommandSender sender) {
		if (!(sender instanceof Player)) {
			return Result.result(this, false, CommandProcessUtils.msg("playerOnly"));
		}
		for (ContextProvider<?> provider : asserters) {
			Object o = provider.provide((Player) sender);
			if (o == null) {
				String error = provider.getErrorMessage();
				return Result.result(this, false, error);
			}
		}
		return Result.result(this, true, null);
	}
	
	/**
	 * Registers this command and its children
	 *
	 * @param prefix    The fallback prefix
	 * @param listeners The listener objects containing method hooks
	 */
	public void register(String prefix, Object... listeners) {
		if (plugin == null) {
			plugin = CommandProcessUtils.getCallingPlugin();
		}
		RedCommand cmd = new RedCommand(plugin, names[0], help == null ? "None" : help, getFullName().substring(1), permission, Arrays.stream(names).skip(1).collect(Collectors.toList())) {
			
			@Override
			public boolean execute(CommandSender sender, String name, String[] args) {
				Command.this.execute(sender, args, new ArrayList<>());
				return true;
			}
			
			@Override
			public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
				return tab(sender, args);
			}
			
		};
		commandMap.register(prefix, cmd);
		new UnregisterListener(plugin, this::unregister);
		registerHook(createHookMap(listeners), plugin);
	}
	
	private void unregister() {
		Arrays.stream(names).forEach(knownCommands::remove);
	}
	
	protected Map<String, MethodHook> createHookMap(Object... listeners) {
		Map<String, MethodHook> hooks = new HashMap<>();
		for (Object listener : listeners) {
			for (Method method : listener.getClass().getDeclaredMethods()) {
				CommandHook cmdHook = method.getAnnotation(CommandHook.class);
				if (cmdHook == null) {
					continue;
				}
				if (hooks.put(cmdHook.value(), new MethodHook(method, listener)) != null) {
					throw new CommandHookException("Duplicate method hook for name '" + cmdHook.value() + "'");
				}
			}
		}
		return hooks;
	}
	
	private String getExpectedParameterTypes() {
		List<String> expectedArgs = new ArrayList<>();
		expectedArgs.add("commandsender");
		boolean lastPostArg = false;
		Command current = this;
		Stack<List<String>> prev = new Stack<>();
		while (current != null) {
			if (lastPostArg) {
				prev.add(current.getArgumentTypeNames());
			}
			lastPostArg = current.postArg;
			current = current.parent;
		}
		while (!prev.isEmpty()) {
			expectedArgs.addAll(prev.pop());
		}
		expectedArgs.addAll(getArgumentTypeNames());
		for (ContextProvider<?> provider : contextProviders) {
			expectedArgs.add("context:" + provider.getName());
		}
		return String.join(", ", expectedArgs);
	}
	
	private List<String> getArgumentTypeNames() {
		List<CommandParameter> params = new ArrayList<>();
		Collections.addAll(params, flags);
		Collections.addAll(params, args);
		params.sort(Comparator.comparingInt(CommandParameter::getPosition));
		return params.stream().map(CommandParameter::getTypeName).collect(Collectors.toList());
	}
	
	protected void registerHook(Map<String, MethodHook> hooks, Plugin plugin) {
		for (Command child : children) {
			child.registerHook(hooks, plugin);
		}
		this.plugin = plugin;
		if (hook == null) {
			return;
		}
		MethodHook mh = hooks.get(hook);
		if (hook != null && mh == null) {
			throw new CommandHookException("Command with hook name " + hook + " has no method hook");
		}
		methodHook = mh.getMethod();
		Class<?>[] params = methodHook.getParameterTypes();
		
		int expectedLength = args.length + contextProviders.length + flags.length + 1;
		Command current = this;
		while (current != null) {
			if (current.postArg) {
				expectedLength += current.parent.args.length + current.parent.contextProviders.length + current.parent.flags.length;
			}
			current = current.parent;
		}
		if (params.length != expectedLength) {
			throw new IllegalStateException("Incorrect number of arguments for method hook! [" + methodHook.getDeclaringClass().getName() + "." + methodHook.getName() + "] "
					+ "Argument count should be " + expectedLength + ", got " + params.length + "\nParameters should be: [" + getExpectedParameterTypes() + "]");
		}
		this.listener = mh.getListener();
		if (!CommandSender.class.isAssignableFrom(params[0])) {
			throw new IllegalStateException("The first argument must be CommandSender or one of its subclasses! [" + methodHook.getDeclaringClass().getName() + "." + methodHook.getName() + "]");
		}
	}
	
	protected List<String> tab(CommandSender sender, String[] args) {
		List<String> argList = CommandProcessUtils.splitArgsForTab(args);
		args = argList.toArray(new String[0]);
		List<String> completions = tab(sender, argList, args);
		String last = args.length > 0 ? args[args.length - 1].toLowerCase(Locale.ROOT) : "";
		return completions.stream().filter(c -> c.toLowerCase(Locale.ROOT).startsWith(last)).map(s -> s.contains(" ") ? "\"" + s + "\"" : s).collect(Collectors.toList());
	}
	
	private List<String> tab(CommandSender sender, List<String> args, String[] completeArgs) {
		List<String> completions = new ArrayList<>();
		boolean childrenTabbed = false;
		for (Command child : children) {
			if (child.noTab || child.postArg || (child.getPermission() != null && !sender.hasPermission(child.getPermission()))) {
				continue;
			}
			if (args.size() > 0 && child.nameMatches(args.get(0))) {
				completions.addAll(child.tab(sender, args.stream().skip(1).collect(Collectors.toList()), completeArgs));
				childrenTabbed = true;
			}
			if (args.size() == 1) {
				completions.add(child.getName());
			}
		}
		if (childrenTabbed) {
			return completions;
		}
		Result<Boolean, List<String>> flagResults = tabCompleteFlags(args, sender);
		if (flagResults.getValue()) {
			return flagResults.getMessage();
		} else {
			completions.addAll(flagResults.getMessage());
		}
		if (this.args.length > 0) {
			CommandArgument last = this.args[Math.max(0, Math.min(args.size() - 1, this.args.length - 1))];
			if (last.isVararg() || args.size() <= this.args.length) {
				completions.addAll(tabCompleteArgument(last, completeArgs, sender));
			}
		}
		if (args.size() > this.args.length && hasPostArgChild) {
			int next = this.args.length;
			String name = args.get(next);
			List<String> toComplete = args.stream().skip(next + 1).collect(Collectors.toList());
			for (Command child : children) {
				if (!child.isPostArg() || (child.getPermission() != null && !sender.hasPermission(child.getPermission()))) {
					continue;
				}
				if (next + 1 == args.size()) {
					completions.add(child.getName());
					continue;
				}
				if (child.nameMatches(name)) {
					completions.addAll(child.tab(sender, toComplete, completeArgs));
				}
			}
		}
		return completions;
	}
	
	private Result<Boolean, List<String>> tabCompleteFlags(List<String> args, CommandSender sender) {
		if (args.size() == 0) {
			return Result.result(this, false, new ArrayList<>());
		}
		Set<CommandFlag> used = new HashSet<>();
		for (int i = 0; i < args.size() - 1; i++) {
			String arg = args.get(i);
			if (!arg.startsWith("-")) {
				continue;
			}
			CommandFlag flag = Arrays.stream(flags).filter(f -> f.nameMatches(arg)).findFirst().orElse(null);
			if (flag == null) {
				continue;
			}
			if (flag.getType().getName().equals("boolean")) {
				used.add(flag);
				args.remove(i);
				i--;
				continue;
			}
			if (i < args.size() - 2) {
				args.subList(i, i + 1).clear();
				i--;
			}
		}
		if (args.size() == 0) {
			return Result.result(this, false, new ArrayList<>());
		}
		List<String> completions = new ArrayList<>();
		String lastArg = args.get(args.size() - 1);
		if (lastArg.startsWith("-")) {
			Arrays.stream(flags).filter(f -> !used.contains(f)).forEach(f -> Collections.addAll(completions, f.getNames()));
			return Result.result(this, true, completions);
		}
		if (args.size() <= 1) {
			return Result.result(this, false, completions);
		}
		String nextToLast = args.get(args.size() - 2);
		CommandFlag flag = Arrays.stream(flags).filter(f -> f.nameMatches(nextToLast)).findFirst().orElse(null);
		if (flag == null) {
			return Result.result(this, false, completions);
		}
		completions.addAll(flag.getType().tabComplete(sender, args.toArray(new String[0]), null));
		return Result.result(this, true, completions);
	}
	
	private List<String> tabCompleteArgument(CommandArgument arg, String[] str, CommandSender sender) {
		return arg.getType().tabComplete(sender, str, getPrevious(str, str.length - 1, arg.getPosition(), sender));
	}
	
	private Object getPrevious(String[] args, int pos, int argNum, CommandSender sender) {
		if ((argNum < 1 || pos < 1) && !postArg) {
			return null;
		}
		if (argNum < 0 || pos < 0 || argNum >= this.args.length || pos >= args.length) {
			return null;
		}
		if (postArg && argNum == 0) {
			pos--;
		}
		CommandArgument arg = this.args[argNum];
		CommandArgument prevArg = null;
		if (argNum - 1 >= 0) {
			prevArg = this.args[argNum - 1];
		} else {
			Command current = parent;
			while (current != null) {
				if (current.args.length > 0) {
					prevArg = current.args[current.args.length - 1];
					break;
				}
				current = current.parent;
			}
		}
		Object previous = null;
		if (arg.getType().getParent() != null) {
			previous = getPrevious(args, pos - 1, argNum - 1, sender);
		}
		try {
			return prevArg.getType().convert(sender, previous, args[pos - 1]);
		} catch (Exception e) {
			return null;
		}
	}
	
	protected Result<Boolean, String> execute(CommandSender sender, String[] args, List<Object> prepend) {
		if (permission != null && !sender.hasPermission(permission)) {
			sender.sendMessage(CommandProcessUtils.msg("noPermission").replace("%permission%", permission));
			return Result.success(this, true);
		}
		if (args.length > 0 && args[0].equalsIgnoreCase("help") && !noHelp) {
			showHelp(sender);
			return Result.success(this, true);
		}
		List<Result<Boolean, String>> results = new ArrayList<>();
		if (methodHook != null || hasPostArgChild) {
			Result<Boolean, String> result = runHook(sender, args, prepend, results);
			if (result.getValue()) {
				return result;
			}
			results.add(result);
		}
		if (args.length >= 1) {
			String[] truncArgs = Arrays.copyOfRange(args, 1, args.length);
			for (Command command : children) {
				if (command.isPostArg() || !command.nameMatches(args[0])) {
					continue;
				}
				Result<Boolean, String> result = command.execute(sender, truncArgs, prepend);
				if (result.getValue()) {
					return Result.success(this, true);
				}
				results.add(result);
			}
		}
		Result<Boolean, String> deepest = CommandProcessUtils.getDeepest(results);
		if (deepest == null) {
			deepest = Result.result(this, false, args.length == 0 ? null : CommandProcessUtils.msg("invalidSubcommand").replace("%value%", args[0]));
		}
		if (!topLevel) {
			return deepest;
		}
		if (deepest.getMessage() != null) {
			Arrays.stream(deepest.getMessage().split("\n")).forEach(sender::sendMessage);
		}
		deepest.getCommand().showHelp(sender);
		return Result.result(this, true, null);
	}
	
	protected Result<Boolean, String> runHook(CommandSender sender, String[] args, List<Object> prepend, List<Result<Boolean, String>> results) {
		switch (type) {
			case EVERYONE:
				break;
			case CONSOLE:
				if (sender instanceof Player) {
					return Result.result(this, false, CommandProcessUtils.msg("consoleOnly"));
				}
				break;
			case PLAYER:
				if (!(sender instanceof Player)) {
					return Result.result(this, false, CommandProcessUtils.msg("playerOnly"));
				}
				break;
		}
		Result<String[], Boolean[]> split = CommandProcessUtils.splitArgs(String.join(" ", args));
		String[] toProcess = split.getValue();
		if (hasPostArgChild) {
			String[] next = new String[Math.min(toProcess.length, this.args.length)];
			System.arraycopy(toProcess, 0, next, 0, next.length);
			toProcess = next;
		}
		Boolean[] quoted = split.getMessage();
		Result<Object[], String> result = processArgs(toProcess, quoted, prepend, sender);
		Object[] objArgs = result.getValue();
		if (asserters.length > 0) {
			Result<Boolean, String> assertionResult = assertAll(sender);
			if (!assertionResult.getValue()) {
				return Result.result(this, false, assertionResult.getMessage());
			}
		}
		if (hasPostArgChild && split.getValue().length > this.args.length && !quoted[toProcess.length]) {
			int spaces = (int) Arrays.stream(toProcess).flatMap(s -> s.chars().boxed()).filter(c -> c == ' ').count();
			int start = this.args.length + spaces;
			String[] truncArgs = Arrays.copyOfRange(args, start + 1, args.length);
			int rangeStart = objArgs == null ? 0 : objArgs.length - (this.args.length + this.flags.length);
			int rangeEnd = objArgs == null ? 0 : objArgs.length;
			for (int i = rangeStart; i < rangeEnd; i++) {
				prepend.add(objArgs[i]);
			}
			for (Command command : children) {
				if (!command.isPostArg() || !command.nameMatches(args[start])) {
					continue;
				}
				if (objArgs == null) {
					return Result.result(command, false, result.getMessage());
				}
				Result<Boolean, String> execResult = command.execute(sender, truncArgs, prepend);
				if (execResult.getValue()) {
					return Result.success(this, true);
				}
				results.add(execResult);
			}
			int rangeSize = rangeEnd - rangeStart;
			prepend.subList(prepend.size() - rangeSize, prepend.size()).clear();
			return Result.result(this, false, null);
		}
		if (objArgs == null) {
			return Result.result(this, false, result.getMessage());
		}
		if (contextProviders.length > 0) {
			Result<Object[], String> contextResult = getContext(sender);
			if (contextResult.getValue() == null) {
				return Result.result(this, false, contextResult.getMessage());
			}
			objArgs = CommandProcessUtils.combine(objArgs, contextResult.getValue());
		}
		if (methodHook != null) {
			try {
				methodHook.invoke(listener, objArgs);
				return Result.success(this, true);
			} catch (IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
				sender.sendMessage(CommandProcessUtils.msg("commandError"));
				return Result.success(this, true);
			} catch (IllegalArgumentException e) {
				StringJoiner joiner = new StringJoiner(", ", "[", "]");
				for (Object o : objArgs) {
					joiner.add(o == null ? "null" : o.getClass().getName());
				}
				Bukkit.getLogger().warning("Could not invoke method hook " + hook + " for plugin " + plugin + " with arguments of types:");
				Bukkit.getLogger().warning(joiner.toString());
				e.printStackTrace();
				if (topLevel) {
					showHelp(sender);
					return Result.success(this, true);
				}
			}
		}
		return Result.result(this, false, null);
	}
	
	/**
	 * Check if a name matches any of this command's aliases
	 * @param name The name to check
	 * @return Whether the name matches any of this command's aliases
	 */
	public boolean nameMatches(String name) {
		return Arrays.stream(names).anyMatch(s -> s.equals(name));
	}
	
	public int getDepth() {
		int depth = 0;
		Command c = this;
		while (c.parent != null) {
			c = c.parent;
			depth++;
		}
		return depth;
	}
	
	protected static ArgType<?> getType(String name, ArgType<?>[] types) {
		for (ArgType<?> type : Command.types) {
			if (type.getName().equals(name)) {
				return type;
			}
		}
		for (ArgType<?> type : types) {
			if (type.getName().equals(name)) {
				return type;
			}
		}
		return null;
	}
	
	/**
	 * @return The command's primary name/first alias
	 */
	public String getName() {
		return names[0];
	}
	
	/**
	 * @return Whether the last argument in this command is consuming or vararg
	 */
	public boolean lastArgTakesAll() {
		return args.length > 0 && args[args.length - 1].takesAll();
	}
	
	/**
	 * @return All of the command's names/aliases
	 */
	public String[] getAliases() {
		return names;
	}
	
	/**
	 * @return Whether this subcommand comes after the arguments of its parent
	 */
	public boolean isPostArg() {
		return postArg;
	}
	
	/**
	 * @return The command's help message
	 */
	public String getHelp() {
		return help;
	}
	
	/**
	 * @return Nullable. The permission required to run the command
	 */
	public String getPermission() {
		return permission;
	}
	
	public static enum SenderType {
		
		CONSOLE,
		PLAYER,
		EVERYONE;
		
	}
	
	protected static class MethodHook {
		
		private Method method;
		private Object listener;
		
		public MethodHook(Method method, Object listener) {
			this.method = method;
			this.listener = listener;
			Player player;
		}
		
		public Method getMethod() {
			return method;
		}
		
		public Object getListener() {
			return listener;
		}
		
	}
	
}

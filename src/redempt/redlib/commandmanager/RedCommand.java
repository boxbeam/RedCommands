package redempt.redlib.commandmanager;

import org.bukkit.command.Command;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;

import java.util.List;

abstract class RedCommand extends Command implements PluginIdentifiableCommand {
	
	private Plugin plugin;
	
	protected RedCommand(Plugin plugin, String name, String description, String usageMessage, List<String> aliases) {
		super(name, description, usageMessage, aliases);
		this.plugin = plugin;
	}
	
	@Override
	public Plugin getPlugin() {
		return plugin;
	}
	
}

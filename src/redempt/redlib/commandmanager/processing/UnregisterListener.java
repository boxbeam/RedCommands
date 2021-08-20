package redempt.redlib.commandmanager.processing;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

public class UnregisterListener implements Listener {
	
	private Plugin plugin;
	private Runnable onDisable;
	
	public UnregisterListener(Plugin plugin, Runnable onDisable) {
		this.plugin = plugin;
		this.onDisable = onDisable;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onDisable(PluginDisableEvent e) {
		if (e.getPlugin().equals(plugin)) {
			onDisable.run();
			HandlerList.unregisterAll(this);
		}
	}
	
}

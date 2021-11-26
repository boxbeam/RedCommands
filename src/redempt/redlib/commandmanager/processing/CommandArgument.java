package redempt.redlib.commandmanager.processing;

import org.bukkit.command.CommandSender;
import redempt.redlib.commandmanager.ArgType;
import redempt.redlib.commandmanager.Constraint;

import java.util.function.Function;

public class CommandArgument implements CommandParameter {
	
	private ArgType<?> type;
	private String name;
	private Constraint<?> constraint;
	private boolean optional;
	private boolean hideType;
	private boolean consume;
	private boolean vararg;
	private boolean contextDefault = false;
	private Function<CommandSender, Object> defaultValue = null;
	public int pos;
	
	public CommandArgument(ArgType<?> type, int pos, String name, Constraint<?> constraint, boolean optional, boolean hideType, boolean consume, boolean vararg) {
		this.name = name;
		this.constraint = constraint;
		this.type = type;
		this.pos = pos;
		this.optional = optional;
		this.hideType = hideType;
		this.consume = consume;
		this.vararg = vararg;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isContextDefault() {
		return contextDefault;
	}
	
	public void setDefaultValue(Function<CommandSender, Object> value, boolean context) {
		this.defaultValue = value;
		this.contextDefault = context;
	}
	
	public Object getDefaultValue(CommandSender sender) {
		return defaultValue == null ? null : defaultValue.apply(sender);
	}
	
	public Function<CommandSender, Object> getDefaultValue() {
		return defaultValue;
	}
	
	public int getPosition() {
		return pos;
	}
	
	@Override
	public String getTypeName() {
		String typeName = type.getName();
		if (vararg) {
			typeName += "[]";
		}
		if (consume) {
			typeName += "...";
		}
		return typeName;
	}
	
	public ArgType<?> getType() {
		return type;
	}
	
	public boolean isOptional() {
		return optional;
	}
	
	public boolean consumes() {
		return consume;
	}
	
	public boolean isVararg() {
		return vararg;
	}
	
	public Constraint<?> getConstraint() {
		return constraint;
	}
	
	public boolean takesAll() {
		return vararg || consume;
	}
	
	@Override
	public String toString() {
		String name = hideType ? this.name : type.getName() + ":" + this.name;
		name += vararg || consume ? "+" : "";
		if (optional) {
			name = "[" + name + "]";
		} else {
			name = "<" + name + ">";
		}
		return name;
	}
	
}
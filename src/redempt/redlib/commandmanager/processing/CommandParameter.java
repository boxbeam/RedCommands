package redempt.redlib.commandmanager.processing;

import redempt.redlib.commandmanager.ArgType;

public interface CommandParameter {
	
	int getPosition();
	String getTypeName();
	ArgType<?> getType();
	String getConstraint();
	String getName();
	
	default String getNameAndConstraint() {
		return getName() + (getConstraint() == null ? "" : "<" + getConstraint() + ">");
	}
	
}

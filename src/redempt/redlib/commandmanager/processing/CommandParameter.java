package redempt.redlib.commandmanager.processing;

import redempt.redlib.commandmanager.ArgType;
import redempt.redlib.commandmanager.Constraint;

public interface CommandParameter {
	
	int getPosition();
	String getTypeName();
	ArgType<?> getType();
	Constraint<?> getConstraint();
	String getName();
	
	default String getNameAndConstraint() {
		return getName() + (getConstraint() == null ? "" : "<" + getConstraint().getName() + ">");
	}
	
}

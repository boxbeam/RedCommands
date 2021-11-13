package redempt.redlib.commandmanager.processing;

import redempt.redlib.commandmanager.Command;

public class Result<T, V> {
	
	public static <T, V> Result<T, V> success(Command cmd, T value) {
		return new Result<>(cmd, value, null);
	}
	
	public static <T, V> Result<T, V> failure(Command cmd, V message) {
		return new Result<>(cmd, null, message);
	}
	
	public static <T, V> Result<T, V> result(Command cmd, T value, V message) {
		return new Result<>(cmd, value, message);
	}
	
	private T value;
	private V message;
	private Command cmd;
	
	public Result(Command cmd, T value, V message) {
		this.value = value;
		this.message = message;
		this.cmd = cmd;
	}
	
	public Command getCommand() {
		return cmd;
	}
	
	public T getValue() {
		return value;
	}
	
	public V getMessage() {
		return message;
	}
	
}

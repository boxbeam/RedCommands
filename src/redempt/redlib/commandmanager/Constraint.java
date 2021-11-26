package redempt.redlib.commandmanager;

import org.bukkit.command.CommandSender;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Represents a condition which can be tested on command arguments, and can dynamically generate error messages
 * @param <T> The type this Constraint checks on
 * @author Redempt
 */
public class Constraint<T> {

    /**
     * Create a Constraint
     * @param error The error message to show when the constraint fails
     * @param condition A predicate to perform the constraint check on a converted argument
     * @param <T> The type of the constraint
     * @return A constructed Constraint
     */
    public static <T> Constraint<T> of(String error, BiPredicate<CommandSender, T> condition) {
        return new Constraint<>(condition, (c, v) -> error);
    }

    /**
     * Create a Constraint
     * @param condition A predicate to perform the constraint check on a converted argument
     * @param <T> The type of the constraint
     * @return A constructed Constraint
     */
    public static <T> Constraint<T> of(BiPredicate<CommandSender, T> condition) {
        return new Constraint<>(condition, null);
    }

    /**
     * Create a Constraint
     * @param error A function to generate the error to be shown on constraint fail
     * @param condition A predicate to perform the constraint check on a converted argument
     * @param <T> The type of the constraint
     * @return A constructed Constraint
     */
    public static <T> Constraint<T> of(Function<T, String> error, BiPredicate<CommandSender, T> condition) {
        return new Constraint<>(condition, (c, v) -> error.apply(v));
    }

    /**
     * Create a Constraint
     * @param error A function to generate the error to be shown on constraint fail
     * @param condition A predicate to perform the constraint check on a converted argument
     * @param <T> The type of the constraint
     * @return A constructed Constraint
     */
    public static <T> Constraint<T> of(BiFunction<CommandSender, T, String> error, BiPredicate<CommandSender, T> condition) {
        return new Constraint<>(condition, error);
    }

    private BiPredicate<CommandSender, T> condition;
    private BiFunction<CommandSender, T, String> error;
    private String name;

    private Constraint(BiPredicate<CommandSender, T> condition, BiFunction<CommandSender, T, String> error) {
        this.condition = condition;
        this.error = error;
    }

    public Constraint<T> setName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public boolean test(CommandSender sender, Object arg) {
        return condition.test(sender, (T) arg);
    }

    public String getError(CommandSender sender, Object arg) {
        return error == null ? null : error.apply(sender, (T) arg);
    }

}

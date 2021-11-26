package redempt.redlib.commandmanager;

import org.bukkit.command.CommandSender;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class Constraint<T> {

    public static <T> Constraint<T> of(String error, BiPredicate<CommandSender, T> condition) {
        return new Constraint<>(condition, (c, v) -> error);
    }

    public static <T> Constraint<T> of(BiPredicate<CommandSender, T> condition) {
        return new Constraint<>(condition, null);
    }

    public static <T> Constraint<T> of(Function<T, String> error, BiPredicate<CommandSender, T> condition) {
        return new Constraint<>(condition, (c, v) -> error.apply(v));
    }

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

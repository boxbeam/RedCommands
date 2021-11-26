package redempt.redlib.commandmanager.processing;

import redempt.redlib.commandmanager.Constraint;

import java.util.function.Function;

public class NumberConstraint {

    /**
     * Create a number constraint from a constraint string formatted like min,max or ,max or min,
     * @param str The constraint string
     * @param parse A function to parse a number of the given type from a string
     * @param <T> The numeric type
     * @return A constraint which can check the range of a number of the given type
     */
    public static <T extends Number & Comparable<T>> Constraint<T> getConstraint(String str, Function<String, T> parse) {
        String[] split = str.split(",", 2);
        T min = split[0].length() == 0 ? null : parse.apply(split[0]);
        T max = split[1].length() == 0 ? null : parse.apply(split[1]);
        String err = null;
        if (min == null) {
            err = "<= " + format(max);
        } else if (max == null) {
            err = ">= " + format(min);
        } else {
            err = format(min) + " - " + format(max);
        }
        return Constraint.of(CommandProcessUtils.msg("numberOutsideRange").replace("%range%", err), (c, v) -> (min == null || v.compareTo(min) >= 0) && (max == null || v.compareTo(max) <= 0));
    }

    private static String format(Number number) {
        String str = number.toString();
        int ind = str.indexOf('.');
        if (ind != -1) {
            int end = Math.min(str.length(), ind + 3);
            str = str.substring(0, end);
        }
        return str;
    }

}

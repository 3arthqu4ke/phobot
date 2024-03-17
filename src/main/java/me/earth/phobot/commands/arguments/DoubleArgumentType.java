package me.earth.phobot.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collection;

/**
 * A better {@link com.mojang.brigadier.arguments.DoubleArgumentType} which also allows us to parse numbers like "1.0E-7".
 */
@Getter
public class DoubleArgumentType implements ArgumentType<Double> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0", "1.2", "1.0E-7", ".5", "-1", "-.5", "-1234.56");

    private final double minimum;
    private final double maximum;

    private DoubleArgumentType(double minimum, double maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public static DoubleArgumentType doubleArg() {
        return doubleArg(-Double.MAX_VALUE);
    }

    public static DoubleArgumentType doubleArg(double min) {
        return doubleArg(min, Double.MAX_VALUE);
    }

    public static DoubleArgumentType doubleArg(double min, double max) {
        return new DoubleArgumentType(min, max);
    }

    public static double getDouble(CommandContext<?> context, String name) {
        return context.getArgument(name, Double.class);
    }

    @Override
    public Double parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        // double result = reader.readDouble();
        double result = readDoubleBetter(reader);
        if (result < minimum) {
            reader.setCursor(start);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooLow().createWithContext(reader, result, minimum);
        }

        if (result > maximum) {
            reader.setCursor(start);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooHigh().createWithContext(reader, result, maximum);
        }

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoubleArgumentType that)) return false;
        return maximum == that.maximum && minimum == that.minimum;
    }

    @Override
    public int hashCode() {
        return (int) (31 * minimum + maximum);
    }

    @Override
    public String toString() {
        if (minimum == -Double.MAX_VALUE && maximum == Double.MAX_VALUE) {
            return "double()";
        } else if (maximum == Double.MAX_VALUE) {
            return "double(" + minimum + ")";
        } else {
            return "double(" + minimum + ", " + maximum + ")";
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    private boolean isAllowedNumberBetter(char ch) {
        return StringReader.isAllowedNumber(ch) || ch == 'E' || ch == 'e'; // TODO: x, X for hex, I/i for Infinity, N for NaN etc. etc.?
    }

    private double readDoubleBetter(StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        while (reader.canRead() && isAllowedNumberBetter(reader.peek())) {
            reader.skip();
        }

        final String number = reader.getString().substring(start, reader.getCursor());
        if (number.isEmpty()) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedDouble().createWithContext(reader);
        }
        try {
            return Double.parseDouble(number);
        } catch (final NumberFormatException ex) {
            reader.setCursor(start);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidDouble().createWithContext(reader, number);
        }
    }

}

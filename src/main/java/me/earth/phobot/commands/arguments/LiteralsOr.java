package me.earth.phobot.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * An {@link ArgumentType} that delegates to an {@link ArgumentType} but also literals.
 */
public class LiteralsOr implements ArgumentType<Object> {
    @Getter
    private final Collection<String> examples;
    private final ArgumentType<?> argumentType;
    private final String[] literals;

    public LiteralsOr(ArgumentType<?> argumentType, String... literals) {
        this.examples = new ArrayList<>();
        this.argumentType = argumentType;
        this.literals = literals;
        examples.addAll(Arrays.asList(literals));
        examples.addAll(argumentType.getExamples());
    }

    @Override
    public Object parse(StringReader reader) throws CommandSyntaxException {
        int cursor = reader.getCursor();
        String potentialLiteral = reader.readUnquotedString();
        for (String literal : literals) {
            if (literal.equalsIgnoreCase(potentialLiteral)) {
                return literal;
            }
        }

        reader.setCursor(cursor);
        return argumentType.parse(reader);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        for (String literal : literals) {
            builder.suggest(literal);
        }

        return argumentType.listSuggestions(context, builder);
    }

}

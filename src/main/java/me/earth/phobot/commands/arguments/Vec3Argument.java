package me.earth.phobot.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * @see net.minecraft.commands.arguments.coordinates.Vec3Argument
 */
public class Vec3Argument implements ArgumentType<Vec3> {
    @Override
    public Vec3 parse(StringReader reader) throws CommandSyntaxException {
        int cursor = reader.getCursor();
        double x = readDouble(reader, cursor);
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(cursor);
            throw net.minecraft.commands.arguments.coordinates.Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
        }

        reader.skip();
        double y = readDouble(reader, cursor);
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(cursor);
            throw net.minecraft.commands.arguments.coordinates.Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
        }

        reader.skip();
        double z = readDouble(reader, cursor);
        return new Vec3(x, y, z);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> ctx, SuggestionsBuilder builder) {
        if (ctx.getSource() instanceof SharedSuggestionProvider sharedSuggestionProvider) {
            String string = builder.getRemaining();
            Collection<SharedSuggestionProvider.TextCoordinates> collection =
                    !string.isEmpty() && string.charAt(0) == '^'
                            ? Collections.singleton(SharedSuggestionProvider.TextCoordinates.DEFAULT_LOCAL)
                            : sharedSuggestionProvider.getRelevantCoordinates();

            return SharedSuggestionProvider.suggestCoordinates(
                    string, collection, builder, Commands.createValidator(this::parse));
        }

        return Suggestions.empty();
    }

    private static double readDouble(StringReader reader, int i) throws CommandSyntaxException {
        if (!reader.canRead()) {
            throw WorldCoordinate.ERROR_EXPECTED_DOUBLE.createWithContext(reader);
        }

        return reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0;
    }

}

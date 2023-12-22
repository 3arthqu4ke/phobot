package me.earth.phobot.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.earth.pingbypass.api.config.JsonParser;
import me.earth.pingbypass.api.config.impl.ConfigTypes;
import me.earth.pingbypass.api.setting.Setting;
import me.earth.pingbypass.api.setting.impl.BuilderWithFactory;
import me.earth.pingbypass.api.setting.impl.SettingImpl;
import net.minecraft.commands.arguments.UuidArgument;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UUIDSetting extends BuilderWithFactory<UUID, Setting<UUID>, UUIDSetting> {
    public UUIDSetting() {
        super(SettingImpl::new);
        this.withArgumentType(new ExtendedUUIDArgument())
                .withConfigType(ConfigTypes.SETTINGS)
                .withValue(UUID.randomUUID())
                .withParser(UUIDParser.INSTANCE);
    }

    public enum UUIDParser implements JsonParser<UUID> {
        INSTANCE;

        @Override
        public UUID deserialize(JsonElement jsonElement) {
            return UUID.fromString(jsonElement.getAsString());
        }

        @Override
        public JsonElement serialize(UUID uuid) {
            return new JsonPrimitive(uuid.toString());
        }
    }

    public static class ExtendedUUIDArgument extends UuidArgument {
        @Override
        public @NotNull UUID parse(StringReader stringReader) throws CommandSyntaxException {
            String string = stringReader.getRemaining();
            if ("random".equalsIgnoreCase(string.trim())) {
                return UUID.randomUUID();
            }

            return super.parse(stringReader);
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            for (int i = 0; i < 3; i++) {
                builder.suggest(UUID.randomUUID().toString());
            }

            return builder.buildFuture();
        }
    }

}

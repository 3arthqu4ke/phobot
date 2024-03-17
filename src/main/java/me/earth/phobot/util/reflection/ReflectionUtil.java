package me.earth.phobot.util.reflection;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Arrays;

@UtilityClass
public class ReflectionUtil {
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T> T getFieldValue(@Nullable Object object, Class<?> clazz, String... fieldNames) {
        Field field = getDeclaredField(clazz, fieldNames);
        field.setAccessible(true);
        return (T) field.get(object);
    }

    @SneakyThrows
    public void setFieldValue(@Nullable Object object, Object value, Class<?> clazz, String... fieldNames) {
        Field field = getDeclaredField(clazz, fieldNames);
        field.setAccessible(true);
        field.set(object, value);
    }

    public static Field getDeclaredField(Class<?> clazz, String... fieldNames) throws NoSuchFieldException {
        Field field = null;
        for (String fieldName : fieldNames) {
            try {
                Field currentField = clazz.getDeclaredField(fieldName);
                if (field != null) {
                    throw new NoSuchFieldException("Found multiple fields: " + currentField + " and " + field);
                }

                field = currentField;
            } catch (NoSuchFieldException ignored) { }
        }

        if (field == null) {
            throw new NoSuchFieldException("Failed to find fields " + Arrays.toString(fieldNames));
        }

        return field;
    }

}

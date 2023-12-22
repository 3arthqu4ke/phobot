package me.earth.phobot.asm;

import lombok.experimental.StandardException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.Arrays;

public interface MakesFieldVolatile {
    default void makeFieldVolatile(ClassNode classNode, Type fieldType, String... fieldNames) throws FailedToMakeFieldVolatileException {
        String found = null;
        for (FieldNode field : classNode.fields) {
            if (Type.getType(field.desc).equals(fieldType) && Arrays.asList(fieldNames).contains(field.name)) {
                if (found != null) {
                    throw new FailedToMakeFieldVolatileException("Found both field " + found + " and " + field.name + " in class " + classNode.name);
                } else {
                    found = field.name;
                    field.access |= Opcodes.ACC_VOLATILE;
                }
            }
        }

        if (found == null) {
            throw new FailedToMakeFieldVolatileException("Failed to find any of " + Arrays.toString(fieldNames) + " in " + classNode.name);
        }
    }

    @StandardException
    class FailedToMakeFieldVolatileException extends Exception { }

}

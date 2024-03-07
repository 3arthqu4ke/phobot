package me.earth.phobot.asm;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.util.world.DelegatingClientLevel;
import me.earth.pingbypass.api.launch.Transformer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Future client has a hook in the {@link ClientLevel} constructor or something.
 * Whenever we create a new {@link DelegatingClientLevel} we cause this hook to get called.
 * This is a problem, as it causes future to think a new world has been loaded, causing it to reset its FakePlayer, Surround and other modules.
 * So here, we make the Mixin hooks in {@link ClientLevel} and {@link Level} overridable, and then generate an overriding method in our DelegatingClientLevel.
 */
@Slf4j
public class FutureLevelPatch extends Transformer {
    private final String annotation = Type.getDescriptor(MixinMerged.class);
    private final Set<Method> methods = ConcurrentHashMap.newKeySet();

    public FutureLevelPatch() {
        super("net/minecraft/world/level/Level", "net/minecraft/class_1937", "net/minecraft/world/World",
                "net/minecraft/client/multiplayer/ClientLevel", "net/minecraft/class_638", "net/minecraft/client/world/ClientWorld",
                "me/earth/phobot/util/world/DelegatingClientLevel");
    }

    @Override
    public void transform(ClassNode classNode) {
        if ("me/earth/phobot/util/world/DelegatingClientLevel".equals(classNode.name)) {
            if (methods.isEmpty()) { // cant be, we got MixinClientLevel
                throw new IllegalStateException("Did not find any methods mixed into Level or ClientLevel!");
            }

            for (Method method : methods) {
                boolean found = false;
                for (MethodNode methodNode : classNode.methods) {
                    if (method.name.equals(methodNode.name) && method.desc.equals(methodNode.desc)) {
                        log.warn("DelegatingClientLevel already has method " + method.name + method.desc);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    log.info("Overriding method " + method.name + method.desc + " in DelegatingClientLevel.");
                    MethodNode methodNode = new MethodNode(method.access & ~Opcodes.ACC_ABSTRACT, method.name, method.desc, method.signature, method.exceptions);
                    classNode.methods.add(methodNode);
                    methodNode.visitCode();
                    methodNode.visitInsn(Opcodes.RETURN);
                    methodNode.visitMaxs(0, 0);
                    methodNode.visitEnd();
                }
            }
        } else {
            for (MethodNode mn : classNode.methods) {
                if (mn.visibleAnnotations == null) {
                    continue;
                }

                for (AnnotationNode annotationNode : mn.visibleAnnotations) {
                    if (annotation.equals(annotationNode.desc)) {
                        if ((mn.access & Opcodes.ACC_STATIC) != 0) {
                            log.debug("Found MixinMerged Annotation on " + classNode.name + "." + mn.name + mn.desc + ", but it was static!");
                        } else if (!Type.getReturnType(mn.desc).equals(Type.VOID_TYPE)) {
                            log.debug("Found MixinMerged Annotation on " + classNode.name + "." + mn.name + mn.desc + ", but it did not return void!");
                        } else {
                            log.debug("Found MixinMerged Annotation on " + classNode.name + "." + mn.name + mn.desc);
                            mn.access &= ~(Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL);
                            mn.access |= Opcodes.ACC_PUBLIC;
                            methods.add(new Method(mn.name, mn.desc, mn.signature, mn.access, mn.exceptions == null ? null : mn.exceptions.toArray(new String[0])));
                        }

                        break;
                    }
                }
            }
            // apparently mixin @Inject hooks get called with INVOKESPECIAL? This prevents the overriding method from being called
            for (MethodNode ctr : classNode.methods) {
                if ("<init>".equals(ctr.name)) {
                    for (AbstractInsnNode insn : ctr.instructions) {
                        if (insn instanceof MethodInsnNode methodInsn
                                && methodInsn.getOpcode() == Opcodes.INVOKESPECIAL
                                && methods.stream().anyMatch(method -> method.name.equals(methodInsn.name) && method.desc.equals(methodInsn.desc))) {
                            log.debug("Found INVOKESPECIAL Mixin MethodInsnNode " + methodInsn.name + methodInsn.desc + " in " + classNode.name + ".<init>" + ctr.desc);
                            methodInsn.setOpcode(Opcodes.INVOKEVIRTUAL);
                        }
                    }
                }
            }
        }
    }

    private record Method(String name, String desc, @Nullable String signature, int access, @Nullable String[] exceptions) {}

}

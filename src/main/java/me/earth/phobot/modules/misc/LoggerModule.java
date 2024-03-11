package me.earth.phobot.modules.misc;

import lombok.extern.slf4j.Slf4j;
import me.earth.phobot.commands.arguments.PacketArgumentType;
import me.earth.phobot.event.PacketBufferEvent;
import me.earth.phobot.modules.PhobotNameSpacedModule;
import me.earth.phobot.util.time.StopWatch;
import me.earth.pingbypass.PingBypass;
import me.earth.pingbypass.api.command.impl.arguments.ContainerArgumentType;
import me.earth.pingbypass.api.config.impl.Parsers;
import me.earth.pingbypass.api.event.listeners.generic.Listener;
import me.earth.pingbypass.api.event.network.PacketEvent;
import me.earth.pingbypass.api.module.impl.Categories;
import me.earth.pingbypass.api.setting.Setting;
import me.earth.pingbypass.api.setting.impl.types.container.Container;
import me.earth.pingbypass.api.setting.impl.types.container.ContainerSetting;
import me.earth.pingbypass.api.traits.Nameable;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// TODO: deobfuscation from 3arthh4ck, Fabric even has built in support
@Slf4j
public class LoggerModule extends PhobotNameSpacedModule {
    private final Setting<Boolean> incoming = bool("Incoming", true, "Logs incoming packets.");
    private final Setting<Boolean> outgoing = bool("Outgoing", true, "Logs outgoing packets.");
    private final Setting<Boolean> info = bool("Info", true, "Logs detailed information about the packets.");
    private final Setting<Boolean> chat = bool("Chat", false, "Logs in chat.");
    private final Setting<Boolean> stackTrace = bool("StackTrace", false, "Dumps the current Stacktrace.");
    private final Setting<Boolean> buffer = bool("Buffer", false, "Dumps packets from the PacketBuffer.");
    private final Setting<Integer> lineCap = number("LineCap", 50, 1, 1_000_000, "Maximum length of log.");
    private final Setting<Boolean> total = bool("Total", true, "Combines incoming and outgoing timers.");
    private final Setting<Filter> filterType = constant("Filter", Filter.Off, "If you want to filter packets and if the filter should exclude or include them.");
    private final Setting<Container<Nameable>> filter = ContainerSetting.Builder
            .of(ServerboundResourcePackPacket.class.getSimpleName(), ServerboundKeepAlivePacket.class.getSimpleName(), ServerboundPongPacket.class.getSimpleName())
            .withName("Packets")
            .withDescription("The packets to exclude or include based on the filter.")
            .withArgumentTypeFactory(setting -> new ContainerArgumentType<>(Parsers.NAME, new PacketArgumentType(), List.of("add " + ClientboundBundlePacket.class.getSimpleName()), setting))
            .registerOnBuild(this)
            .build();

    private final StopWatch.ForMultipleThreads lastIncoming = new StopWatch.ForMultipleThreads();
    private final StopWatch.ForMultipleThreads lastOutgoing = new StopWatch.ForMultipleThreads();

    public LoggerModule(PingBypass pingBypass) {
        super(pingBypass, "Logger", Categories.MISC, "Logs incoming and outgoing packets.");
        lastIncoming.reset();
        lastOutgoing.reset();
        listen(new Listener<PacketEvent.Receive<?>>(Integer.MIN_VALUE) {
            @Override
            public void onEvent(PacketEvent.Receive<?> event) {
                // TODO: use the new changes in PingBypass, no cast required at IConnection
                if (event.getConnection() instanceof Connection connection && connection.getReceiving() == PacketFlow.CLIENTBOUND) {
                    logPacket(event.getPacket(), "Incoming ", event.isCancelled(), chat.getValue(), incoming.getValue(), total.getValue() ? lastOutgoing : lastIncoming);
                }
            }
        });

        listen(new Listener<PacketEvent.Send<?>>(Integer.MIN_VALUE) {
            @Override
            public void onEvent(PacketEvent.Send<?> event) {
                // TODO: use the new changes in PingBypass, no cast required at IConnection
                if (!buffer.getValue() && event.getConnection() instanceof Connection connection && connection.getReceiving() == PacketFlow.CLIENTBOUND) {
                    logPacket(event.getPacket(), "Outgoing ", event.isCancelled(), chat.getValue(), outgoing.getValue(), lastOutgoing);
                }
            }
        });

        listen(new Listener<PacketBufferEvent>(Integer.MIN_VALUE) {
            @Override
            public void onEvent(PacketBufferEvent event) {
                if (buffer.getValue()) {
                    event.buffer().markReaderIndex();
                    try {
                        FriendlyByteBuf buf = new FriendlyByteBuf(event.buffer());
                        int id = buf.readVarInt();
                        Packet<?> packet = ConnectionProtocol.PLAY.codec(PacketFlow.SERVERBOUND).createPacket(id, buf);
                        logPacket(packet, "Outgoing ", false, chat.getValue(), outgoing.getValue(), lastOutgoing);
                    } catch (Exception e) {
                        log.error("Error reading packet from buffer", e);
                    }

                    event.buffer().resetReaderIndex();
                }
            }
        });
    }

    public void logPacket(Packet<?> packet, String message, boolean cancelled, boolean allowChat, boolean shouldLog, StopWatch.ForMultipleThreads stopWatch) {
        if (!shouldLog || !filterType.getValue().shouldLog(packet, filter)) {
            return;
        }

        long time = stopWatch.getPassedTime();
        stopWatch.reset();
        String packetName = packet.getClass().getSimpleName();
        var out = new StringBuilder(message).append(packetName).append(", cancelled : ").append(cancelled).append(", time : ").append(time).append("ms\n");
        appendInfo(out, packet);
        String s = out.toString();
        printChat(s, allowChat);
        log.info(s);
        if (stackTrace.getValue()) {
            Thread.dumpStack();
        }
    }

    private void appendInfo(StringBuilder out, Packet<?> packet) {
        if (info.getValue()) {
            logObject(packet, "    ", out, new HashSet<>());
        }
    }

    private void logObject(Object object, String indent, StringBuilder out, Set<Object> logged) {
        logged.add(object);
        try {
            Class<?> clazz = object.getClass();
            while (clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (field != null) {
                        if (Modifier.isStatic(field.getModifiers())) {
                            continue;
                        }

                        // TODO: ArrayList cannot be made accessible, catch InaccessibleOjectException and log to String?
                        field.setAccessible(true);

                        Object obj = field.get(object);
                        String objToString;
                        if (obj != null && obj.getClass().isArray()) {
                            StringBuilder builder = new StringBuilder("[");
                            for (int i = 0; i < Array.getLength(obj); i++) {
                                builder.append(Array.get(obj, i));
                                if (i < Array.getLength(obj) - 1) {
                                    builder.append(", ");
                                }
                            }

                            objToString = builder.append("]").toString();
                        } else {
                            objToString = String.valueOf(obj);
                            if (objToString.contains("@") && !(obj instanceof String)) { // TODO: better detection of default toString()
                                if (logged.contains(obj)) {
                                    objToString += " (already logged)";
                                } else {
                                    out.append(indent).append(field.getName()).append(" : ").append("\n");
                                    logObject(obj, indent + "    ", out, logged);
                                    continue;
                                }
                            }
                        }

                        if (objToString.length() > lineCap.getValue()) {
                            objToString = objToString.substring(0, lineCap.getValue());
                        }

                        out.append(indent).append(field.getName()).append(" : ").append(objToString).append("\n");
                    }
                }

                clazz = clazz.getSuperclass();
            }
        } catch (IllegalAccessException e) {
            log.error("Error while logging " + object, e);
        }
    }

    private void printChat(String message, boolean allowChat) {
        if (allowChat) {
            mc.submit(() -> getPingBypass().getChat().sendWithoutLogging(Component.literal(message)));
        }
    }

    public enum Filter {
        Off {
            @Override
            public boolean shouldLog(Packet<?> packet, Setting<Container<Nameable>> setting) {
                return true;
            }
        },
        Exclude {
            @Override
            public boolean shouldLog(Packet<?> packet, Setting<Container<Nameable>> setting) {
                return !setting.getValue().contains(packet.getClass().getSimpleName());
            }
        },
        Include {
            @Override
            public boolean shouldLog(Packet<?> packet, Setting<Container<Nameable>> setting) {
                return setting.getValue().contains(packet.getClass().getSimpleName());
            }
        };

        public abstract boolean shouldLog(Packet<?> packet, Setting<Container<Nameable>> setting);
    }

}

package me.earth.phobot.commands;

import me.earth.phobot.Phobot;
import me.earth.pingbypass.api.command.AbstractPbCommand;

public abstract class AbstractPhobotCommand extends AbstractPbCommand {
    protected final Phobot phobot;

    public AbstractPhobotCommand(String name, String description, Phobot phobot) {
        super(name, description, phobot.getPingBypass(), phobot.getMinecraft());
        this.phobot = phobot;
    }

}

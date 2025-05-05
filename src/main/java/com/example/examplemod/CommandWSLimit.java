package com.example.examplemod;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class CommandWSLimit extends CommandBase {

    @Override
    public String getCommandName() {
        return "wslimit";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/wslimit <number>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /wslimit <number>"));
            return;
        }

        try {
            int newLimit = Integer.parseInt(args[0]);
            MMCOverlay.setWSLimit(newLimit);


            EnumChatFormatting color = EnumChatFormatting.GRAY;
            if (newLimit >= 7 && newLimit <= 19) {
                color = EnumChatFormatting.YELLOW;
            } else if (newLimit >= 20 && newLimit <= 39) {
                color = EnumChatFormatting.RED;
            } else if (newLimit >= 40 && newLimit <= 69) {
                color = EnumChatFormatting.DARK_RED;
            } else if (newLimit >= 70) {
                color = EnumChatFormatting.LIGHT_PURPLE;
            }

            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.GREEN + "ws limit: " + color + newLimit
            ));
        } catch (NumberFormatException e) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Invalid number."));
        }
    }


    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}

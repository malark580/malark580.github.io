package com.malark.aibuddy;

import com.malark.aibuddy.buddy.BuddyController;
import com.malark.aibuddy.command.BuddyCommand;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(AIBuddyMod.MOD_ID)
public class AIBuddyMod {
    public static final String MOD_ID = "aibuddy";
    private static final Logger LOGGER = LogUtils.getLogger();

    public AIBuddyMod() {
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("AI Buddy loaded. Use /buddy spawn to start.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        BuddyCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            BuddyController.tickAll();
        }
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        String raw = event.getMessage().getString().trim();
        if (raw.toLowerCase().startsWith("@buddy")) {
            String prompt = raw.substring("@buddy".length()).trim();
            BuddyController.handlePrompt(event.getPlayer(), prompt);
        }
    }
}

package com.malark.aibuddy.buddy;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class BuddyController {
    private static final Map<UUID, BuddyState> STATES = new HashMap<>();
    private static final RandomSource RNG = RandomSource.create();

    private BuddyController() {
    }

    public static void spawnFor(ServerPlayer owner) {
        FakePlayer buddy = getOrCreateBuddy(owner);
        buddy.teleportTo(owner.getX() + 2, owner.getY(), owner.getZ() + 2);
        say(owner, "Hey! I'm here. Ask me to mine or build.");
    }

    public static void mineFor(ServerPlayer owner) {
        BuddyState state = STATES.computeIfAbsent(owner.getUUID(), id -> new BuddyState(getOrCreateBuddy(owner)));
        state.intent = BuddyIntent.MINE;
        say(owner, "On it. I'll gather something nearby.");
    }

    public static void buildFor(ServerPlayer owner) {
        BuddyState state = STATES.computeIfAbsent(owner.getUUID(), id -> new BuddyState(getOrCreateBuddy(owner)));
        state.intent = BuddyIntent.BUILD;
        say(owner, "I'll try placing a block close to you.");
    }

    public static void handlePrompt(ServerPlayer owner, String prompt) {
        String normalized = prompt.toLowerCase();
        if (normalized.contains("mine") || normalized.contains("gather")) {
            mineFor(owner);
            return;
        }
        if (normalized.contains("build") || normalized.contains("place")) {
            buildFor(owner);
            return;
        }
        if (normalized.contains("come") || normalized.contains("follow")) {
            spawnFor(owner);
            return;
        }

        say(owner, "Try telling me: mine, build, or follow.");
    }

    public static void tickAll() {
        for (Map.Entry<UUID, BuddyState> entry : STATES.entrySet()) {
            BuddyState state = entry.getValue();
            ServerPlayer owner = state.buddy.server.getPlayerList().getPlayer(entry.getKey());
            if (owner == null) {
                continue;
            }

            state.buddy.teleportTo(owner.getX() + 1.5, owner.getY(), owner.getZ() + 1.5);

            if (state.cooldownTicks > 0) {
                state.cooldownTicks--;
                continue;
            }

            if (state.intent == BuddyIntent.MINE) {
                doMine(owner, state);
            } else if (state.intent == BuddyIntent.BUILD) {
                doBuild(owner, state);
            }
        }
    }

    private static void doMine(ServerPlayer owner, BuddyState state) {
        Optional<BlockPos> maybeTarget = findMineable(owner);
        if (maybeTarget.isEmpty()) {
            say(owner, "I couldn't find anything to mine close by.");
            state.cooldownTicks = 60;
            return;
        }

        BlockPos target = maybeTarget.get();
        ServerLevel level = owner.serverLevel();
        ItemStack drop = new ItemStack(level.getBlockState(target).getBlock().asItem());
        level.destroyBlock(target, false);
        state.buddy.getInventory().placeItemBackInInventory(drop);

        say(owner, "Mined " + drop.getHoverName().getString() + ".");
        state.cooldownTicks = 40;
    }

    private static void doBuild(ServerPlayer owner, BuddyState state) {
        Inventory inventory = state.buddy.getInventory();
        int cobbleSlot = findItem(inventory, Items.COBBLESTONE);
        if (cobbleSlot < 0) {
            say(owner, "I need cobblestone first. Ask me to mine.");
            state.cooldownTicks = 60;
            return;
        }

        BlockPos placePos = owner.blockPosition().offset(RNG.nextInt(3) - 1, 0, RNG.nextInt(3) - 1);
        if (!owner.serverLevel().getBlockState(placePos).isAir()) {
            placePos = placePos.above();
        }

        owner.serverLevel().setBlockAndUpdate(placePos, Blocks.COBBLESTONE.defaultBlockState());
        inventory.removeItem(cobbleSlot, 1);
        say(owner, "Placed cobblestone at " + placePos.toShortString() + ".");
        state.cooldownTicks = 40;
    }

    private static Optional<BlockPos> findMineable(ServerPlayer owner) {
        BlockPos center = owner.blockPosition();
        ServerLevel level = owner.serverLevel();

        for (int y = -1; y <= 2; y++) {
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!level.getBlockState(pos).isAir() && !level.getBlockState(pos).is(Blocks.BEDROCK)) {
                        return Optional.of(pos);
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static int findItem(Inventory inventory, net.minecraft.world.item.Item item) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(item)) {
                return i;
            }
        }
        return -1;
    }

    private static FakePlayer getOrCreateBuddy(ServerPlayer owner) {
        return STATES.computeIfAbsent(owner.getUUID(), ignored -> {
            GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("buddy-" + owner.getGameProfile().getName()).getBytes()), "BuddyAI");
            FakePlayer fakePlayer = FakePlayerFactory.get((ServerLevel) owner.level(), profile);
            return new BuddyState(fakePlayer);
        }).buddy;
    }

    private static void say(ServerPlayer owner, String text) {
        owner.server.getPlayerList().broadcastSystemMessage(
                Component.literal("[BuddyAI] " + text).withStyle(ChatFormatting.AQUA),
                false
        );
    }

    private enum BuddyIntent {
        IDLE,
        MINE,
        BUILD
    }

    private static final class BuddyState {
        private final FakePlayer buddy;
        private BuddyIntent intent = BuddyIntent.IDLE;
        private int cooldownTicks = 0;

        private BuddyState(FakePlayer buddy) {
            this.buddy = buddy;
        }
    }
}

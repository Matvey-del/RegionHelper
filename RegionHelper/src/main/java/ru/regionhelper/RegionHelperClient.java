package ru.regionhelper;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.HashMap;
import java.util.Map;

public final class RegionHelperClient implements ClientModInitializer {
    private static final long DISPLAY_MILLIS = 60_000L;
    private static final Map<Item, Marker> MARKERS = new HashMap<>();
    private static final Map<BlockPos, Long> RECENT = new HashMap<>();

    static {
        MARKERS.put(ItemsCompat.IRON_BLOCK, new Marker(3, 0xFFFF5555));
        MARKERS.put(ItemsCompat.GOLD_BLOCK, new Marker(5, 0xFFFFAA00));
        MARKERS.put(ItemsCompat.DIAMOND_BLOCK, new Marker(7, 0xFF55FFFF));
        MARKERS.put(ItemsCompat.DIAMOND_ORE, new Marker(10, 0xFF5555FF));
        MARKERS.put(ItemsCompat.EMERALD_ORE, new Marker(15, 0xFF55FF55));
        MARKERS.put(ItemsCompat.COMMAND_BLOCK, new Marker(25, 0xFFFF55FF));
    }

    @Override
    public void onInitializeClient() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient && player instanceof ClientPlayerEntity clientPlayer && hand == Hand.MAIN_HAND) {
                ItemStack stack = clientPlayer.getStackInHand(hand);
                Marker marker = MARKERS.get(stack.getItem());
                if (marker != null) {
                    BlockPos target = hit.getBlockPos().offset(hit.getSide());
                    // Store only as a candidate. The tick handler confirms the actual placed block.
                    PendingPlacement.pending = new Pending(target.toImmutable(), stack.getItem(), System.currentTimeMillis());
                }
            }
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Pending p = PendingPlacement.pending;
            if (p != null && client.world != null && System.currentTimeMillis() - p.time < 1500L) {
                Block expected = blockForItem(p.item);
                if (expected != null && client.world.getBlockState(p.pos).isOf(expected)) {
                    RECENT.put(p.pos, System.currentTimeMillis());
                    PendingPlacement.pending = null;
                }
            } else if (p != null && System.currentTimeMillis() - p.time >= 1500L) {
                PendingPlacement.pending = null;
            }
            long now = System.currentTimeMillis();
            RECENT.entrySet().removeIf(e -> now - e.getValue() > DISPLAY_MILLIS);
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> render(
                context.matrixStack(), context.consumers(), context.camera().getPos(),
                MinecraftClient.getInstance()));
    }

    private static void render(MatrixStack matrices,
                                net.minecraft.client.render.VertexConsumerProvider consumers,
                                Vec3d cameraPos, MinecraftClient client) {
        if (client.world == null || client.player == null || matrices == null || consumers == null) return;

        Marker held = MARKERS.get(client.player.getMainHandStack().getItem());
        Map<BlockPos, Marker> toRender = new HashMap<>();

        if (held != null) {
            BlockHitResult hit = client.crosshairTarget instanceof BlockHitResult bhr ? bhr : null;
            if (hit != null) {
                BlockPos center = hit.getBlockPos();
                toRender.put(center, held);
            }
        }

        long now = System.currentTimeMillis();
        RECENT.forEach((pos, time) -> {
            if (now - time <= DISPLAY_MILLIS) {
                Marker marker = MARKERS.get(itemForBlock(client.world.getBlockState(pos).getBlock()));
                if (marker != null) toRender.put(pos, marker);
            }
        });

        if (toRender.isEmpty()) return;
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        for (Map.Entry<BlockPos, Marker> entry : toRender.entrySet()) {
            drawSquare(matrices, lines, entry.getKey(), entry.getValue());
        }
        matrices.pop();
    }

    private static void drawSquare(MatrixStack matrices, VertexConsumer lines, BlockPos center, Marker marker) {
        // The side length is measured in Minecraft blocks. Odd dimensions put the marker at the exact center.
        int lowOffset = (marker.size - 1) / 2;
        double minX = center.getX() - lowOffset;
        double maxX = minX + marker.size;
        double minZ = center.getZ() - lowOffset;
        double maxZ = minZ + marker.size;
        double y = center.getY() + 0.02;

        // Four sides have distinct colors. Each side is drawn as its own line segment.
        drawLine(matrices, lines, minX, y, minZ, maxX, y, minZ, 1.0f, 0.25f, 0.25f, 1.0f);
        drawLine(matrices, lines, maxX, y, minZ, maxX, y, maxZ, 1.0f, 0.75f, 0.1f, 1.0f);
        drawLine(matrices, lines, maxX, y, maxZ, minX, y, maxZ, 0.2f, 0.9f, 0.35f, 1.0f);
        drawLine(matrices, lines, minX, y, maxZ, minX, y, minZ, 0.25f, 0.55f, 1.0f, 1.0f);
    }

    private static void drawLine(MatrixStack matrices, VertexConsumer consumer,
                                 double x1, double y1, double z1, double x2, double y2, double z2,
                                 float r, float g, float b, float a) {
        var entry = matrices.peek();
        consumer.vertex(entry, (float)x1, (float)y1, (float)z1).color(r, g, b, a)
                .normal(entry, 0.0f, 1.0f, 0.0f);
        consumer.vertex(entry, (float)x2, (float)y2, (float)z2).color(r, g, b, a)
                .normal(entry, 0.0f, 1.0f, 0.0f);
    }

    private static Item itemForBlock(Block block) {
        if (block == Blocks.IRON_BLOCK) return ItemsCompat.IRON_BLOCK;
        if (block == Blocks.GOLD_BLOCK) return ItemsCompat.GOLD_BLOCK;
        if (block == Blocks.DIAMOND_BLOCK) return ItemsCompat.DIAMOND_BLOCK;
        if (block == Blocks.DIAMOND_ORE) return ItemsCompat.DIAMOND_ORE;
        if (block == Blocks.EMERALD_ORE) return ItemsCompat.EMERALD_ORE;
        if (block == Blocks.COMMAND_BLOCK) return ItemsCompat.COMMAND_BLOCK;
        return null;
    }

    private static Block blockForItem(Item item) {
        if (item == ItemsCompat.IRON_BLOCK) return Blocks.IRON_BLOCK;
        if (item == ItemsCompat.GOLD_BLOCK) return Blocks.GOLD_BLOCK;
        if (item == ItemsCompat.DIAMOND_BLOCK) return Blocks.DIAMOND_BLOCK;
        if (item == ItemsCompat.DIAMOND_ORE) return Blocks.DIAMOND_ORE;
        if (item == ItemsCompat.EMERALD_ORE) return Blocks.EMERALD_ORE;
        if (item == ItemsCompat.COMMAND_BLOCK) return Blocks.COMMAND_BLOCK;
        return null;
    }

    private record Marker(int size, int color) {}
    private record Pending(BlockPos pos, Item item, long time) {}
    private static final class PendingPlacement { private static Pending pending; }
}

package com.junggyeol.mininggacha;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * /mg 명령어 등록과 처리
 * - convertGui : 클라이언트로 GUI 열라고 요청 (서버 -> 클라이언트 패킷)
 * - convertPoints : 기존 convertPoints 명령 (인벤토리 전체 검사, 전체 스택 전환)
 * - 부분 전환은 GUI 또는 /mg convertPartial <slot> <count>로 직접 구현 가능 (현재 GUI 제공)
 */
public class GachaCommands {
    private static final GachaService gachaService = new GachaService();

    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mg")
                .then(Commands.literal("setQuitChance")
                        .then(Commands.argument("chance", DoubleArgumentType.doubleArg(0.0, 1.0))
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> setQuitChance(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "chance")))))
                .then(Commands.literal("setGachaCost")
                        .then(Commands.argument("cost", IntegerArgumentType.integer(0))
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> setGachaCost(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "cost")))))
                .then(Commands.literal("setOrePoints")
                        .then(Commands.argument("ore", StringArgumentType.string())
                                .then(Commands.argument("points", IntegerArgumentType.integer(0))
                                        .requires(src -> src.hasPermission(2))
                                        .executes(ctx -> setOrePoints(ctx.getSource(), StringArgumentType.getString(ctx, "ore"), IntegerArgumentType.getInteger(ctx, "points"))))))
                .then(Commands.literal("givePoints")
                        .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                .then(Commands.argument("points", IntegerArgumentType.integer())
                                        .requires(src -> src.hasPermission(2))
                                        .executes(ctx -> givePoints(ctx.getSource(), net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "points"))))))
                .then(Commands.literal("tryGacha")
                        .executes(ctx -> tryGacha(ctx.getSource())))
                .then(Commands.literal("convertPoints")
                        .executes(ctx -> convertPoints(ctx.getSource())))
                .then(Commands.literal("convertGui")
                        .executes(ctx -> openConvertGui(ctx.getSource())))
        );
    }

    private static int setQuitChance(CommandSourceStack src, double chance) {
        MiningGachaMod.CONFIG.quitChance = chance;
        MiningGachaMod.CONFIG.save();
        src.sendSuccess(Component.literal("Set quit chance to " + chance), true);
        return 1;
    }

    private static int setGachaCost(CommandSourceStack src, int cost) {
        MiningGachaMod.CONFIG.gachaCost = cost;
        MiningGachaMod.CONFIG.save();
        src.sendSuccess(Component.literal("Set gacha cost to " + cost), true);
        return 1;
    }

    private static int setOrePoints(CommandSourceStack src, String ore, int points) {
        MiningGachaMod.CONFIG.orePoints.put(ore, points);
        MiningGachaMod.CONFIG.save();
        src.sendSuccess(Component.literal("Set ore " + ore + " points to " + points), true);
        return 1;
    }

    private static int givePoints(CommandSourceStack src, ServerPlayer player, int points) {
        player.getCapability(com.junggyeol.mininggacha.player.PlayerPointsProvider.PLAYER_POINTS_CAP).ifPresent(pp -> {
            pp.addPoints(points);
            // 동기화
            com.junggyeol.mininggacha.network.NetworkHandler.CHANNEL.sendToPlayer(new com.junggyeol.mininggacha.network.SyncPlayerDataPacket(pp.getPoints(), pp.isAllowedToQuit()), player);
        });
        src.sendSuccess(Component.literal("Gave " + points + " points to " + player.getName().getString()), true);
        return 1;
    }

    private static int tryGacha(CommandSourceStack src) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            player.getCapability(com.junggyeol.mininggacha.player.PlayerPointsProvider.PLAYER_POINTS_CAP).ifPresent(pp -> {
                try {
                    boolean success = gachaService.tryGacha(pp);
                    MiningGachaMod.LOGGER.info("Player {} tried gacha: success={} (points left={})", player.getName().getString(), success, pp.getPoints());
                    // 동기화
                    com.junggyeol.mininggacha.network.NetworkHandler.CHANNEL.sendToPlayer(new com.junggyeol.mininggacha.network.SyncPlayerDataPacket(pp.getPoints(), pp.isAllowedToQuit()), player);
                    player.sendSystemMessage(Component.literal(success ? "Gacha success! You may now quit." : "Gacha failed."));
                } catch (IllegalStateException ex) {
                    player.sendSystemMessage(Component.literal("Not enough points to try gacha."));
                }
            });
        } catch (Exception e) {
            src.sendFailure(Component.literal("This command can only be used by a player."));
        }
        return 1;
    }

    /**
     * 기존 전체 전환(인벤토리 전체 탐색하여 스택 전체를 전환) 명령
     */
    private static int convertPoints(CommandSourceStack src) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            // 서버 전용 작업
            int totalGained = 0;

            // iterate over inventory (server-side)
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                var stack = inventory.getItem(i);
                if (stack.isEmpty()) continue;
                String itemId = stack.getItem().getRegistryName() != null ? stack.getItem().getRegistryName().toString() : stack.getItem().toString();

                Integer perPoint = MiningGachaMod.CONFIG.orePoints.get(itemId);
                if (perPoint != null && perPoint > 0) {
                    int count = stack.getCount();
                    int gained = perPoint * count;
                    totalGained += gained;

                    // remove the stack
                    inventory.removeItem(i, count);

                    // add points to capability
                    player.getCapability(com.junggyeol.mininggacha.player.PlayerPointsProvider.PLAYER_POINTS_CAP).ifPresent(pp -> {
                        pp.addPoints(gained);
                    });
                }
            }

            // 동기화: capability -> 클라이언트
            player.getCapability(com.junggyeol.mininggacha.player.PlayerPointsProvider.PLAYER_POINTS_CAP).ifPresent(pp -> {
                com.junggyeol.mininggacha.network.NetworkHandler.CHANNEL.sendToPlayer(new com.junggyeol.mininggacha.network.SyncPlayerDataPacket(pp.getPoints(), pp.isAllowedToQuit()), player);
                player.sendSystemMessage(Component.literal("Converted items into " + totalGained + " points. Total points: " + pp.getPoints()));
            });

            MiningGachaMod.LOGGER.info("Player {} converted inventory into {} points.", player.getName().getString(), totalGained);
        } catch (Exception e) {
            src.sendFailure(Component.literal("This command can only be used by a player."));
        }
        return 1;
    }

    /**
     * 서버 -> 클라이언트로 GUI 열라고 패킷 전송
     */
    private static int openConvertGui(CommandSourceStack src) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            // 서버가 해당 플레이어에게 GUI 열라는 패킷을 보냄
            com.junggyeol.mininggacha.network.NetworkHandler.CHANNEL.sendToPlayer(new com.junggyeol.mininggacha.network.OpenConvertGuiPacket(), player);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }
    }
}
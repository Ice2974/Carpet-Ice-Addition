package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

public final class PhantomSpawnWarningHelper {
    private static final long NIGHT_WARNING_TICK = 13000L;
    private static final long NIGHT_WARNING_END_TICK = 13020L;
    private static final int MIN_TIME_SINCE_REST = 72000;
    private static final String WARNING_MESSAGE_ZH = "\u4eca\u665a\u53ef\u80fd\u4f1a\u751f\u6210\u5e7b\u7ffc\uff0c\u5efa\u8bae\u7761\u89c9\u3002";
    private static long lastProcessedNight = Long.MIN_VALUE;

    private PhantomSpawnWarningHelper() {
    }

    public static void tick(ServerLevel world) {
        if (!Level.OVERWORLD.equals(world.dimension())) {
            return;
        }

        long dayTime = world.getOverworldClockTime();
        long dayTick = dayTime % 24000L;
        if (dayTick < NIGHT_WARNING_TICK || dayTick >= NIGHT_WARNING_END_TICK) {
            return;
        }

        long nightIndex = dayTime / 24000L;
        if (nightIndex == lastProcessedNight) {
            return;
        }
        lastProcessedNight = nightIndex;

        if (!CarpetIceAdditionSettings.phantomSpawnWarning
                || !CarpetIceAdditionMod.shouldEnablePhantomSpawnWarning()
                || world.getDifficulty() == Difficulty.PEACEFUL
                || !isDoInsomniaEnabled(world)
                || isServerAccelerated(world)) {
            return;
        }

        if (world.getServer() == null || world.getServer().getPlayerList() == null) {
            return;
        }

        List<ServerPlayer> onlinePlayers = world.getServer().getPlayerList().getPlayers();
        if (onlinePlayers.isEmpty()) {
            return;
        }

        if (!hasInsomniaThresholdPlayer(onlinePlayers)) {
            return;
        }
        for (ServerPlayer player : onlinePlayers) {
            player.sendSystemMessage(Component.literal(WARNING_MESSAGE_ZH), false);
        }
    }

    private static boolean hasInsomniaThresholdPlayer(List<ServerPlayer> onlinePlayers) {
        Stat<?> stat = Stats.CUSTOM.get(Stats.TIME_SINCE_REST);
        for (ServerPlayer player : onlinePlayers) {
            if (player.getStats().getValue(stat) >= MIN_TIME_SINCE_REST) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDoInsomniaEnabled(ServerLevel world) {
        try {
            Object gameRules = world.getGameRules();
            Object ruleKey = resolveInsomniaRuleKey();
            if (ruleKey == null) {
                return true;
            }
            Method getBoolean = gameRules.getClass().getMethod("getBoolean", ruleKey.getClass());
            Object value = getBoolean.invoke(gameRules, ruleKey);
            return value instanceof Boolean && (Boolean) value;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static Object resolveInsomniaRuleKey() {
        String[] classNames = {"net.minecraft.world.GameRules", "net.minecraft.world.level.GameRules"};
        String[] fieldNames = {"RULE_DOINSOMNIA", "DO_INSOMNIA", "DOINSOMNIA"};
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                for (String fieldName : fieldNames) {
                    try {
                        Field field = clazz.getField(fieldName);
                        if (Modifier.isStatic(field.getModifiers())) {
                            return field.get(null);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static boolean isServerAccelerated(ServerLevel world) {
        var server = world.getServer();
        if (server == null) {
            return false;
        }
        try {
            var tickManager = server.tickRateManager();
            if (tickManager != null) {
                if (tickManager.isSprinting()) {
                    return true;
                }
                if (tickManager.tickrate() > 20.0F) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // Keep running when server tick manager APIs are unavailable.
        }
        Boolean carpet = isCarpetAccelerationActive();
        return Boolean.TRUE.equals(carpet);
    }

    private static Boolean isCarpetAccelerationActive() {
        String[] classNames = {"carpet.helpers.TickSpeed", "carpet.utils.TickSpeed"};
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                Boolean sprinting = invokeBooleanNoArgNullable(clazz, "isSprinting", "isWarping", "isWarpRunning");
                if (Boolean.TRUE.equals(sprinting)) {
                    return true;
                }
                if (staticLongGreaterThanZero(clazz, "time_warp_start_time", "timeWarpStartTime", "time_warp_scheduled_ticks", "timeWarpScheduledTicks", "warp_ticks_to_run", "warpTicksToRun")) {
                    return true;
                }
                Double tickRate = getStaticDouble(clazz, "tickrate", "tickRate", "msptGoal");
                if (tickRate != null && tickRate > 20.0D) {
                    return true;
                }
                if (sprinting != null || tickRate != null) {
                    return false;
                }
            } catch (Throwable ignored) {
                // Safe downgrade: if internal symbols are unavailable on a platform, skip this detector.
            }
        }
        return null;
    }

    private static Object invokeNoArg(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                return method.invoke(target);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Boolean invokeBooleanNoArgNullable(Object target, String... methodNames) {
        Class<?> clazz = target instanceof Class<?> ? (Class<?>) target : target.getClass();
        boolean attempted = false;
        for (String methodName : methodNames) {
            try {
                Method method = clazz.getMethod(methodName);
                attempted = true;
                if (target instanceof Class<?>) {
                    if (!Modifier.isStatic(method.getModifiers())) {
                        continue;
                    }
                    Object value = method.invoke(null);
                    if (value instanceof Boolean && (Boolean) value) {
                        return true;
                    }
                } else {
                    Object value = method.invoke(target);
                    if (value instanceof Boolean && (Boolean) value) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return attempted ? Boolean.FALSE : null;
    }

    private static Double invokeDoubleNoArg(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object value = method.invoke(target);
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static boolean staticLongGreaterThanZero(Class<?> clazz, String... fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                if (!Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(null);
                if (value instanceof Number && ((Number) value).longValue() > 0L) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static Double getStaticDouble(Class<?> clazz, String... fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                if (!Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(null);
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}






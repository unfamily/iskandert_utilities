package net.unfamily.iskautils;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = IskaUtils.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Vector plates speed configuration, ordered from slowest to fastest
    private static final ModConfigSpec.DoubleValue SLOW_VECTOR_SPEED = BUILDER
            .comment("Speed of the Slow Vector Plate")
            .defineInRange("0_slowVectorSpeed", 0.1D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue MODERATE_VECTOR_SPEED = BUILDER
            .comment("Speed of the Moderate Vector Plate")
            .defineInRange("1_moderateVectorSpeed", 0.5D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue FAST_VECTOR_SPEED = BUILDER
            .comment("Speed of the Fast Vector Plate")
            .defineInRange("2_fastVectorSpeed", 1.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue EXTREME_VECTOR_SPEED = BUILDER
            .comment("Speed of the Extreme Vector Plate")
            .defineInRange("3_extremeVectorSpeed", 5.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ULTRA_VECTOR_SPEED = BUILDER
            .comment("Speed of the Ultra Vector Plate")
            .defineInRange("4_ultraVectorSpeed", 15.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.IntValue HELLFIRE_IGNITER_CONSUME = BUILDER
            .comment("Amount of energy consumed by the Hellfire Igniter")
            .defineInRange("5_hellfireIgniterConsume", 10, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue HELLFIRE_IGNITER_BUFFER = BUILDER
            .comment("Quantity of energy the Hellfire Igniter can be stored")
            .defineInRange("6_hellfireIgniterBuffer", 1000, 0, Integer.MAX_VALUE);

    static final ModConfigSpec SPEC = BUILDER.build();

    // Public variables to access configuration values
    public static double slowVectorSpeed;
    public static double moderateVectorSpeed;
    public static double fastVectorSpeed;
    public static double extremeVectorSpeed;
    public static double ultraVectorSpeed;
    public static int hellfireIgniterConsume;
    public static int hellfireIgniterBuffer;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        slowVectorSpeed = SLOW_VECTOR_SPEED.get();
        moderateVectorSpeed = MODERATE_VECTOR_SPEED.get();
        fastVectorSpeed = FAST_VECTOR_SPEED.get();
        extremeVectorSpeed = EXTREME_VECTOR_SPEED.get();
        ultraVectorSpeed = ULTRA_VECTOR_SPEED.get();
        hellfireIgniterConsume = HELLFIRE_IGNITER_CONSUME.get();
        hellfireIgniterBuffer = HELLFIRE_IGNITER_BUFFER.get();
    }
}

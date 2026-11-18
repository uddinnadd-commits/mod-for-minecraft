package com.sorrowed;

import net.minecraftforge.common.ForgeConfigSpec;

public class SorrowedConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> FRIEND_NAME;
    public static final ForgeConfigSpec.BooleanValue ETERNAL_RAIN;
    public static final ForgeConfigSpec.BooleanValue PROGRAMMER_ART;
    public static final ForgeConfigSpec.BooleanValue GIVE_BOOK;
    public static final ForgeConfigSpec.BooleanValue ECHOES;
    public static final ForgeConfigSpec.BooleanValue EVENTS;
    public static final ForgeConfigSpec.BooleanValue CUTSCENES;
    public static final ForgeConfigSpec.IntValue AMBIENT_MINUTES;
    public static final ForgeConfigSpec.BooleanValue TITLE_SCREEN;
    public static final ForgeConfigSpec.BooleanValue OBJECTIVE_HUD;
    public static final ForgeConfigSpec.BooleanValue SCREEN_EFFECTS;
    public static final ForgeConfigSpec.BooleanValue AMBIENT_PARTICLES;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        FRIEND_NAME = b.comment("Name of the friend whose world this used to be. Used in the letter and in your thoughts.")
                .define("friendName", "Alex");
        ETERNAL_RAIN = b.comment("The overworld rain never stops.")
                .define("eternalRain", true);
        PROGRAMMER_ART = b.comment("Switch the Programmer Art resource pack on when you join (client side).")
                .define("programmerArt", true);
        GIVE_BOOK = b.comment("Give the papers (his letter and drawing) the first time a player joins.")
                .define("giveBook", true);
        ECHOES = b.comment("Let Echoes appear in the rain now and then.")
                .define("echoes", true);
        EVENTS = b.comment("Psychological system: the Sorrow meter, hallucinations and overworld events.")
                .define("psychologicalEvents", true);
        CUTSCENES = b.comment("Play the camera cutscenes (first join, first arrival in each dimension, the grave, breakdowns).")
                .define("cutscenes", true);
        AMBIENT_MINUTES = b.comment("Minimum minutes between idle typewriter thoughts. Thoughts caused by events and entities are not limited by this.")
                .defineInRange("ambientThoughtMinutes", 8, 1, 120);
        TITLE_SCREEN = b.comment("Replace the main menu with his house in the rain (client side).")
                .define("titleScreen", true);
        OBJECTIVE_HUD = b.comment("Show the objective for the world you are in, top left, with an arrow toward it (client side).")
                .define("objectiveHud", true);
        SCREEN_EFFECTS = b.comment("Vignette, film grain and per-world colour over the view (client side).")
                .define("screenEffects", true);
        AMBIENT_PARTICLES = b.comment("Dust, ash, petals and the like drifting around you in each world (client side).")
                .define("ambientParticles", true);
        SPEC = b.build();
    }

    public static String friend() {
        return FRIEND_NAME.get();
    }
}

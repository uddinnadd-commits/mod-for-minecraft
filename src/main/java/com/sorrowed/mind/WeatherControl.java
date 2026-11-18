package com.sorrowed.mind;

import net.minecraft.server.level.ServerLevel;

/** Eternal rain, with two exceptions the events can ask for: a sudden silence, and thunder. */
public final class WeatherControl {
    private WeatherControl() {}

    private static final int RAIN_TIME = 24000;
    private static long clearUntil;
    private static long thunderUntil;

    /** The rain stops dead for 'ticks'. When it returns, it returns with thunder. */
    public static void silence(ServerLevel level, int ticks) {
        long now = level.getGameTime();
        clearUntil = now + ticks;
        thunderUntil = clearUntil + 600L;
        level.setWeatherParameters(ticks + 40, 0, false, false);
    }

    public static void thunder(ServerLevel level, int ticks) {
        if (com.sorrowed.SorrowedData.get(level).ended) return;   // it stopped raining for good
        thunderUntil = level.getGameTime() + ticks;
        level.setWeatherParameters(0, RAIN_TIME, true, true);
    }

    /** Called every second on the overworld when eternal rain is on. */
    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        if (now < clearUntil) return;
        level.setWeatherParameters(0, RAIN_TIME, true, now < thunderUntil);
    }
}

package com.sorrowed.mind;

import com.sorrowed.Sorrowed;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Run something N server ticks from now. */
public final class Scheduler {
    private Scheduler() {}

    private record Task(long due, Runnable run) {}

    private static final List<Task> TASKS = new ArrayList<>();

    public static void after(MinecraftServer server, int ticks, Runnable run) {
        TASKS.add(new Task(server.getTickCount() + ticks, run));
    }

    public static void tick(MinecraftServer server) {
        if (TASKS.isEmpty()) return;
        long now = server.getTickCount();
        List<Runnable> due = new ArrayList<>();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.due() <= now) {
                due.add(t.run());
                it.remove();
            }
        }
        for (Runnable r : due) {
            try {
                r.run();
            } catch (Exception e) {
                Sorrowed.LOGGER.warn("Scheduled task failed", e);
            }
        }
    }

    public static void clear() {
        TASKS.clear();
    }
}

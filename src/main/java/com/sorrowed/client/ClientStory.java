package com.sorrowed.client;

import com.sorrowed.net.ObjectivesPacket;
import com.sorrowed.story.Objectives;
import com.sorrowed.story.Papers;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;

/** What the server last told this client about the story: progress, stage, and where to walk. */
public final class ClientStory {
    private ClientStory() {}

    public static final int PLAYING = 0;
    public static final int READY = 1;
    public static final int ENDED = 2;
    public static final int FIGHTING = 3;
    public static final int FAREWELL = 4;

    private static final int N = Objectives.ALL.size();

    public static boolean synced;
    public static int stage;
    public static boolean[] done = new boolean[N];
    public static int[] steps = new int[N];
    public static String reply = "";
    public static List<BlockPos> targets = List.of();

    public static void update(ObjectivesPacket msg) {
        synced = true;
        stage = msg.stage;
        if (msg.done.length == N) {
            done = msg.done;
            steps = msg.steps;
        }
        reply = msg.reply;
        targets = List.copyOf(msg.targets);
        ObjectiveHud.bump();
        Objectives.Objective o = msg.justDone.isEmpty() ? null : Objectives.get(msg.justDone);
        if (o != null) ScreenEffects.memory(o);
    }

    public static void clear() {
        synced = false;
        stage = PLAYING;
        done = new boolean[N];
        steps = new int[N];
        reply = "";
        targets = List.of();
    }

    public static int index(String id) {
        for (int i = 0; i < N; i++) {
            if (Objectives.ALL.get(i).id().equals(id)) return i;
        }
        return -1;
    }

    public static boolean isDone(String id) {
        int i = index(id);
        return i >= 0 && done[i];
    }

    public static int stepsOf(String id) {
        int i = index(id);
        return i < 0 ? 0 : steps[i];
    }

    public static int count() {
        int n = 0;
        for (boolean b : done) {
            if (b) n++;
        }
        return n;
    }

    /** The objective for the world the player is standing in, or null. */
    public static Objectives.Objective here() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? null : Objectives.in(mc.level.dimension());
    }

    /** "chat", "console", "laptop", "ending", "boss:..." or "page:&lt;id&gt;", from the server. */
    public static void open(String what) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (what.startsWith("boss:")) {
            BossClient.command(what.substring(5));
            return;
        }
        switch (what) {
            case "chat", "console", "laptop" -> mc.setScreen(new TerminalScreen(what));
            case "ending" -> mc.setScreen(new EndingScreen());
            default -> {
                if (what.startsWith("page:")) {
                    Papers.Page page = Papers.get(what.substring(5));
                    if (page != null) PaperScreen.open(page);
                }
            }
        }
    }
}

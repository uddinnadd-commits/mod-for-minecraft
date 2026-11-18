package com.sorrowed.client;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Whether this player has ever seen it end. Kept outside any world, so the title screen knows. */
public final class EndingMemory {
    private EndingMemory() {}

    private static Boolean ended;

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("sorrowed-ended.txt");
    }

    public static boolean ended() {
        if (ended == null) ended = Files.exists(file());
        return ended;
    }

    public static void remember() {
        ended = true;
        try {
            Files.writeString(file(), "it stopped raining.\n");
        } catch (IOException ignored) {
            // Only the title screen cares; it will just keep raining there.
        }
    }
}

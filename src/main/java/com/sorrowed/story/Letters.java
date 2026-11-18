package com.sorrowed.story;

import com.sorrowed.item.PapersItem;
import net.minecraft.world.item.ItemStack;

/** The notes he left. Each is one sheet of Papers; the words live in {@link Papers}. */
public final class Letters {
    private Letters() {}

    public static ItemStack keeperLog() {
        return PapersItem.of("keeper");
    }

    public static ItemStack names() {
        return PapersItem.of("names");
    }

    public static ItemStack ticket() {
        return PapersItem.of("ticket");
    }

    public static ItemStack unsaid() {
        return PapersItem.of("unsaid");
    }

    public static ItemStack reply() {
        return PapersItem.of("reply");
    }

    public static ItemStack notesBook() {
        return PapersItem.of("notes");
    }

    public static ItemStack lastPage() {
        return PapersItem.of("last");
    }

    public static ItemStack gateDrawing() {
        return PapersItem.of("gate");
    }

    public static ItemStack orchardNote() {
        return PapersItem.of("orchard");
    }

    public static ItemStack wardNote() {
        return PapersItem.of("ward");
    }

    public static ItemStack wardLast() {
        return PapersItem.of("ward_2");
    }

    public static ItemStack serverLog() {
        return PapersItem.of("server_log");
    }

    public static ItemStack serverReadme() {
        return PapersItem.of("server_readme");
    }

    /** The sheets given on first join, in reading order, plus the drawing folded in with them. */
    public static ItemStack[] welcome() {
        return new ItemStack[] {
                PapersItem.of("welcome_1"), PapersItem.of("welcome_2"),
                PapersItem.of("welcome_3"), PapersItem.of("welcome_4"), PapersItem.of("first_night")
        };
    }
}

package com.sorrowed.client;

import com.sorrowed.Sorrowed;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Turns the built-in Programmer Art resource pack on (once) and reloads resources. */
public class ProgrammerArt {
    // Matches both "programmer_art" and the older "programer_art" spelling.
    private static final Pattern ID = Pattern.compile("programm?er_art", Pattern.CASE_INSENSITIVE);

    public static void enable() {
        Minecraft mc = Minecraft.getInstance();
        PackRepository repo = mc.getResourcePackRepository();

        String id = null;
        for (String candidate : repo.getAvailableIds()) {
            if (ID.matcher(candidate).find()) {
                id = candidate;
                break;
            }
        }
        if (id == null) {
            Sorrowed.LOGGER.warn("Programmer Art pack not found, skipping.");
            return;
        }

        List<String> selected = new ArrayList<>(repo.getSelectedIds());
        if (selected.contains(id)) return;
        selected.add(id);
        repo.setSelected(selected);

        mc.options.resourcePacks.clear();
        for (Pack pack : repo.getSelectedPacks()) {
            if (!pack.isFixedPosition()) {
                mc.options.resourcePacks.add(pack.getId());
            }
        }
        mc.options.save();
        mc.reloadResourcePacks();
    }
}

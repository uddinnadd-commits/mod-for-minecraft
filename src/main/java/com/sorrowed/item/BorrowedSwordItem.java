package com.sorrowed.item;

import com.sorrowed.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The sword that falls out of the sky when he turns. It is only lent: it goes back up when the
 * fight is over, and it never drops when you die. Full damage against the Memory Shatterer, and
 * every hit with it gives you a little of your life back.
 */
public class BorrowedSwordItem extends SwordItem {
    public BorrowedSwordItem(Properties props) {
        super(Tiers.NETHERITE, 5, -2.4F, props);
    }

    public static ItemStack create() {
        ItemStack stack = new ItemStack(ModItems.BORROWED_SWORD.get());
        stack.getOrCreateTag().putBoolean("Unbreakable", true);
        stack.getOrCreateTag().putInt("HideFlags", 4);
        return stack;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack)).withStyle(s -> s.withColor(0xFFF0C8));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("it fell out of the sky. supposedly.").withStyle(s -> s.withColor(0x9AA0AC).withItalic(true)));
        tooltip.add(Component.literal("it isn't yours. you'll have to give it back.").withStyle(s -> s.withColor(0x9AA0AC).withItalic(true)));
    }
}

package com.sorrowed.item;

import com.sorrowed.client.PaperScreen;
import com.sorrowed.story.Papers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

/** A wrinkled sheet of paper. Use it and it glides up onto the screen. */
public class PapersItem extends Item {
    public static final String TAG = "page";

    public PapersItem(Properties props) {
        super(props);
    }

    public static ItemStack of(String pageId) {
        ItemStack stack = new ItemStack(com.sorrowed.registry.ModItems.PAPERS.get());
        stack.getOrCreateTag().putString(TAG, pageId);
        return stack;
    }

    public static String pageId(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getString(TAG) : "";
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Papers.Page page = Papers.get(pageId(stack));
        if (page == null) return InteractionResultHolder.pass(stack);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PaperScreen.open(page));
        } else {
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 0.9F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Papers.Page page = Papers.get(pageId(stack));
        if (page != null) {
            tooltip.add(Component.literal(page.title()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
}

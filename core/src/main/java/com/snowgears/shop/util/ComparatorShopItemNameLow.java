package com.snowgears.shop.util;

import com.snowgears.shop.shop.AbstractShop;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;

public class ComparatorShopItemNameLow implements Comparator<AbstractShop>{
	@Override
    public int compare(AbstractShop o1, AbstractShop o2) {
        return Comparator.nullsLast(Comparator.<String>naturalOrder()).compare(iconName(o1), iconName(o2));
    }

    // Shops still being created have no item, so no icon
    static String iconName(AbstractShop shop) {
        ItemStack icon = shop.getGuiIcon();
        return icon == null ? null : icon.getItemMeta().getDisplayName();
    }
}

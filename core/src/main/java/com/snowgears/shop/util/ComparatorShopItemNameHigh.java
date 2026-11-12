package com.snowgears.shop.util;

import com.snowgears.shop.shop.AbstractShop;

import java.util.Comparator;

public class ComparatorShopItemNameHigh implements Comparator<AbstractShop>{
	@Override
    public int compare(AbstractShop o1, AbstractShop o2) {
        return Comparator.nullsLast(Comparator.<String>reverseOrder()).compare(ComparatorShopItemNameLow.iconName(o1), ComparatorShopItemNameLow.iconName(o2));
    }
}

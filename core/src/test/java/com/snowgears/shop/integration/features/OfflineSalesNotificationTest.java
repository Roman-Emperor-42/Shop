package com.snowgears.shop.integration.features;

import com.snowgears.shop.shop.AbstractShop;
import com.snowgears.shop.testsupport.BaseMockBukkitTest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
public class OfflineSalesNotificationTest extends BaseMockBukkitTest {

    @Test
    void owner_sees_sales_summary_on_login() throws Exception {
        ServerMock server = getServer();
        World world = server.addSimpleWorld("world");
        PlayerMock owner = server.addPlayer();
        owner.setOp(true);
        setConfig("usePerms", false);

        AbstractShop shop = ShopCreationChestTest.createShop(server, getPlugin(), owner, world, new Location(world, 54, 65, 10), new ItemStack(Material.DIRT), "sell", 8, "1");
        shop.getChestLocation().getChunk().load(true);
        shop.getInventory().addItem(new ItemStack(Material.DIRT, 8));
        shop.updateStock();

        owner.disconnect();
        Thread.sleep(5); // sale timestamp must be strictly after the owner's last played time

        PlayerMock buyer = server.addPlayer();
        buyer.getInventory().addItem(new ItemStack(Material.EMERALD, 10));
        server.getPluginManager().callEvent(new PlayerInteractEvent(buyer, Action.RIGHT_CLICK_BLOCK, null, shop.getSignLocation().getBlock(), BlockFace.NORTH, EquipmentSlot.HAND));
        server.getScheduler().waitAsyncTasksFinished(); // sale written to the log db
        while (owner.nextMessage() != null) { }

        // owner logs back in. Events are fired by hand: PlayerMock.reconnect() resets last played to "now" first
        server.getScheduler().executeAsyncEvent(new AsyncPlayerPreLoginEvent(owner.getName(), InetAddress.getLoopbackAddress(), owner.getUniqueId())).get();
        server.getScheduler().waitAsyncTasksFinished(); // ticks below are instant, so let the log query finish first
        server.getPluginManager().callEvent(new PlayerJoinEvent(owner, "joined"));

        StringBuilder seen = new StringBuilder();
        for (int tick = 0; tick < 120; tick++) {
            server.getScheduler().performTicks(1);
            for (String m = owner.nextMessage(); m != null; m = owner.nextMessage()) seen.append(m).append('\n');
        }
        assertTrue(seen.toString().contains("Shop Sales"), "expected offline sales summary, got: " + seen);
        assertTrue(seen.toString().contains("Transactions: §b1"), "expected 1 transaction, got: " + seen);
    }
}

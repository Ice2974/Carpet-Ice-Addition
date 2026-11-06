package com.ice2974.carpeticeaddition.rules;

import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientRecipeContainer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 使用原版客户端菜单的实际 setItem/initializeContents/slotsChanged 验证同步。
 * Unsafe 仅绕过游戏、连接、世界构造；配方筛选、槽位回调和缓存逻辑均为原版代码。
 * 不启动客户端，不替代实际网络与渲染验收。
 */
class StonecutterRefresherDispatcherTest {
    private static Unsafe allocator;

    @BeforeAll
    static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        allocator = (Unsafe) field.get(null);
    }

    @Test
    void enablingRecipeRefreshesUnmoddedClientWithoutMovingServerItems() throws Exception {
        Fixture fixture = new Fixture(false, new ItemStack(Items.DRIPSTONE_BLOCK, 7));
        ItemStack input = fixture.server.getSlot(0).getItem();
        fixture.server.setCarried(new ItemStack(Items.DIAMOND, 3));
        fixture.setRecipes(true);

        // 原来的全量同步仍保留旧列表，证明本例覆盖相同输入物品的缓存守卫。
        fixture.server.sendAllDataToRemote();
        assertEquals(0, fixture.client.getVisibleRecipes().size());
        fixture.events.clear();

        fixture.refresh();

        assertEquals(List.of("bundle", "full"), fixture.events);
        assertEquals(1, fixture.client.getVisibleRecipes().size());
        assertSame(input, fixture.server.getSlot(0).getItem());
        assertEquals(7, input.getCount());
        assertEquals(7, fixture.client.getSlot(0).getItem().getCount());
        assertTrue(ItemStack.matches(fixture.server.getCarried(), fixture.client.getCarried()));
        assertEquals(fixture.server.getStateId(), fixture.client.getStateId());
    }

    @Test
    void disablingRecipeRemovesStaleSelectionAndResultOnBothSides() throws Exception {
        Fixture fixture = new Fixture(true, new ItemStack(Items.DRIPSTONE_BLOCK, 4));
        assertEquals(1, fixture.client.getVisibleRecipes().size());
        fixture.server.getSlot(1).set(new ItemStack(Items.CALCITE));
        fixture.server.selected.set(0);
        fixture.server.sendAllDataToRemote();
        fixture.setRecipes(false);

        fixture.refresh();

        assertEquals(0, fixture.client.getVisibleRecipes().size());
        assertEquals(-1, fixture.client.getSelectedRecipeIndex());
        assertTrue(fixture.server.getSlot(1).getItem().isEmpty());
        assertTrue(fixture.client.getSlot(1).getItem().isEmpty());
        assertEquals(4, fixture.server.getSlot(0).getItem().getCount());
    }

    @Test
    void emptyInputNeedsNoSyntheticSlotPacketAndStillSynchronizesCleanup() throws Exception {
        Fixture fixture = new Fixture(false, ItemStack.EMPTY);
        fixture.server.getSlot(1).set(new ItemStack(Items.CALCITE));
        fixture.server.selected.set(0);

        fixture.refresh();

        assertEquals(List.of("full"), fixture.events);
        assertTrue(fixture.client.getSlot(0).getItem().isEmpty());
        assertTrue(fixture.client.getSlot(1).getItem().isEmpty());
        assertEquals(-1, fixture.client.getSelectedRecipeIndex());
    }

    private static final class Fixture implements ContainerSynchronizer {
        final StonecutterMenu client;
        final ClientPacketListener connection;
        final ServerMenu server = new ServerMenu();
        final List<String> events = new ArrayList<>();

        Fixture(boolean enabled, ItemStack input) throws Exception {
            connection = (ClientPacketListener) allocator.allocateInstance(ClientPacketListener.class);
            ClientLevel level = (ClientLevel) allocator.allocateInstance(ClientLevel.class);
            set(level, "connection", connection);
            setRecipes(enabled);
            client = (StonecutterMenu) allocator.allocateInstance(StonecutterMenu.class);
            set(client, "level", level);
            set(client, "input", ItemStack.EMPTY);
            set(client, "recipesForInput", SelectableRecipe.SingleInputSet.empty());
            DataSlot selected = DataSlot.standalone();
            selected.set(-1);
            set(client, "selectedRecipeIndex", selected);
            set(client, "dataSlots", new ArrayList<>(List.of(selected)));
            set(client, "slotUpdateListener", (Runnable) () -> {});
            Constructor<?> constructor = Class.forName("net.minecraft.world.inventory.StonecutterMenu$1")
                    .getDeclaredConstructor(StonecutterMenu.class, int.class);
            constructor.setAccessible(true);
            SimpleContainer container = (SimpleContainer) constructor.newInstance(client, 1);
            ResultContainer result = new ResultContainer();
            Slot inputSlot = new Slot(container, 0, 0, 0);
            Slot resultSlot = new Slot(result, 0, 0, 0);
            set(client, "container", container);
            set(client, "resultContainer", result);
            set(client, "inputSlot", inputSlot);
            set(client, "resultSlot", resultSlot);
            NonNullList<Slot> slots = NonNullList.create();
            slots.add(inputSlot);
            slots.add(resultSlot);
            set(client, "slots", slots);
            server.getSlot(0).set(input);
            server.setSynchronizer(this);
            events.clear();
        }

        void setRecipes(boolean enabled) throws Exception {
            SelectableRecipe.SingleInputSet<StonecutterRecipe> recipes = enabled
                    ? new SelectableRecipe.SingleInputSet<>(List.of(new SelectableRecipe.SingleInputEntry<>(
                            Ingredient.of(Items.DRIPSTONE_BLOCK),
                            new SelectableRecipe<>(new SlotDisplay.ItemSlotDisplay(Items.CALCITE), Optional.empty()))))
                    : SelectableRecipe.SingleInputSet.empty();
            set(connection, "recipes", new ClientRecipeContainer(Map.of(), recipes));
        }

        void refresh() {
            events.clear();
            StonecutterRefresherDispatcher.refreshOpenStonecutterMenu(server, this::sendInputRefresh);
        }

        void sendInputRefresh(ClientboundBundlePacket bundle) {
            events.add("bundle");
            int count = 0;
            for (Packet<? super ClientGamePacketListener> child : bundle.subPackets()) {
                ClientboundContainerSetSlotPacket packet = assertInstanceOf(ClientboundContainerSetSlotPacket.class, child);
                assertEquals(server.containerId, packet.getContainerId());
                assertEquals(StonecutterMenu.INPUT_SLOT, packet.getSlot());
                if (count++ == 0) {
                    assertTrue(packet.getItem().isEmpty());
                } else {
                    assertTrue(ItemStack.matches(server.getSlot(0).getItem(), packet.getItem()));
                }
                client.setItem(packet.getSlot(), packet.getStateId(), packet.getItem());
            }
            assertEquals(2, count, "空输入与恢复必须在同一个原版 bundle 中连续处理");
            assertTrue(ItemStack.matches(server.getSlot(0).getItem(), client.getSlot(0).getItem()),
                    "bundle 处理结束时不能留下可操作的临时空槽");
        }

        @Override
        public void sendInitialData(AbstractContainerMenu menu, List<ItemStack> items,
                                    ItemStack carried, int[] data) {
            events.add("full");
            client.initializeContents(menu.incrementStateId(), items, carried);
            for (int i = 0; i < data.length; i++) {
                client.setData(i, data[i]);
            }
        }

        @Override
        public void sendSlotChange(AbstractContainerMenu menu, int slot, ItemStack item) {
            client.setItem(slot, menu.incrementStateId(), item);
        }

        @Override
        public void sendCarriedChange(AbstractContainerMenu menu, ItemStack carried) {
            client.setCarried(carried);
        }

        @Override
        public void sendDataChange(AbstractContainerMenu menu, int index, int value) {
            client.setData(index, value);
        }

        @Override
        public RemoteSlot createSlot() {
            return RemoteSlot.PLACEHOLDER;
        }
    }

    /** 服务端 duck 只替代 Mixin 接线；槽位及全量同步使用真实 AbstractContainerMenu。 */
    private static final class ServerMenu extends AbstractContainerMenu implements StonecutterRecipeRefresher {
        final DataSlot selected = DataSlot.standalone();

        ServerMenu() {
            super(MenuType.STONECUTTER, 9);
            addSlot(new Slot(new SimpleContainer(1), 0, 0, 0));
            addSlot(new Slot(new ResultContainer(), 0, 0, 0));
            selected.set(-1);
            addDataSlot(selected);
        }

        @Override
        public void carpetIceAddition$refreshStonecutterRecipes() {
            carpetIceAddition$clearStonecutterResult();
            selected.set(-1);
        }

        @Override
        public void carpetIceAddition$clearStonecutterResult() {
            getSlot(1).set(ItemStack.EMPTY);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static void set(Object target, String name, Object value) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                // Continue to the field's declaring superclass.
            }
        }
        throw new NoSuchFieldException(name);
    }
}

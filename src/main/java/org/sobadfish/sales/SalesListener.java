package org.sobadfish.sales;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.block.BlockUpdateEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.inventory.InventoryTransactionEvent;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.inventory.transaction.InventoryTransaction;
import cn.nukkit.inventory.transaction.action.InventoryAction;
import cn.nukkit.item.Item;
import me.onebone.economyapi.EconomyAPI;
import org.sobadfish.sales.entity.SalesEntity;
import org.sobadfish.sales.form.AdminForm;
import org.sobadfish.sales.form.SellItemForm;
import org.sobadfish.sales.items.MoneyItem;
import org.sobadfish.sales.panel.DisplayPlayerPanel;
import org.sobadfish.sales.panel.button.BasePlayPanelItemInstance;
import org.sobadfish.sales.panel.lib.ChestPanel;

import java.util.LinkedHashMap;

/**
 * @author Sobadfish
 * @date 2023/11/16
 */
public class SalesListener implements Listener {

    public SalesMainClass main;

    public static LinkedHashMap<String,DisplayPlayerPanel> chestPanelLinkedHashMap = new LinkedHashMap<>();

//    public List<Player> breakLock = new ArrayList<>();

    public SalesListener(SalesMainClass salesMainClass){
        this.main = salesMainClass;
    }



    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event){
        Block block = event.getBlock();
        BlockEntity entity = block.level.getBlockEntity(block);

        if(entity instanceof SalesEntity.SalesBlockEntity){

            SalesEntity entity1 = ((SalesEntity.SalesBlockEntity) entity).sales;
            Player player = event.getPlayer();
            if(player.isSneaking()){
                if(event.getAction() == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK){
                    if(player.isOp() || (entity1.master != null && entity1.master.equalsIgnoreCase(player.getName()))){
                        if(player.getInventory().getItemInHand().getId() == 0){
                            return;
                        }
                        SellItemForm sellItemForm = new SellItemForm(entity1,player.getInventory().getItemInHand());
                        sellItemForm.display(player);
                        event.setCancelled();
                    }
                }
            }else{
                if(event.getAction() == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK || event.getAction() == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK){
                    DisplayPlayerPanel displayPlayerPanel = new DisplayPlayerPanel(entity1);
                    displayPlayerPanel.open(player);
                    chestPanelLinkedHashMap.put(player.getName(),displayPlayerPanel);
                    event.setCancelled();

                }

            }

        }
        //使用金币
        Item item = event.getItem();
        Player player = event.getPlayer();
        if(item.hasCompoundTag() && item.getNamedTag().contains(MoneyItem.TAG)){
            Item cl = event.getItem().clone();
            cl.setCount(1);
            double money = item.getNamedTag().getDouble(MoneyItem.TAG);
            EconomyAPI.getInstance().addMoney(player,money);
            player.getInventory().removeItem(cl);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        Item it = event.getItem();
        if(it.hasCompoundTag()){
            if(it.getNamedTag().contains("saleskey")){
                event.setCancelled();
                Player player = event.getPlayer();
                if(SalesEntity.spawnToAll(event.getBlock(),player.getDirection(),player.getName())){
                    SalesMainClass.sendMessageToObject("&a生成成功！",player);
                    Item re = it.clone();
                    re.setCount(1);
                    player.getInventory().removeItem(re);
                }else{
                    SalesMainClass.sendMessageToObject("&c生成失败！ 请保证周围没有其他方块",player);
                }
            }
        }
        if(event.getBlockAgainst().getId() == main.iBarrier.getBid()) {
            BlockEntity entity = event.getBlockAgainst().level.getBlockEntity(event.getBlockAgainst());
            if (entity instanceof SalesEntity.SalesBlockEntity) {
                event.setCancelled();
            }
        }
    }

    @EventHandler
    public void onFormListener(PlayerFormRespondedEvent event) {
        if (event.wasClosed()) {
            return;
        }
        if(SellItemForm.DISPLAY_FROM.containsKey(event.getPlayer())){
            SellItemForm form = SellItemForm.DISPLAY_FROM.get(event.getPlayer());
            if(form.getId() == event.getFormID()){
                if(event.getResponse() instanceof FormResponseCustom){
                    form.onListener(event.getPlayer(), (FormResponseCustom) event.getResponse());

                }
                SellItemForm.DISPLAY_FROM.remove(event.getPlayer());
            }else{
                SellItemForm.DISPLAY_FROM.remove(event.getPlayer());
            }
        }
        if(AdminForm.DISPLAY_FROM.containsKey(event.getPlayer())){
            AdminForm form = AdminForm.DISPLAY_FROM.get(event.getPlayer());
            if(form.getId() == event.getFormID()){
                if(event.getResponse() instanceof FormResponseCustom){
                    form.onListener(event.getPlayer(), (FormResponseCustom) event.getResponse());

                }
                AdminForm.DISPLAY_FROM.remove(event.getPlayer());
            }else{
                AdminForm.DISPLAY_FROM.remove(event.getPlayer());
            }
        }
    }


    @EventHandler
    public void onBlockUpdate(BlockUpdateEvent event){
        Block upblock = event.getBlock();
        Block old = upblock.level.getBlock(upblock);
        if(old.getId() != main.iBarrier.getBid() && upblock.getId() == main.iBarrier.getBid()){

            BlockEntity entity = old.level.getBlockEntity(old);
            if(entity instanceof SalesEntity.SalesBlockEntity){
                SalesEntity entity1 = ((SalesEntity.SalesBlockEntity) entity).sales;
                if(entity1.finalClose){
                    return;
                }
                entity1.toClose();
            }
        }

    }

    @EventHandler(ignoreCancelled = true,priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event){
        Block block = event.getBlock();
        if(event.isCancelled()){
            return;
        }
        if(event.getPlayer().isOp()){
            if(block.getId() == main.iBarrier.getBid()){
                BlockEntity entity = block.level.getBlockEntity(block);
                if(entity instanceof SalesEntity.SalesBlockEntity){
                    event.setCancelled();
                    SalesEntity entity1 = ((SalesEntity.SalesBlockEntity) entity).sales;
                    entity1.toClose();

                }
            }
        }

    }

    @EventHandler
    public void onItemChange(InventoryTransactionEvent event) {
        InventoryTransaction transaction = event.getTransaction();
        for (InventoryAction action : transaction.getActions()) {
            for (Inventory inventory : transaction.getInventories()) {
                if (inventory instanceof ChestPanel) {
                    Player player = ((ChestPanel) inventory).getPlayer();
                    event.setCancelled();
                    Item i = action.getSourceItem();
                    if (i.hasCompoundTag() && i.getNamedTag().contains("index")) {
                        int index = i.getNamedTag().getInt("index");
                        BasePlayPanelItemInstance item = ((ChestPanel) inventory).getPanel().getOrDefault(index, null);

                        if (item != null) {
                            ((ChestPanel) inventory).clickSolt = index;
                            item.onClick((ChestPanel) inventory, player);
                            ((ChestPanel) inventory).update();
                        }
                    }

                }
            }
        }
    }


    @EventHandler
    public void onEntityDamage(EntityDamageEvent event){
        if(event.getEntity() instanceof SalesEntity){
            event.setCancelled();

        }
    }
}

package rout.world.blocks.payload;

import arc.Core;
import arc.math.Mathf;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ButtonGroup;
import arc.scene.ui.ImageButton;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.payloads.BlockProducer;
import mindustry.world.blocks.payloads.Constructor;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.consumers.ConsumeItemDynamic;
import mindustry.world.consumers.ConsumeLiquidsDynamic;
import rout.world.blocks.environment.RouterFloor;

/** A payload Constructor whose per-block production cost can be overridden. */
public class RouterConstructor extends Constructor {
    /** Per-recipe item override. */
    public ObjectMap<Block, ItemStack[]> recipeRequirements = new ObjectMap<>();
    /** Per-recipe liquid override. */
    public ObjectMap<Block, LiquidStack[]> recipeLiquids = new ObjectMap<>();

    public RouterConstructor(String name) {
        super(name);

        //BlockProducer's constructor installs its own ConsumeItemDynamic that simply
        //echoes each recipe block's requirements. Drop it via the public helper, then
        //install a single ConsumeItemDynamic that honours recipeRequirements.
        //After this point, the only item consumer in consumers[] is ours.
        removeConsumers(c -> c instanceof ConsumeItemDynamic);
        consume(new ConsumeItemDynamic((BlockProducerBuild e) -> {
            Block block = e.recipe();
            if(block == null) return ItemStack.empty;

            //Resolve requirements: prefer the override entry, fall back to the block's
            //own requirements so blocks not in the map still work.
            ItemStack[] src = recipeRequirements.containsKey(block)
                ? recipeRequirements.get(block)
                : block.requirements;

            //Copy + scale so we never mutate the override array or block.requirements
            //(which would affect every other consumer of those arrays).
            ItemStack[] out = new ItemStack[src.length];
            for(int i = 0; i < src.length; i++){
                out[i] = new ItemStack(src[i].item,
                    Mathf.ceil(src[i].amount * Vars.state.rules.buildCostMultiplier));
            }
            return out;
        }));

        //Same pattern for liquids: ConsumeLiquidsDynamic returns the configured liquids
        //per recipe, or an empty stack if the recipe has no liquid requirements.
        consume(new ConsumeLiquidsDynamic((BlockProducerBuild e) -> {
            Block block = e.recipe();
            if(block == null) return LiquidStack.empty;
            return recipeLiquids.get(block, LiquidStack.empty);
        }));
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation) {
        if (tile == null) return false;
        //Only change here is letting the core be placed on any router floor
        tile.getLinkedTilesAs(this, tempTiles);
        if(!tempTiles.contains(o -> (!o.floor().allowCorePlacement && !(o.floor() instanceof RouterFloor)) || o.block() instanceof CoreBlock)){
            return true;
        }
        super.canPlaceOn(tile, team, rotation);
        return false;
    }

    /** True when the block has a custom item or liquid cost registered. */
    public boolean hasCustomCost(Block block){
        return recipeRequirements.containsKey(block) || recipeLiquids.containsKey(block);
    }

    @Override
    public boolean canProduce(Block block){
        return hasCustomCost(block);
    }

    public class RouterConstructorBuild extends ConstructorBuild{
        @Override
        public void buildConfiguration(Table table){
            //Custom selection grid: ItemSelection.buildTable filters by unlockedNow(),
            //which hides unresearched blocks. This skips that gate entirely.
            Seq<Block> items = Vars.content.blocks().select(b -> hasCustomCost(b));
            ButtonGroup<ImageButton> group = new ButtonGroup<>();
            group.setMinCheckCount(0);
            Table cont = new Table().top();
            cont.defaults().size(40);

            int cols = selectionColumns;
            int i = 0;
            for(Block item : items){
                ImageButton button = cont.button(Tex.whiteui, Styles.clearNoneTogglei, 40f, () -> {
                    Vars.control.input.config.hideConfig();
                }).tooltip(item.localizedName).group(group).get();
                button.changed(() -> recipe = button.isChecked() ? item : null);
                button.getStyle().imageUp = new TextureRegionDrawable(item.fullIcon);
                button.update(() -> button.setChecked(recipe == item));

                if(i++ % cols == (cols - 1)){
                    cont.row();
                }
            }

            ScrollPane pane = new ScrollPane(cont, Styles.smallPane);
            pane.setScrollingDisabled(true, false);
            pane.setOverscroll(false, false);
            table.top().add(pane).maxHeight(40 * selectionRows);
        }
        /** Override acceptItem so the building only fills up to 2x the override amount
         *  (matching the consume rate * 2, the same ratio vanilla uses). Without this,
         *  the building accepts up to 2x the BLOCK's vanilla requirements - so e.g.
         *  router2 (which costs 2 fragments) lets you fill it with 4 even when the
         *  override charges 1 per cycle. */
        @Override
        public int getMaximumAccepted(Item item){
            Block block = recipe();
            if(block == null) return 0;
            ItemStack[] reqs = recipeRequirements.get(block);
            if(reqs == null) return super.getMaximumAccepted(item);
            for(ItemStack stack : reqs){
                if(stack.item == item){
                    return Mathf.ceil(stack.amount * 2f * Vars.state.rules.buildCostMultiplier);
                }
            }
            return 0;
        }

        /** ConsumeLiquidsDynamic never populates block.liquidFilter, so vanilla
         *  hasLiquid(liquid) (and therefore acceptLiquid) returns false. Override to
         *  read from the configured recipe's liquids AND any liquids registered via
         *  the block's own consumeLiquid(...) calls. */
        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            int id = liquid.id;
            int cap = Mathf.ceil(liquidCapacity * Vars.state.rules.buildCostMultiplier);
            //1) Recipe override (highest priority - explicit per-recipe liquid cost).
            Block block = recipe();
            if(block != null){
                LiquidStack[] reqs = recipeLiquids.get(block, LiquidStack.empty);
                for(LiquidStack stack : reqs){
                    if(stack.liquid.id == id){
                        return liquids.get(liquid) < Mathf.ceil(stack.amount * 2f * Vars.state.rules.buildCostMultiplier);
                    }
                }
            }
            //2) Block's own consumeLiquid(...) registrations.
            if(block != null && block.liquidFilter != null && id < block.liquidFilter.length && block.liquidFilter[id]){
                return liquids.get(liquid) < cap;
            }
            return false;
        }
    }
}

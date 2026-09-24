package rout.world.blocks.payload;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.util.Strings;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.blocks.payloads.BuildPayload;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.PayloadBlock;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.consumers.ConsumeItemDynamic;
import mindustry.world.consumers.ConsumeLiquidsDynamic;
import mindustry.world.consumers.ConsumePowerDynamic;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import rout.world.blocks.environment.RouterFloor;

/** A payload block that takes a BuildPayload and transforms it into a different block,
 *  the way Reconstructor transforms a unit payload. The source block is selected via the
 *  configuration UI (like Constructor); the destination is looked up in transmutations. */
public class RouterTransmutator extends PayloadBlock {
    /** Source block -> destination block. The destination is what the source payload becomes. */
    public ObjectMap<Block, Block> transmutations = new ObjectMap<>();
    /** Optional per-source-block item requirement override; falls back to the destination's
     *  item requirements when no entry exists. */
    public ObjectMap<Block, ItemStack[]> recipeRequirements = new ObjectMap<>();
    /** Optional per-source-block item requirement override; falls back to the destination's
     *  item requirements when no entry exists. */
    public ObjectMap<Block, Float> powerRecipeRequirements = new ObjectMap<>();
    /** Optional per-source-block liquid requirement override; falls back to the
     *  destination's liquid requirements when no entry exists. */
    public ObjectMap<Block, LiquidStack[]> recipeLiquids = new ObjectMap<>();
    /** Seconds a transmutation takes once the payload is in position and resources are met. */
    public float constructTime = 5f;
    /** Effect played when the payload is finally transformed. */
    public Effect craftEffect = Fx.pulverize;
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
    public RouterTransmutator(String name){
        super(name);

        size = 3;
        hasItems = true;
        hasLiquids = true;
        itemCapacity = 30;
        configurable = true;
        outputsPayload = true;
        rotate = true;
        drawArrow = true;

        //Wire up the configuration UI: clicking a block in the buildTable sets recipe
        //on the build, double-clicking clears it. Mirrors what Constructor does.
        configClear((RouterTransmutatorBuild tile) -> tile.recipe = null);
        config(Block.class, (RouterTransmutatorBuild tile, Block block) -> {
            if(tile.recipe != block) tile.progress = 0f;
            if(canTransmute(block)){
                tile.recipe = block;
            }
        });

        //Dynamic item consumer: returns the configured recipe's override requirements
        //if any, otherwise the destination block's vanilla requirements.
        consume(new ConsumeItemDynamic((Building e) -> {
            if(!(e instanceof RouterTransmutatorBuild b)) return ItemStack.empty;
            Block source = b.recipe;
            if(source == null) return ItemStack.empty;
            //check by source first, then by target — recipes may be keyed either way
            if(recipeRequirements.containsKey(source)){
                return recipeRequirements.get(source);
            }
            Block target = transmutations.get(source);
            if(target != null && recipeRequirements.containsKey(target)){
                return recipeRequirements.get(target);
            }
            return target == null ? ItemStack.empty : target.requirements;
        }));
        consume(new ConsumePowerDynamic((Building e) -> {
            if(!(e instanceof RouterTransmutatorBuild b)) return 0;
            Block source = b.recipe;
            if(source == null) return 0;
            Float v = powerRecipeRequirements.get(source);
            return v == null ? 0f : v;
        }){
            @Override
            public float efficiency(Building build){
                //when usage is 0 no power is drawn, so power.status stays at 0 and
                //the inherited efficiency returns 0 even though nothing is needed.
                return requestedPower(build) <= 0f ? 1f : build.power.status;
            }
        });

        //Dynamic liquid consumer: same pattern, but for liquids. If no override is
        //configured for the recipe, no liquids are required (Block doesn't expose a
        //generic liquidRequirements field).
        consume(new ConsumeLiquidsDynamic((Building e) -> {
            if(!(e instanceof RouterTransmutatorBuild b)) return LiquidStack.empty;
            Block source = b.recipe;
            if(source == null) return LiquidStack.empty;
            return recipeLiquids.get(source, LiquidStack.empty);
        }));
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.productionTime, constructTime, StatUnit.seconds);
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("progress", (RouterTransmutatorBuild e) -> new Bar(
            () -> Core.bundle.format("bar.progress", Strings.autoFixed(e.fraction() * 100f, 0)),
            () -> Pal.ammo,
            e::fraction
        ));
    }
    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, inRegion, outRegion, topRegion};
    }
    /** True if this block can be used as a transmutation source. */
    public boolean canTransmute(Block b){
        return transmutations.containsKey(b);
    }

    public class RouterTransmutatorBuild extends PayloadBlockBuild<BuildPayload>{
        /** Configured input source block. Null means "no recipe selected". */
        public @org.jetbrains.annotations.Nullable Block recipe;
        public float progress;
        public float time;
        public float warmup;
        public float productionEfficiency;
        public float fraction(){
            return Mathf.clamp(progress);
        }

        /** ItemStack[] the current recipe needs (override > destination vanilla). */
        private ItemStack[] currentRecipeReqs(){
            if(recipe == null) return null;
            ItemStack[] reqs = recipeRequirements.get(recipe);
            if(reqs != null) return reqs;
            Block target = transmutations.get(recipe);
            if(target != null){
                reqs = recipeRequirements.get(target);
                if(reqs != null) return reqs;
                return target.requirements;
            }
            return null;
        }

        /** ConsumeItemDynamic never populates block.itemFilter, so the vanilla
         *  consumesItem check rejects every item. Override acceptItem (which calls
         *  block.consumesItem, hence overriding consumesItem on the build wouldn't
         *  take effect) to accept items based on the configured recipe instead.
         *  Items are accepted at any time - even while a payload is processing -
         *  so the buffer can pre-fill before the next cycle. */
        @Override
        public boolean acceptItem(Building source, Item item){
            ItemStack[] reqs = currentRecipeReqs();
            if(reqs == null) return false;
            for(ItemStack stack : reqs){
                if(stack.item == item){
                    return items.get(item) < Mathf.ceil(stack.amount * 2f * Vars.state.rules.buildCostMultiplier);
                }
            }
            return false;
        }

        /** Same trick as acceptItem - ConsumeLiquidsDynamic never populates
         *  block.liquidFilter, so hasLiquid() returns false. Override acceptLiquid
         *  to honour both recipeLiquids AND any liquids registered via the block's
         *  own consumeLiquid(...) calls. */
        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            int id = liquid.id;
            int cap = Mathf.ceil(liquidCapacity * Vars.state.rules.buildCostMultiplier);
            //1) Recipe override (highest priority - explicit per-recipe liquid cost).
            Block src = recipe;
            if(src != null){
                LiquidStack[] reqs = recipeLiquids.get(src, LiquidStack.empty);
                for(LiquidStack stack : reqs){
                    if(stack.liquid.id == id){
                        return liquids.get(liquid) < Mathf.ceil(stack.amount * 2f * Vars.state.rules.buildCostMultiplier);
                    }
                }
            }
            //2) Block's own consumeLiquid(...) registrations - ConsumeLiquid.apply
            //sets block.liquidFilter so we can read it directly here.
            if(block.liquidFilter != null && id < block.liquidFilter.length && block.liquidFilter[id]){
                return liquids.get(liquid) < cap;
            }
            return false;
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            if(this.payload != null || !(payload instanceof BuildPayload)) return false;
            if(recipe == null) return false;
            Block src = ((BuildPayload)payload).build.block;
            if(src != recipe) return false;
            Block target = transmutations.get(src);
            if(target == null || !target.environmentBuildable() || Vars.state.rules.isBanned(target)) return false;
            return super.acceptPayload(source, payload);
        }

        @Override
        public void handlePayload(Building source, Payload payload){
            super.handlePayload(source, payload);
            progress = 0f;
        }

        @Override
        public void buildConfiguration(Table table){
            //Show every block that has a transmutation entry as a configurable recipe.
            ItemSelection.buildTable(RouterTransmutator.this, table,
                Vars.content.blocks().select(RouterTransmutator.this::canTransmute),
                () -> recipe, this::configure, selectionRows, selectionColumns);
        }

        @Override
        public Object config(){
            return recipe;
        }

        @Override
        public void updateTile(){
            //compute efficiency BEFORE super.updateTile() so payload movement works.
            getEfficiency();
            super.updateTile();

            if(payload == null){
                warmup = Mathf.approachDelta(warmup, 0f, 0.05f);
                return;
            }

            //If the payload's block doesn't match the configured recipe anymore, eject it.
            if(recipe == null || payload.build.block != recipe){
                moveOutPayload();
                return;
            }

            if(moveInPayload()){
                getEfficiency();
                boolean valid = productionEfficiency > 0;
                warmup = Mathf.approachDelta(warmup, valid ? 1f : 0f, 0.05f);

                if(valid){
                    //use productionEfficiency * delta() directly — overriding edelta()
                    //would create a circular dependency with ConsumeLiquidsDynamic.efficiency()
                    //which calls build.edelta() internally.
                    progress += productionEfficiency * delta() / constructTime;
                    time += productionEfficiency * delta();

                    if(progress >= 1f){
                        Block target = transmutations.get(recipe);
                        Building old = payload.build;
                        BuildPayload next = new BuildPayload(target, team);
                        if(old != null){
                            next.build.tile = old.tile;
                            next.build.rotation = old.rotation;
                            if(old.config() != null) next.build.configure(old.config());
                        }
                        payload = next;
                        progress = 0f;
                        consume();
                        craftEffect.at(x, y);
                    }
                }
            }else{
                warmup = Mathf.approachDelta(warmup, 0f, 0.05f);
            }
        }
        public void getEfficiency(){
            ConsumeLiquidsDynamic consLiq = block.findConsumer(f -> f instanceof ConsumeLiquidsDynamic);
            ConsumeItemDynamic consItem = block.findConsumer(f -> f instanceof ConsumeItemDynamic);
            ConsumePowerDynamic consPower = block.findConsumer(f -> f instanceof ConsumePowerDynamic);
            float liq = consLiq == null ? 1f : consLiq.efficiency(this);
            float item = consItem == null ? 1f : consItem.efficiency(this);
            float pow = consPower == null ? 1f : consPower.efficiency(this);
            productionEfficiency = liq * item * pow;
        }
        @Override
        public void draw(){
            Draw.rect(region, x, y);

            for(int i = 0; i < 4; i++){
                if(blends(i)){
                    Draw.rect(inRegion, x, y, (i * 90) - 180);
                }
            }
            Draw.rect(outRegion, x, y, rotdeg());

            if(payload != null && payload.build != null && payload.build.block != null){
                Draw.z(Layer.blockOver);
                updatePayload();
                if(!hasArrived()){
                    payload.draw();
                }
                if(hasArrived()){
                    Draw.color(Color.white.cpy().lerp(team.color, progress));
                    Draw.alpha(1-progress);
                    Draw.rect(payload.block().fullIcon, x, y, 0);
                    Draw.color(Color.white.cpy().lerp(team.color, 1-progress));
                    Draw.alpha(progress);
                    Draw.rect(transmutations.get(recipe).fullIcon, x, y, 0);
                    Draw.alpha(1);
                    Draw.color();
                }

            }

            Draw.z(Layer.blockOver + 0.1f);
            Draw.rect(topRegion, x, y);
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(progress);
            write.f(warmup);
            write.s(recipe == null ? -1 : recipe.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            progress = read.f();
            warmup = read.f();
            short id = read.s();
            recipe = id < 0 ? null : Vars.content.block(id);
        }
    }
}

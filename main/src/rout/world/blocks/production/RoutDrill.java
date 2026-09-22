package rout.world.blocks.production;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.type.Item;
import mindustry.world.Tile;
import mindustry.world.blocks.production.Drill;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawDefault;
import mindustry.world.draw.DrawMulti;
import rout.world.blocks.environment.RouterFloor;

public class RoutDrill extends Drill {

    public DrawBlock drawer;
    //How much this block mines at a time. Pretty much turns it into a burstDrill except not needing a separate class.
    //Take notes Anuke :troll:
    public int mineAmount = 1;

    public RoutDrill(String name) {
        super(name);
    }
    @Override
    public void load(){
        super.load();
        drawer.load(this);
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

    @Override
    public TextureRegion[] icons(){
        return drawer.finalIcons(this);
    }
    
    public class RoutDrillBuild extends DrillBuild{
        float totalProgress = 0;
        //Vanilla has not added drill drawers to this version of mindustry.
        //Fine I'll do it myself. Again...
        @Override
        public void draw() {
            drawer.draw(this);
            Draw.z(Layer.blockCracks);

            Draw.z(Layer.blockAfterCracks);

            if(dominantItem != null && drawMineItem){
                Draw.color(dominantItem.color);
                Draw.rect(itemRegion, x, y);
                Draw.color();
            }
        }

        //Helper methods for drawers.
        @Override
        public float progress(){
            return progress;
        }
        @Override
        public float totalProgress(){
            return totalProgress;
        }
        //Progress was already being handled before. TotalProgress however, did not exist.
        //Unfortunately I have to copy-paste the entire vanilla method to even consider doing this.
        //In-turn I see this god-awful code...
        @Override
        public void update(){
            if(timer(timerDump, dumpTime / timeScale)){
                dump(dominantItem != null && items.has(dominantItem) ? dominantItem : null);
            }

            if(dominantItem == null){
                return;
            }

            timeDrilled += warmup * delta();

            float delay = getDrillTime(dominantItem);

            if(items.total() < itemCapacity && dominantItems > 0 && efficiency > 0){
                float speed = Mathf.lerp(1f, liquidBoostIntensity, optionalEfficiency) * efficiency;

                lastDrillSpeed = (speed * dominantItems * warmup) / delay;
                warmup = Mathf.approachDelta(warmup, speed, warmupSpeed);
                progress += delta() * dominantItems * speed * warmup;
                //Added
                totalProgress  += warmup * Time.delta;
                if(Mathf.chanceDelta(updateEffectChance * warmup))
                    updateEffect.at(x + Mathf.range(size * 2f), y + Mathf.range(size * 2f));
            }else{
                lastDrillSpeed = 0f;
                warmup = Mathf.approachDelta(warmup, 0f, warmupSpeed);
                return;
            }

            if(dominantItems > 0 && progress >= delay && items.total() < itemCapacity){
                int amount = (int)(progress / delay);
                for(int i = 0; i < amount+mineAmount; i++){
                    offload(dominantItem);
                }

                progress %= delay;

                if(wasVisible && Mathf.chanceDelta(drillEffectChance * warmup)) drillEffect.at(x + Mathf.range(drillEffectRnd), y + Mathf.range(drillEffectRnd), dominantItem.color);
            }
        }
        @Override
        public void write(Writes write){
            super.write(write);
            write.f(totalProgress);
        }
        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            totalProgress = read.f();
        }
    }
}

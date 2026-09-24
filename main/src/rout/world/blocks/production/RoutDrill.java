package rout.world.blocks.production;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Strings;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.ui.Bar;
import mindustry.world.Tile;
import mindustry.world.blocks.production.Drill;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawDefault;
import mindustry.world.draw.DrawMulti;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import rout.world.blocks.environment.RouterFloor;

import static mindustry.Vars.iconSmall;
import static mindustry.Vars.tilesize;

public class RoutDrill extends Drill {
    public float consumeTime = 0f;
    public DrawBlock drawer;
    //How many items this block produces per mining cycle. Pretty much turns it into a
    //burstDrill except not needing a separate class. Take notes Anuke :troll:
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
        //Allow placement on RouterFloor zones even though no ore is there (the vanilla
        //check would reject it via canMine). On any other surface, fall through to the
        //standard Drill placement logic so the drill can actually mine.
        tile.getLinkedTilesAs(this, tempTiles);
        if(super.canPlaceOn(tile,team,rotation) && !tempTiles.contains(o -> (!o.floor().allowCorePlacement && !(o.floor() instanceof RouterFloor)) || o.block() instanceof CoreBlock)){
            return true;
        }
        return super.canPlaceOn(tile, team, rotation);
    }

    @Override
    public TextureRegion[] icons(){
        return drawer.finalIcons(this);
    }

    @Override
    public void setStats(){
        super.setStats();

        //Vanilla's Stat.drillSpeed is computed once in Drill.setStats. Replace it with
        //a mineAmount-scaled value so the UI reflects the burst output, not just the
        //per-cycle count.
        stats.remove(Stat.drillSpeed);
        stats.add(Stat.drillSpeed, 60f / drillTime * size * size * mineAmount, StatUnit.itemsSecond);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        //super.drawPlace(x, y, rotation, valid);

        Tile tile = Vars.world.tile(x, y);
        if(tile == null) return;

        countOre(tile);

        if(returnItem != null){
            //Mirror the setStats formula exactly so the PlaceDisplay text matches the
            //stats panel and accounts for the burst output.
            float width = drawPlaceText(Core.bundle.formatFloat("bar.drillspeed", 60f / getDrillTime(returnItem) * returnCount * mineAmount, 2), x, y, valid);
            float dx = x * tilesize + offset - width/2f - 4f, dy = y * tilesize + offset + size * tilesize / 2f + 5, s = iconSmall / 4f;
            Draw.mixcol(Color.darkGray, 1f);
            Draw.rect(returnItem.fullIcon, dx, dy - 1, s, s);
            Draw.reset();
            Draw.rect(returnItem.fullIcon, dx, dy, s, s);

            if(drawMineItem){
                Draw.color(returnItem.color);
                Draw.rect(itemRegion, tile.worldx() + offset, tile.worldy() + offset);
                Draw.color();
            }
        }else{
            Tile to = tile.getLinkedTilesAs(this, tempTiles).find(t -> t.drop() != null && (t.drop().hardness > tier || (blockedItems != null && blockedItems.contains(t.drop()))));
            Item item = to == null ? null : to.drop();
            if(item != null){
                drawPlaceText(Core.bundle.get("bar.drilltierreq"), x, y, valid);
            }
        }
    }

    public class RoutDrillBuild extends DrillBuild{
        //NEW fields only - DO NOT redeclare progress / warmup / lastDrillSpeed /
        //dominantItems / dominantItem. DrillBuild already owns those, and Java
        //field-shadowing would mean vanilla onProximityUpdate writes to one set
        //while this updateTile reads from a different set, so dominantItem stays
        //null and the drill never mines.
        public float totalProgress;
        public float consTimer;
        public float realProgress;
        //Vanilla has not added drill drawers to this version of mindustry.
        //Fine I'll do it myself. Again...
        @Override
        public void draw() {
            if(drawer != null) drawer.draw(this);

            Draw.z(Layer.blockCracks);
            drawDefaultCracks();

            Draw.z(Layer.blockAfterCracks);

            if(dominantItem != null && drawMineItem){
                Draw.color(dominantItem.color);
                Draw.rect(itemRegion, x, y);
                Draw.color();
            }
        }

        //TotalProgress however, did not exist on Building for this version of mindustry.
        //In-turn I see this god-awful code...
        @Override
        public void updateTile(){
            //does nothing for most Drills, as those do not require items.
            if(consumeTime > 0f && (consTimer += delta()) >= consumeTime){
                consume();
                consTimer %= consumeTime;
            }

            if(timer(timerDump, dumpTime / timeScale)){
                dump(dominantItem != null && items.has(dominantItem) ? dominantItem : null);
            }

            if(dominantItem == null){
                return;
            }

            totalProgress += warmup * delta();

            float delay = getDrillTime(dominantItem);

            if(items.total() + mineAmount <= itemCapacity && dominantItems > 0 && efficiency > 0){
                float speed = Mathf.lerp(1f, liquidBoostIntensity, optionalEfficiency) * efficiency;

                //Per-second rate scaled by mineAmount so the bar shows the burst output.
                //dominantItems still affects progress rate (more ore = shorter cycles), but
                //each cycle produces exactly mineAmount items.
                lastDrillSpeed = (speed * dominantItems * warmup) / delay * mineAmount;
                warmup = Mathf.approachDelta(warmup, efficiency, warmupSpeed);
                progress += delta() * dominantItems * speed * warmup;

                if(Mathf.chanceDelta(updateEffectChance * warmup))
                    updateEffect.at(x + Mathf.range(size * 2f), y + Mathf.range(size * 2f));
            }else{
                lastDrillSpeed = 0f;
                warmup = Mathf.approachDelta(warmup, 0f, warmupSpeed);
                return;
            }
            realProgress = progress/delay;
            if(dominantItems > 0 && progress >= delay && items.total() < itemCapacity){
                int cycles = (int)(progress / delay);
                //Cap completed cycles by available capacity so we never partially fill and
                //overflow. Each completed cycle emits exactly mineAmount items regardless
                //of how many ore tiles (dominantItems) are under the drill - mineAmount is
                //the per-cycle count, not per-tile.
                int maxCycles = (itemCapacity - items.total()) / Math.max(1, mineAmount);
                cycles = Math.min(cycles, maxCycles);
                int totalMined = cycles * mineAmount;
                for(int i = 0; i < totalMined; i++){
                    offload(dominantItem);
                }

                progress %= delay;

                if(wasVisible && Mathf.chanceDelta(drillEffectChance * warmup)){
                    drillEffect.at(x + Mathf.range(drillEffectRnd), y + Mathf.range(drillEffectRnd), dominantItem.color);
                }
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

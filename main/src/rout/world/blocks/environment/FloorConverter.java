package rout.world.blocks.environment;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Queue;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.ctype.ContentType;
import mindustry.entities.Effect;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import rout.annotations.Annotations;
import rout.gen.routSounds;
import rout.world.blocks.bases.RouterBlock;

import static mindustry.Vars.*;


public class FloorConverter extends RouterBlock {
    public ObjectMap<Floor, Floor> conversions = new ObjectMap<>();
    public ObjectSet<Floor> exclude = new ObjectSet<>();
    public int radius = 12;
    public float convertInterval = 6f;
    public float revertInterval = 4f;
    public int convertsPerStep = 4;
    public int revertsPerStep = 2;
    public Effect derich, enrich = Fx.none;
    public @Annotations.Load("@-glow") TextureRegion glow;
    public FloorConverter(String name){
        super(name);
        update = true;
        sync = true;
        solid = true;
        destructible = true;
        rotate = false;
    }

    @Override
    public void setStats(){
        super.setStats();
        if(radius > 0){
            stats.add(Stat.range, radius, StatUnit.blocks);
        }
    }

    /** Draws the conversion radius while the block is being placed. */
    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        if(radius <= 0) return;

        float cx = x * tilesize + offset;
        float cy = y * tilesize + offset;
        float rad = radius * tilesize;

        Color color = valid ? Pal.accent : Pal.remove;
        Draw.color(color, 0.05f);
        Fill.circle(cx, cy, rad);

        Draw.color(color);
        Lines.stroke(2f);
        Lines.circle(cx, cy, rad);
        Draw.reset();
    }
    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation) {
        if (tile == null) return false;
        tile.getLinkedTilesAs(this, tempTiles);
        if(!tempTiles.contains(o -> (!o.floor().allowCorePlacement && !(o.floor() instanceof RouterFloor)) || o.block() instanceof CoreBlock)){
            return true;
        }
        super.canPlaceOn(tile, team, rotation);
        return false;
    }
    public class FloorConverterBuild extends Building {
        private final ObjectMap<Tile, Floor> converted = new ObjectMap<>();
        private final Queue<Tile> frontier = new Queue<>();
        private final ObjectSet<Tile> visited = new ObjectSet<>();
        private boolean expanding;
        private float convertTimer, revertTimer, rescanTimer;

        private float capSq(){
            return (float)radius * radius * tilesize * tilesize;
        }

        private boolean inRadius(Tile t){
            float dx = t.worldx() - x, dy = t.worldy() - y;
            return dx * dx + dy * dy <= capSq();
        }

        private boolean eligible(Tile t){
            if(t == null || t.build == this) return false;
            if(!conversions.containsKey(t.floor())) return false;
            if(exclude.contains(t.floor())) return false;
            if(converted.containsKey(t)) return false;
            return true;
        }

        private void seedFrontier(){
            frontier.clear();
            visited.clear();
            int ax = tile.x + size / 2;
            int ay = tile.y + size / 2;
            for(int dx = -1; dx <= 1; dx++){
                for(int dy = -1; dy <= 1; dy++){
                    Tile t = world.tile(ax + dx, ay + dy);
                    if(t == null) continue;
                    if(visited.add(t)){
                        frontier.addLast(t);
                    }
                }
            }
        }

        private void advanceWave(int count){
            for(int i = 0; i < count && frontier.size > 0; i++){
                Tile t = frontier.removeFirst();
                if(t == null) continue;
                if(inRadius(t) && eligible(t)){
                    converted.put(t, t.floor());
                    routSounds.RouterSpread.at(t.worldx(),t.worldy());
                    enrich.at(t.worldx(),t.worldy());
                    t.setFloor(conversions.get(t.floor()));
                }
                //expand to cardinal neighbours within the radius, passing over
                //converted floors so the wave keeps propagating.
                for(int d = 0; d < 4; d++){
                    Tile n = world.tile(t.x + Geometry.d4[d].x, t.y + Geometry.d4[d].y);
                    if(n == null) continue;
                    if(!inRadius(n)) continue;
                    if(visited.add(n)){
                        frontier.addLast(n);
                    }
                }
            }
        }

        private void revertStep(int count){
            var it = converted.entries();
            for(int i = 0; i < count && it.hasNext(); i++){
                var entry = it.next();
                Tile t = entry.key;
                if(conversions.containsValue(t.floor(), false)){
                    //any building left on a reverted floor is destroyed with it
                    if(t.build != null){
                        t.build.kill();
                    }
                    routSounds.RouterSpread.at(t.worldx(),t.worldy());
                    derich.at(t.worldx(),t.worldy());
                    t.setFloor(entry.value);
                }
                it.remove();
            }
        }

        private void revertAll(){
            for(var entry : converted){
                if(conversions.containsValue(entry.key.floor(), false)){
                    //any building left on a reverted floor is destroyed with it
                    if(entry.key.build != null){
                        entry.key.build.kill();
                    }
                    entry.key.setFloor(entry.value);
                }
            }
            converted.clear();
        }

        @Override
        public void onRemoved(){
            revertAll();
            super.onRemoved();
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            if(efficiency > 0.0001f){
                Draw.color(team.color, Mathf.absin(Time.time, 12f, 0.25f));
                Draw.rect(glow, x, y);
                Draw.color();
            }
        }

        @Override
        public void updateTile(){
            if(conversions.isEmpty()) return;

            if(efficiency > 0.0001f){
                if(!expanding){
                    seedFrontier();
                    expanding = true;
                }
                if(frontier.size == 0){
                    rescanTimer += Time.delta;
                    if(rescanTimer >= 45f){
                        rescanTimer = 0f;
                        seedFrontier();
                    }
                }else{
                    rescanTimer = 0f;
                }
                convertTimer += Time.delta;
                if(convertTimer >= convertInterval){
                    convertTimer = 0f;
                    advanceWave(Math.max(1, Math.round(convertsPerStep * efficiency)));
                }
            }else{
                expanding = false;
                frontier.clear();
                visited.clear();
                rescanTimer = 0f;
                revertTimer += Time.delta;
                if(revertTimer >= revertInterval){
                    revertTimer = 0f;
                    revertStep(revertsPerStep);
                }
            }
        }

        @Override
        public byte version(){
            return 4;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(converted.size);
            for(ObjectMap.Entry<Tile, Floor> entry : converted){
                write.s(entry.key.x);
                write.s(entry.key.y);
                write.s(entry.value.id);
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            converted.clear();
            if(revision >= 2){
                int n = read.i();
                for(int i = 0; i < n; i++){
                    short x = read.s(), y = read.s(), fid = read.s();
                    Tile t = world.tile(x, y);
                    Floor f = content.getByID(ContentType.block, fid);
                    if(t != null && f != null && conversions.containsValue(t.floor(), false)){
                        converted.put(t, f);
                    }
                }
            }
        }
    }
}
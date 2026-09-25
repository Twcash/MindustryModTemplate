package rout.world.campaign;

import arc.Core;
import arc.math.geom.Vec2;
import arc.math.geom.Vec3;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.core.GameState;
import mindustry.game.Waves;
import mindustry.maps.planet.SerpuloPlanetGenerator;
import mindustry.type.Sector;
import mindustry.world.Tile;
import mindustry.world.TileGen;
import mindustry.world.Tiles;
import arc.graphics .*;
import arc.math .*;
import arc.math.geom .*;
import arc.struct .*;
import arc.util .*;
import arc.util.noise .*;
import mindustry.ai .*;
import mindustry.ai.BaseRegistry .*;
import mindustry.content .*;
import mindustry.game .*;
import mindustry.gen .*;
import mindustry.maps.generators .*;
import mindustry.type .*;
import mindustry.world .*;
import mindustry.world.blocks.environment .*;
import rout.content.RoutLoadouts;

import static mindustry.Vars .*;

    public class RoutPlanetGenerator extends PlanetGenerator {

        //alternate, less direct generation
        public static boolean indirectPaths = true;
        //random water patches
        public static boolean genLakes = true;

        BaseGenerator basegen = new BaseGenerator();
        float heightYOffset = 42.7f;
        float scl = 5f;
        float waterOffset = 0.04f;
        float heightScl = 1.01f;

        Block[][] arr =
                {
                        {Blocks.water, Blocks.darksandWater, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.darksandTaintedWater, Blocks.stone, Blocks.stone},
                        {Blocks.water, Blocks.darksandWater, Blocks.darksand, Blocks.darksand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.darksandTaintedWater, Blocks.stone, Blocks.stone, Blocks.stone},
                        {Blocks.water, Blocks.darksandWater, Blocks.darksand, Blocks.sand, Blocks.salt, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.darksandTaintedWater, Blocks.stone, Blocks.stone, Blocks.stone},
                        {Blocks.water, Blocks.sandWater, Blocks.sand, Blocks.salt, Blocks.salt, Blocks.salt, Blocks.sand, Blocks.stone, Blocks.stone, Blocks.stone, Blocks.snow, Blocks.iceSnow, Blocks.ice},
                        {Blocks.deepwater, Blocks.water, Blocks.sandWater, Blocks.sand, Blocks.salt, Blocks.sand, Blocks.sand, Blocks.basalt, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.ice},
                        {Blocks.deepwater, Blocks.water, Blocks.sandWater, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.moss, Blocks.iceSnow, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.snow, Blocks.ice},
                        {Blocks.deepwater, Blocks.sandWater, Blocks.sand, Blocks.sand, Blocks.moss, Blocks.moss, Blocks.snow, Blocks.basalt, Blocks.basalt, Blocks.basalt, Blocks.ice, Blocks.snow, Blocks.ice},
                        {Blocks.deepTaintedWater, Blocks.darksandTaintedWater, Blocks.darksand, Blocks.darksand, Blocks.basalt, Blocks.moss, Blocks.basalt, Blocks.hotrock, Blocks.basalt, Blocks.ice, Blocks.snow, Blocks.ice, Blocks.ice},
                        {Blocks.darksandWater, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.moss, Blocks.sporeMoss, Blocks.snow, Blocks.basalt, Blocks.basalt, Blocks.ice, Blocks.snow, Blocks.ice, Blocks.ice},
                        {Blocks.darksandWater, Blocks.darksand, Blocks.darksand, Blocks.sporeMoss, Blocks.ice, Blocks.ice, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.ice, Blocks.ice},
                        {Blocks.deepTaintedWater, Blocks.darksandTaintedWater, Blocks.darksand, Blocks.sporeMoss, Blocks.sporeMoss, Blocks.ice, Blocks.ice, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice},
                        {Blocks.taintedWater, Blocks.darksandTaintedWater, Blocks.darksand, Blocks.sporeMoss, Blocks.moss, Blocks.sporeMoss, Blocks.iceSnow, Blocks.snow, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice},
                        {Blocks.darksandWater, Blocks.darksand, Blocks.snow, Blocks.ice, Blocks.iceSnow, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice}
                };

        ObjectMap<Block, Block> dec = ObjectMap.of(
                Blocks.sporeMoss, Blocks.sporeCluster,
                Blocks.moss, Blocks.sporeCluster,
                Blocks.taintedWater, Blocks.water,
                Blocks.darksandTaintedWater, Blocks.darksandWater
        );

        ObjectMap<Block, Block> tars = ObjectMap.of(
                Blocks.sporeMoss, Blocks.shale,
                Blocks.moss, Blocks.shale
        );

        float water = 2f / arr[0].length;
        //megabase position
        Vec3 basePos = new Vec3(0.9341721, 0.0, 0.3568221);

        float rawHeight(Vec3 position){
            return (Mathf.pow(Simplex.noise3d(seed, 7, 0.5f, 1f/3f, position.x * scl, position.y * scl + heightYOffset, position.z * scl) * heightScl, 2.3f) + waterOffset) / (1f + waterOffset);
        }

        @Override
        public void onSectorCaptured(Sector sector){
            sector.planet.reloadMeshAsync();
        }

        @Override
        public void onSectorLost(Sector sector){
            sector.planet.reloadMeshAsync();
        }

        @Override
        public void beforeSaveWrite(Sector sector){
            sector.planet.reloadMeshAsync();
        }

        @Override
        public boolean isEmissive(){
            return true;
        }

        public boolean allowNumberedLaunch(Sector s){
            return s.hasBase() && !s.isAttacked();
        }

        @Override
        public boolean allowLanding(Sector sector){
            return sector.planet.allowLaunchToNumbered && (sector.hasBase() || sector.near().contains(this::allowNumberedLaunch));
        }

        @Override
        public @Nullable Sector findLaunchCandidate(Sector destination, @Nullable Sector selected){
            if(destination.preset == null || !destination.preset.requireUnlock){
                if(selected != null && selected.isNear(destination) && allowNumberedLaunch(selected)){
                    return selected;
                }else{
                    return destination.near().find(this::allowNumberedLaunch);
                }
            }else{
                return super.findLaunchCandidate(destination, selected);
            }
        }

        @Override
        public void getLockedText(Sector hovered, StringBuilder out){
            //no locked message
        }

        @Override
        public float getHeight(Vec3 position){
            float height = rawHeight(position);
            return Math.max(height, water);
        }

        @Override
        public void getColor(Vec3 position, Color out){
            Block block = getBlock(position, true);
            //replace salt with sand color
            if(block == Blocks.salt) block = Blocks.sand;
            out.set(block.mapColor).a(1f - block.albedo);
        }

        @Override
        public void getEmissiveColor(Vec3 position, Color out){
            float dst = 999f, captureDst = 999f, lightScl = 0f;

            Object[] sectors = Planets.serpulo.sectors.items;
            int size = Planets.serpulo.sectors.size;

            for(int i = 0; i < size; i ++){
                var sector = (Sector)sectors[i];

                if(sector.hasEnemyBase() && !sector.isCaptured()){
                    dst = Math.min(dst, position.dst(sector.tile.v) - (sector.preset != null ? sector.preset.difficulty/10f * 0.03f - 0.03f : 0f));
                }else if(sector.hasBase()){
                    float cdst = position.dst(sector.tile.v);
                    if(cdst < captureDst){
                        captureDst = cdst;
                        lightScl = sector.info.lightCoverage;
                    }
                }
            }

            lightScl = Math.min(lightScl / 50000f, 1.3f);
            if(lightScl < 1f) lightScl = Interp.pow5Out.apply(lightScl);

            float freq = 0.05f;
            //TODO: once the old megabase returns, change it to 0.55f
            if(position.dst(basePos) < 0.3f ?

                    dst*metalDstScl + Simplex.noise3d(seed + 1, 3, 0.4, 5.5f, position.x, position.y + 200f, position.z)*0.08f + ((basePos.dst(position) + 0.00f) % freq < freq/2f ? 1f : 0f) * 0.07f < 0.08f/* || dst <= 0.0001f*/ :
                    dst*metalDstScl + Simplex.noise3d(seed, 3, 0.4, 9f, position.x, position.y + 370f, position.z)*0.06f < 0.045){

                out.set(Team.crux.color)
                        .mul(0.8f + Simplex.noise3d(seed, 1, 1, 9f, position.x, position.y + 99f, position.z) * 0.4f)
                        .lerp(Team.sharded.color, 0.2f*Simplex.noise3d(seed, 1, 1, 9f, position.x, position.y + 999f, position.z)).toFloatBits();
            }else if(captureDst*metalDstScl + Simplex.noise3d(seed, 3, 0.4, 9f, position.x, position.y + 600f, position.z)*0.07f < 0.05 * lightScl){
                out.set(Team.sharded.color).mul(0.7f + Simplex.noise3d(seed, 1, 1, 9f, position.x, position.y + 99f, position.z) * 0.4f)
                        .lerp(Team.crux.color, 0.3f*Simplex.noise3d(seed, 1, 1, 9f, position.x, position.y + 999f, position.z)).toFloatBits();

            }
        }

        @Override
        public void genTile(Vec3 position, TileGen tile){
            tile.floor = getBlock(position, false);
            if(tile.floor == Blocks.darkPanel6) tile.floor = Blocks.darkPanel3;
            tile.block = tile.floor.asFloor().wall;

            if(Ridged.noise3d(seed + 1, position.x, position.y, position.z, 2, 22) > 0.31){
                tile.block = Blocks.air;
            }
        }

        static double metalDstScl = 0.25;

        Block getBlock(Vec3 position, boolean visualOnly){
            float height = rawHeight(position);
            float px = position.x * scl, py = position.y * scl, pz = position.z * scl;

            float rad = scl;
            float temp = Mathf.clamp(Math.abs(py * 2f) / (rad));
            float tnoise = Simplex.noise3d(seed, 7, 0.56, 1f/3f, px, py + 999f - 0.1f, pz);
            temp = Mathf.lerp(temp, tnoise, 0.5f);
            height *= 1.2f;
            height = Mathf.clamp(height);

            float tar = Simplex.noise3d(seed, 4, 0.55f, 1f/2f, px, py + 999f, pz) * 0.3f + position.dst(0, 0, 1f) * 0.2f;

            Block res = arr[Mathf.clamp((int)(temp * arr.length), 0, arr[0].length - 1)][Mathf.clamp((int)(height * arr[0].length), 0, arr[0].length - 1)];
            if(tar > 0.5f){
                return tars.get(res, res);
            }else{
                if(visualOnly && position.within(basePos, 0.65f)){

                    float dst = 999f;

                    Object[] sectors = Planets.serpulo.sectors.items;
                    int size = Planets.serpulo.sectors.size;

                    for(int i = 0; i < size; i ++){
                        var sector = (Sector)sectors[i];

                        if(sector.hasEnemyBase()){
                            dst = Math.min(dst, position.dst(sector.tile.v));
                        }
                    }

                    float freq = 0.05f, freq2 = 0.07f;

                    if(dst*0.85f + Simplex.noise3d(seed, 3, 0.4, 5.5f, position.x, position.y + 200f, position.z)*0.015f + ((basePos.dst(position) + 0.00f) % freq < freq/2f ? 1f : 0f) * 0.07f < 0.15f){
                        return ((basePos.dst(position) + 0.01f) % freq2 < freq2*0.65f) ? Blocks.metalFloor : Blocks.darkPanel6;
                    }
                }
                return res;
            }
        }

        @Override
        protected float noise(float x, float y, double octaves, double falloff, double scl, double mag){
            Vec3 v = sector.rect.project(x, y).scl(5f);
            return Simplex.noise3d(seed, octaves, falloff, 1f / scl, v.x, v.y, v.z) * (float)mag;
        }


        // ---- modular biomes (latitude-based) ----
        // latitude 0 = equator, ~1 = pole (|sector.tile.v.y|)

        void generateTerrain(){
            //latitude 0 = equator, ~1 = pole
            float lat = Math.abs(sector.tile.v.y);
            //shared noise field, remapped [0,1] -> [-1,1] to match tool preview.
            //v[0..4] = the tool's n1..n5.
            float[] v = new float[5];
            if(lat > 0.6f){
                pass((x, y) -> {
                    block = Blocks.air;
                    float v_n1 = ((float)Simplex.noise2d(seed + 2, 7, 0.65f, 1f/89f,
                            x + 30.00f * ((float)Simplex.noise2d(seed, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f, y + 30.00f * ((float)Simplex.noise2d(seed + 1, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f) - 0.5f) * 2f;
                    float v_n2 = ((float)Simplex.noise2d(seed + 2, 3, 0.50f, 1f/103f,
                            x + 4.00f * ((float)Simplex.noise2d(seed, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f, y + 4.00f * ((float)Simplex.noise2d(seed + 1, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f) - 0.5f) * 2f;
                    float v_n3 = ((float)Simplex.noise2d(seed + 2, 6, 0.70f, 1f/127f,
                            x + 4.00f * ((float)Simplex.noise2d(seed, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f, y + 4.00f * ((float)Simplex.noise2d(seed + 1, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f) - 0.5f) * 2f;
                    float v_n4 = ((float)Simplex.noise2d(seed + 2, 4, 0.55f, 1f/123f,
                            x + 6.00f * ((float)Simplex.noise2d(seed, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f, y + 6.00f * ((float)Simplex.noise2d(seed + 1, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f) - 0.5f) * 2f;
                    float v_n5 = Mathf.dst(x, y, width/2f, height/2f) / Math.max(width, height);

                    floor = Blocks.ice;

                    if(v_n1 > 0.00f){ floor = Blocks.snow; }
                    if(v_n1 > 0.00f && v_n1 < 0.03f){ floor = Blocks.iceSnow; }
                    if(v_n1 < 0.00f && v_n1 > -0.08f && v_n2 > 0.00f){ floor = Blocks.iceSnow; }
                    if(v_n1 > 0.14f){ floor = Blocks.basalt; }
                    if(v_n1 > 0.63f && v_n2 < 0.00f){ floor = Blocks.hotrock; }
                    if(v_n1 > 0.73f){ floor = Blocks.magmarock; }
                    if(v_n3 > 0.09f && v_n1 < -0.14f){ floor = Blocks.iceSnow; }
                    if(v_n3 > 0.15f && v_n2 < 0.00f){ floor = Blocks.snow; }
                    if(v_n5 > 0.46f && v_n3 < -0.01f){ block = Blocks.iceWall; }
                    if(v_n5 > 0.49f){ block = Blocks.snowWall; }
                });
            } else if(lat > 0.4f) {
                pass((x, y) -> {
                    block = Blocks.air;
                    float v_n1 = ((float)Simplex.noise2d(seed + 2, 7, 0.50f, 1f/36f,
                            x + 13.00f * ((float)Simplex.noise2d(seed, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f, y + 13.00f * ((float)Simplex.noise2d(seed + 1, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f) - 0.5f) * 2f;
                    float v_n2 = ((float)Simplex.noise2d(seed + 2, 5, 0.50f, 1f/51f,
                            x + 4.00f * ((float)Simplex.noise2d(seed, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f, y + 4.00f * ((float)Simplex.noise2d(seed + 1, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f) - 0.5f) * 2f;
                    float v_n3 = ((float)Simplex.noise2d(seed + 2, 6, 0.70f, 1f/90f,
                            x + 4.00f * ((float)Simplex.noise2d(seed, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f, y + 4.00f * ((float)Simplex.noise2d(seed + 1, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f) - 0.5f) * 2f;
                    float v_n4 = ((float)Simplex.noise2d(seed + 2, 4, 0.55f, 1f/60f,
                            x + 6.00f * ((float)Simplex.noise2d(seed, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f, y + 6.00f * ((float)Simplex.noise2d(seed + 1, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f) - 0.5f) * 2f;
                    float v_n5 = Mathf.dst(x, y, width/2f, height/2f) / Math.max(width, height);

                    floor = Blocks.taintedWater;

                    if(v_n1 > 0.00f){ floor = Blocks.moss; }
                    if(v_n1 > 0.00f && v_n1 < 0.03f){ floor = Blocks.shale; }
                    if(v_n1 < 0.00f && v_n1 > -0.08f && v_n2 > 0.00f){ floor = Blocks.shale; }
                    if(v_n1 > 0.14f){ floor = Blocks.moss; }
                    if(v_n1 > 0.63f && v_n2 < 0.00f){ floor = Blocks.sporeMoss; }
                    if(v_n1 > 0.73f){ floor = Blocks.sporeMoss; }
                    if(v_n3 > 0.09f && v_n1 < -0.14f){ floor = Blocks.shale; }
                    if(v_n3 > 0.15f && v_n2 < 0.00f){ floor = Blocks.moss; }
                    if(v_n5 > 0.46f && v_n3 < -0.01f){ block = Blocks.sporePine; }
                    if(v_n5 > 0.49f){ block = Blocks.sporeWall; }
                });
            } else if (lat > 0.2f) {
                pass((x, y) -> {
                    block = Blocks.air;
                    float v_n1 = ((float)Simplex.noise2d(seed + 2, 7, 0.50f, 1f/36f,
                            x + 13.00f * ((float)Simplex.noise2d(seed, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f, y + 13.00f * ((float)Simplex.noise2d(seed + 1, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f) - 0.5f) * 2f;
                    float v_n2 = ((float)Simplex.noise2d(seed + 2, 5, 0.50f, 1f/51f,
                            x + 4.00f * ((float)Simplex.noise2d(seed, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f, y + 4.00f * ((float)Simplex.noise2d(seed + 1, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f) - 0.5f) * 2f;
                    float v_n3 = ((float)Simplex.noise2d(seed + 2, 6, 0.70f, 1f/90f,
                            x + 4.00f * ((float)Simplex.noise2d(seed, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f, y + 4.00f * ((float)Simplex.noise2d(seed + 1, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f) - 0.5f) * 2f;
                    float v_n4 = ((float)Simplex.noise2d(seed + 2, 4, 0.55f, 1f/60f,
                            x + 6.00f * ((float)Simplex.noise2d(seed, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f, y + 6.00f * ((float)Simplex.noise2d(seed + 1, 2, 0.50f, 1f/90f, x, y) - 0.5f) * 2f) - 0.5f) * 2f;
                    float v_n5 = Mathf.dst(x, y, width/2f, height/2f) / Math.max(width, height);

                    floor = Blocks.sandWater;

                    if(v_n1 > 0.00f){ floor = Blocks.sand; }
                    if(v_n1 > 0.00f && v_n1 < 0.03f){ floor = Blocks.sand; }
                    if(v_n1 < 0.00f && v_n1 > -0.08f && v_n2 > 0.00f){ floor = Blocks.sand; }
                    if(v_n1 > 0.14f){ floor = Blocks.stone; }
                    if(v_n1 > 0.63f && v_n2 < 0.00f){ floor = Blocks.stone; }
                    if(v_n1 > 0.73f){ floor = Blocks.stone; }
                    if(v_n3 > 0.09f && v_n1 < -0.14f){ floor = Blocks.stone; }
                    if(v_n3 > 0.15f && v_n2 < 0.00f){ floor = Blocks.stone; }
                    if(v_n5 > 0.46f && v_n3 < -0.01f){ block = Blocks.sandWall; }
                    if(v_n5 > 0.49f){ block = Blocks.sandWall; }
                });
            } else {
                pass((x, y) -> {
                    block = Blocks.air;
                    float v_n2 = ((float)Simplex.noise2d(seed + 2, 4, 0.55f, (1f / (55.00f * width / 160f)),
                            x + 30.00f * ((float)Simplex.noise2d(seed + 0, 2, 0.50f, 1f / (90f * width / 160f), x, y) - 0.5f) * 2f, y + 30.00f * ((float)Simplex.noise2d(seed + 1, 2, 0.50f, 1f / (90f * width / 160f), x, y) - 0.5f) * 2f) - 0.5f) * 2f;
                    float v_n3 = (((float)Ridged.noise2d(seed + 9, x, y, 3, (1f / (22.00f * width / 160f))) + 1f) / 2f);
                    float v_n4 = (v_n2 * v_n3);

                    floor = Blocks.stone;

                    if(v_n4 > 0.00f){ floor = Blocks.sand; }
                    if(((floor == Blocks.sand) ? 1f : 0f) != 0f){ floor = Blocks.moss; }
                });
            }
        }

        @Override
        protected void generate(){

            class Room{
                int x, y, radius;
                ObjectSet<Room> connected = new ObjectSet<>();

                Room(int x, int y, int radius){
                    this.x = x;
                    this.y = y;
                    this.radius = radius;
                    connected.add(this);
                }

                void join(int x1, int y1, int x2, int y2){
                    float nscl = rand.random(100f, 140f) * 6f;
                    int stroke = rand.random(3, 9);
                    brush(pathfind(x1, y1, x2, y2, tile -> (tile.solid() ? 50f : 0f) + noise(tile.x, tile.y, 2, 0.4f, 1f / nscl) * 500, Astar.manhattan), stroke);
                }

                void connect(Room to){
                    if(!connected.add(to) || to == this) return;

                    Vec2 midpoint = Tmp.v1.set(to.x, to.y).add(x, y).scl(0.5f);
                    rand.nextFloat();

                    if(indirectPaths){
                        midpoint.add(Tmp.v2.set(1, 0f).setAngle(Angles.angle(to.x, to.y, x, y) + 90f * (rand.chance(0.5) ? 1f : -1f)).scl(Tmp.v1.dst(x, y) * 2f));
                    }else{
                        //add randomized offset to avoid straight lines
                        midpoint.add(Tmp.v2.setToRandomDirection(rand).scl(Tmp.v1.dst(x, y)));
                    }

                    midpoint.sub(width/2f, height/2f).limit(width / 2f / Mathf.sqrt3).add(width/2f, height/2f);

                    int mx = (int)midpoint.x, my = (int)midpoint.y;

                    join(x, y, mx, my);
                    join(mx, my, to.x, to.y);
                }

                void joinLiquid(int x1, int y1, int x2, int y2){
                    float nscl = rand.random(100f, 140f) * 6f;
                    int rad = rand.random(7, 11);
                    int avoid = 2 + rad;
                    var path = pathfind(x1, y1, x2, y2, tile -> (tile.solid() || !tile.floor().isLiquid ? 70f : 0f) + noise(tile.x, tile.y, 2, 0.4f, 1f / nscl) * 500, Astar.manhattan);
                    path.each(t -> {
                        //don't place liquid paths near the core
                        if(Mathf.dst2(t.x, t.y, x2, y2) <= avoid * avoid){
                            return;
                        }

                        for(int x = -rad; x <= rad; x++){
                            for(int y = -rad; y <= rad; y++){
                                int wx = t.x + x, wy = t.y + y;
                                if(Structs.inBounds(wx, wy, width, height) && Mathf.within(x, y, rad)){
                                    Tile other = tiles.getn(wx, wy);
                                    other.setBlock(Blocks.air);
                                    if(Mathf.within(x, y, rad - 1) && !other.floor().isLiquid){
                                        Floor floor = other.floor();
                                        //TODO does not respect tainted floors
                                        //other.setFloor((Floor)(floor == Blocks.sand || floor == Blocks.salt ? Blocks.sandWater : Blocks.darksandTaintedWater));
                                    }
                                }
                            }
                        }
                    });
                }

                void connectLiquid(Room to){
                    if(to == this) return;

                    Vec2 midpoint = Tmp.v1.set(to.x, to.y).add(x, y).scl(0.5f);
                    rand.nextFloat();

                    //add randomized offset to avoid straight lines
                    midpoint.add(Tmp.v2.setToRandomDirection(rand).scl(Tmp.v1.dst(x, y)));
                    midpoint.sub(width/2f, height/2f).limit(width / 2f / Mathf.sqrt3).add(width/2f, height/2f);

                    int mx = (int)midpoint.x, my = (int)midpoint.y;

                    joinLiquid(x, y, mx, my);
                    joinLiquid(mx, my, to.x, to.y);
                }
            }

            //=== open-center map design ===
            //The player base sits in a wide open center. Wall density rises with distance
            //from the centre (noise-modulated so it's organic, not a uniform ring), slowly
            //constricting into enemy spawns near the map edges.
            //Player spawn: exact centre, always cleared open.
            Room spawn = new Room(width / 2, height / 2, Math.max(width / 5, 18));
            Seq<Room> enemies = new Seq<>();
            Seq<Room> roomseq = new Seq<>();
            roomseq.add(spawn);

            //Enemy spawns: one per adjacent uncaptured sector, around the perimeter.
            Seq<Sector> adjSectors = sector.near().select(s -> !s.isCaptured());
            int enemySpawns = Math.max(adjSectors.size, 1);
            for(int j = 0; j < enemySpawns; j++){
                float angle = 360f * j / enemySpawns + rand.range(18f);
                float len = width / 2f - 22f - rand.random(0f, 14f);
                int ex = (int)(width / 2f + Angles.trnsx(angle, len));
                int ey = (int)(height / 2f + Angles.trnsy(angle, len));
                ex = Mathf.clamp(ex, 3, width - 4);
                ey = Mathf.clamp(ey, 3, height - 4);
                Room espawn = new Room(ex, ey, rand.random(6, 10));
                roomseq.add(espawn);
                enemies.add(espawn);
            }

            //Terrain: radial constriction. Center stays open; walls grow with distance and
            //are warped by 2D tile-space noise (NOT noise(), which is flat inside a sector).
            //open terrain: no wall generation (wall ring around the map edge removed)
            generateTerrain();

            //clear radius around each room (open clearings + carved spawn pockets)
            for(Room room : roomseq){
                erase(room.x, room.y, room.radius);
            }

            //carve a path from every enemy spawn in toward the centre
            for(Room room : enemies){
                spawn.connect(room);
            }

            int tlen = tiles.width * tiles.height;
            int total = 0, waters = 0;

            for(int i = 0; i < tlen; i++){
                Tile tile = tiles.geti(i);
                if(tile.block() == Blocks.air){
                    total ++;
                    if(tile.floor().liquidDrop == Liquids.water){
                        waters ++;
                    }
                }
            }

            boolean naval = (float)waters / total >= 0.19f;

            //create water pathway if the map is flooded
            if(naval){
                for(Room room : enemies){
                    room.connectLiquid(spawn);
                }
            }

            Seq<Block> ores = Seq.with(Blocks.oreCopper);
            float poles = Math.abs(sector.tile.v.y);
            float nmag = 0.5f;
            float scl = 1f;
            float addscl = 1.3f;

            if(Simplex.noise3d(seed, 2, 0.5, scl, sector.tile.v.x, sector.tile.v.y, sector.tile.v.z)*nmag + poles > 0.25f*addscl){
                //ores.add(Blocks.oreCoal);
            }

            if(Simplex.noise3d(seed, 2, 0.5, scl, sector.tile.v.x + 1, sector.tile.v.y, sector.tile.v.z)*nmag + poles > 0.5f*addscl){
                //ores.add(Blocks.oreTitanium);
            }

            //218 doesn't have thorium generation due to proximity (TODO remove the special case and replace with hidden preset)
            if(Simplex.noise3d(seed, 2, 0.5, scl, sector.tile.v.x + 2, sector.tile.v.y, sector.tile.v.z)*nmag + poles > 0.7f*addscl && sector.id != 218){
                //ores.add(Blocks.oreThorium);
            }

            if(rand.chance(0.25)){
                //ores.add(Blocks.oreScrap);
            }

            FloatSeq frequencies = new FloatSeq();
            for(int i = 0; i < ores.size; i++){
                frequencies.add(rand.random(-0.1f, 0.01f) - i * 0.01f + poles * 0.04f);
            }

            pass((x, y) -> {
                if(!floor.asFloor().hasSurface()) return;

                int offsetX = x - 4, offsetY = y + 23;
                for(int i = ores.size - 1; i >= 0; i--){
                    Block entry = ores.get(i);
                    float freq = frequencies.get(i);
                    if(Math.abs(0.5f - noise(offsetX, offsetY + i*999, 2, 0.7, (40 + i * 2))) > 0.22f + i*0.01 &&
                            Math.abs(0.5f - noise(offsetX, offsetY - i*999, 1, 1, (30 + i * 4))) > 0.37f + freq){
                        ore = entry;
                        break;
                    }
                }

                if(ore == Blocks.oreScrap && rand.chance(0.33)){
                    floor = Blocks.metalFloorDamaged;
                }
            });

            float difficulty = sector.threat;
            //difficulty scales with distance from the start sector (11): further away = harder
            Sector start = sector.planet.sectors.find(s -> s.id == 11);
            if(start != null){
                float dist = sector.tile.v.dst(start.tile.v);
                float maxDist = 0f;
                for(Sector s : sector.planet.sectors){
                    maxDist = Math.max(maxDist, s.tile.v.dst(start.tile.v));
                }
                if(maxDist > 0f){
                    difficulty = Mathf.clamp(0.3f + 0.9f * (dist / maxDist), 0.3f, 1.2f);
                }
            }
            int ruinCount = rand.random(-2, 4);

            if(ruinCount > 0){
                IntSeq ints = new IntSeq(width * height / 4);

                int padding = 25;

                //create list of potential positions
                for(int x = padding; x < width - padding; x++){
                    for(int y = padding; y < height - padding; y++){
                        Tile tile = tiles.getn(x, y);
                        if(!tile.solid() && (tile.drop() != null || tile.floor().liquidDrop != null)){
                            ints.add(tile.pos());
                        }
                    }
                }

                ints.shuffle(rand);

                int placed = 0;
                float diffRange = 0.4f;
                //try each position
                for(int i = 0; i < ints.size && placed < ruinCount; i++){
                    int val = ints.items[i];
                    int x = Point2.x(val), y = Point2.y(val);

                    //do not overwrite player spawn
                    if(Mathf.within(x, y, spawn.x, spawn.y, 18f)){
                        continue;
                    }

                    float range = difficulty + rand.random(diffRange);

                    Tile tile = tiles.getn(x, y);
                    BasePart part = null;
                    if(tile.overlay().itemDrop != null){
                        part = bases.forResource(tile.drop()).getFrac(range);
                    }else if(tile.floor().liquidDrop != null && rand.chance(0.05)){
                        part = bases.forResource(tile.floor().liquidDrop).getFrac(range);
                    }else if(rand.chance(0.05)){ //ore-less parts are less likely to occur.
                        part = bases.parts.getFrac(range);
                    }

                    //actually place the part
                    if(part != null && BaseGenerator.tryPlace(part, x, y, Team.derelict, rand, (cx, cy) -> {
                        Tile other = tiles.getn(cx, cy);
                        if(other.floor().hasSurface()){
                            //other.setOverlay(Blocks.oreScrap);
                            for(int j = 1; j <= 2; j++){
                                for(Point2 p : Geometry.d8){
                                    Tile t = tiles.get(cx + p.x*j, cy + p.y*j);
                                    if(t != null && t.floor().hasSurface() && rand.chance(j == 1 ? 0.4 : 0.2)){
                                        //t.setOverlay(Blocks.oreScrap);
                                    }
                                }
                            }
                        }
                    })){
                        placed ++;

                        int debrisRadius = Math.max(part.schematic.width, part.schematic.height)/2 + 3;
                        Geometry.circle(x, y, tiles.width, tiles.height, debrisRadius, (cx, cy) -> {
                            float dst = Mathf.dst(cx, cy, x, y);
                            float removeChance = Mathf.lerp(0.05f, 0.5f, dst / debrisRadius);

                            Tile other = tiles.getn(cx, cy);
                            if(other.build != null && other.isCenter()){
                                if(other.team() == Team.derelict && rand.chance(removeChance)){
                                    other.remove();
                                }else if(rand.chance(0.5)){
                                    other.build.health = other.build.health - rand.random(other.build.health * 0.9f);
                                }
                            }
                        });
                    }
                }
            }

            //remove invalid ores
            for(Tile tile : tiles){
                if(tile.overlay().needsSurface && !tile.floor().hasSurface()){
                    tile.setOverlay(Blocks.air);
                }
            }
            Schematics.place(RoutLoadouts.routerCore, spawn.x, spawn.y, Team.sharded);

            //router floor around the core
            Geometry.circle(spawn.x, spawn.y, tiles.width, tiles.height, 8, (x, y) -> {
                Tile t = tiles.getn(x, y);
                if(!t.floor().isLiquid){
                    t.setFloor(rout.content.RoutBlocks.routerFloor.asFloor());
                    t.setOverlay(Blocks.air);
                }
            });

            for(Room espawn : enemies){
                tiles.getn(espawn.x, espawn.y).setOverlay(Blocks.spawn);
            }

            if(sector.hasEnemyBase()){
                basegen.generate(tiles, enemies.map(r -> tiles.getn(r.x, r.y)), tiles.get(spawn.x, spawn.y), state.rules.waveTeam, sector, difficulty);

                state.rules.attackMode = sector.info.attack = true;
            }else{
                state.rules.winWave = sector.info.winWave = 10 + 5 * (int)Math.max(difficulty * 10, 1);
            }

            float waveTimeDec = 0.4f;

            state.rules.waveSpacing = Mathf.lerp(60 * 65 * 2, 60f * 60f * 1f, Math.max(difficulty - waveTimeDec, 0f));
            state.rules.waves = true;
            state.rules.env = sector.planet.defaultEnv;
            state.rules.enemyCoreBuildRadius = 600f;

            //spawn air only when spawn is blocked
            state.rules.spawns = Waves.generate(difficulty, new Rand(sector.id), state.rules.attackMode, state.rules.attackMode && spawner.countGroundSpawns() == 0, naval);
        }

        @Override
        public void postGenerate(Tiles tiles){
            if(sector.hasEnemyBase()){
                basegen.postGenerate();

                //spawn air enemies
                if(spawner.countGroundSpawns() == 0){
                    state.rules.spawns = Waves.generate(sector.threat, new Rand(sector.id), state.rules.attackMode, true, false);
                }
            }
        }
    }
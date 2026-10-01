package rout.world.blocks.defense;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.EnumSet;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.BlockFlag;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import rout.world.draw.RoutFx;

import static mindustry.Vars.*;

/** A single-target mender. It charges a reload, then heals a flat amount of the
 *  nearest damaged allied building in range. The heal is a shared RoutFx effect: a
 *  bunch of router blocks arc toward the target, slowly spinning, and shrink as they
 *  hit. */
public class RouterMender extends Block {
    /** Heal range in world units. */
    public float range = 110f;
    /** Ticks between heals. */
    public float reload = 60f;
    /** Flat health restored per heal. */
    public float healAmount = 20f;
    /** Ticks the packet bunch takes to fly to the target (matches RoutFx.routerMend). */
    public float packetTime = 40f;

    public RouterMender(String name){
        super(name);
        update = true;
        sync = true;
        solid = true;
        destructible = true;
        flags = EnumSet.of(BlockFlag.blockRepair);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.range, range / tilesize, StatUnit.blocks);
        stats.add(Stat.reload, reload / 60f, StatUnit.seconds);
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("reload", (RouterMenderBuild e) -> new Bar(
            () -> Core.bundle.get("bar.reload"),
            () -> Pal.heal,
            () -> Mathf.clamp(e.reloadCounter / reload)
        ));
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, Pal.heal);
    }

    public class RouterMenderBuild extends Building {
        public float reloadCounter;
        private boolean flipCurve;
        private float healTimer;
        private @Nullable Building pendingTarget;

        @Override
        public void updateTile(){
            //apply the heal when the packets actually arrive at the target.
            if(healTimer > 0f){
                healTimer -= delta();
                if(healTimer <= 0f){
                    if(pendingTarget != null && !pendingTarget.dead()){
                        pendingTarget.heal(healAmount);
                        pendingTarget.recentlyHealed();
                        Fx.heal.at(pendingTarget.x, pendingTarget.y);
                    }
                    pendingTarget = null;
                }
            }

            //charge the reload while it has efficiency.
            if(efficiency <= 0.0001f) return;
            reloadCounter += delta();
            if(reloadCounter < reload) return;

            //single target: the nearest damaged allied building in range.
            Building t = Units.findAllyTile(team, x, y, range, b -> b.damaged() && !b.isHealSuppressed());
            if(t == null) return;

            reloadCounter = 0f;

            //launch the packets; the heal is applied later, when they hit.
            flipCurve = !flipCurve;
            RoutFx.routerMend.at(x, y, flipCurve ? 180f : 0f, Pal.heal, new Vec2(t.x, t.y));
            pendingTarget = t;
            healTimer = packetTime;
        }

        @Override
        public void drawSelect(){
            Drawf.dashCircle(x, y, range, Pal.heal);
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(reloadCounter);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                reloadCounter = read.f();
            }
        }
    }
}
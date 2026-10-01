package rout.world.entities;

import arc.graphics.Color;
import arc.math.geom.Vec2;
import arc.struct.ObjectMap;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.Puddles;
import mindustry.entities.bullet.PointBulletType;
import mindustry.gen.Bullet;
import mindustry.gen.Hitboxc;
import mindustry.gen.Unit;
import mindustry.type.Liquid;
import mindustry.world.Tile;
import rout.world.draw.RoutFx;

import static mindustry.Vars.*;

/** An instant-hit PointBulletType that fires a spiraling cone of router blocks
 *  (throttled by {@link #coneInterval}) converging on the target, and leaves a big
 *  puddle of liquid router where it kills. */
public class RoutPointBulletType extends PointBulletType {
    /** Liquid deposited as a big puddle when the bullet kills a unit. */
    public Liquid killLiquid;
    /** Puddle amount (capped at the max puddle size, 70). */
    public float killLiquidAmount = 70f;
    /** Effect spawned at the kill, rotated to the bullet's direction, with the killed
     *  unit's hitSize passed as data. */
    public Effect killEffect;
    /** Minimum ticks between cone spawns (throttles continuous fire). */
    public float coneInterval = 8f;
    /** Ticks between shootEffect spawns at the muzzle. */
    public float shootEffectInterval = 8f;
    /** Ticks between smokeEffect spawns at the muzzle. */
    public float smokeEffectInterval = 10f;
    /** Ticks between damage/hit intervals — the hit (and its hitEffect) only lands when
     *  this elapses, so a continuous laser doesn't damage every frame. */
    public float damageInterval = 5f;

    private static final ObjectMap<Object, Float> lastCone = new ObjectMap<>();
    private static final ObjectMap<Object, Float> lastDamage = new ObjectMap<>();
    private static final ObjectMap<Object, Float> lastShoot = new ObjectMap<>();
    private static final ObjectMap<Object, Float> lastSmoke = new ObjectMap<>();

    public RoutPointBulletType(){
        //PointBulletType spawns trailEffect at points along the beam — that must stay
        //off for a point laser, or rotating the turret fans missile-trail effects out.
        trailEffect = Fx.none;
    }

    @Override
    public void init(Bullet b){
        float px = b.x + b.lifetime * b.vel.x, py = b.y + b.lifetime * b.vel.y;
        //throttle per owner (the firing turret) so multiple beams don't suppress each
        //other's cones, effects, or damage intervals.
        Object owner = b.owner;

        if(owner == null || Time.time - lastCone.get(owner, -1000f) >= coneInterval){
            if(owner != null) lastCone.put(owner, Time.time);
            RoutFx.routerPointCone.at(b.x, b.y, 0f, Color.white, new Vec2(px, py));
        }

        //spawn the muzzle shoot/smoke effects on their own intervals.
        if(owner == null || Time.time - lastShoot.get(owner, -1000f) >= shootEffectInterval){
            if(owner != null) lastShoot.put(owner, Time.time);
            shootEffect.at(b.x, b.y, b.rotation());
        }
        if(owner == null || Time.time - lastSmoke.get(owner, -1000f) >= smokeEffectInterval){
            if(owner != null) lastSmoke.put(owner, Time.time);
            smokeEffect.at(b.x, b.y, b.rotation());
        }

        //the hit (damage + hitEffect) is gated to the damage interval.
        boolean hit = owner == null || Time.time - lastDamage.get(owner, -1000f) >= damageInterval;
        if(hit){
            if(owner != null) lastDamage.put(owner, Time.time);
            super.init(b);
        }else{
            //within the interval: move to the endpoint and vanish without dealing
            //damage. Mark it as hit so remove() doesn't fire despawnEffect every tick.
            b.time = b.lifetime;
            b.set(px, py);
            b.hit = true;
            b.remove();
            b.vel.setZero();
        }
    }

    @Override
    public void hitEntity(Bullet b, Hitboxc entity, float health){
        boolean wasDead = entity instanceof Unit u && u.dead;
        super.hitEntity(b, entity, health);
        if(!wasDead && entity instanceof Unit unit && unit.dead){
            if(killLiquid != null){
                Tile tile = world.tileWorld(unit.x(), unit.y());
                if(tile != null){
                    Puddles.deposit(tile, killLiquid, killLiquidAmount);
                }
            }
            if(killEffect != null){
                killEffect.at(unit.x(), unit.y(), b.rotation(), Color.white, unit.hitSize);
            }
        }
    }
}
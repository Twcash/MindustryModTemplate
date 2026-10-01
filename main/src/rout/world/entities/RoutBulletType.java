package rout.world.entities;

import arc.graphics.Color;
import mindustry.entities.Effect;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Bullet;
import mindustry.gen.Hitboxc;
import mindustry.gen.Unit;

/** A basic bullet that spawns a kill effect (rotated to the bullet's direction) when
 *  it kills a unit, passing the killed unit's hitSize as the effect's data. */
public class RoutBulletType extends BasicBulletType {
    /** Effect spawned at the kill, rotated to match the bullet's direction. */
    public Effect killEffect;

    public RoutBulletType(float speed, float damage, String sprite){
        super(speed, damage, sprite);
    }

    public RoutBulletType(float speed, float damage){
        super(speed, damage);
    }

    @Override
    public void hitEntity(Bullet b, Hitboxc entity, float health){
        boolean wasDead = entity instanceof Unit u && u.dead;
        super.hitEntity(b, entity, health);
        //the vanilla kill path fires UnitBulletDestroyEvent on this exact transition.
        if(killEffect != null && !wasDead && entity instanceof Unit unit && unit.dead){
            killEffect.at(unit.x(), unit.y(), b.rotation(), Color.white, unit.hitSize);
        }
    }
}
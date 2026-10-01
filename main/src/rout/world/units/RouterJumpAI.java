package rout.world.units;

import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.ai.types.GroundAI;

/**
 * Pet-like legged AI that follows the nearest player's cursor,
 * and leaps at enemy units or buildings, dealing damage on impact.
 */
public class RouterJumpAI extends GroundAI{
    /** The unit stops approaching once this close to the cursor target. */
    public float stopRange = 3f;
    /** In normal mode the unit sticks within this range of the player. */
    public float followRange = 8f * 8f;
    /** Enemies within this range get jumped at. */
    public float jumpRange = 8f * 8f;
    /** Damage dealt to enemies the jump lands on. */
    public float jumpDamage = 35f;
    /** Knockback applied to jumped enemies. */
    public float jumpKnockback = 6f;
    /** How long a jump lasts, in ticks. */
    public float jumpDuration = 20f;
    /** Cooldown between jumps, in ticks. */
    public float jumpCooldown = 90f;
    /** Damage dealt while physically overlapping an enemy. */
    public float collisionDamage = 12f;
    /** How often overlapping enemies take collision damage. */
    public float collisionInterval = 20f;

    protected boolean jumping;
    protected float jumpTimer, jumpCooldownTimer, collisionTimer;
    protected Teamc jumpTarget;

    @Override
    public void updateMovement(){
        //during a jump only control the leap
        if(jumping){
            updateJump();
            return;
        }
        //settle back down to the ground after a jump so the unit isn't
        //left hovering and treated as an air unit
        if(unit.elevation() > 0f){
            unit.elevation(Mathf.approachDelta(unit.elevation(), 0f, 0.3f));
        }

        updateCollision();

        //direct control: if the player is piloting this unit, just use their movement
        if(unit == Vars.player.unit()){
            float mx = Vars.player.unit().vel().x, my = Vars.player.unit().vel().y;
            if(mx != 0f || my != 0f){
                unit.movePref(Tmp.v1.set(mx, my).setLength(prefSpeed()));
                unit.lookAt(Tmp.v1.angle());
            }else{
                unit.movePref(Tmp.v1.setZero());
            }
            return;
        }

        if(commandMode()){
            //only follow the cursor while in command mode
            Player player = findLocalPlayer();
            if(player != null && player.unit() != null){
                moveToward(player.unit().aimX(), player.unit().aimY());
            }else{
                unit.movePref(Tmp.v1.setZero());
            }
        }else{
            //otherwise just stick close to the nearest player
            Unit player = findNearestPlayerUnit();
            if(player != null && unit.dst(player) > followRange){
                moveToward(player.x, player.y);
            }else{
                unit.movePref(Tmp.v1.setZero());
            }
        }

        //leap at the nearest enemy unit or building once off cooldown,
        //but stay obedient while the player is directing us
        if(!commandMode()){
            Teamc target = Units.closestTarget(unit.team, unit.x, unit.y, jumpRange, u -> !u.dead(), b -> !b.dead());
            if(target != null && jumpCooldownTimer <= 0f){
                jumping = true;
                jumpTimer = 0f;
                jumpTarget = target;
            }else{
                jumpCooldownTimer -= Time.delta;
            }
        }
    }

    protected void updateJump(){
        jumpTimer += Time.delta;

        float ang = unit.angleTo(jumpTarget);
        float speed = prefSpeed() * (1.5f + 1.5f * Mathf.clamp(jumpTimer / jumpDuration));
        unit.movePref(Tmp.v1.trns(ang, speed));
        unit.lookAt(ang);
        //hop up during the leap, settle back down on landing
        unit.elevation(Mathf.approachDelta(unit.elevation(), 0.5f, 0.04f));

        //land on arrival or when the jump times out
        if(unit.within(jumpTarget, unit.hitSize + 10f) || jumpTimer >= jumpDuration){
            landDamage();
            jumping = false;
            jumpCooldownTimer = jumpCooldown;
            unit.elevation(Mathf.approachDelta(unit.elevation(), 0f, 0.2f));
        }
    }

    protected void landDamage(){
        Units.nearby(null, unit.x, unit.y, unit.hitSize + 12f, other -> {
            if(other != unit && other.team != unit.team && !other.dead()){
                other.damage(jumpDamage);
                other.vel().add(Tmp.v2.set(other.x - unit.x, other.y - unit.y).setLength(jumpKnockback));
            }
        });
        //also slam into buildings
        Units.nearbyBuildings(unit.x, unit.y, unit.hitSize + 12f, b -> {
            if(b.team != unit.team && !b.dead()){
                b.damage(jumpDamage);
            }
        });
    }

    protected void updateCollision(){
        collisionTimer += Time.delta;
        if(collisionTimer < collisionInterval) return;
        collisionTimer = 0f;

        Units.nearby(null, unit.x, unit.y, unit.hitSize + 6f, other -> {
            if(other != unit && other.team != unit.team && !other.dead()
            && unit.within(other, unit.hitSize + other.hitSize())){
                other.damage(collisionDamage);
            }
        });
        Units.nearbyBuildings(unit.x, unit.y, unit.hitSize + 10f, b -> {
            if(b.team != unit.team && !b.dead() && unit.within(b, unit.hitSize + b.block.size * 8f)){
                b.damage(collisionDamage);
            }
        });
    }

    protected void moveToward(float x, float y){
        if(unit.within(x, y, stopRange)){
            unit.movePref(Tmp.v1.setZero());
            return;
        }
        float ang = unit.angleTo(x, y);
        unit.movePref(Tmp.v1.trns(ang, prefSpeed()));
        unit.lookAt(ang);
    }

    protected Player findNearestPlayer(){
        Player best = null;
        float bestDist = Float.MAX_VALUE;
        for(Player p : Groups.player){
            if(p.unit() == null) continue;
            float d = unit.dst(p.unit());
            if(d < bestDist){
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    /** The local player, if they're on our team. */
    protected Player findLocalPlayer(){
        if(Vars.player != null && Vars.player.team() == unit.team && Vars.player.unit() != null){
            return Vars.player;
        }
        return findNearestPlayer();
    }

    protected Unit findNearestPlayerUnit(){
        Player best = findNearestPlayer();
        return best == null ? null : best.unit();
    }

    /** Whether the local player is in command (RTS) mode. */
    protected boolean commandMode(){
        return Vars.control != null && Vars.control.input != null && Vars.control.input.commandMode;
    }
}
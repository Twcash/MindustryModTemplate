package rout.world.blocks.environment;

import arc.math.Mathf;
import arc.util.Time;
import mindustry.entities.Puddles;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.Puddle;
import mindustry.type.Liquid;
import mindustry.type.UnitType;

import static mindustry.Vars.state;

/**
 * A liquid that births units out of its puddles on the ground.
 * Spawned units use the default team unless {@link #spawnTeam} is set.
 */
public class RouterLiquid extends Liquid{
    /** The unit spawned from puddles of this liquid. */
    public UnitType spawnUnit;
    /** Average ticks between spawns for a full puddle. */
    public float spawnInterval = 240f;
    /** Minimum puddle fraction of {@link #maxLiquid} needed to spawn. */
    public float minAmount = 0.25f;
    /** Team of spawned units. Null spawns on the default team. */
    public Team spawnTeam;
    /** Spawning stops once this many spawned units are within this range of the puddle. */
    public float maxNearbyRange = 12f * 8f;
    public int maxNearby = 40;

    public RouterLiquid(String name, arc.graphics.Color color){
        super(name, color);
    }

    @Override
    public void update(Puddle puddle){
        if(spawnUnit == null) return;

        float fraction = Mathf.clamp(puddle.amount / Puddles.maxLiquid);
        if(fraction < minAmount) return;

        //spawn at roughly spawnInterval ticks per puddle, faster for bigger puddles
        if(!Mathf.chance((float)Time.delta / spawnInterval * fraction)) return;

        Team team = spawnTeam != null ? spawnTeam : state.rules.defaultTeam;
        int[] count = {0};
        Units.nearby(team, puddle.x, puddle.y, maxNearbyRange, u -> {
            if(u.type == spawnUnit) count[0]++;
        });
        if(count[0] >= maxNearby) return;

        spawnUnit.spawn(team, puddle.x, puddle.y);
    }
}
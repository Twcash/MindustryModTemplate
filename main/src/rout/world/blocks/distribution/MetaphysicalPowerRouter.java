package rout.world.blocks.distribution;

import mindustry.gen.Building;

/**
 * Metaphysical router specialised for power distribution.
 * Transfers power to/from blocks at offset positions in the 4 cardinal directions.
 */
public class MetaphysicalPowerRouter extends MetaphysicalRouter {

    /** Power transfer rate per tick per direction (fraction of buffer). */
    public float transferRate = 0.05f;

    public MetaphysicalPowerRouter(String name) {
        super(name);
        hasItems = false;
        acceptsItems = false;
        hasPower = true;
        consumesPower = false;
        outputsPower = true;
        connectedPower = true;
        conductivePower = true;
    }

    public class MetaphysicalPowerRouterBuild extends MetaphysicalRouterBuild {
        @Override
        public void updateTile() {
            updateRoles();
            distributePower();
        }

        protected void distributePower() {
            float rate = transferRate * delta();

            for (int n = 0; n < 4; n++) {
                int i = (nextDir + n) % 4;
                Building other = reachBuild(this, i);
                if (other == null || other == this || !other.block.hasPower) continue;

                // push — skip if this direction is a pull-only source
                if (role[i] != 1 && power.status > 0 && other.power.status < 1f) {
                    float amount = Math.min(rate, power.status);
                    amount = Math.min(amount, 1f - other.power.status);
                    if (amount > 0) {
                        power.status -= amount;
                        other.power.status += amount;
                        role[i] = 2;
                        roleTimer[i] = roleDuration;
                        nextDir = (i + 1) % 4;
                    }
                }

                // pull — skip if this direction is a push-only target
                if (role[i] != 2 && other.power.status > 0 && power.status < 1f) {
                    float amount = Math.min(rate, other.power.status);
                    amount = Math.min(amount, 1f - power.status);
                    if (amount > 0) {
                        other.power.status -= amount;
                        power.status += amount;
                        role[i] = 1;
                        roleTimer[i] = roleDuration;
                        nextDir = (i + 1) % 4;
                    }
                }
            }
        }
    }
}

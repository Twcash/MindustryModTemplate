package rout.world.blocks.distribution;

import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.Liquid;

/**
 * Metaphysical router specialised for liquid distribution.
 * Transfers liquids to/from blocks at offset positions in the 4 cardinal directions.
 */
public class MetaphysicalLiquidRouter extends MetaphysicalRouter {

    public MetaphysicalLiquidRouter(String name) {
        super(name);
        hasItems = false;
        acceptsItems = false;
        hasLiquids = true;
        liquidCapacity = 20f;
    }

    public class MetaphysicalLiquidRouterBuild extends MetaphysicalRouterBuild {
        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            return liquids.get(liquid) < liquidCapacity;
        }

        @Override
        public void updateTile() {
            updateRoles();
            distributeLiquids();
        }

        protected void distributeLiquids() {
            for (int n = 0; n < 4; n++) {
                int i = (nextDir + n) % 4;
                Building other = reachBuild(this, i);
                if (other == null || other == this || !other.block.hasLiquids) continue;

                float rate = liquidCapacity * delta();

                // push — skip if this direction is a pull-only source
                if (role[i] != 1) {
                    var current = liquids.current();
                    if (current != null && liquids.get(current) > 0) {
                        float space = other.block.liquidCapacity - other.liquids.get(current);
                        float amount = Math.min(Math.min(liquids.get(current), rate), Math.max(space, 0f));
                        if (amount > 0) {
                            other.liquids.add(current, amount);
                            liquids.remove(current, amount);
                            role[i] = 2;
                            roleTimer[i] = roleDuration;
                            nextDir = (i + 1) % 4;
                        }
                    }
                }

                // pull — skip if this direction is a push-only target,
                // and never steal inputs the destination consumes.
                if (role[i] != 2) {
                    for (int j = 0; j < Vars.content.liquids().size; j++) {
                        Liquid liquid = Vars.content.liquid(j);
                        if (other.block.liquidFilter != null && liquid.id < other.block.liquidFilter.length && other.block.liquidFilter[liquid.id]) continue;
                        float space = liquidCapacity - liquids.get(liquid);
                        if (other.liquids.get(liquid) > 0 && space > 0) {
                            float amount = Math.min(Math.min(other.liquids.get(liquid), rate), space);
                            if (amount > 0) {
                                other.liquids.remove(liquid, amount);
                                liquids.add(liquid, amount);
                                role[i] = 1;
                                roleTimer[i] = roleDuration;
                                nextDir = (i + 1) % 4;
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}

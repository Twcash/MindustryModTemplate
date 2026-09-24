package rout.world.blocks.distribution;

import mindustry.gen.Building;
import mindustry.type.Item;

/**
 * Metaphysical router specialised for item distribution.
 * Transfers items to/from blocks at offset positions in the 4 cardinal directions.
 */
public class MetaphysicalItemRouter extends MetaphysicalRouter {

    public MetaphysicalItemRouter(String name) {
        super(name);
    }

    public class MetaphysicalItemRouterBuild extends MetaphysicalRouterBuild {
        @Override
        public boolean acceptItem(Building source, Item item) {
            return items.get(item) < itemCapacity;
        }

        @Override
        public int getMaximumAccepted(Item item) {
            return itemCapacity;
        }
    }
}

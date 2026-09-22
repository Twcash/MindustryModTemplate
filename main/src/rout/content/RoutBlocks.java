package rout.content;

import arc.func.Cons;
import mindustry.content.UnitTypes;
import mindustry.ctype.UnlockableContent;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.draw.DrawDefault;
import mindustry.world.draw.DrawMulti;
import mindustry.world.meta.BuildVisibility;
import rout.world.blocks.bases.RouterBlock;
import rout.world.blocks.core.RouterCore;
import rout.world.blocks.distribution.RouterRouter;
import rout.world.blocks.environment.RouterFloor;
import rout.world.blocks.production.RoutDrill;
import rout.world.draw.drawers.DrawHammer;

import static mindustry.type.ItemStack.with;

public class RoutBlocks {

    //environmentBlocks
    public static RouterFloor routerFloor;
    //Player blocks

    public static Block
            //Core blocks
            routerCore,
    //defenseBlocks
    routerWall,
    //distribution
    router2,
    //production
    routerDrill;

    public static <T extends UnlockableContent> void overwrite(UnlockableContent target, Cons<T> setter) {
        setter.get((T) target);
    }

    public static void loadContent(){
        routerFloor = new RouterFloor("router-floor", 4){{
            itemDrop = RoutItems.routerDust;
        }};
        routerCore = new RouterCore("core-route"){{
            requirements(Category.effect, with(RoutItems.routerFragment, 120));
            buildVisibility = BuildVisibility.shown;
            unitType = UnitTypes.alpha;
            isFirstTier = true;
            size = 2;
            floor = routerFloor;
        }};
        routerWall = new RouterBlock("router-wall"){{
            requirements(Category.defense, with(RoutItems.routerFragment, 8));
            size = 1;
            scaledHealth = 250;
            armor = 2;
        }};
        router2 = new RouterRouter("router2"){{
            requirements(Category.defense, with(RoutItems.routerFragment, 2));
        }};
        routerDrill = new RoutDrill("router-drill"){{
            requirements(Category.production, with(RoutItems.routerDust, 10));
            drillTime = 240;
            itemCapacity = 15;
            mineAmount = 4;
            size = 1;
            tier = 1;
            drawer = new DrawMulti(new DrawDefault(), new DrawHammer(){{
                uprot = 180;
                pow = 4;
            }});
        }};
    }
}

package rout.content;

import arc.graphics.Color;
import mindustry.type.Item;
import mindustry.type.Liquid;
import rout.world.blocks.environment.RouterLiquid;

public class RoutItems {
    public static Item routerFragment, routerDust, routerium, distributiveCore, clearRouter, yellowRouterium;

    public static Liquid liquidRouter, routerGoo;
    public static void loadContent(){
        routerDust = new Item("router-dust", Color.valueOf("b8bccc"));
        routerFragment = new Item("router-fragment", Color.valueOf("b3bce7")){{
            hardness = 1;
        }};
        routerium = new Item("routerium", Color.valueOf("989aa4")){{
            cost = 1.2f;
        }};
        distributiveCore = new Item("distributive-core", Color.valueOf("a3a9cb")){{
            cost = 3;
            hardness = 1;
        }};
        clearRouter = new Item("clear-router", Color.valueOf("ffffff")){{
            cost = 2;
        }};
        yellowRouterium = new Item("yellow-routerium"){{
            cost = 2.2f;
            charge = 2;
        }};
        liquidRouter = new RouterLiquid("liquid-router", Color.valueOf("989aa4")){{
            viscosity = 0.9f;
            spawnUnit = RoutUnits.roustalker;
            spawnInterval = 360;
        }};
    }
}

package rout.content;

import arc.graphics.Color;
import mindustry.type.Item;
import mindustry.type.Liquid;

public class RoutItems {
    public static Item routerFragment, routerDust, routerium, clearRouter, yellowRouterium;

    public static Liquid liquidRouter;
    public static void loadContent(){
        routerDust = new Item("router-dust", Color.valueOf("b8bccc"));
        routerFragment = new Item("router-fragment", Color.valueOf("b3bce7")){{
            hardness = 1;
        }};
        routerium = new Item("routerium", Color.valueOf("989aa4")){{
            cost = 1.2f;
        }};
        clearRouter = new Item("clear-router", Color.valueOf("ffffff")){{
            cost = 2;
        }};
        yellowRouterium = new Item("yellow-routerium"){{
            cost = 2.2f;
            charge = 2;
        }};
        liquidRouter = new Liquid("liquid-router", Color.valueOf("989aa4")){{
            viscosity = 0.9f;
        }};
    }
}

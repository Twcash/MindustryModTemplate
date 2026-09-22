package rout.content;

import arc.graphics.Color;
import mindustry.type.Item;

public class RoutItems {
    public static Item routerFragment, routerDust;
    public static void loadContent(){
        routerDust = new Item("router-dust", Color.valueOf("b8bccc"));
        routerFragment = new Item("router-fragment", Color.valueOf("b3bce7"));

    }
}

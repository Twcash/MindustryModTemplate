package rout.content;

import mindustry.game.Schematic;
import mindustry.game.Schematics;

public class RoutLoadouts {
    public static Schematic routerCore;
    public static void loadContent(){
        routerCore = Schematics.readBase64("bXNjaAF4nCXLywmAMBBF0WfwA7qwEhuwBysQF2McMGASyYwrsXcJuetzYWAa9DYG5aAL3TDvh/ainS+BWTfUgTxnkXhO8VHGcLDY5G51MQDoRJm8O9CLPdmTOisVxmynfE3lAiqUfmzdH/M=");
    }
}

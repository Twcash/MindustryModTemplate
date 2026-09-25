package rout.content;

import arc.struct.Seq;
import mindustry.game.Schematic;
import mindustry.game.Schematics;
import mindustry.world.blocks.storage.CoreBlock;

import static mindustry.Vars.schematics;

public class RoutLoadouts {
    public static Schematic routerCore;
    public static void loadContent(){
        routerCore = Schematics.readBase64("bXNjaAF4nCXLywmAMBBF0WfwA7qwEhuwBysQF2McMGASyYwrsXcJuetzYWAa9DYG5aAL3TDvh/ainS+BWTfUgTxnkXhO8VHGcLDY5G51MQDoRJm8O9CLPdmTOisVxmynfE3lAiqUfmzdH/M=");
    }

    /** Registers the launch loadout for the router core. Must run after Schematics.load(),
     *  which wipes loadouts during content load. Without this the LaunchLoadoutDialog
     *  falls back to the coreShard loadout, which requires copper — unbuildable here. */
    public static void registerLoadouts(){
        schematics.getLoadouts().get((CoreBlock)RoutBlocks.coreRouter, Seq::new).add(routerCore);
    }
}

package rout;

import arc.*;
import rout.content.RoutBlocks;
import rout.content.RoutItems;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;
import rout.annotations.Annotations.*;
import rout.gen.*;

import static mindustry.Vars.*;

/**
 * The mod's main mod class. Contains static references to other modules.
 * @author Avant Team
 */
@LoadRegs("error")// Need this temporarily, so the class gets generated.
@EnsureLoad
public class Rout extends Mod{
    public static boolean tools = false;

    public Rout(){
        this(false);
    }

    public Rout(boolean tools){

        Rout.tools = tools;

        if(!headless){
            Events.on(FileTreeInitEvent.class, e -> Core.app.post(routSounds::load));

        }


        Events.on(ContentInitEvent.class, e -> {

            if(!headless){
                Regions.load();
                content.each(content -> {
                    if(isTemplate(content) && content instanceof MappableContent mContent){
                        routContentRegionRegistry.load(mContent);
                    }
                });
            }
        });
    }

    @Override
    public void init(){
    }

    @Override
    public void loadContent(){
        RoutItems.loadContent();
        RoutBlocks.loadContent();
        routEntityMapping.init();
    }

    public static boolean isTemplate(Content content){
        return content.minfo.mod != null && content.minfo.mod.name.equals("rout");
    }
}
package template.tools.proc;

import arc.graphics.g2d.*;
import arc.util.*;

import template.*;
import template.tools.GenAtlas.*;
import template.tools.*;

import java.util.concurrent.*;

import static mindustry.Vars.*;
import static template.tools.Tools.*;

/**
 * Mirrors the logic from mindustry.tools.Generators#generate("item-icons") for items, liquids and status effects.
 * @author stabu_
 */
public class ItemProcessor implements Processor{
    @Override
    public void process(ExecutorService exec){
        // Items
        content.items().each(Template::isTemplate, item -> submit(exec, item.name + "-ui", () -> {
            GenRegion baseRegion = atlas.find(item.name);
            if(!baseRegion.found()) return;

            String uiIconName = item.name + "-ui";
            if(atlas.has(uiIconName)) return;

            GenRegion uiRegion = new GenRegion(uiIconName, baseRegion.pixmap().copy());
            uiRegion.relativePath = "ui";
            uiRegion.save(true);
        }));

        // Liquids
        content.liquids().each(Template::isTemplate, liquid -> submit(exec, liquid.name + "-ui", () -> {
            GenRegion baseRegion = atlas.find(liquid.name);
            if(!baseRegion.found()) return;

            String uiIconName = liquid.name + "-ui";
            if(atlas.has(uiIconName)) return;

            GenRegion uiRegion = new GenRegion(uiIconName, baseRegion.pixmap().copy());
            uiRegion.relativePath = "ui";
            uiRegion.save(true);
        }));

        // Status Effects
        content.statusEffects().each(Template::isTemplate, effect -> submit(exec, effect.name + "-ui", () -> {
            GenRegion baseRegion = atlas.find(effect.name);
            if(!baseRegion.found()){
                Log.warn("Base region not found for status effect '@'. Skipping.", effect.name);
                return;
            }

            String uiIconName = effect.name + "-ui";
            if(atlas.has(uiIconName)) return;

            GenRegion uiRegion = new GenRegion(uiIconName, baseRegion.pixmap().copy());
            uiRegion.relativePath = "ui";
            uiRegion.save(true);
        }));
    }
}

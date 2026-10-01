package rout.world.units;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.UnitType;

/** Unit type that draws a line to each chained neighbour, beneath the bodies. */
public class ChainUnitType extends UnitType{
    public Color linkColor = Pal.accent;
    public float linkAlpha = 0.5f;
    public float linkWidth = 1.5f;

    public ChainUnitType(String name){
        super(name);
    }

    @Override
    public void draw(Unit unit){
        drawLink(unit);
        super.draw(unit);
    }

    protected void drawLink(Unit unit){
        if(!(unit.controller() instanceof ChainAI ai)) return;
        Unit parent = ai.parent;
        if(parent == null || !parent.isAdded() || parent.dead()) return;

        Draw.z(Layer.groundUnit - 1f);
        Draw.color(linkColor, linkAlpha);
        Lines.stroke(linkWidth);
        Lines.line(unit.x, unit.y, parent.x, parent.y);
        Draw.reset();
    }
}
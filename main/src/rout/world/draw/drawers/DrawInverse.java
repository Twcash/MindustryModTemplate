package rout.world.draw.drawers;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Interp;
import arc.util.Eachable;
import arc.util.Nullable;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawRegion;

public class DrawInverse extends DrawRegion {
    public String suffix = "-top";
    public @Nullable String name;
    public float x, y = 0;
    public TextureRegion top;
    public float max = 0.25f;
    public Interp interp = Interp.linear;

    @Override
    public void load(Block block){
        super.load(block);
        top = Core.atlas.find(name != null ? name : block.name + suffix);
    }
    @Override
    public void draw(Building build){
        float progress = interp.apply(build.progress());
        Draw.alpha(progress);
        Draw.rect(top, build.x + x, build.y + y, 0);
        Draw.scl();
    }
    @Override
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{top};
    }
    @Override
    public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(top, plan.drawx()+ x, plan.drawy() + y, 0);
    }
}
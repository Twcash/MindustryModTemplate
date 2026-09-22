package rout.world.draw.drawers;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Interp;
import arc.math.Mathf;
import arc.util.Eachable;
import arc.util.Nullable;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawRegion;

//Omaloon ripoff
//Basically a hammer that raises up with the buildings 'craft' progress.
public class DrawHammer extends DrawRegion {
    public String suffix = "-hammer";
    public @Nullable String name;
    public float x, y = 0;
    public TextureRegion hammer;
    public float pow = 4;
    public float max = 1.2f;
    public float uprot = 0;
    public Interp interp = Interp.linear;
    public float shadowElevation = 2;

    @Override
    public void load(Block block){
        super.load(block);
        hammer = Core.atlas.find(name != null ? name : block.name + suffix);
    }
    @Override
    public void draw(Building build){
        float progress = interp.apply(build.progress());
        float scl = 1 * (1.2f*-(Mathf.pow(((2*(progress-1))),pow)+1));
        Draw.color(Pal.shadow);
        Drawf.spinSprite(hammer, build.x + x-(shadowElevation*progress), build.y + y-(shadowElevation*progress), uprot * progress);
        Draw.color();
        Draw.alpha(1);
        Draw.scl(scl);
        Drawf.spinSprite(hammer, build.x + x, build.y + y, uprot * progress);
    }
    @Override
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{hammer};
    }
    @Override
    public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
            Draw.rect(hammer, plan.drawx()+ x, plan.drawy() + y, 0);
    }
}

package rout.world.draw.drawers;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.util.Eachable;
import arc.util.Tmp;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.blocks.environment.StaticWall;
import mindustry.world.draw.DrawBlock;

import static mindustry.Vars.tilesize;

public class DrawCutoff extends DrawBlock {
    public TextureRegion region;
    public float layer = -1;
    public String suffix = "-reg";
    public DrawCutoff(String suffix){
    }
    @Override
    public void load(Block block){
        super.load(block);
        region = Core.atlas.find(block.name + suffix);
    }
    @Override
    public void draw(Building build){

        int crop = (region.width - tilesize*4) / 2;
        TextureRegion r = Tmp.tr1;
        r.set(region);
        float ox = 0;
        float oy = 0;
        for(int i = 0; i < 4; i++){
            if(build.tile.nearby(i) != null && !(build.tile.nearby(i).block().isAir()|| build.tile.nearby(i).block().underBullets)){

                if(i == 0){
                    r.setWidth(r.width - crop);
                    ox -= crop /2f;
                }else if(i == 1){
                    r.setY(r.getY() + crop);
                    oy -= crop /2f;
                }else if(i == 2){
                    r.setX(r.getX() + crop);
                    ox += crop /2f;
                }else{
                    r.setHeight(r.height - crop);
                    oy += crop /2f;
                }
            }
        }
        float z = Draw.z();
        if(layer > 0) Draw.z(layer);
        Draw.z(z);
        Draw.rect(r, build.x + ox * Draw.scl, build.y + oy * Draw.scl);
    }
    @Override
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{region};
    }
    @Override
    public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(region, plan.drawx(), plan.drawy(), 0);
    }
}

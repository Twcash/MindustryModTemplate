package rout.world.draw;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Vec2;
import arc.util.Tmp;
import mindustry.entities.Effect;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;

import static arc.graphics.g2d.Draw.color;
import static arc.math.Angles.randLenVectors;

public class RoutFx {
    public static final Rand rand = new Rand();
    public static final Vec2 v = new Vec2();

    public static final Effect routerSpread = new Effect(400f, 300f, b -> {
        float intensity = 3f;
        for(int i = 0; i < 2; i++){
            rand.setSeed(b.id*2 + i);
            float lenScl = rand.random(0.25f, .75f);
            int fi = i;
            b.scaled(b.lifetime * lenScl, e -> {
                color(Color.valueOf("4a4b53").lerp(Color.valueOf("6e7080"),e.fin()));
                randLenVectors(e.id + fi - 1, e.fin(Interp.pow10Out), (int)(5f * intensity), 8f * intensity, (x, y, in, out) -> {
                    float fout = e.fout(Interp.pow5Out) * rand.random(0.5f, 1f);
                    float rad = fout * ((2f + intensity) * 0.4f);

                    Fill.square(e.x + x, e.y + y, rad);
                });
            });
        }
    }).layer(Layer.debris),
    metaphysicalRouterSend = new Effect(30, 100, e -> {
        if(e.data instanceof Vec2 data){
            //lerp from spawn position to target over the effect's lifetime
            float px = Mathf.lerp(e.x, data.x, e.fin());
            float py = Mathf.lerp(e.y, data.y, e.fin());
            TextureRegion region = Core.atlas.find("rout-metaphysical-router-connection");
            Draw.alpha(e.fout() * 0.8f);
            Draw.rect(region, px, py, e.rotation);
            Draw.alpha(1f);
        }
    });
}

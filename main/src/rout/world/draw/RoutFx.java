package rout.world.draw;

import arc.Core;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Vec2;
import arc.util.Tmp;
import arc.util.Time;
import mindustry.entities.Effect;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import rout.content.RoutBlocks;

import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.stroke;
import static arc.math.Angles.*;
import static mindustry.Vars.tilesize;

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
            distributiveEnrich = new Effect(400f, 300f, b -> {
                float intensity = 3f;
                for(int i = 0; i < 2; i++){
                    rand.setSeed(b.id*2 + i);
                    float lenScl = rand.random(0.25f, .75f);
                    int fi = i;
                    b.scaled(b.lifetime * lenScl, e -> {
                        color(Color.valueOf("777ea2").lerp(Color.valueOf("a3a9cb"),e.fin()));
                        randLenVectors(e.id + fi - 1, e.fin(Interp.pow10Out), (int)(5f * intensity), 8f * intensity, (x, y, in, out) -> {
                            float fout = e.fout(Interp.pow5Out) * rand.random(0.5f, 1f);
                            float rad = fout * ((2f + intensity) * 0.4f);

                            Fill.square(e.x + x, e.y + y, rad);
                        });
                    });
                }
            }).layer(Layer.debris),
            distributivederich = new Effect(400f, 300f, b -> {
                float intensity = 3f;
                for(int i = 0; i < 2; i++){
                    rand.setSeed(b.id*2 + i);
                    float lenScl = rand.random(0.25f, .75f);
                    int fi = i;
                    b.scaled(b.lifetime * lenScl, e -> {
                        color(Color.valueOf("544c4c").lerp(Color.valueOf("473f3f"),e.fin()));
                        randLenVectors(e.id + fi - 1, e.fin(Interp.pow10Out), (int)(5f * intensity), 8f * intensity, (x, y, in, out) -> {
                            float fout = e.fout(Interp.pow5Out) * rand.random(0.5f, 1f);
                            float rad = fout * ((2f + intensity) * 0.4f);

                            Fill.circle(e.x + x, e.y + y, rad);
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
            alpha(e.fout() * 0.8f);
            Draw.rect(region, px, py, e.rotation);
            alpha(1f);
        }
    }),routerBlastColor = new Effect(10, e -> {
        color(e.color, Color.white, e.fin());

        e.scaled(7f, s -> {
            stroke(0.5f + s.fout());
            Lines.circle(e.x, e.y, s.fin() * 6f);
        });

        stroke(0.5f + e.fout());
        TextureRegion reg = Core.atlas.find("router");
        randLenVectors(e.id, 5, e.fin() * 17f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            Draw.scl(e.fout(Interp.pow2Out));
            alpha(e.fout(Interp.pow2Out));
            Draw.rect(reg, e.x + x, e.y + y, ang);
        });

        Drawf.light(e.x, e.y, 20f, e.color, 0.6f * e.fout());
    }),hitRouterSmall = new Effect(10, e -> {
        color(Pal.lightOrange, Color.white, e.fin());

        e.scaled(7f, s -> {
            stroke(0.3f + s.fout());
            Lines.circle(e.x, e.y, s.fin() * 3f);
        });

        stroke(0.5f + e.fout());
        TextureRegion reg = Core.atlas.find("router");
        randLenVectors(e.id, 5, e.fin() * 17f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            Draw.scl(e.fout(Interp.pow2Out) * 0.8f);
            alpha(e.fout(Interp.pow2Out));
            Draw.rect(reg, e.x + x, e.y + y, ang);
        });

        Drawf.light(e.x, e.y, 20f, e.color, 0.6f * e.fout());
    }),hitRouter = new Effect(10, e -> {
        color(Pal.lightOrange, Color.white, e.fin());

        e.scaled(7f, s -> {
            stroke(0.5f + s.fout());
            Lines.circle(e.x, e.y, s.fin() * 6f);
        });

        stroke(0.5f + e.fout());
        TextureRegion reg = Core.atlas.find("router");
        randLenVectors(e.id, 5, e.fin() * 17f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            Draw.scl(e.fout(Interp.pow2Out));
            alpha(e.fout(Interp.pow2Out));
            Draw.rect(reg, e.x + x, e.y + y, ang);
        });

        Drawf.light(e.x, e.y, 20f, e.color, 0.6f * e.fout());
    }),shootRouterSmall = new Effect(15, e -> {
        color(Pal.lightOrange, Color.white, e.fin());

        stroke(0.5f + e.fout());
        TextureRegion reg = Core.atlas.find("router");
        rand.setSeed(e.id);
        for(int i = 0; i < 6; i++){
            float rot = e.rotation + rand.range(22f);
            v.trns(rot, rand.random(e.finpow() * 21f));
            Draw.scl(e.fout()*0.8f);
            Draw.rect(reg, e.x + v.x,e.y + v.y , rand.random(360f));
        }
        Drawf.light(e.x, e.y, 20f, e.color, 0.6f * e.fout());
    }),killRouter = new Effect(600, e -> {
        color(Color.white, Color.white, e.fin());
        if(e.data instanceof Float data){
            TextureRegion reg = Core.atlas.find("router");
            randLenVectors(e.id, 12, Interp.pow10Out.apply(e.fin()) *( 24f*data/5f), e.rotation, 60, (x, y) -> {
                float ang = Mathf.angle(x, y);
                Draw.scl(e.fout(Interp.pow10Out) );
                Draw.rect(reg, e.x + x, e.y + y,  data*Interp.pow10Out.apply(e.fout()), data*Interp.pow10Out.apply(e.fout()),ang);
            });

            Drawf.light(e.x, e.y, 20f, e.color, 0.6f * e.fout());
        }
    }).layer(Layer.debris),casing1 = new Effect(30f, e -> {
        color(Pal.lightOrange, Color.lightGray, Pal.lightishGray, e.fin());
        alpha(e.fout(0.3f));
        float rot = Math.abs(e.rotation) + 90f;
        int i = -Mathf.sign(e.rotation);

        float len = (2f + e.finpow() * 6f) * i;
        float lr = rot + e.fin() * 30f * i;
        TextureRegion reg = Core.atlas.find("router");
        Draw.rect(reg,
                e.x + trnsx(lr, len) + Mathf.randomSeedRange(e.id + i + 7, 3f * e.fin()),
                e.y + trnsy(lr, len) + Mathf.randomSeedRange(e.id + i + 8, 3f * e.fin()),
                1f, 2f, rot + e.fin() * 50f * i
        );

    }).layer(Layer.bullet);

    // ---- router-block effects (shared by the mender and point-laser bullets) ----

    /** Router blocks arcing to a target and shrinking as they hit (target in e.data).
     *  Arc direction comes from e.rotation (flips every heal). */
    public static int mendPackets = 3;
    public static float mendGap = 0.05f, mendCurve = 22f, mendSpin = 1f;
    public static final Effect routerMend = new Effect(40f, 250f, b -> {
        if(!(b.data instanceof Vec2 target)) return;
        float sx = b.x, sy = b.y, tx = target.x, ty = target.y;
        float dx = tx - sx, dy = ty - sy;
        float len = Mathf.len(dx, dy);
        if(len < 0.001f) len = 0.001f;
        float dir = Mathf.cosDeg(b.rotation);
        float px = -dy / len, py = dx / len;

        TextureRegion reg = RoutBlocks.router2 == null ? null : RoutBlocks.router2.fullIcon;
        if(reg == null) return;

        float prog = Mathf.clamp(b.fin());
        float spin = b.time * mendSpin;
        for(int i = 0; i < mendPackets; i++){
            float p = Mathf.clamp(prog - i * mendGap);
            float baseX = Mathf.lerp(sx, tx, p), baseY = Mathf.lerp(sy, ty, p);
            float off = Mathf.sin(p * Mathf.PI) * mendCurve * dir;
            float cx = baseX + px * off, cy = baseY + py * off;
            float scl = Mathf.lerp(1f, 0.15f, Mathf.curve(p, 0.5f, 1f));
            color(Pal.heal);
            blend(Blending.additive);
            Draw.rect(reg, cx, cy, scl * tilesize, scl * tilesize, spin);
            blend(Blending.normal);
            Draw.reset();
        }
    }).layer(Layer.power - 1f);

    public static int coneRouters = 7;
    public static float coneRadius = 7f, coneTurns = 1.5f, coneSpin = 6f;
    public static final Effect routerPointCone = new Effect(50f, 300f, b -> {
        if(!(b.data instanceof Vec2 target)) return;
        float sx = b.x, sy = b.y, tx = target.x, ty = target.y;
        float dx = tx - sx, dy = ty - sy;
        float len = Mathf.len(dx, dy);
        if(len < 0.001f) len = 0.001f;
        float dirX = dx / len, dirY = dy / len, perpX = -dirY, perpY = dirX;

        TextureRegion reg = RoutBlocks.router2 == null ? null : RoutBlocks.router2.fullIcon;
        if(reg == null) return;

        Draw.alpha(b.fout());
        for(int i = 0; i < coneRouters; i++){
            float p = coneRouters <= 1 ? 0f : (float)i / (coneRouters - 1);
            float baseX = Mathf.lerp(sx, tx, p), baseY = Mathf.lerp(sy, ty, p);
            //wrap around the beam at a constant radius: half over, half under.
            float ang = p * coneTurns * 360f + b.time * coneSpin;
            float over = Mathf.cosDeg(ang);
            float along = Mathf.sinDeg(ang);
            float cx = baseX + (perpX * over + dirX * along * 0.5f) * coneRadius;
            float cy = baseY + (perpY * over + dirY * along * 0.5f) * coneRadius;
            float scl = Mathf.lerp(0.7f, 0.5f, p);
            Draw.rect(reg, cx, cy, scl * tilesize, scl * tilesize);
        }
        Draw.alpha(1f);
    }).layer(Layer.bullet + 1f);

    public static float laserWidth = 1f, laserOscScl = 2f, laserOscMag = 0.3f;

    /** Ring + router blobs when roustalkers merge into a chain or a Prime. */
    public static final Effect routerMerge = new Effect(35f, 80f, e -> {
        color(Pal.lightOrange, Color.white, e.fin());

        //bright core flash that swells then fades
        e.scaled(20f, s -> {
            blend(Blending.additive);
            Fill.circle(e.x, e.y, s.fin() * 9f * s.fout());
            blend(Blending.normal);
        });

        //expanding ring
        e.scaled(30f, s -> {
            stroke(0.5f + s.fout() * 1.5f);
            Lines.circle(e.x, e.y, Mathf.lerp(2f, 14f, s.fin()));
        });

        //router blobs converging into the center
        TextureRegion reg = Core.atlas.find("router");
        randLenVectors(e.id, 8, Mathf.lerp(20f, 0f, e.fin()), (x, y) -> {
            float ang = Mathf.angle(x, y);
            Draw.scl(Mathf.lerp(0.9f, 0.2f, e.fin()));
            alpha(e.fout(Interp.pow2Out));
            Draw.rect(reg, e.x + x, e.y + y, ang);
        });

        Drawf.light(e.x, e.y, 26f, e.color, 0.7f * e.fout());
    });
    public static void drawPointLaser(float x1, float y1, float x2, float y2, float alpha){
        TextureRegion laser = Core.atlas.find("rout-distroute-laser");
        TextureRegion laserEnd = Core.atlas.find("rout-distroute-laser-end");
        Draw.alpha(alpha);
        float scale = laserWidth * (1f - laserOscMag + Mathf.absin(Time.time, laserOscScl, laserOscMag));
        Drawf.laser(laser, laserEnd, x1, y1, x2, y2, scale);
        Draw.reset();
    }
}

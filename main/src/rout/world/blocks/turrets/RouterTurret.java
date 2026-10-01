package rout.world.blocks.turrets;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.math.Angles;
import arc.math.Mathf;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.Effect;
import mindustry.entities.Mover;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.PointBulletType;
import mindustry.entities.pattern.ShootPattern;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Posc;
import mindustry.gen.Teamc;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.ui.ReqImage;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.consumers.Consume;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;
import mindustry.world.meta.StatUnit;
import mindustry.world.meta.StatValues;
import rout.world.blocks.environment.RouterFloor;
import rout.world.draw.RoutFx;
import rout.world.entities.RoutPointLaserBulletType;

import static mindustry.Vars.*;
//This class can shoot *any* storable UnlockableContent
public class RouterTurret extends Turret {
    public static final Stat burstTime = new Stat("burstTime", StatCat.function);
    public static final Stat burstReload = new Stat("burstReload", StatCat.function);
    public ObjectMap<UnlockableContent, BulletType> ammoTypes = new ObjectMap<>();
    public ObjectMap<UnlockableContent, Integer> ammoUsage = new ObjectMap<>();
    public boolean continuous = false;
    public float continuousTime = -1f;
    public float fxInterval = 8f;

    public RouterTurret(String name){
        super(name);
        outlineIcon = true;
        hasItems = true;
        hasLiquids = true;
        acceptsPayload = true;
    }

    /** Initializes accepted ammo map. Format: [content1, bullet1, content2, bullet2...] */
    public void ammo(Object... objects){
        ammoTypes = ObjectMap.of(objects);
    }

    /** Amount of this ammo type consumed per shot. */
    public int usage(UnlockableContent content){
        return ammoUsage.get(content, ammoPerShot);
    }

    public void limitRange(){
        limitRange(1f);
    }

    /** Makes copies of all bullets and limits their range. */
    public void limitRange(float margin){
        for(var entry : ammoTypes.copy().entries()){
            entry.value.lifetime = (range + margin) / entry.value.speed;
        }
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(Stat.itemCapacity);
        stats.add(Stat.ammo, StatValues.ammo(ammoTypes));
        stats.add(Stat.ammoCapacity, maxAmmo, StatUnit.shots);
        if(continuous && continuousTime >= 0f){
            stats.add(burstTime, continuousTime / 60f, StatUnit.seconds);
            stats.add(burstReload, reload / 60f, StatUnit.seconds);
        }
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("ammo", (RouterTurretBuild e) -> new Bar(
            () -> Core.bundle.get("stat.ammo"),
            () -> Pal.ammo,
            () -> e.getAmmoFraction()
        ));
    }

    @Override
    public void init(){
        consume(new Consume(){
            @Override
            public void apply(Block block){
                block.hasItems = true;
                block.hasLiquids = true;
                block.acceptsPayload = true;
            }

            @Override
            public void build(Building build, Table table){
                Table cont = new Table();
                for(var entry : ammoTypes.entries()){
                    if(entry.key.unlockedNow()){
                        cont.add(new ReqImage(new Image(entry.key.uiIcon), () -> build instanceof RouterTurretBuild b && b.getAmmoContent() == entry.key)).pad(3);
                    }
                }
                table.add(cont);
            }

            @Override
            public float efficiency(Building build){
                //valid when the turret can shoot with any ammo type.
                return build instanceof RouterTurretBuild b && b.hasAmmo() ? 1f : 0f;
            }
        });
        super.init();
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        if(tile == null) return false;
        tile.getLinkedTilesAs(this, tempTiles);
        if(!tempTiles.contains(o -> (!o.floor().allowCorePlacement && !(o.floor() instanceof RouterFloor)) || o.block() instanceof CoreBlock)){
            return true;
        }
        return super.canPlaceOn(tile, team, rotation);
    }

    public class RouterTurretBuild extends TurretBuild {
        public float burstTimer;
        public boolean reloading;
        private float fxTimer;
        private boolean suppressFx;
        private float inactiveTimer;

        float ammoMultiplier(UnlockableContent content){
            BulletType type = ammoTypes.get(content);
            return type == null ? 1f : type.ammoMultiplier;
        }

        void addAmmo(UnlockableContent content, float amount){
            if(amount <= 0f || content == null || !ammoTypes.containsKey(content)) return;
            totalAmmo += amount;
            for(int i = 0; i < ammo.size; i++){
                ContentEntry entry = (ContentEntry)ammo.get(i);
                if(entry.content == content){
                    entry.amount += (int)amount;
                    ammo.swap(i, ammo.size - 1);
                    return;
                }
            }
            ammo.add(new ContentEntry(content, (int)amount));
        }

        // ---- items ----
        @Override
        public boolean acceptItem(Building source, Item item){
            return ammoTypes.containsKey(item) && totalAmmo + ammoMultiplier(item) <= maxAmmo;
        }

        @Override
        public void handleItem(Building source, Item item){
            addAmmo(item, ammoMultiplier(item));
        }

        @Override
        public int acceptStack(Item item, int amount, Teamc source){
            BulletType type = ammoTypes.get(item);
            if(type == null) return 0;
            return Math.min((int)((maxAmmo - totalAmmo) / Math.max(type.ammoMultiplier, 1)), amount);
        }

        @Override
        public void handleStack(Item item, int amount, Teamc source){
            for(int i = 0; i < amount; i++){
                handleItem(null, item);
            }
        }

        @Override
        public int removeStack(Item item, int amount){
            return 0;
        }

        // ---- liquids ----
        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            return ammoTypes.containsKey(liquid) && liquids.get(liquid) < liquidCapacity;
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount){
            float prev = liquids.get(liquid);
            liquids.add(liquid, amount);
            addAmmo(liquid, liquids.get(liquid) - prev);
        }

        // ---- payloads (blocks/units) ----
        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return payload != null && ammoTypes.containsKey(payload.content()) && totalAmmo + 1f <= maxAmmo;
        }

        @Override
        public void handlePayload(Building source, Payload payload){
            addAmmo(payload.content(), 1f);
        }

        @Override
        public boolean hasAmmo(){
            //find any entry with enough ammo and move it to the top.
            for(int i = ammo.size - 1; i >= 0; i--){
                ContentEntry entry = (ContentEntry)ammo.get(i);
                if(entry.amount >= usage(entry.content) || cheating()){
                    ammo.swap(i, ammo.size - 1);
                    return true;
                }
            }
            return false;
        }

        @Override
        public BulletType useAmmo(){
            if(cheating()) return peekAmmo();
            if(ammo.size == 0) return null;
            ContentEntry entry = (ContentEntry)ammo.peek();
            BulletType type = entry.type();
            int usage = usage(entry.content);

            if(entry.content instanceof Liquid liquid){
                liquids.remove(liquid, usage);
                entry.amount = (int)liquids.get(liquid);
            }else{
                entry.amount -= usage;
            }
            if(entry.amount <= 0) ammo.pop();
            totalAmmo = Math.max(totalAmmo - usage, 0);
            return type;
        }

        @Override
        public BulletType peekAmmo(){
            return ammo.size == 0 ? null : ammo.peek().type();
        }

        @Override
        public UnlockableContent getAmmoContent(){
            return ammo.size == 0 ? null : ((ContentEntry)ammo.peek()).content;
        }

        @Override
        public float getAmmoFraction(){
            return (float)totalAmmo / maxAmmo;
        }

        /** Point bullets are instant-hits, so aim at the target's current position
         *  instead of leading/predicting ahead of it. */
        @Override
        public void targetPosition(Posc pos){
            if(pos != null && peekAmmo() instanceof PointBulletType){
                targetPos.set(pos.x(), pos.y());
            }else{
                super.targetPosition(pos);
            }
        }

        /** Draws a continuous point-laser beam to the target while firing a laser
         *  bullet, so the beam stays on until the turret stops shooting. */
@Override
        public void draw(){
            super.draw();
            //only draw the beam while actually firing within the shootcone (wasShooting
            //is set only inside the turret's aim-cone gate), not just while a target
            //exists.
            if(wasShooting && !(continuous && reloading)){
                BulletType type = peekAmmo();
                if(type instanceof RoutPointLaserBulletType laser){
                    float mx = x + Angles.trnsx(rotation - 90, shootX, shootY);
                    float my = y + Angles.trnsy(rotation - 90, shootX, shootY);
                    //clamp the beam to the max hit range (targets can sit slightly
                    //outside it thanks to hitSize).
                    float len = Math.min(Mathf.dst(x, y, targetPos.x, targetPos.y), range());
                    float ang = Mathf.angle(targetPos.x - x, targetPos.y - y);
                    float ex = x + Angles.trnsx(ang, len);
                    float ey = y + Angles.trnsy(ang, len);
                    Draw.z(Layer.bullet);
                    RoutFx.drawPointLaser(mx, my, ex, ey, 1f);
                }
            }
        }

        @Override
        public void updateTile(){
            //keep totalAmmo accurate (liquid entries mirror the liquid tank).
            totalAmmo = 0;
            for(AmmoEntry entry : ammo){
                totalAmmo += entry.amount;
            }

            //continuous mode: if the turret sits inactive for a full reload, reset the
            //burst cooldown so its next burst is ready to fire immediately.
            if(continuous){
                if(!isShooting){
                    inactiveTimer += delta();
                    if(inactiveTimer >= reload){
                        inactiveTimer = 0f;
                        reloading = false;
                        burstTimer = 0f;
                    }
                }else{
                    inactiveTimer = 0f;
                }
            }

            super.updateTile();
        }

        /** Continuous-fire mode: shoots every tick during a burst, then stops and lets
         *  the reload (handleReload) charge before the next burst. continuousTime -1 =
         *  endless continuous fire. */
        @Override
        protected void updateShooting(){
            if(!continuous){
                super.updateShooting();
                return;
            }

            //reloading after a burst: wait for the reload to finish.
            if(reloading){
                if(reloadCounter >= reload){
                    reloadCounter = 0f;
                    reloading = false;
                }
                return;
            }

            //fire continuously.
            if(hasAmmo() && !charging() && shootWarmup >= minWarmup){
                //throttle the per-tick sound/effects so they don't spam/flashbang.
                fxTimer += delta();
                suppressFx = fxTimer < fxInterval;
                if(!suppressFx) fxTimer = 0f;
                shoot(peekAmmo());

                //optional burst cap: stop and reload for the next burst.
                if(continuousTime >= 0f){
                    burstTimer += delta();
                    if(burstTimer >= continuousTime){
                        burstTimer = 0f;
                        reloading = true;
                        reloadCounter = 0f;
                    }
                }
            }
        }

        /** Fires the bullet like vanilla, but in continuous mode the sound/effects are
         *  throttled to {@link #fxInterval} and the particle effects are played WITHOUT
         *  rotation (rotation on a per-tick muzzle flash piles up into a flashbang). */
        @Override
        protected void shoot(BulletType type){
            float
            bulletX = x + Angles.trnsx(rotation - 90, shootX, shootY),
            bulletY = y + Angles.trnsy(rotation - 90, shootX, shootY);

            //gate the charge effects on the same interval.
            if(!suppressFx && shoot.firstShotDelay > 0){
                chargeSound.at(bulletX, bulletY, Mathf.random(soundPitchMin, soundPitchMax));
                type.chargeEffect.at(bulletX, bulletY, continuous ? 0f : rotation);
            }

            ShootPattern pattern = type.shootPattern != null ? type.shootPattern : shoot;

            pattern.shoot(barrelCounter, (xOffset, yOffset, angle, delay, mover) -> {
                queuedBullets++;
                int barrel = barrelCounter;

                if(delay > 0f){
                    Time.run(delay, () -> {
                        int prev = barrelCounter;
                        barrelCounter = barrel;
                        bullet(type, xOffset, yOffset, angle, mover);
                        barrelCounter = prev;
                    });
                }else{
                    bullet(type, xOffset, yOffset, angle, mover);
                }
            }, () -> barrelCounter++);

            if(consumeAmmoOnce){
                useAmmo();
            }
        }

        /** Fires the bullet like vanilla, but in continuous mode the sound/effects are
         *  throttled to {@link #fxInterval} and the particle effects are played WITHOUT
         *  rotation (rotation on a per-tick muzzle flash piles up into a flashbang). */
        @Override
        protected void bullet(BulletType type, float xOffset, float yOffset, float angleOffset, Mover mover){
            queuedBullets--;
            if(dead || (!consumeAmmoOnce && !hasAmmo())) return;

            float
            xSpread = Mathf.range(xRand),
            bulletX = x + Angles.trnsx(rotation - 90, shootX + xOffset + xSpread, shootY + yOffset),
            bulletY = y + Angles.trnsy(rotation - 90, shootX + xOffset + xSpread, shootY + yOffset),
            shootAngle = rotation + angleOffset + Mathf.range(inaccuracy + type.inaccuracy);

            float baseLife = (1f - lifeRnd) + Mathf.random(lifeRnd) + extraLife,
                  lifeScl = type.scaleLife ? Mathf.clamp((baseLife + scaleLifetimeOffset) * Mathf.dst(bulletX, bulletY, targetPos.x, targetPos.y) / type.range, minRange() / type.range, range() / type.range) : baseLife;

            handleBullet(type.create(this, team, bulletX, bulletY, shootAngle, -1f, (1f - velocityRnd) + Mathf.random(velocityRnd) + extraVelocity, lifeScl, null, mover, targetPos.x, targetPos.y), xOffset, yOffset, shootAngle - rotation);

            if(!suppressFx){
                if(continuous){
                    //no rotation on the per-tick particle effects.
                    (shootEffect == null ? type.shootEffect : shootEffect).at(bulletX, bulletY, type.hitColor);
                    (smokeEffect == null ? type.smokeEffect : smokeEffect).at(bulletX, bulletY, type.hitColor);
                    ammoUseEffect.at(x - Angles.trnsx(rotation, ammoEjectBack), y - Angles.trnsy(rotation, ammoEjectBack));
                }else{
                    (shootEffect == null ? type.shootEffect : shootEffect).at(bulletX, bulletY, rotation + angleOffset, type.hitColor);
                    (smokeEffect == null ? type.smokeEffect : smokeEffect).at(bulletX, bulletY, rotation + angleOffset, type.hitColor);
                    ammoUseEffect.at(x - Angles.trnsx(rotation, ammoEjectBack), y - Angles.trnsy(rotation, ammoEjectBack), rotation * Mathf.sign(xOffset));
                }
                shootSound.at(bulletX, bulletY, Mathf.random(soundPitchMin, soundPitchMax), shootSoundVolume);
                if(shake > 0){
                    Effect.shake(shake, shake, this);
                }
            }

            curRecoil = 1f;
            if(recoils > 0){
                curRecoils[barrelCounter % recoils] = 1f;
            }
            heat = 1f;
            totalShots++;
            if(!consumeAmmoOnce){
                useAmmo();
            }
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(ammo.size);
            for(AmmoEntry entry : ammo){
                ContentEntry e = (ContentEntry)entry;
                write.b(e.content.getContentType().ordinal());
                write.s(e.content.id);
                write.s((short)e.amount);
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            ammo.clear();
            totalAmmo = 0;
            int amount = read.ub();
            for(int i = 0; i < amount; i++){
                byte type = read.b();
                short cid = read.s();
                short amt = read.s();
                Content c = content.getByID(ContentType.all[type], cid);
                if(c instanceof UnlockableContent u && ammoTypes.containsKey(u)){
                    ammo.add(new ContentEntry(u, amt));
                    totalAmmo += amt;
                }
            }
        }
    }

    public class ContentEntry extends AmmoEntry {
        public UnlockableContent content;

        ContentEntry(UnlockableContent content, int amount){
            this.content = content;
            this.amount = amount;
        }

        @Override
        public BulletType type(){
            return ammoTypes.get(content);
        }
    }
}
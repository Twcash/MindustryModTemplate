package rout.content;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.content.Fx;
import mindustry.gen.LegsUnit;
import mindustry.gen.UnitEntity;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.type.weapons.BuildWeapon;
import rout.world.draw.RoutFx;
import rout.world.entities.RoutBulletType;
import rout.world.units.ChainAI;
import rout.world.units.ChainUnitType;

public class RoutUnits {
    public static UnitType necessity, essential, roustalker, routecerate;

    public static void loadContent() {
        necessity = new UnitType("necessity") {{
            constructor = UnitEntity::create;
            hitSize = 12;
            isEnemy = false;
            lowAltitude = true;
            flying = true;
            mineSpeed = 7f;
            mineTier = 0;
            buildSpeed = 0.5f;
            drag = 0.05f;
            speed = 3f;
            rotateSpeed = 15f;
            accel = 0.1f;
            fogRadius = 0f;
            itemCapacity = 45;
            health = 200f;
            engineOffset = 6f;
            alwaysUnlocked = true;
            coreUnitDock = true;
            wreckSoundVolume = 0.8f;
            deathSoundVolume = 0.7f;
            mineFloor = true;
            outlineColor = Color.valueOf("30313b");
            drawBuildBeam = false;
            weapons.add(new BuildWeapon() {{
                x = 23 / 8f;
                y = 0;
                recoil = 0;
                rotate = true;
                rotateSpeed = 7f;
            }});
        }};
        essential = new UnitType("essential") {{
            constructor = UnitEntity::create;
            hitSize = 22;
            isEnemy = false;
            lowAltitude = true;
            flying = true;
            mineSpeed = 12f;
            mineTier = 0;
            buildSpeed = 1.1f;
            drag = 0.05f;
            speed = 4f;
            rotateSpeed = 15f;
            accel = 0.1f;
            fogRadius = 0f;
            coreUnitDock = true;
            itemCapacity = 45;
            health = 600f;
            engineOffset = 6f;
            engineSize = 4;
            wreckSoundVolume = 1f;
            alwaysUnlocked = false;
            deathSoundVolume = 0.9f;
            outlineColor = Color.valueOf("30313b");
            drawBuildBeam = false;
            weapons.add(
                    new BuildWeapon() {{
                        x = 0;
                        y = 37/8f;
                        recoil = 0;
                        rotate = true;
                        rotateSpeed = 7f;
                        mirror = false;
                        }},
                    new BuildWeapon() {{
                        x = 30 / 8f;
                        y = 15 / 8f;
                        rotate = true;
                        rotateSpeed = 7f;
                        recoil = 0;
                        mirror = true;
                    }});
        }};
        routecerate = new ChainUnitType("routecerate") {
            {
                constructor = LegsUnit::create;
                controller = u -> new ChainAI(){{
                    chainMergeType = null;
                    //routecerates only chain with lesser roustalkers, not each other
                    chainables = Seq.with(RoutUnits.roustalker);
                }};
                aiController = ChainAI::new;
                physics = true;
                flying = false;
                lowAltitude = false;
                targetGround = true;
                targetAir = false;
                engineSize = 0;
                hitSize = 16f;
                health = 450f;
                speed = 1.8f;
                accel = 0.1f;
                drag = 0.06f;
                rotateSpeed = 12f;
                legCount = 4;
                legGroupSize = 2;
                legLength = 11f;
                legMoveSpace = 1.2f;
                lockLegBase = true;
                legMaxLength = 1.1f;
                legMinLength = 0.9f;
                useUnitCap = false;
                legExtension = -1f;
                legPairOffset = 40f;
                legContinuousMove = true;
                outlineColor = Color.valueOf("30313b");
                shadowElevation = 0.5f;
                wreckSoundVolume = 0.8f;
                deathSoundVolume = 0.7f;
                weapons.add(new Weapon() {{
                    reload = 20;
                    rotate = true;
                    mirror = false;
                    x = 0;
                    shoot.shots = 5;
                    shoot.shotDelay = 2;
                    bullet = new RoutBulletType(2.5f, 25f, "rout-router-bullet") {{
                        width = 6;
                        height = 6;
                        smokeEffect = RoutFx.shootRouterSmall;
                        shootEffect = Fx.shootSmallColor;
                        hitEffect = despawnEffect = RoutFx.hitRouterSmall;
                        killEffect = RoutFx.killRouter;
                        homingDelay = 10;
                        homingPower = 0.05f;
                    }};
                }});
            }
        };
        roustalker = new ChainUnitType("roustalker") {{
            constructor = LegsUnit::create;
            controller = u -> new ChainAI(){{
                chainMergeType = RoutUnits.routecerate;
                //roustalkers chain with each other and with routecerates
                chainables = Seq.with(RoutUnits.roustalker, RoutUnits.routecerate);
            }};
            aiController = ChainAI::new;
            physics = true;
            flying = false;
            lowAltitude = false;
            targetGround = true;
            targetAir = false;
            engineSize = 0;
            hitSize = 8f;
            health = 120f;
            speed = 2f;
            accel = 0.12f;
            drag = 0.06f;
            rotateSpeed = 14f;
            legCount = 4;
            legGroupSize = 2;
            legLength = 10f;
            legMoveSpace = 1f;
            lockLegBase = true;
            legMaxLength = 1.1f;
            legMinLength = 0.9f;
            useUnitCap = false;
            legExtension = -1f;
            legPairOffset = 40f;
            legContinuousMove = true;
            mechLegColor = Color.valueOf("30313b");
            outlineColor = Color.valueOf("30313b");
            shadowElevation = 0.5f;
            wreckSoundVolume = 0.8f;
            deathSoundVolume = 0.7f;
            weapons.add(new Weapon(){{
                reload = 15;
                rotate = true;
                mirror = false;
                shoot.shots = 2;
                shoot.shotDelay = 3f;
                x = 0;
                bullet = new RoutBulletType(2, 15, "rout-router-bullet"){{
                    width = 4;
                    height = 4;
                    smokeEffect = RoutFx.shootRouterSmall;
                    shootEffect = Fx.shootSmallColor;
                    hitEffect = despawnEffect = RoutFx.hitRouterSmall;
                    killEffect = RoutFx.killRouter;
                    homingDelay = 10;
                    homingPower = 0.05f;
                }};
            }});
        }};

    }
}
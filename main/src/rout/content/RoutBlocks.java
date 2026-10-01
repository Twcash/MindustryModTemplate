package rout.content;

import arc.func.Cons;
import arc.graphics.Color;
import arc.math.Interp;
import arc.struct.Seq;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Liquids;
import mindustry.content.UnitTypes;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.FlakBulletType;
import mindustry.entities.effect.WrapEffect;
import mindustry.entities.part.DrawPart;
import mindustry.entities.pattern.ShootAlternate;
import mindustry.graphics.CacheLayer;
import mindustry.graphics.Layer;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.LiquidStack;
import mindustry.world.Block;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.draw.*;
import mindustry.world.meta.BuildVisibility;
import rout.Rout;
import rout.world.blocks.bases.RouterBlock;
import rout.world.blocks.core.RouterCore;
import rout.world.blocks.crafting.RouterCrafter;
import rout.world.blocks.defense.RouterMender;
import rout.world.blocks.distribution.DuctRouterRouter;
import rout.world.blocks.distribution.MetaphysicalItemRouter;
import rout.world.blocks.distribution.RouterRouter;
import rout.world.blocks.liquids.RouterLiquidRouter;
import rout.world.blocks.payload.RouterConstructor;
import rout.world.blocks.payload.RouterDeconstructor;
import rout.world.blocks.environment.FloorConverter;
import rout.world.blocks.environment.RouterFloor;
import rout.world.blocks.payload.RouterPayloadRouter;
import rout.world.blocks.payload.RouterTransmutator;
import rout.world.blocks.power.RouterBattery;
import rout.world.blocks.power.RouterGenerator;
import rout.world.blocks.production.RoutDrill;
import rout.world.blocks.production.RouterPump;
import rout.world.blocks.turrets.RouterTurret;
import rout.world.blocks.turrets.RouterPayloadTurret;
import rout.world.draw.RoutFx;
import rout.world.draw.drawers.DrawCutoff;
import rout.world.draw.drawers.DrawHammer;
import rout.world.draw.drawers.DrawInverse;
import rout.world.draw.drawers.RoutDrawTurret;
import rout.world.entities.RoutBulletType;
import rout.world.entities.RoutPointBulletType;
import rout.world.entities.RoutPointLaserBulletType;
import rout.world.entities.draw.NewRegPart;

import static mindustry.type.ItemStack.with;

public class RoutBlocks {

    //environmentBlocks
    public static Floor routerFloor, richRouterFloor, looseRouterFloor, distributiveSubstrate, dormantDistributiveSubstrate;

    public static Block
            //Core blocks
            coreRouter, routineCore, routerEnricher,
    //defenseBlocks
            routerWall, largeRouterWall, routerMender,
    //distribution
            router2, ductRouter2,distributor2, metaphysicalRouter ,
    //production
            routerDrill, bigRouterDrill,
    //crafting
            routerCompressor, routerMelter, routerHeater,
    //payload
            routerFabricator, largeRouterFabricator,  routerDefabricator, payloadRouter,largePayloadRouter, routerTransmutator,
    //liquid
            liquidRouter2, routerPumpjack,
    //turrets
            routerTurret, detour, distroute,
    //power
            routerTurbine, routerBattery;

    public static <T extends UnlockableContent> void overwrite(UnlockableContent target, Cons<T> setter) {
        setter.get((T) target);
    }

    public static void loadContent(){
        routerFloor = new RouterFloor("router-floor", 4){{
            itemDrop = RoutItems.routerDust;
        }};
        looseRouterFloor = new RouterFloor("loose-router-substrate", 4){{
            liquidDrop = RoutItems.liquidRouter;
            speedMultiplier = 0.9f;
            shallow = true;
            cacheLayer = CacheLayer.mud;
        }};
        richRouterFloor = new RouterFloor("rich-router-floor", 4){{
            itemDrop = RoutItems.routerFragment;

        }};
        dormantDistributiveSubstrate = new Floor("distributive-substrate-dormant", 3);
        distributiveSubstrate = new RouterFloor("distributive-substrate", 3){{
            itemDrop = RoutItems.distributiveCore;
        }};
        coreRouter = new RouterCore("core-route"){{
            requirements(Category.effect, with(RoutItems.routerFragment, 150, RoutItems.routerium, 50));
            blacklistedFloors.add(distributiveSubstrate.asFloor());
            blacklistedFloors.add(dormantDistributiveSubstrate.asFloor());
            blacklistedFloors.add(looseRouterFloor.asFloor());

            buildVisibility = BuildVisibility.shown;
            unitType = RoutUnits.necessity;
            health = 1000;
            alwaysUnlocked = true;
            isFirstTier = true;
            size = 2;
            floor = (RouterFloor) routerFloor;
            itemCapacity = 250;
            coreMerge = true;
        }};
        routineCore = new RouterCore("core-routine"){{
            requirements(Category.effect, with(RoutItems.routerFragment, 500, RoutItems.routerium, 250, RoutItems.clearRouter, 50));
            blacklistedFloors.add(distributiveSubstrate.asFloor());
            blacklistedFloors.add(dormantDistributiveSubstrate.asFloor());
            blacklistedFloors.add(looseRouterFloor.asFloor());
            buildVisibility = BuildVisibility.shown;
            unitType = RoutUnits.essential;
            size = 3;
            isFirstTier = true;
            floor = (RouterFloor) routerFloor;
            itemCapacity = 700;
            health = 3500;
            spreadRange = 20;
            spreadInterval = 0.5f;
            coreMerge = true;
        }};
        routerEnricher = new FloorConverter("router-enricher"){{
            requirements(Category.effect, with(RoutItems.routerFragment, 150, RoutItems.routerium, 90));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            size = 2;
            consumeLiquid(Liquids.slag, 0.25f);
            radius = 10;
            conversions.put((Floor)Blocks.water, looseRouterFloor);
            conversions.put((Floor)Blocks.deepwater, looseRouterFloor);
            conversions.put((Floor)Blocks.taintedWater, looseRouterFloor);
            conversions.put((Floor)Blocks.deepTaintedWater, looseRouterFloor);
            conversions.put((Floor)Blocks.sandWater, looseRouterFloor);
            conversions.put((Floor)Blocks.darksandWater, looseRouterFloor);
            conversions.put((Floor)Blocks.darksandTaintedWater, looseRouterFloor);
            conversions.put(dormantDistributiveSubstrate, distributiveSubstrate);
            enrich = RoutFx.distributiveEnrich;
            derich = RoutFx.distributivederich;
        }};
        routerWall = new RouterBlock("router-wall"){{
            requirements(Category.defense, with(RoutItems.routerFragment, 8));
            size = 1;
            scaledHealth = 250;
            armor = 2;
        }};
        largeRouterWall = new RouterBlock("large-router-wall"){{
            requirements(Category.defense, with(RoutItems.routerFragment, 32));
            size = 2;
            scaledHealth = 250*4;
            armor = 2;
        }};
        routerMender = new RouterMender("router-mender"){{
            requirements(Category.defense, with(RoutItems.routerFragment, 40, RoutItems.routerDust, 100));
            consumeLiquid(RoutItems.liquidRouter, 120);
            alwaysUnlocked = true;
            size = 1;
        }};
        router2 = new RouterRouter("router2"){{
            requirements(Category.distribution, with(RoutItems.routerDust, 20));
        }};
        distributor2 = new RouterRouter("distributor"){{
            requirements(Category.distribution, with(RoutItems.distributiveCore, 10, RoutItems.routerDust, 50));
            size = 2;
        }};
        ductRouter2 = new DuctRouterRouter("duct-router2"){{
            requirements(Category.distribution, with(RoutItems.routerium, 5));
        }};
        metaphysicalRouter = new MetaphysicalItemRouter("metaphysical-router"){{
            requirements(Category.distribution, with(RoutItems.routerium, 15, RoutItems.yellowRouterium, 10, RoutItems.distributiveCore, 20));
            size = 1;
            reach = 2;
        }};
        liquidRouter2 = new RouterLiquidRouter("liquid-router2"){{
            requirements(Category.liquid, with(RoutItems.clearRouter, 10));
            liquidCapacity = 90;
        }};
        routerDrill = new RoutDrill("router-drill"){{
            requirements(Category.production, with(RoutItems.routerDust, 10));
            drillTime = 240;
            alwaysUnlocked = true;
            itemCapacity = 15;
            mineAmount = 4;
            size = 1;
            tier = 0;
            updateEffect = Fx.none;
            hasLiquids = false;
            drillEffectChance = 100;
            drawer = new DrawMulti(new DrawDefault(), new DrawHammer(){{
                uprot = 180;
                pow = 4;
                shadowElevation = 1f;
                interp = Interp.pow5Out;
            }});
        }};
        bigRouterDrill = new RoutDrill("big-router-drill"){{
            requirements(Category.production, with(RoutItems.routerFragment, 25, RoutItems.routerDust, 50));
            drillTime = 600;
            hardnessDrillMultiplier = 600;
            itemCapacity = 30;
            mineAmount = 6;
            size = 2;
            tier = 1;
            updateEffect = Fx.none;
            hasLiquids = false;
            drillEffect = Fx.mineBig;
            drillEffectChance = 100;
            consumeLiquid(RoutItems.liquidRouter, 15).boost();
            liquidBoostIntensity = 1.5f;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawRegion("-rotator"){{
                spinSprite = true;
                rotateSpeed = 2.5f;
            }}, new DrawDefault(), new DrawHammer(){{
                uprot = 0;
                pow = 4;
                max = -0.25f;
                shadowElevation = 1f;
                interp = Interp.pow5In;
            }});
        }};
        routerPumpjack = new RouterPump("router-pumpjack"){{
            requirements(Category.production, with(RoutItems.routerFragment, 24, RoutItems.routerium, 40, RoutItems.distributiveCore, 40));
            size = 2;
            pumpAmount = 0.5f;
            liquidCapacity = 120;
            //only placeable on router-bearing floors; destroys itself if its floor reverts away
            whitelist.add(routerFloor.asFloor());
            whitelist.add(richRouterFloor.asFloor());
            whitelist.add(looseRouterFloor.asFloor());
            whitelist.add(distributiveSubstrate.asFloor());
            whitelist.add(dormantDistributiveSubstrate.asFloor());
            drawer = new DrawMulti(new DrawDefault(), new DrawLiquidTile(RoutItems.liquidRouter, 0.5f), new DrawRegion("-top"));
        }};
        routerCompressor = new RouterCrafter("router-compressor"){{
            requirements(Category.crafting, with(RoutItems.routerDust, 30));
            size = 1;
            itemCapacity = 20;
            consumeItem(RoutItems.routerDust, 10);
            outputItem = new ItemStack(RoutItems.routerFragment,1);
            craftTime = 120;
            hasLiquids = false;
            craftEffect = Fx.coalSmeltsmoke;
            drawer = new DrawMulti(new DrawDefault(), new DrawInverse(){{
                interp = Interp.pow5Out;
            }});
        }};
        routerMelter = new RouterCrafter("router-melter"){{
            requirements(Category.crafting, with(RoutItems.routerFragment, 35));
            consumeItem(RoutItems.routerDust, 1);
            outputLiquid = new LiquidStack(RoutItems.liquidRouter, 0.5f);
            craftTime = 30;
            liquidCapacity = 120;
            itemCapacity = 10;
            size = 1;
            craftEffect = Fx.reactorsmoke;
            drawer = new DrawMulti(new DrawDefault(), new DrawLiquidTile(RoutItems.liquidRouter, 0.1f), new DrawRegion("-top"));
        }};
        routerHeater = new RouterCrafter("router-heater"){{
            requirements(Category.crafting, with(RoutItems.routerFragment, 35, RoutItems.routerium, 25));
            consumeItem(RoutItems.routerium, 1);
            consumeLiquid(RoutItems.liquidRouter, 2);
            outputLiquid = new LiquidStack(Liquids.slag, 1f);
            craftTime = 240;
            liquidCapacity = 120;
            itemCapacity = 10;
            size = 2;
            craftEffect = Fx.heatReactorSmoke;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(Liquids.slag, 1), new DrawBubbles(){{
                color = Liquids.slag.color.mul(1.1f);
                sides = 10;
                recurrence = 3f;
                spread = 6;
                radius = 1.5f;
                amount = 20;
            }}, new DrawDefault(), new DrawGlowRegion(){{
                alpha = 0.7f;
                glowIntensity = 0.3f;
                glowScale = 6f;
            }});
        }};
        payloadRouter = new RouterPayloadRouter("payload-router"){{
            requirements(Category.units, with(RoutItems.routerFragment, 10));
            size = 1;
            payloadLimit = 1;
        }};
        largePayloadRouter = new RouterPayloadRouter("large-payload-router"){{
            requirements(Category.units, with(RoutItems.routerFragment, 30, RoutItems.distributiveCore, 10));
            size = 3;
            payloadLimit = 3;
        }};
        routerFabricator = new RouterConstructor("router-fabricator"){{
            requirements(Category.units, with(RoutItems.routerFragment, 25));
            size = 3;
            buildSpeed = 4/60f;
            filter.add(router2);
            recipeRequirements.put(router2, ItemStack.with(RoutItems.routerFragment, 5));
            filter.add(liquidRouter2);
            recipeRequirements.put(liquidRouter2, ItemStack.with(RoutItems.distributiveCore, 10));
            recipeLiquids.put(liquidRouter2, LiquidStack.with(Liquids.slag, 4));
            recipeRequirements.put(routerWall, ItemStack.with(RoutItems.routerFragment, 4));
            filter.add(routerWall);
        }};
        largeRouterFabricator = new RouterConstructor("large-router-fabricator"){{
            requirements(Category.units, with(RoutItems.routerFragment, 125, RoutItems.routerium, 100, RoutItems.routerDust, 200, RoutItems.distributiveCore, 50));
            size = 5;
            maxBlockSize = 3;
            buildSpeed = 20/60f;
            consumeLiquid(RoutItems.liquidRouter, 4);
            filter.add(router2);
            filter.add(distributor2);
            filter.add(largeRouterWall);
            recipeRequirements.put(routerWall, ItemStack.with(RoutItems.routerFragment, 8));
            recipeRequirements.put(router2, ItemStack.with(RoutItems.routerFragment, 1));
            filter.add(liquidRouter2);
            recipeRequirements.put(liquidRouter2, ItemStack.with(RoutItems.distributiveCore, 20));
            recipeLiquids.put(liquidRouter2, LiquidStack.with(Liquids.slag, 4));
            recipeRequirements.put(routerWall, ItemStack.with(RoutItems.routerFragment, 4));
            filter.add(routerWall);
        }};
        routerTurbine = new RouterGenerator("router-turbine"){{
            requirements(Category.power, with(RoutItems.routerium, 75, RoutItems.clearRouter, 10, RoutItems.routerFragment, 125));
            size = 1;
            liquidCapacity = 180;
            consumeLiquid(Liquids.slag, 60);
            powerProduction = 2;
            squareSprite = false;
            drawer = new DrawMulti(new DrawCutoff("-bottom"){{
                suffix = "-bottom";
            }}, new DrawLiquidTile(Liquids.slag), new DrawBlurSpin("-rotator", 0.6f * 9f){{
                blurThresh = 0.01f;
            }},new DrawRegion("-top"){{
            }});
        }};
        routerBattery = new RouterBattery("router-battery"){{
            requirements(Category.power, with(RoutItems.yellowRouterium, 10));
            size = 1;
            consumePowerBuffered(1250);
        }};
        routerTransmutator = new RouterTransmutator("router-tansmutator"){{
            requirements(Category.units, with(RoutItems.routerFragment, 25));
            size = 3;
            constructTime = 600;
            transmutations.put(router2, ductRouter2);
            transmutations.put(ductRouter2, routerBattery);
            consumeLiquid(RoutItems.liquidRouter, 1);
            recipeRequirements.put(router2,  ItemStack.with(RoutItems.routerFragment,5));
            powerRecipeRequirements.put(router2, 0f);
            recipeRequirements.put(routerBattery, ItemStack.with(RoutItems.routerium, 5));
            powerRecipeRequirements.put(routerBattery, 4f);
        }};
        routerDefabricator = new RouterDeconstructor("router-defabricator"){{
            size = 2;
            deconstructSpeed = 0.02f;
            requirements(Category.units, with(RoutItems.routerFragment, 10));
            deconstructionResults.put(router2, ItemStack.with(RoutItems.routerFragment, 5));
            deconstructionResults.put(ductRouter2, ItemStack.with(RoutItems.routerium, 5));
            deconstructionResults.put(ductRouter2, ItemStack.with(RoutItems.routerium, 5));
            deconstructionResults.put(liquidRouter2, ItemStack.with(RoutItems.clearRouter, 5));
            deconstructionResults.put(routerBattery, ItemStack.with(RoutItems.yellowRouterium, 10));
            deconstructionResults.put(routerWall, ItemStack.with(RoutItems.routerDust, 30));
        }};
        routerTurret = new RouterTurret("router-turret"){{
            requirements(Category.turret, with(RoutItems.routerFragment, 10, RoutItems.routerDust, 35));
            size = 1;
            scaledHealth = 100;
            reload = 20;
            shoot = new ShootAlternate(3);
            shoot.shots = 2;
            shoot.shotDelay = 2;
            maxAmmo = 20;
            consumeCoolant(0.1f);
            outlineColor = Color.valueOf("30313b");
            ammoUseEffect = RoutFx.casing1;
            ammo(RoutItems.routerDust, new RoutBulletType(2, 11, "rout-router-bullet"){{
                width = 4;
                height = 4;
                smokeEffect = RoutFx.shootRouterSmall;
                shootEffect = Fx.shootSmallColor;
                hitEffect = despawnEffect = RoutFx.hitRouterSmall;
                killEffect = RoutFx.killRouter;
            }},RoutItems.routerFragment, new RoutBulletType(1.5f, 20, "rout-router-bullet"){{
                width = 8;
                height = 8;
                spin = 0.2f;
                pierce = true;
                pierceCap = 3;
                smokeEffect = RoutFx.shootRouterSmall;
                shootEffect = Fx.shootSmallColor;
                hitEffect = despawnEffect = RoutFx.hitRouterSmall;
                killEffect = RoutFx.killRouter;
            }},RoutItems.routerium, new RoutBulletType(1, 45, "rout-router-bullet"){{
                width = 8;
                height = 8;
                homingDelay = 10;
                homingPower = 0.1f;
                rangeChange = 24;
                spin = 0.5f;
                smokeEffect = RoutFx.shootRouterSmall;
                shootEffect = Fx.shootSmallColor;
                hitEffect = despawnEffect = RoutFx.hitRouter;
                trailEffect = RoutFx.routerBlastColor;
                trailColor = Color.white;
                killEffect = RoutFx.killRouter;
            }});
            limitRange();
        }};
        detour = new RouterTurret("detour"){{
            requirements(Category.turret, with(RoutItems.routerFragment, 40, RoutItems.routerium, 10));
            size = 2;
            reload = 90;
            scaledHealth = 300;
            maxAmmo = 5;
            shootY = 0;
            range = 180;
            coolantMultiplier = 1f;
            consumeCoolant(0.4f);
            outlineColor = Color.valueOf("30313b");
            ammo(RoutBlocks.routerWall, new BasicBulletType(2, 90, "rout-router-bullet"){{
                scaleLife = true;
                splashDamageRadius = 32;
                splashDamage = 50;
                ammoMultiplier = 12;
                width = 8;
                height = 8;
                shrinkX = shrinkY = 0.5f;
                trailLength = 12;
                trailWidth = 2;
                smokeEffect = RoutFx.shootRouterSmall;
                hitEffect = despawnEffect = RoutFx.hitRouter;
            }},RoutBlocks.liquidRouter2, new FlakBulletType(){{
                scaleLife = true;
                splashDamageRadius = 32;
                speed = 2;
                damage = 90;
                splashDamage = 50;
                ammoMultiplier = 12;
                sprite = "rout-router-bullet";
                trailColor = Color.white;
                width = 8;
                height = 8;
                shrinkX = shrinkY = 0.5f;
                trailLength = 12;
                trailWidth = 2;
                smokeEffect = RoutFx.shootRouterSmall;
                hitEffect = despawnEffect = RoutFx.hitRouter;
                fragBullets = 5;
                fragBullet = new BasicBulletType(2, 50, "rout-router-bullet-clear"){{
                    width = 4;
                    height = 4;
                    smokeEffect = RoutFx.shootRouterSmall;
                    shootEffect = Fx.shootSmallColor;
                    hitEffect = despawnEffect = RoutFx.hitRouterSmall;
                }};
            }});
            drawer = new RoutDrawTurret(){{
                setAmmoParts(routerWall, Seq.with(new NewRegPart("-router-wall"){{
                    progress = DrawPart.PartProgress.reload.curve(Interp.pow5In);
                    alphaTo = 0;
                    alpha = 1;
                }}),liquidRouter2, Seq.with(new NewRegPart("-liquid-router"){{
                    progress = DrawPart.PartProgress.reload.curve(Interp.pow5In);
                    alphaTo = 0;
                    alpha = 1;
                }}));
            }};
            limitRange();
        }};
        distroute = new RouterTurret("distroute"){{
            requirements(Category.turret, with(RoutItems.routerium, 90, RoutItems.clearRouter, 50, RoutItems.routerDust, 150, RoutItems.distributiveCore, 80));
            size = 3;
            continuous = true;
            shootY = 0;
            continuousTime = 90;
            liquidCapacity = 100;
            reload = 120;
            range = 150;
            cooldownTime = 120;
            ammo(RoutItems.liquidRouter, new RoutPointLaserBulletType(){{
                killLiquid = RoutItems.liquidRouter;
                killLiquidAmount = 120;
                damage = 10;
                damageInterval = 2;
                smokeEffect = new WrapEffect(RoutFx.routerBlastColor, Color.valueOf("ff8393"));
                smokeEffectInterval = 5;
                shootEffect = Fx.none;
                fxInterval = 30;
                killEffect = RoutFx.killRouter;
                hitEffect = RoutFx.hitRouterSmall;
            }});
            limitRange();
            drawer = new DrawTurret(){{
                parts.addAll(
                        new NewRegPart("-side-r"){{
                            moveRot = -35;
                            moveX = 4;
                            progress = PartProgress.warmup.mul(PartProgress.recoil);
                            children.add(new NewRegPart("-side-r-side"){{
                                moveX = -2.5f;
                                under = true;
                                progress = PartProgress.warmup.mul(PartProgress.time.absin(4,1));
                            }});
                        }},
                        new NewRegPart("-side-l"){{
                            moveRot = 35;
                            moveX = -4;
                            progress = PartProgress.warmup.mul(PartProgress.recoil);
                            children.add(new NewRegPart("-side-l-side"){{
                                moveX = 2.5f;
                                under = true;
                                progress = PartProgress.warmup.mul(PartProgress.time.absin(4,1));
                            }});
                        }}
                );
            }};
        }};
    }
}

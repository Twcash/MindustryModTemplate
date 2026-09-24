package rout.content;

import arc.func.Cons;
import arc.graphics.Color;
import arc.math.Interp;
import arc.struct.Seq;
import mindustry.content.Fx;
import mindustry.content.Liquids;
import mindustry.content.UnitTypes;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.part.DrawPart;
import mindustry.entities.pattern.ShootAlternate;
import mindustry.graphics.Layer;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.LiquidStack;
import mindustry.world.Block;
import mindustry.world.draw.*;
import mindustry.world.meta.BuildVisibility;
import rout.Rout;
import rout.world.blocks.bases.RouterBlock;
import rout.world.blocks.core.RouterCore;
import rout.world.blocks.crafting.RouterCrafter;
import rout.world.blocks.distribution.DuctRouterRouter;
import rout.world.blocks.distribution.MetaphysicalItemRouter;
import rout.world.blocks.distribution.RouterRouter;
import rout.world.blocks.liquids.RouterLiquidRouter;
import rout.world.blocks.payload.RouterConstructor;
import rout.world.blocks.payload.RouterDeconstructor;
import rout.world.blocks.environment.RouterFloor;
import rout.world.blocks.payload.RouterPayloadRouter;
import rout.world.blocks.payload.RouterTransmutator;
import rout.world.blocks.power.RouterBattery;
import rout.world.blocks.power.RouterGenerator;
import rout.world.blocks.production.RoutDrill;
import rout.world.blocks.turrets.RouterItemTurret;
import rout.world.blocks.turrets.RouterPayloadTurret;
import rout.world.draw.drawers.DrawCutoff;
import rout.world.draw.drawers.DrawHammer;
import rout.world.draw.drawers.DrawInverse;
import rout.world.draw.drawers.RoutDrawTurret;
import rout.world.entities.draw.NewRegPart;

import static mindustry.type.ItemStack.with;

public class RoutBlocks {

    //environmentBlocks
    public static RouterFloor routerFloor, richRouterFloor;
    //Player blocks

    public static Block
            //Core blocks
            coreRouter, routineCore,
    //defenseBlocks
            routerWall,
    //distribution
            router2, ductRouter2, metaphysicalRouter,
    //production
            routerDrill, bigRouterDrill,
    //crafting
            routerCompressor, routerMelter, routerHeater,
    //payload
            routerFabricator, routerDefabricator, payloadRouter, routerTransmutator,
    //liquid
            liquidRouter2,
    //turrets
            routerTurret, detour,
    //power
            routerTurbine, routerBattery;

    public static <T extends UnlockableContent> void overwrite(UnlockableContent target, Cons<T> setter) {
        setter.get((T) target);
    }

    public static void loadContent(){
        routerFloor = new RouterFloor("router-floor", 4){{
            itemDrop = RoutItems.routerDust;
        }};
        richRouterFloor = new RouterFloor("rich-router-floor", 4){{
            itemDrop = RoutItems.routerFragment;
        }};
        coreRouter = new RouterCore("core-route"){{
            requirements(Category.effect, with(RoutItems.routerFragment, 150, RoutItems.routerium, 50));
            buildVisibility = BuildVisibility.shown;
            unitType = RoutUnits.necessity;
            alwaysUnlocked = true;
            isFirstTier = true;
            size = 2;
            floor = routerFloor;
            itemCapacity = 250;
            coreMerge = true;
        }};
        routineCore = new RouterCore("core-routine"){{
            requirements(Category.effect, with(RoutItems.routerFragment, 500, RoutItems.routerium, 250, RoutItems.clearRouter, 50));
            buildVisibility = BuildVisibility.shown;
            unitType = RoutUnits.essential;
            size = 3;
            isFirstTier = true;
            floor = routerFloor;
            itemCapacity = 700;
            spreadRange = 20;
            spreadInterval = 0.5f;
            coreMerge = true;
        }};

        routerWall = new RouterBlock("router-wall"){{
            requirements(Category.defense, with(RoutItems.routerFragment, 8));
            size = 1;
            scaledHealth = 250;
            armor = 2;
        }};
        router2 = new RouterRouter("router2"){{
            requirements(Category.distribution, with(RoutItems.routerDust, 20));
        }};
        ductRouter2 = new DuctRouterRouter("duct-router2"){{
            requirements(Category.distribution, with(RoutItems.routerium, 5));
        }};
        metaphysicalRouter = new MetaphysicalItemRouter("metaphysical-router"){{
            requirements(Category.distribution, with(RoutItems.routerium, 15, RoutItems.yellowRouterium, 10));
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
            requirements(Category.production, with(RoutItems.routerFragment, 24));
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
            consumeLiquid(RoutItems.liquidRouter, 0.25f).boost();
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
            craftTime = 120;
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
        routerFabricator = new RouterConstructor("router-fabricator"){{
            requirements(Category.units, with(RoutItems.routerFragment, 25));
            size = 3;
            buildSpeed = 0.05f;
            filter.add(router2);
            recipeRequirements.put(router2, ItemStack.with(RoutItems.routerFragment, 1));
            filter.add(liquidRouter2);
            recipeRequirements.put(liquidRouter2, ItemStack.empty);
            recipeLiquids.put(liquidRouter2, LiquidStack.with(Liquids.slag, 4));
        }};
        routerTurbine = new RouterGenerator("router-turbine"){{
            requirements(Category.power, with(RoutItems.routerium, 75, RoutItems.clearRouter, 10, RoutItems.routerFragment, 125));
            size = 1;
            liquidCapacity = 180;
            consumeLiquid(Liquids.slag, 1);
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
            deconstructSpeed = 0.01f;
            requirements(Category.units, with(RoutItems.routerFragment, 10));
            deconstructionResults.put(router2, ItemStack.with(RoutItems.routerFragment, 5));
            deconstructionResults.put(ductRouter2, ItemStack.with(RoutItems.routerium, 5));
            deconstructionResults.put(ductRouter2, ItemStack.with(RoutItems.routerium, 5));
            deconstructionResults.put(liquidRouter2, ItemStack.with(RoutItems.clearRouter, 5));
            deconstructionResults.put(routerBattery, ItemStack.with(RoutItems.yellowRouterium, 10));
        }};
        routerTurret = new RouterItemTurret("router-turret"){{
            requirements(Category.defense, with(RoutItems.routerFragment, 4));
            size = 1;
            scaledHealth = 100;
            reload = 20;
            shoot = new ShootAlternate(3);
            shoot.shots = 2;
            maxAmmo = 20;
            range = 150;
            outlineColor = Color.valueOf("30313b");
            ammo(RoutItems.routerDust, new BasicBulletType(2, 10, "router-bullet"){{
                width = 8;
                height = 12;
                smokeEffect = Fx.shootSmallSmoke;
                shootEffect = Fx.shootSmallColor;
                hitEffect = despawnEffect = Fx.hitBulletSmall;
            }});
        }};
        detour = new RouterPayloadTurret("detour"){{
            requirements(Category.defense, with(RoutItems.routerFragment, 40, RoutItems.routerium, 10));
            size = 2;
            reload = 90;
            scaledHealth = 300;
            shoot.firstShotDelay = 20;
            maxAmmo = 5;
            range = 250;
            outlineColor = Color.valueOf("30313b");
            ammo(RoutBlocks.routerWall, new BasicBulletType(2, 60, "router-bullet"){{
                scaleLife = true;
                splashDamageRadius = 24;
                splashDamage = 50;
                width = 8;
                height = 8;
                shrinkX = shrinkY = 0.5f;
                trailLength = 12;
                trailWidth = 2;
            }});
            drawer = new RoutDrawTurret(){{
                setAmmoParts(routerWall, Seq.with(new NewRegPart(){{
                    name = "rout-router-bullet";
                    progress = DrawPart.PartProgress.reload.curve(Interp.pow5In);
                    alphaTo = 0;
                    under = true;
                    alpha = 1;
                }}));
            }};
        }};
    }
}

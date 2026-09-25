package rout.content;

import arc.struct.ObjectFloatMap;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.Liquids;
import mindustry.game.Objectives;
import mindustry.type.Item;

import static mindustry.content.TechTree.*;

public class RoutTechTree {
    public static void loadContent(){
        ObjectFloatMap<Item> costMultipliers = new ObjectFloatMap<Item>();

        for(Item item : Vars.content.items()) costMultipliers.put(item, 0.08f);

        RoutPlanets.routulo.techTree = nodeRoot("serpulo", RoutBlocks.coreRouter, () -> {
            context().researchCostMultipliers = costMultipliers;
            nodeProduce(RoutItems.routerDust, ()->{
                nodeProduce(RoutItems.routerFragment, ()->{
                    nodeProduce(RoutItems.liquidRouter, ()->{
                        nodeProduce(Liquids.slag, ()->{

                        });
                    });
                    nodeProduce(RoutItems.routerium, ()->{
                        nodeProduce(RoutItems.clearRouter, ()->{
                            nodeProduce(RoutItems.yellowRouterium, ()->{

                            });
                        });
                    });
                });
            });
            node(RoutBlocks.routerTurret, ()->{
                node(RoutBlocks.detour, Seq.with(
                        new Objectives.Research(RoutBlocks.payloadRouter),
                        new Objectives.Research(RoutBlocks.routerFabricator)
                ), ()->{});
            });
            node(RoutUnits.necessity, ()->{
                node(RoutUnits.essential);
            });
            node(RoutBlocks.routineCore);
            node(RoutBlocks.routerDrill, ()->{
                node(RoutBlocks.bigRouterDrill);
            });
            node(RoutBlocks.router2, ()->{
                node(RoutBlocks.payloadRouter);
                node(RoutBlocks.ductRouter2, ()->{
                    node(RoutBlocks.liquidRouter2, ()->{
                        node(RoutBlocks.routerBattery);
                        node(RoutBlocks.metaphysicalRouter);
                    });
                });
            });
            node(RoutBlocks.routerCompressor, ()->{
                node(RoutBlocks.routerFabricator, ()->{
                    node(RoutBlocks.routerTransmutator, ()->{
                        node(RoutBlocks.routerDefabricator);
                    });
                });
                node(RoutBlocks.routerMelter, ()->{
                    node(RoutBlocks.routerHeater, ()->{
                        node(RoutBlocks.routerTurbine);
                    });
                });
            });
            node(RoutBlocks.routerWall);
        });
    };
}
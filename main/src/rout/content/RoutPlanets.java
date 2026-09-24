package rout.content;

import arc.graphics.Color;
import mindustry.content.Planets;
import mindustry.graphics.Pal;
import mindustry.graphics.g3d.HexMesh;
import mindustry.graphics.g3d.HexSkyMesh;
import mindustry.graphics.g3d.MultiMesh;
import mindustry.maps.planet.SerpuloPlanetGenerator;
import mindustry.type.Planet;
import rout.world.campaign.RoutPlanetGenerator;

public class RoutPlanets {
    public static Planet routulo;
    public static void loadContent(){
        Planets.serpulo.visible = false;
        Planets.serpulo.accessible = false;
        Planets.serpulo.removed = true;
        Planets.serpulo.removeContent();
        routulo = new Planet("routulo", Planets.sun, 1f, 3){{
            generator = new RoutPlanetGenerator();
            accessible = true;
            alwaysUnlocked = true;
            visible = true;
            clearSectorOnLose = true;
            defaultCore = RoutBlocks.coreRouter;
            startSector = 11;
            meshLoader = () -> new HexMesh(this, 6);
            cloudMeshLoader = () -> new MultiMesh(
                    new HexSkyMesh(this, 11, 0.15f, 0.13f, 5, new Color().set(Pal.spore).mul(0.9f).a(0.75f), 2, 0.45f, 0.9f, 0.38f),
                    new HexSkyMesh(this, 1, 0.6f, 0.16f, 5, Color.white.cpy().lerp(Pal.spore, 0.55f).a(0.75f), 2, 0.45f, 1f, 0.41f)
            );
            ruleSetter = r->{
                r.loadout.clear();
            };
        }};
    }
}

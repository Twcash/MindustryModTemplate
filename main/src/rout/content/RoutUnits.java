package rout.content;

import arc.graphics.Color;
import mindustry.gen.UnitEntity;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.type.weapons.BuildWeapon;

public class RoutUnits {
    public static UnitType necessity, essential;

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
    }
}
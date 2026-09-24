package rout.world.blocks.distribution;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;
import rout.annotations.Annotations;
import rout.world.blocks.environment.RouterFloor;
import rout.world.draw.RoutFx;

/**
 * Base class for routers that connect to blocks at a fixed offset distance
 * in each of the 4 cardinal directions, rather than to adjacent tiles.
 */
public class MetaphysicalRouter extends Block {
    /** Distance in tiles to the connected block in each cardinal direction. */
    public int reach = 2;
    /** Ticks before a direction's source/target role resets to neutral. */
    public float roleDuration = 60f;

    public @Annotations.Load("@-connection") TextureRegion connection;
    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation) {
        if (tile == null) return false;
        //Only change here is letting the core be placed on any router floor
        tile.getLinkedTilesAs(this, tempTiles);
        if(!tempTiles.contains(o -> (!o.floor().allowCorePlacement && !(o.floor() instanceof RouterFloor)) || o.block() instanceof CoreBlock)){
            return true;
        }
        super.canPlaceOn(tile, team, rotation);
        return false;
    }
    public MetaphysicalRouter(String name) {
        super(name);
        update = true;
        solid = true;
        destructible = true;
        hasItems = true;
        acceptsItems = true;
        itemCapacity = 10;
        unloadable = false;
        noUpdateDisabled = true;
    }

    public Tile reachTile(Building build, int direction) {
        Point2 p = Geometry.d4[direction];
        return Vars.world.tile(build.tileX() + p.x * reach, build.tileY() + p.y * reach);
    }

    /** Building at the offset position, or null. */
    public Building reachBuild(Building build, int direction) {
        Tile t = reachTile(build, direction);
        return t == null ? null : t.build;
    }

    public class MetaphysicalRouterBuild extends Building {
        // 0 = neutral (can push or pull), 1 = source (pull-only), 2 = target (push-only)
        protected final int[] role = new int[4];
        protected final float[] roleTimer = new float[4];
        protected int nextDir;

        @Override
        public boolean acceptItem(Building source, Item item) {
            return items.get(item) < itemCapacity;
        }

        @Override
        public int getMaximumAccepted(Item item) {
            return itemCapacity;
        }

        protected void updateRoles() {
            for (int i = 0; i < 4; i++) {
                if (roleTimer[i] > 0f) {
                    roleTimer[i] -= delta();
                    if (roleTimer[i] <= 0f) {
                        role[i] = 0;
                        roleTimer[i] = 0f;
                    }
                }
            }
        }

        @Override
        public void draw(){
            super.draw();
            for(int i = 0; i < 4; i++){
                Tile t = reachTile(this, i);
                if(t == null || t.build == null || t.build == this) continue;
                Draw.z(Layer.blockOver);
                Draw.rect(connection, t.worldx(), t.worldy());
            }
        }

        @Override
        public void updateTile() {
            super.updateTile();
            updateRoles();
            distributeItems();
        }

        protected void distributeItems() {
            for (int n = 0; n < 4; n++) {
                int i = (nextDir + n) % 4;
                Building other = reachBuild(this, i);
                if (other == null || other == this || !other.block.hasItems || other.items == null) continue;

                // push — skip if this direction is a pull-only source
                if (role[i] != 1) {
                    for (Item item : Vars.content.items()) {
                        int space = other.block.itemCapacity - other.items.get(item);
                        if (items.get(item) > 0 && space > 0) {
                            int toTransfer = Math.min(items.get(item), space);
                            other.items.add(item, toTransfer);
                            RoutFx.metaphysicalRouterSend.at(x, y, angleTo(other), new Vec2(other.x, other.y));
                            items.remove(item, toTransfer);
                            role[i] = 2;
                            roleTimer[i] = roleDuration;
                            nextDir = (i + 1) % 4;
                            break;
                        }
                    }
                }

                if (role[i] != 2) {
                    for (Item item : Vars.content.items()) {
                        if (other.block.itemFilter != null && item.id < other.block.itemFilter.length && other.block.itemFilter[item.id]) continue;
                        int space = itemCapacity - items.get(item);
                        if (other.items.get(item) > 0 && space > 0) {
                            int toTransfer = Math.min(other.items.get(item), space);
                            items.add(item, toTransfer);
                            RoutFx.metaphysicalRouterSend.at(other.x, other.y, angleTo(other), new Vec2(x, y));
                            other.items.remove(item, toTransfer);
                            role[i] = 1;
                            roleTimer[i] = roleDuration;
                            nextDir = (i + 1) % 4;
                            break;
                        }
                    }
                }
            }
        }
    }
}

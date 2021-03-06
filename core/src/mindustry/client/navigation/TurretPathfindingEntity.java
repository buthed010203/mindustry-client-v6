package mindustry.client.navigation;

import arc.math.geom.Circle;

public class TurretPathfindingEntity extends Circle {
    public boolean canHitPlayer;
    private static long nextId = 0;
    public long id;

    {
        id = nextId++;
    }

    public TurretPathfindingEntity(float x, float y, float range, boolean canHitPlayer){
        this.x = x;
        this.y = y;
        this.radius = range;
        this.canHitPlayer = canHitPlayer;
    }

    @Override
    public String toString(){
        return "TurretPathfindingEntity{" +
        "x=" + x +
        ", y=" + y +
        ", radius=" + radius +
        '}';
    }

    @Override
    public boolean equals(Object o) {
        if(o == this) return true;
        if(o == null || o.getClass() != this.getClass()) return false;
        TurretPathfindingEntity c = (TurretPathfindingEntity) o;
        return this.id == c.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}

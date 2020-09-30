package mindustry.client.navigation

import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Lines
import arc.math.geom.Position
import arc.struct.Seq

class WaypointPath(private val waypoints: Seq<Waypoint>) : Path {
    private val finished: Seq<Waypoint> = Seq()
    override val isDone: Boolean
        get() = waypoints.isEmpty

    override fun follow() {
        val waypoint: Waypoint? = waypoints.peek()
        if (waypoint != null) {
            waypoint.run()
            if (waypoint.isDone) {
                finished.add(waypoints.pop())
            }
        }
    }

    override fun progress(): Float {
        //TODO make this work better
        return waypoints.size / (waypoints.size + finished.size).toFloat()
    }

    override fun draw() {
        if (Path.show) {
            var lastWaypoint: Waypoint? = null
            for (waypoint in waypoints) {
                if (waypoint is Position) {
                    if (lastWaypoint != null) {
                        Draw.color(Color.blue)
                        Draw.alpha(0.4f)
                        Lines.stroke(3f)
                        Lines.line((lastWaypoint as Position).x, (lastWaypoint as Position).y, (waypoint as Position).x, (waypoint as Position).y)
                    }
                    lastWaypoint = waypoint
                }
                waypoint?.draw()
            }
        }
    }
}

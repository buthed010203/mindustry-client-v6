package mindustry.client.navigation

import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.math.Mathf
import arc.math.geom.Position
import mindustry.Vars

class PositionWaypoint(private val drawX: Float, private val drawY: Float) : Waypoint, Position {
    var tolerance = 16f
    override val isDone: Boolean
        get() = Vars.player.within(this, tolerance)

    override fun run() {
        val direction: Float = Vars.player.angleTo(this)
        var x: Float = Mathf.cosDeg(direction) * 2f
        var y: Float = Mathf.sinDeg(direction) * 2f
        x = Mathf.clamp(x / 10, -1f, 1f)
        y = Mathf.clamp(y / 10, -1f, 1f)
        Vars.control.input.updateMovementCustom(Vars.player.unit(), x, y, direction)
    }

    override fun getX(): Float {
        return drawX
    }

    override fun getY(): Float {
        return drawY
    }

    override fun draw() {
        Draw.color(Color.green)
        Draw.alpha(0.3f)
        Fill.circle(x, y, tolerance)
        Draw.color()
    }
}
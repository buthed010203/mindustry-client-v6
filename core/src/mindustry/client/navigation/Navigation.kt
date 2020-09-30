package mindustry.client.navigation

import arc.struct.Seq
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.world.blocks.defense.turrets.Turret

object Navigation {
    private var currentlyFollowing: Path? = null
    var isPaused = false
    fun follow(path: Path?) {
        currentlyFollowing = path
    }

    fun update() {
        if (currentlyFollowing != null && !isPaused) {
            currentlyFollowing!!.follow()
            if (currentlyFollowing!!.isDone) {
                currentlyFollowing!!.onFinish()
                currentlyFollowing = null
            }
        }
    }

    val isFollowing: Boolean
        get() = currentlyFollowing != null && !isPaused

    fun draw() {
        currentlyFollowing?.draw()
    }

    fun navigateTo(drawX: Float, drawY: Float) {
        val turrets: Seq<Turret.TurretBuild?> = Seq<Turret.TurretBuild?>()
        val dropZones: Seq<TurretPathfindingEntity?> = Seq<TurretPathfindingEntity?>()
        for (tile in Vars.world.tiles) {
            if (tile != null) {
                if (tile.block() is Turret) {
                    if (tile.team() !== Vars.player.team()) {
                        turrets.add(tile.build as Turret.TurretBuild)
                    }
                } else if (tile.block() === Blocks.spawn) {
                    dropZones.add(TurretPathfindingEntity(tile.x.toInt(), tile.y.toInt(), Vars.state.rules.dropZoneRadius))
                }
            }
        }
        val points: Seq<IntArray>? = AStar.findPathTurretsDropZone(turrets, Vars.player.x, Vars.player.y, drawX, drawY, Vars.world.width(), Vars.world.height(), Vars.player.team(), dropZones)
        if (points != null) {
            val waypoints: Seq<Waypoint> = Seq<Waypoint>()
            for (point in points) {
                waypoints.add(PositionWaypoint(point[0].toFloat(), point[1].toFloat()))
            }
            follow(WaypointPath(waypoints))
        }
    }
}
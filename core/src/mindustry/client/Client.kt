package mindustry.client

import arc.Events
import mindustry.Vars
import mindustry.client.antigreif.TileLog
import mindustry.client.navigation.Navigation
import mindustry.game.EventType.WorldLoadEvent

object Client {
    lateinit var tileLogs: Array<Array<TileLog?>>
    fun initialize() {
        Events.on(WorldLoadEvent::class.java) { tileLogs = Array(Vars.world.height()) { arrayOfNulls(Vars.world.width()) } }
    }

    fun update() {
        Navigation.update()
    }

    fun getLog(x: Int, y: Int): TileLog? {
        if (tileLogs[y][x] == null) {
            tileLogs[y][x] = TileLog(Vars.world.tile(x, y))
        }
        return tileLogs[y][x]
    }
}
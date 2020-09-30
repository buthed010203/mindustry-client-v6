package mindustry.client.antigreif

import arc.scene.Element
import arc.scene.ui.Label
import mindustry.gen.Unitc
import mindustry.world.Tile
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

open class TileLogItem(player: Unitc, tile: Tile, var time: Long, var additionalInfo: String) {
    var player: String = if (player.isPlayer) player.player.name else if (player.type() == null) "Null unit" else player.type().name
    var x = tile.x.toInt()
    var y = tile.y.toInt()

    protected open fun formatDate(date: String?, minutes: Long): String {
        return String.format("%s interacted with tile at %s UTC (%d minutes ago).  %s", player, date, minutes, additionalInfo)
    }

    fun format(): String {
        val instant: Instant = Instant.ofEpochSecond(time)
        val timezone = TimeZone.getTimeZone("UTC")
        val format = SimpleDateFormat("yyyy-MM-dd  HH:mm:ss")
        format.timeZone = timezone
        val formatted = format.format(Date.from(instant))
        val duration = Duration.between(instant, Instant.now())
        val minutes = duration[ChronoUnit.SECONDS] / 60L
        return formatDate(formatted, minutes)
    }

    fun toElement(): Element {
        return Label(format())
    }

}
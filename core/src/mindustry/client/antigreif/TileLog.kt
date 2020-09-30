package mindustry.client.antigreif

import arc.scene.ui.Label
import arc.scene.ui.layout.Table
import arc.struct.Seq
import mindustry.world.Tile
import java.util.function.Consumer

class TileLog(tile: Tile) {
    var log: Seq<TileLogItem> = Seq<TileLogItem>()
    var x = tile.x.toInt()
    var y = tile.y.toInt()
    fun addItem(item: TileLogItem?) {
        log.add(item)
    }

    fun toTable(): Table {
        val table = Table()
        if (log.isEmpty()) {
            table.add(Label(String.format("No logs for %d,%d", x, y)))
        } else {
            table.add(Label(String.format("Logs for %d,%d:", x, y)))
            log.forEach(Consumer { item: TileLogItem ->
                table.row()
                table.add(item.toElement())
            })
        }
        return table
    }

}
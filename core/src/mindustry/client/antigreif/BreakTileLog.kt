package mindustry.client.antigreif

import mindustry.gen.Unitc
import mindustry.world.Block
import mindustry.world.Tile

class BreakTileLog (player: Unitc, tile: Tile, time: Long, additionalInfo: String, var block: Block) : TileLogItem(player, tile, time, additionalInfo) {
    /**
     * Creates a TileLogItem.  time is unix time.
     */
    override fun formatDate(date: String?, minutes: Long): String {
        return String.format("%s broke %s at %s UTC (%d minutes ago).  %s", player, block.name, date, minutes, additionalInfo)
    }
}
package mindustry.client.navigation

interface Waypoint {
    val isDone: Boolean
    fun run()
    fun draw()
}
package mindustry.client.navigation

import arc.struct.Seq
import java.util.function.Consumer

interface Path {
    fun addListener(listener: Runnable?) {
        listeners.add(listener)
    }

    fun follow()
    fun progress(): Float
    val isDone: Boolean
        get() = progress() >= 0.99

    fun onFinish() {
        listeners.forEach(Consumer { obj: Runnable -> obj.run() })
    }

    fun draw() {}

    companion object {
        val listeners: Seq<Runnable> = Seq<Runnable>()
        const val show = false
    }
}
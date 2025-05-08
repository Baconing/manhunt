package ng.baconi.manhunt.listeners

import com.github.shynixn.mccoroutine.bukkit.launch
import ng.baconi.manhunt.Manhunt
import ng.baconi.manhunt.ManhuntManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.player.PlayerTeleportEvent

class PortalListener : Listener {
    @EventHandler
    fun onPlayerPortal(event: PlayerPortalEvent) {
        if (!ManhuntManager.manhuntActive) return
        if (event.from.world == null || !event.from.world!!.name.startsWith("manhunt_")) return

        when (event.cause) {
            PlayerTeleportEvent.TeleportCause.NETHER_PORTAL -> {
                when (event.from.world?.environment) {
                    World.Environment.NORMAL -> {
                        event.to?.world = Bukkit.getWorld("manhunt_nether")
                    }
                    World.Environment.NETHER -> {
                        val newTo = event.from.multiply(8.0)
                        newTo.world = Bukkit.getWorld("manhunt_world")
                        event.setTo(newTo)
                    }
                    else -> return
                }
            }
            PlayerTeleportEvent.TeleportCause.END_PORTAL -> {
                when (event.from.world?.environment) {
                    World.Environment.NORMAL -> {
                        val newTo = Location(Bukkit.getWorld("manhunt_end"), 100.0, 50.5, 0.0, -90F, 0F)
                        event.to?.world = Bukkit.getWorld("manhunt_end")
                        event.setTo(newTo)
                    }
                    World.Environment.THE_END -> {
                        val newTo: Location = event.player.bedSpawnLocation ?: Bukkit.getWorld("manhunt_world")?.spawnLocation ?: Bukkit.getWorlds().first().spawnLocation
                        event.setTo(newTo)
                        if (ManhuntManager.runner == event.player) {
                            event.isCancelled = true
                            Manhunt.instance.launch {
                                ManhuntManager.runnerWin()
                            }
                        }
                    }

                    else -> return
                }
            }
            else -> return
        }
    }
}
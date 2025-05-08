package ng.baconi.manhunt.listeners

import com.github.shynixn.mccoroutine.bukkit.launch
import ng.baconi.manhunt.Manhunt
import ng.baconi.manhunt.ManhuntManager
import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent

class DeathListener : Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        println(ManhuntManager.manhuntActive)
        if (!ManhuntManager.manhuntActive) return

        println(event.entity.player)
        println(ManhuntManager.runner)

        if (event.entity.player != null && event.entity.player == ManhuntManager.runner) {
            Manhunt.instance.launch {
                ManhuntManager.runnerLose(event.deathMessage)
            }
        }

        if (event.entity.bedSpawnLocation != null && !event.entity.bedSpawnLocation!!.world!!.name.startsWith("manhunt_")) {
            // If the player has a bed spawn location outside of the manhunt worlds, we reset it to null
            event.entity.bedSpawnLocation = null
        }
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        if (!ManhuntManager.manhuntActive) return

        if (event.player.bedSpawnLocation != null && !event.player.bedSpawnLocation!!.world!!.name.startsWith("manhunt_")) {
            // If the player has a bed spawn location outside of the manhunt worlds, we reset it to null
            event.player.bedSpawnLocation = null
            event.respawnLocation = Bukkit.getWorld("manhunt_world")?.spawnLocation ?: run {
                Manhunt.instance.logger.warning("Runner world not found, respawning in default world.")
                Bukkit.getWorlds().first().spawnLocation
            }
        }
    }
}
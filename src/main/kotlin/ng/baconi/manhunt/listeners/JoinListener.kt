package ng.baconi.manhunt.listeners

import ng.baconi.manhunt.Manhunt
import ng.baconi.manhunt.ManhuntManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class JoinListener : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!ManhuntManager.manhuntActive) event.player.gameMode = GameMode.SPECTATOR
        if (ManhuntManager.manhuntActive && event.player.location.world?.name?.startsWith("manhunt_") != true && !ManhuntManager.runnerTeamLimbo && !ManhuntManager.huntersLimbo) {
            event.player.isInvulnerable = true
            // If the player joins while manhunt is active and is not in a manhunt world, we teleport them to the main world
            if (event.player.bedSpawnLocation != null && event.player.bedSpawnLocation!!.world?.name?.startsWith("manhunt_") != true) {
                event.player.teleport(event.player.bedSpawnLocation!!)
            } else {
                if (Bukkit.getWorld("manhunt_world")?.spawnLocation == null) {
                    event.player.kickPlayer("a shitty ass join player bug hhappened dm baocn buts its probably ggs for you")
                    return
                }
                event.player.teleport(Bukkit.getWorld("manhunt_world")?.spawnLocation!!)
            }
            event.player.isInvulnerable = false
            event.player.gameMode = GameMode.SURVIVAL
        }
    }

}
package ng.baconi.manhunt.listeners

import ng.baconi.manhunt.ManhuntManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerMoveEvent

class LimboListener : Listener {
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (!ManhuntManager.manhuntActive) return

        if (event.to != null && event.to?.world != event.from.world) return

        if ((ManhuntManager.runnerTeamLimbo && ManhuntManager.runnerTeam.contains(event.player)) ||
            (ManhuntManager.huntersLimbo && ManhuntManager.hunters.contains(event.player))) {
            // Prevent movement if in limbo
            event.isCancelled = true
            return
        }
    }


    @EventHandler(ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!ManhuntManager.manhuntActive) return

        if ((ManhuntManager.runnerTeamLimbo && ManhuntManager.runnerTeam.contains(event.player)) ||
            (ManhuntManager.huntersLimbo && ManhuntManager.hunters.contains(event.player))) {
            // Prevent movement if in limbo
            event.isCancelled = true
            return
        }
    }

}
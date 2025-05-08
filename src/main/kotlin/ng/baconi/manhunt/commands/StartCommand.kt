package ng.baconi.manhunt.commands

import com.github.shynixn.mccoroutine.bukkit.launch
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import ng.baconi.manhunt.Manhunt
import ng.baconi.manhunt.ManhuntManager
import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission
import kotlin.time.Duration.Companion.minutes

class StartCommand {
    @Command("manhunt start")
    @CommandPermission("manhunt.start")
    fun onStartCommand(actor: BukkitCommandActor, headstartMinutes: Double, coordinatesAnnouncementMinutes: Double, runner: Player, helpers: List<Player>) {
        if (ManhuntManager.manhuntActive) {
            actor.reply(Component.text("Manhunt is already active, use /manhunt stop to end it.", NamedTextColor.RED))
            return
        }

        val startingComponent = Component.text("Starting Manhunt...").color(NamedTextColor.GREEN)
            .append(
                Component.join(JoinConfiguration.newlines(), listOf(
                    Component.text("\tHeadstart: $headstartMinutes minutes, "),
                    Component.text("\tCoordinates announcement: every $coordinatesAnnouncementMinutes minutes, "),
                    Component.text("\tRunner: ${runner.name}, "),
                    Component.text("\tHelpers: ${helpers.joinToString(", ") { it.name }}"),
                )).style(Style.style(TextDecoration.ITALIC).color(NamedTextColor.GRAY))
            )

        actor.reply(startingComponent.append(Component.text("Generating world...")))
        Manhunt.instance.adventure.permission(Key.key("manhunt.admin")).sendMessage(Component.text("${actor.name()}: ", NamedTextColor.GRAY).append(startingComponent))

        // Call out to Manhunt Manager
        try {
            Manhunt.instance.launch {
                ManhuntManager.startManhunt(
                    headstartMinutes.minutes,
                    coordinatesAnnouncementMinutes.minutes,
                    runner,
                    helpers
                )
            }
        } catch (e: Exception) {
            actor.reply(Component.text("Error starting manhunt: ${e.message}", NamedTextColor.RED))
            return
        }

        val startedComponent = Component.text("Manhunt started... good luck.", NamedTextColor.GREEN)
        actor.reply(startedComponent)
        Manhunt.instance.adventure.permission(Key.key("manhunt.admin")).sendMessage(Component.text("${actor.name()}: ", NamedTextColor.GRAY).append(startedComponent))
    }
}
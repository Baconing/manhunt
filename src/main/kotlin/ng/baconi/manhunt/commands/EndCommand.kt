package ng.baconi.manhunt.commands

import com.github.shynixn.mccoroutine.bukkit.launch
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import ng.baconi.manhunt.Manhunt
import ng.baconi.manhunt.ManhuntManager
import revxrsal.commands.annotation.Command
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.bukkit.annotation.CommandPermission

class EndCommand {
    @Command("manhunt end")
    @CommandPermission("manhunt.end")
    fun onEndCommand(actor: BukkitCommandActor) {
        if (!ManhuntManager.manhuntActive) {
            actor.reply("Manhunt is not currently active.")
            return
        }

        Manhunt.instance.launch {
            ManhuntManager.endManhunt()
        }

        val component = Component.text("Manhunt is ending...", NamedTextColor.GREEN)
        actor.reply(component)
        Manhunt.instance.adventure.permission(Key.key("manhunt.admin")).sendMessage(Component.text("${actor.name()}:", NamedTextColor.GRAY).append(component))
    }
}
package ng.baconi.manhunt

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import ng.baconi.manhunt.commands.EndCommand
import ng.baconi.manhunt.commands.StartCommand
import ng.baconi.manhunt.listeners.DeathListener
import ng.baconi.manhunt.listeners.JoinListener
import ng.baconi.manhunt.listeners.LimboListener
import ng.baconi.manhunt.listeners.PortalListener
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import revxrsal.commands.bukkit.BukkitLamp

class Manhunt : SuspendingJavaPlugin() {
    lateinit var adventure: BukkitAudiences
        private set

    override fun onEnable() {
        instance = this

        ManhuntManager

        val lamp = BukkitLamp.builder(this).build()
        lamp.register(StartCommand(), EndCommand())

        adventure = BukkitAudiences.create(this)

        Bukkit.getPluginManager().registerEvents(DeathListener(), this)
        Bukkit.getPluginManager().registerEvents(JoinListener(), this)
        Bukkit.getPluginManager().registerEvents(LimboListener(), this)
        Bukkit.getPluginManager().registerEvents(PortalListener(), this)
    }

    override fun onDisable() {
        ManhuntManager.cleanupWorlds()
        if (this::adventure.isInitialized) {
            this.adventure.close();
        }
    }

    companion object {
        lateinit var instance: Manhunt
    }
}

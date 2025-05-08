package ng.baconi.manhunt

import com.github.shynixn.mccoroutine.bukkit.launch
import kotlinx.coroutines.delay
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


object ManhuntManager {
    var manhuntActive: Boolean = false
        private set

    var runnerTeamLimbo: Boolean = true

    var huntersLimbo: Boolean = true

    var runner: Player? = null
        private set

    var helpers: List<Player> = listOf()
        private set

    val runnerTeam: List<Player>
        get() {
            if (runner == null) {
                return emptyList()
            }
            return helpers + runner!!
        }

    val hunters: List<Player>
        get() = Bukkit.getOnlinePlayers().filter { !helpers.contains(it) && it != runner }

    init {
        // Delete old manhunt worlds
        cleanupWorlds()
    }

    fun cleanupWorlds() {
        Bukkit.getWorld("manhunt_world")?.let { world ->
            Bukkit.unloadWorld(world, false)
            world.worldFolder.deleteRecursively()
        }

        Bukkit.getWorld("manhunt_nether")?.let { world ->
            Bukkit.unloadWorld(world, false)
            world.worldFolder.deleteRecursively()
        }

        Bukkit.getWorld("manhunt_end")?.let { world ->
            Bukkit.unloadWorld(world, false)
            world.worldFolder.deleteRecursively()
        }
    }

    suspend fun startManhunt(
        headstart: Duration,
        coordinateAnnouncement: Duration,
        runner: Player,
        helpers: List<Player>
    ) {
        if (manhuntActive) {
            throw IllegalStateException("Manhunt is already active.")
        }

        manhuntActive = true

        val normalWorld = WorldCreator("manhunt_world").environment(World.Environment.NORMAL).generator("natural").generateStructures(true).createWorld()
        val netherWorld = WorldCreator("manhunt_nether").environment(World.Environment.NETHER).generator("natural").generateStructures(true).createWorld()
        val endWorld = WorldCreator("manhunt_end").environment(World.Environment.THE_END).generator("natural").generateStructures(true).createWorld()

        if (normalWorld == null || netherWorld == null || endWorld == null) {
            throw IllegalStateException("Failed to create worlds for manhunt.")
        }

        // Set the runner and helpers
        this.runner = runner
        this.helpers = helpers

        // Set limbo state
        runnerTeamLimbo = true
        huntersLimbo = true

        // Reset all players' inventories and health
        Bukkit.getOnlinePlayers().forEach { player ->
            player.health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
            player.foodLevel = 20
            player.inventory.clear()
            player.isInvulnerable = true
            Bukkit.getServer().advancementIterator().forEach { advancement ->
                val progress = player.getAdvancementProgress(advancement)
                progress.awardedCriteria.forEach { criterion ->
                    progress.revokeCriteria(criterion)
                }
            }
        }

        // Teleport runner team
        runner.teleport(normalWorld.spawnLocation)
        helpers.forEach { it.teleport(normalWorld.spawnLocation) }

        countdown(5.seconds)

        // Free runner team from limbo
        runnerTeamLimbo = false
        runnerTeam.forEach {
            it.isInvulnerable = false
            it.gameMode = GameMode.SURVIVAL
        }

        startHeadstartTimer(headstart)

        // Teleport and free hunters from limbo
        hunters.forEach { hunter ->
            Manhunt.instance.adventure.players().sendMessage(Component.text("teleporting $hunter"))
            hunter.teleport(normalWorld.spawnLocation)
            hunter.isInvulnerable = false
            hunter.gameMode = GameMode.SURVIVAL
        }
        huntersLimbo = false

        val coordinatesBossbar = BossBar.bossBar(
            Component.text("Coordinates: $coordinateAnnouncement"),
            (coordinateAnnouncement / headstart).coerceIn(0.0, 1.0).toFloat(),
            BossBar.Color.GREEN,
            BossBar.Overlay.PROGRESS
        )

        Manhunt.instance.adventure.players().showBossBar(coordinatesBossbar)
        Manhunt.instance.adventure.players().sendMessage(Component.text("starting the task"))
        // Launch our task for announcing coordinates
        Manhunt.instance.launch {
            Manhunt.instance.adventure.players().sendMessage(Component.text("started task"))
            var lastCoordinates: Location? = null

            var timeSinceLastAnnouncement = 0.seconds
            var timeLeft = coordinateAnnouncement
            while (manhuntActive) {
                coordinatesBossbar.name(Component.text("Coordinates: $timeLeft"))
                coordinatesBossbar.progress((timeLeft / coordinateAnnouncement).coerceIn(0.0, 1.0).toFloat())

                for (hunter in hunters) {
                    if (lastCoordinates != null) {
                        val color = if (timeSinceLastAnnouncement <= 1.5.minutes) NamedTextColor.GREEN
                        else if (timeSinceLastAnnouncement > 1.5.minutes) NamedTextColor.YELLOW
                        else NamedTextColor.RED

                        Manhunt.instance.adventure.player(hunter).sendActionBar(Component.text("Coordinates: ${lastCoordinates.blockX}, ${lastCoordinates.blockY}, ${lastCoordinates.blockZ} (${lastCoordinates.world?.environment?.name?.lowercase()?.capitalize()?.replace("_", " ") ?: "Unknown Dimension"})", color))
                    }
                }

                if (timeLeft <= 5.seconds && timeLeft.isPositive()) {
                    // Count down aggressively for the last 5 seconds
                    Manhunt.instance.adventure.players().showTitle(Title.title(Component.text(timeLeft.inWholeSeconds.toString(), NamedTextColor.RED), Component.empty()))
                    Manhunt.instance.adventure.players().playSound(Sound.sound(Key.key("entity.experience_orb.pickup"), Sound.Source.MASTER, 1.0f, 1.0f))
                } else if (!timeLeft.isPositive()) {
                    // Announce coordinates every coordinateAnnouncement duration
                    lastCoordinates = runner.location
                    timeSinceLastAnnouncement = 0.seconds
                    timeLeft = coordinateAnnouncement

                    Manhunt.instance.adventure.players().sendMessage(
                        Component.text(
                            "Coordinates: ${lastCoordinates.blockX}, ${lastCoordinates.blockY}, ${lastCoordinates.blockZ} (${lastCoordinates.world?.environment?.name?.lowercase()?.capitalize()?.replace("_", " ") ?: "Unknown Dimension"})",
                            NamedTextColor.GREEN
                        )
                    )
                    Manhunt.instance.adventure.players()
                        .playSound(Sound.sound(Key.key("entity.player.levelup"), Sound.Source.MASTER, 1.0f, 1.0f))
                }
                timeSinceLastAnnouncement += 1.seconds
                timeLeft -= 1.seconds
                delay(1.seconds)
            }
            Manhunt.instance.adventure.players().hideBossBar(coordinatesBossbar)
        }
    }

    suspend fun endManhunt() {
        genericWin(Title.title(Component.text("Nobody wins!", NamedTextColor.RED), Component.text("Manhunt was manually ended.", NamedTextColor.GRAY)))
    }

    private suspend fun genericWin(title: Title) {
        if (!manhuntActive) return

        manhuntActive = false
        runnerTeamLimbo = true
        huntersLimbo = true

        Bukkit.getOnlinePlayers().forEach { player -> player.gameMode = GameMode.SPECTATOR }
        Manhunt.instance.adventure.players().showTitle(title)
        repeat(4) {
            Manhunt.instance.adventure.players().playSound(Sound.sound(Key.key("block.bell.use"), Sound.Source.MASTER, 24.0f, 1.0f))
            Manhunt.instance.adventure.players().playSound(Sound.sound(Key.key("block.bell.resonate"), Sound.Source.MASTER, 1.0f, 1.0f))
            delay(500L)
        }

        var returnDuration = 15.seconds
        while (returnDuration.isPositive()) {
            Manhunt.instance.adventure.players().sendActionBar(Component.text("Returning in $returnDuration...", NamedTextColor.GRAY))
            returnDuration -= 1.seconds
            delay(1.seconds)
        }

        // cant run it in async scope
        Bukkit.getScheduler().run {
            Bukkit.getOnlinePlayers().forEach {
                it.teleport(Bukkit.getWorlds().first().spawnLocation)
            }
        }

        Manhunt.instance.adventure.permission(Key.key("manhunt.admin")).sendMessage(Component.text("Cleaning up worlds...", NamedTextColor.GRAY))
        cleanupWorlds()
        Manhunt.instance.adventure.permission(Key.key("manhunt.admin")).sendMessage(Component.text("Cleaned worlds...ready for new game.", NamedTextColor.GRAY))
    }

    suspend fun runnerWin() = genericWin(Title.title(Component.text("${runner?.name} won!", NamedTextColor.GREEN), Component.empty()))

    suspend fun runnerLose(deathMessage: String?) {
        val subtitle: Component = deathMessage?.let { Component.text(deathMessage, NamedTextColor.GRAY) } ?: Component.empty()
        genericWin(Title.title(Component.text("Hunters win!", NamedTextColor.GREEN), subtitle))
    }

    private suspend fun countdown(duration: Duration) {
        var timeLeft = duration

        while (timeLeft.isPositive()) {
            Manhunt.instance.adventure.players().showTitle(Title.title(Component.text(timeLeft.inWholeSeconds.toString(), NamedTextColor.RED), Component.empty()))
            timeLeft -= 1.seconds
            delay(1.seconds)
        }

        Manhunt.instance.adventure.players().clearTitle()
    }

    private suspend fun startHeadstartTimer(headstart: Duration) {
        var timeLeft = headstart
        val bossbar = BossBar.bossBar(Component.text("Headstart: $timeLeft"), (timeLeft / headstart).toFloat(), BossBar.Color.GREEN, BossBar.Overlay.PROGRESS)
        Manhunt.instance.adventure.players().showBossBar(bossbar)

        while (timeLeft.isPositive()) {
            if (timeLeft <= 5.seconds) {
                Manhunt.instance.adventure.players().showTitle(Title.title(Component.text("Headstart ends in $timeLeft", NamedTextColor.RED), Component.empty()))
            } else {
                for (hunter in hunters) {
                    Manhunt.instance.adventure.player(hunter).showTitle(Title.title(Component.text(timeLeft.inWholeSeconds.toString(), NamedTextColor.RED), Component.empty()))
                }
            }

            bossbar.name(Component.text("Headstart: $timeLeft"))
            bossbar.progress((timeLeft / headstart).coerceIn(0.0, 1.0).toFloat())

            timeLeft -= 1.seconds
            delay(1.seconds)
        }

        Manhunt.instance.adventure.players().clearTitle()
        Manhunt.instance.adventure.players().hideBossBar(bossbar)
    }
}
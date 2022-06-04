package dev.emortal.backrooms

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.event.player.*
import net.minestom.server.extras.MojangAuth
import net.minestom.server.extras.bungee.BungeeCordProxy
import net.minestom.server.extras.velocity.VelocityProxy
import net.minestom.server.instance.Chunk
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType
import world.cepi.sabre.server.ConfigurationHelper.initConfigFile
import java.nio.file.Path
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

val messages = setOf(
    "Are you there?",
    "Can anyone hear me?",
    "Is anyone seeing this?",
    "I think something is following me.",
    "Did you hear that?",
    "I don't know how long I've been in here."
)

fun main() {
    println("Spawning backrooms...")
    val server = MinecraftServer.init()

    val config = initConfigFile(Path.of("./config.json"), Config())

    when (config.proxy) {
        Forwarder.BUNGEE -> BungeeCordProxy.enable()
        Forwarder.VELOCITY -> VelocityProxy.enable(config.velocitySecret)
        else -> MojangAuth.init()
    }

    val instanceManager = MinecraftServer.getInstanceManager()
    val dimensionManager = MinecraftServer.getDimensionTypeManager()
    val globalEvent = MinecraftServer.getGlobalEventHandler()

    val backroomsDimension = DimensionType.builder(NamespaceID.from("backrooms"))
        .ambientLight(0.04f)
        .bedSafe(false)
        .build()

    dimensionManager.addDimension(backroomsDimension)

    val backroomsInstance = instanceManager.createInstanceContainer(backroomsDimension)
    backroomsInstance.timeRate = 0
    backroomsInstance.timeUpdate = null
    backroomsInstance.setGenerator {
        BackroomsGenerator.generateRoom(it, 50)
    }

    val playerScopeMap = ConcurrentHashMap<UUID, CoroutineScope>()

    globalEvent.addListener(PlayerBlockBreakEvent::class.java) {
        it.isCancelled = true
    }

    globalEvent.addListener(PlayerChatEvent::class.java) {
        it.isCancelled = true
    }

    globalEvent.addListener(PlayerLoginEvent::class.java) {
        playerScopeMap[it.player.uuid] = CoroutineScope(Dispatchers.IO)

        it.setSpawningInstance(backroomsInstance)

        val rand = ThreadLocalRandom.current()
        var validSpawn = false
        val pos = Pos(rand.nextInt(-24_494, 24_494).toDouble(), -13.0, rand.nextInt(-24_494, 24_494).toDouble())
        var smallerPos = Pos.ZERO
        backroomsInstance.loadChunk(pos).thenAccept {
            while (!validSpawn) {
                smallerPos = Pos(rand.nextInt(0, Chunk.CHUNK_SIZE_X).toDouble(), -13.0, rand.nextInt(0, Chunk.CHUNK_SIZE_Z).toDouble())
                if (it.getBlock(smallerPos).isAir) {
                    validSpawn = true
                }
            }
        }
        
        it.player.respawnPoint = pos.add(smallerPos)
        //it.player.isAutoViewable = false

        it.player.gameMode = GameMode.ADVENTURE

    }

    globalEvent.addListener(PlayerMoveEvent::class.java) {
        it.player.sendMessage("minX: ${it.player.boundingBox.minX()}, maxX: ${it.player.boundingBox.maxX()}, width: ${it.player.boundingBox.width()}")
        it.player.sendMessage("proper width: ${it.player.boundingBox.maxX() - it.player.boundingBox.minZ()}")
    }

    globalEvent.addListener(PlayerSpawnEvent::class.java) {
        val playerScope = playerScopeMap[it.player.uuid]!!

        it.player.health = 1f
        it.player.food = 0
        it.player.foodSaturation = 0f

        object : MinestomRunnable(coroutineScope = playerScope, repeat = Duration.ofMillis(43150)) {
            override suspend fun run() {
                it.player.playSound(Sound.sound(SoundEvent.AMBIENT_BASALT_DELTAS_LOOP, Sound.Source.MASTER, 0.45f, 1f), Sound.Emitter.self())
            }
        }

        object : MinestomRunnable(coroutineScope = playerScope, repeat = Duration.ofMinutes(7)) {
            override suspend fun run() {
                val sounds = listOf(SoundEvent.AMBIENT_BASALT_DELTAS_MOOD, SoundEvent.AMBIENT_CAVE)
                it.player.playSound(Sound.sound(sounds.random(), Sound.Source.MASTER, 1f, 0.3f), Sound.Emitter.self())
            }
        }

        object : MinestomRunnable(coroutineScope = playerScope, repeat = Duration.ofMillis(500), delay = Duration.ofMillis(500), iterations = 15) {
            val chars = "abcdefghijkl-!\\/.,  !!  ,. ,  ABCDHIJKLWXYZ"

            override suspend fun run() {
                val sb = StringBuilder()
                repeat(25) {
                    sb.append(chars.random())
                }
                it.player.sendActionBar(
                    Component.text(sb.toString(), NamedTextColor.YELLOW).decorate(
                    TextDecoration.OBFUSCATED))
            }

            override fun cancelled() {
                it.player.sendActionBar(Component.text(messages.random(), NamedTextColor.RED))
                it.player.playSound(Sound.sound(SoundEvent.ENTITY_BLAZE_SHOOT, Sound.Source.MASTER, 0.2f, 0.2f), Sound.Emitter.self())
            }
        }
    }

    globalEvent.addListener(PlayerDisconnectEvent::class.java) {
        playerScopeMap[it.player.uuid]?.cancel()
        playerScopeMap.remove(it.player.uuid)
    }

    server.start(config.ip, config.port)
}
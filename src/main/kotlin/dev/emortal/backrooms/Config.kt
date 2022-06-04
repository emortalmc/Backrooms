package dev.emortal.backrooms


enum class Forwarder {
    BUNGEE,
    VELOCITY,
    NONE
}

@kotlinx.serialization.Serializable
class Config(
    val ip: String = "0.0.0.0",
    val port: Int = 25565,
    val proxy: Forwarder = Forwarder.NONE,
    val velocitySecret: String = "",
    val compressionThreshold: Int = 256,

) {

    companion object {

        // Allows for custom config setting during boot.
        private var _config: Config? = null

        var config: Config
            get() = _config ?: run {
                throw IllegalArgumentException("Config does not exist! Set it correctly or boot from a file.")
            }
            set(value) {
                _config = value
            }
    }

}

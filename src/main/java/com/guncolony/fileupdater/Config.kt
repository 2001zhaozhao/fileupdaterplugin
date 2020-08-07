package com.guncolony.fileupdater

import org.bukkit.Bukkit
import java.util.logging.Level

object Config {
    private val plugin: FileUpdater get() = FileUpdater.instance

    val instanceMap: HashMap<String, Instance> = HashMap()
    val pathMap: HashMap<String, List<Path>> = HashMap()

    fun loadConfig() {
        plugin.reloadConfig()
        plugin.saveDefaultConfig()

        val config = plugin.config
        val instancesSection = config.getConfigurationSection("Instances")
        val pathsSection = config.getConfigurationSection("Paths")

        instanceMap.clear()
        instancesSection.getKeys(false).forEach{
            try {
                val inst = Instances.getInstanceFromConfig(instancesSection.getConfigurationSection(it), it)
                if (inst != null) instanceMap[it] = inst
            } catch (ex: Exception) {
                Bukkit.getLogger().log(Level.WARNING, "Exception when loading config for host $it", ex)
            }
        }

        instanceMap.clear()
        pathsSection.getKeys(false).forEach{
            pathMap[it] = Paths.getPathsFromConfig(pathsSection.getStringList(it))
        }
    }

}
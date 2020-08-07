package com.guncolony.fileupdater

object Config {
    private val plugin: FileUpdater get() = FileUpdater.instance

    val instanceMap: HashMap<String, Instance> = HashMap()
    val pathMap: HashMap<String, List<Path>> = HashMap()

    fun loadConfig() {
        plugin.saveDefaultConfig()

        val config = plugin.config
        val instancesSection = config.getConfigurationSection("Instances")
        val pathsSection = config.getConfigurationSection("Paths")

        instanceMap.clear()
        instancesSection.getKeys(false).forEach{
            val inst = Instances.getInstanceFromConfig(instancesSection.getConfigurationSection(it), it)
            if(inst != null) instanceMap[it] = inst
        }

        instanceMap.clear()
        pathsSection.getKeys(false).forEach{
            pathMap[it] = Paths.getPathsFromConfig(pathsSection.getStringList(it))
        }
    }

}
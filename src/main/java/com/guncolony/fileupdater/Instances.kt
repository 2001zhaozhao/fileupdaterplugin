package com.guncolony.fileupdater

import org.apache.commons.vfs2.FileObject
import org.bukkit.configuration.ConfigurationSection

object Instances {
    fun getInstanceFromConfig(config: ConfigurationSection, name: String): Instance? {
        // SFTP instance
        if(config.getString("type").equals("sftp", ignoreCase = true)) {
            return object : RemoteInstance(config, name) {
                override val connectionUrl: String get() {
                    val ip = config.getString("ip")
                    val port = config.getString("port")
                    val username = config.getString("username")
                    val password = config.getString("password")
                    return "sftp://$username:$password@$ip:$port"}
            }
        }
        return null
    }
}

object LocalInstance: Instance {
    // Gets the current server's root folder
    override val directory: FileObject
        get() = FileUpdater.instance.fileSystemManager.resolveFile(FileUpdater.instance.server.worldContainer.toURI())

    override fun toString(): String = "Local Instance"
}

interface Instance {
    /**
     * Supply a list of files from a list of paths. The resulting map will only contain files that are valid.
     */
    fun supplyFiles(paths: Iterable<Path>): HashMap<Path, FileObject> {
        val dir = directory
        val map = HashMap<Path, FileObject>()
        paths.forEach{val path = it.supply(dir); if(path != null) map[it] = path}
        return map
    }

    /**
     * Get a list of file paths to receive files to.
     * It will only check for validity if the path specifies itself to be optional, i.e. the file must already be
     * valid on the receiving end to receive updates.
     */
    fun getFilePaths(paths: Iterable<Path>): HashMap<Path, FileObject> {
        val dir = directory
        val map = HashMap<Path, FileObject>()
        paths.forEach{val path = if(it.optional) it.supply(dir) else it.getPath(dir); if(path != null) map[it] = path}
        return map
    }

    /**
     * FileObject representing the server root folder
     */
    val directory: FileObject
}

abstract class RemoteInstance(config: ConfigurationSection, val name: String): Instance {
    /**
     * Gets the connection URL string used in
     */
    abstract val connectionUrl: String

    /**
     * Gets the base path of the server folder
     */
    val path: String get() = if(configPath.endsWith("/")) configPath else "$configPath/"

    val configPath: String = config.getString("path")

    val remoteRootDirectory get() = FileUpdater.instance.fileSystemManager.resolveFile(connectionUrl)
    override val directory = remoteRootDirectory.resolveFile(path)

    override fun toString(): String = name
}
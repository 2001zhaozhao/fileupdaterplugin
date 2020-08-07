package com.guncolony.fileupdater

import org.apache.commons.vfs2.FileObject
import org.bukkit.configuration.ConfigurationSection

private val plugin: FileUpdater get() = FileUpdater.instance

object Paths {
    fun getPathsFromConfig(config: List<String>): List<Path> = config.mapNotNull{getPathFromString(it)}

    fun getPathFromString(string: String): Path? {
        var str = string
        // Modifiers
        var optional = false
        var default = false
        var skipsimilar = false
        while(str.indexOf(' ') >= 0) {
            val space = str.indexOf(' ')
            val modifier = str.substring(0, space)
            str = str.substring(space + 1)
            when(modifier) {
                "optional" -> optional = true
                "default" -> default = true
                "skipsimilar" -> skipsimilar = true
                "file" -> {
                    return FilePath(str, optional = optional, default = default, skipsimilar = skipsimilar)
                }
                "folder" -> {
                    return DirectoryPath(str, optional = optional, default = default, skipsimilar = skipsimilar)
                }
            }
        }
        return null
    }
}

abstract class Path(val default: Boolean, val optional: Boolean, val skipsimilar: Boolean) {
    /**
     * Supplies the file object given the base path. Returns null if it does not exist or not the right type
     */
    abstract fun supply(dir: FileObject): FileObject?

    /**
     * Gets the file pbject at the base path regardless of whether or not it exists, this is used to get a path
     * to receive files
     */
    abstract fun getPath(dir: FileObject): FileObject
}

/**
 * Directory path that represents a folder
 */
class DirectoryPath(val path: String, optional: Boolean = false, default: Boolean = false, skipsimilar: Boolean = false)
    : Path(default = default, optional = optional, skipsimilar = skipsimilar) {
    override fun supply(dir: FileObject): FileObject? {
        val file = dir.resolveFile(path)
        return if(file.exists() && file.isFolder) file else null
    }

    override fun getPath(dir: FileObject): FileObject = dir.resolveFile(path)

    override fun hashCode(): Int = path.hashCode()
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DirectoryPath

        if (path != other.path) return false

        return true
    }
}

/**
 * File path that represents a file
 */
class FilePath(val path: String, optional: Boolean = false, default: Boolean = false, skipsimilar: Boolean = false)
    : Path(default = default, optional = optional, skipsimilar = skipsimilar) {
    override fun supply(dir: FileObject): FileObject? {
        val file = dir.resolveFile(path)
        return if(file.exists() && file.isFile) file else null
    }

    override fun getPath(dir: FileObject): FileObject = dir.resolveFile(path)

    override fun hashCode(): Int = path.hashCode()
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FilePath

        if (path != other.path) return false

        return true
    }
}
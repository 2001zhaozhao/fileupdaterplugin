package com.guncolony.fileupdater

import org.apache.commons.vfs2.AllFileSelector
import org.apache.commons.vfs2.FileSelectInfo
import org.apache.commons.vfs2.FileSelector
import org.apache.commons.vfs2.impl.StandardFileSystemManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.util.logging.Level


const val prefix: String = "\u00A7c[FileUpdater] \u00A7f"

class FileUpdater: JavaPlugin() {
    var fileSystemManager: StandardFileSystemManager = StandardFileSystemManager()

    companion object {
        lateinit var instance: FileUpdater
    }

    override fun onEnable() {
        // Load the config file
        Config.loadConfig()
        instance = this
        fileSystemManager = StandardFileSystemManager()
        fileSystemManager.init()
    }

    override fun onDisable() {

    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        try {
            if(args.isEmpty()) {
                return false
            }
            when(args[0]) {
                "update" -> {
                    val paths = getConfigPathList(args, 1)
                    sender.sendMessage("$prefix Starting transfer of files")
                    // Perform the file copy operation on all remote instances
                    object : BukkitRunnable() {
                        override fun run() =
                                sendFiles(LocalInstance, Config.instanceMap.values.toList(), paths, sender)
                    }.runTaskAsynchronously(this)
                }
            }
            return false
        }
        catch (ex: Exception) {
            Bukkit.getLogger().log(Level.WARNING, "", ex)
            sender.sendMessage("$prefix \u00A74Command error: \u00A7f" + ex.message)
            return true
        }
    }

    /**
     * Gets a path list from command arguments. If no arguments then it will use the default paths
     */
    fun getConfigPathList(args: Array<String>, startIndex: Int): List<Path> {
        val paths: MutableList<Path>
        if(args.size == startIndex) {
            // Get all paths set as default
            paths = Config.pathMap.values.flatten().filter{it.default}.toMutableList()
        }
        else {
            // Get all paths from the further arguments
            paths = ArrayList()
            for(i in startIndex until args.size) {
                val path = Config.pathMap[args[i]]
                if(path == null) {
                    throw IllegalArgumentException("Path invalid: " + args[i])
                }
                paths.addAll(path)
            }
        }
        return paths
    }

    /**
     * Send files from the source instance to a list of destination instances.
     */
    fun sendFiles(from: Instance, to: List<Instance>, paths: List<Path>, sender: CommandSender?) {
        val fromFiles = from.supplyFiles(paths)
        to.forEach{instance ->
            // Run an async task for each copy operation because why not?
            object : BukkitRunnable() {
                override fun run() {
                    sender?.sendMessage("$prefix Transfering from $from to $instance...")
                    val toFiles = instance.getFilePaths(fromFiles.keys)

                    // Get a list of path | fromfile | tofile triples
                    val triples = toFiles.map{Triple(it.key, fromFiles[it.key]!!, it.value)}

                    // Copy the files! (API crunching)
                    triples.forEach{(path, from, to) ->
                        to.copyFrom(from, if(path.skipsimilar)
                            object : FileSelector {
                                override fun includeFile(fileInfo: FileSelectInfo): Boolean {
                                    if(fileInfo.depth == 0) return true

                                    val content = fileInfo.file.content
                                    // Get the file we are writing to
                                    // The URL is the extra part of the url past the folder that we are traversing in
                                    // We also remove the slash after the folder name so that it is a relative path
                                    val toFile = to.resolveFile(
                                            fileInfo.file.url.path.substring(from.url.path.length+1))

                                    val toContent = toFile.content

                                    // Copy the file if the from file is more recent or the file size changed
                                    return content.lastModifiedTime > toContent.lastModifiedTime
                                            || content.size != toContent.size
                                }

                                override fun traverseDescendents(fileInfo: FileSelectInfo?): Boolean = true
                            }
                            else AllFileSelector())
                    }

                    sender?.sendMessage("$prefix ${triples.size} files transferred from $from to $instance")
                }
            }.runTaskAsynchronously(this)
        }
    }

}
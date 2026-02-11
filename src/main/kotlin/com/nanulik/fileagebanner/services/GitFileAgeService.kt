package com.nanulik.fileagebanner.services

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.nanulik.fileagebanner.helpers.AgeStatus
import git4idea.repo.GitRepositoryManager
import git4idea.config.GitExecutableManager
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * @author Nane Petrosyan
 * 11.02.26
 */
@Service(Service.Level.PROJECT)
class GitFileAgeService(private val project: Project) {

    private data class CacheEntry(val epochSeconds: Long?, val cachedAtMs: Long)

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val ttlMs = 5 * 60 * 1000L

    fun requestUpdate(file: VirtualFile, onResult: (String) -> Unit) {
        val key = file.path
        val now = System.currentTimeMillis()

        cache[key]?.let { entry ->
            if (now - entry.cachedAtMs <= ttlMs) {
                onResult(format(entry.epochSeconds))
                return
            }
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            val epoch = getLastCommitEpochSeconds(file)
            cache[key] = CacheEntry(epoch, System.currentTimeMillis())
            onResult(format(epoch))
        }
    }

    private fun getLastCommitEpochSeconds(file: VirtualFile): Long? {
        val repo = GitRepositoryManager.getInstance(project).getRepositoryForFileQuick(file) ?: return null
        val repoRoot = repo.root

        val gitExe = GitExecutableManager.getInstance().getPathToGit(project)
        val relativePath = file.path.removePrefix(repoRoot.path).removePrefix("/")

        val cmd = GeneralCommandLine(gitExe)
            .withWorkDirectory(repoRoot.path)
            .withParameters("log", "-1", "--format=%ct", "--", relativePath)

        val output = CapturingProcessHandler(cmd).runProcess(5_000)
        if (output.exitCode != 0) return null

        val s = output.stdout.trim()
        return s.toLongOrNull()
    }

    private fun format(epochSeconds: Long?): String {
        if (epochSeconds == null) return "untracked 🥚"

        val now = Instant.now()
        val then = Instant.ofEpochSecond(epochSeconds)
        val days = max(0, (now.epochSecond - then.epochSecond) / 86400)

        val status = AgeStatus.fromDays(days)
        return "${status.label}  •  last updated ${status.human(days)}"
    }

    companion object {
        fun getInstance(project: Project): GitFileAgeService =
            project.getService(GitFileAgeService::class.java)
    }
}

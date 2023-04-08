/*
 * Copyright (c) 2009 - 2023 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */

package org.candlepin.gradle.release

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

abstract class MailTask : DefaultTask() {

    @TaskAction
    fun execute() {
        val currentTag = currentTag()
        val previousTag = previousTag(currentTag)
        val changeSet = changes(previousTag, currentTag)

//        val urls = gatherUrlsToBrew(currentTag)
        val urls = listOf(
            "el9 link: https://brewweb.engineering.redhat.com/brew/taskinfo?taskID=9879897"
        )
        val buildChanges = changeSet.buildChanges()
        val dependencyChanges = changeSet.dependencyChanges()

        val mailBody = MailBody(
            urls,
            buildChanges,
            dependencyChanges
        )
        logger.lifecycle(mailBody.asText())
        // TODO send the mail
    }

    private fun gatherUrlsToBrew(currentTag: Tag) : Map<String, String> {
        return currentTag.version().supportedOsVersions()
            .map { it to brewTaskIdOf(currentTag, it) }
            .associate { it.first to "https://brewweb.engineering.redhat.com/brew/taskinfo?taskID=${it.second}" }
    }

    private fun brewTaskIdOf(tag: Tag, rhelVersion: String): String {
        val result = executeCommand("brew", "buildinfo", "${tag.value}.el${rhelVersion}sat")
            ?: throw RuntimeException("Failed")

        return result.lines()
            .filter { it.startsWith("Task: ") }
            .map { it.split(" ")[1] }
            .map { it.trim() }
            .first()
    }

    private fun currentTag(): Tag {
        return findTag("HEAD")
    }

    private fun previousTag(tag: Tag): Tag {
        return findTag("${tag.value}~1")
    }

    private fun findTag(tag: String): Tag {
        val result = executeCommand("git", "describe", "--abbrev=0", tag)
        val foundTag = result?.let { Tag(it) } ?: throw RuntimeException("tag not found")
        logger.debug("found tag: {}", foundTag)
        return foundTag
    }

    private fun changes(from: Tag, to: Tag): ChangeSet {
        val result = executeCommand("git", "log", "${from.value}..${to.value}", "--no-merges", "--pretty=' - %s (%aE)'")
        return ChangeSet.from(result)
    }

    private fun executeCommand(vararg args: String): String? {
        val stdout = ByteArrayOutputStream()
        project.exec {
            commandLine(args.asList())
            standardOutput = stdout
            workingDir = project.rootDir
        }
        return stdout.toString(Charset.defaultCharset())?.trim()
    }

}

data class ChangeSet(private val changes: List<String>) {

    fun buildChanges(): List<String> {
        return this.changes.asSequence()
            .filter { !it.contains("dependabot") }
            .filter { !it.contains("Automatic commit") }
            .filter { !it.contains("Add new translations using Weblate") }
            .filter { !it.contains("noreply@weblate.org") }
            .filter { !it.contains("updated po/keys.pot template") }
            .toList()
    }

    fun dependencyChanges(): List<String> {
        return this.changes.filter { it.contains("dependabot") }
    }

    companion object {
        fun from(text: String?): ChangeSet {
            return ChangeSet(text?.lines() ?: throw RuntimeException())
        }
    }
}

data class CandlepinVersion(val parts: List<Int>) {

    private val RHEL7_END = "";
    private val RHEL9_START = "";

    fun supportedOsVersions(): List<String> {
        val supported: MutableList<Int> = mutableListOf()
        if (supportsRHEL7()) {
            supported.add(7)
        }
        if (supportsRHEL8()) {
            supported.add(8)
        }
        if (supportsRHEL9()) {
            supported.add(9)
        }

        return listOf()
    }

    private fun supportsRHEL7(): Boolean {
        val listOf = listOf(4, 2, 11)
        this.parts.take(listOf.size)
            .zip(listOf)
            .any { pair -> pair.first > pair.second }
        return false
    }

    private fun supportsRHEL8(): Boolean {
        return true
    }

    private fun supportsRHEL9(): Boolean {
        val listOf = listOf(4, 2, 11)
        this.parts.take(listOf.size)
            .zip(listOf)
            .any { pair -> pair.first > pair.second }
        return false
    }

}

data class Tag(val value: String) {
    fun version(): CandlepinVersion {
        val versionParts = this.value.removePrefix("candlepin-")
            .split("[.-]")
            .map { it.toInt() }
            .toList()
        return CandlepinVersion(versionParts)
    }
}

data class MailBody(
    private val urls: List<String>,
    private val changes: List<String>,
    private val deps: List<String>
) {

    fun asText(): String {
        return buildString {
            appendLines(urls)
            appendLine()
            appendLine("Changes in this build:")
            appendLine()
            appendLines(changes)
            appendLine()
            appendLine("Automated library upgrades:")
            appendLine()
            appendLines(deps)
        }
    }

    private fun StringBuilder.appendLines(lines: List<String>) {
        for (line in lines) {
            appendLine(line.trimStart('\'').trimEnd('\'').trim())
        }
    }

}

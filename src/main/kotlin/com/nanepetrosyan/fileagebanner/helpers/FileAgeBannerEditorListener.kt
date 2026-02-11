package com.nanepetrosyan.fileagebanner.helpers;

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.nanepetrosyan.fileagebanner.services.GitFileAgeService
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

/**
 * @author Nane Petrosyan
 * 11.02.26
 */

class FileAgeBannerEditorListener : EditorFactoryListener {

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val project = editor.project ?: return

        val file = getFile(editor) ?: return
        if (file.isDirectory) return

        // Avoid duplicates
        if (editor.getUserData(HEADER_LABEL_KEY) != null) return

        val label = ensureHeader(editor) ?: return

        // Compute git age in background; update UI on EDT
        GitFileAgeService.getInstance(project).requestUpdate(file) { text ->
            ApplicationManager.getApplication().invokeLater {
                // Editor might be already released; just be defensive
                val current = editor.getUserData(HEADER_LABEL_KEY) ?: return@invokeLater
                current.text = text
            }
        }
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        val editor = event.editor
        val editorEx = editor as? EditorEx ?: return
        editorEx.setHeaderComponent(null)
        editor.putUserData(HEADER_LABEL_KEY, null)
    }

    private fun getFile(editor: Editor): VirtualFile? =
        FileDocumentManager.getInstance().getFile(editor.document)

    private fun ensureHeader(editor: Editor): JLabel? {
        val editorEx = editor as? EditorEx ?: return null
        editor.getUserData(HEADER_LABEL_KEY)?.let { return it }

        val scheme = EditorColorsManager.getInstance().globalScheme
        val panel = JPanel(BorderLayout())
        panel.isOpaque = true
        panel.background = scheme.defaultBackground.brighter()
        panel.border = EmptyBorder(4, 0, 4, 0)

        val label = JLabel("… loading")
        label.foreground = scheme.defaultForeground
        label.border = EmptyBorder(4, 8, 4, 8)
        panel.add(label, BorderLayout.WEST)

        editorEx.setHeaderComponent(panel)
        editor.putUserData(HEADER_LABEL_KEY, label)
        return label
    }

    companion object {
        private val HEADER_LABEL_KEY: Key<JLabel> = Key.create("FileAgeBanner.HeaderLabel")
    }
}

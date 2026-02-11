package com.nanepetrosyan.fileagebanner.helpers

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.EditorCustomElementRenderer
import java.awt.*

/**
 * @author Nane Petrosyan
 * 11.02.26
 */
class FileAgeBannerRenderer(
    private val text: String
) : EditorCustomElementRenderer {

    override fun calcWidthInPixels(inlay: com.intellij.openapi.editor.Inlay<*>): Int {
        val editor = inlay.editor
        val fm = editor.contentComponent.getFontMetrics(getFont(editor))
        return fm.stringWidth(text) + 16
    }

    override fun calcHeightInPixels(inlay: com.intellij.openapi.editor.Inlay<*>): Int {
        val editor = inlay.editor
        val fm = editor.contentComponent.getFontMetrics(getFont(editor))
        return fm.height + 10
    }

    override fun paint(
        inlay: com.intellij.openapi.editor.Inlay<*>,
        g: Graphics,
        targetRegion: Rectangle,
        textAttributes: TextAttributes
    ) {
        val editor = inlay.editor
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            val font = getFont(editor)
            g2.font = font

            val fm = g2.fontMetrics
            val paddingX = 8
            val paddingY = 5

            val scheme = EditorColorsManager.getInstance().globalScheme
            val bg = scheme.defaultBackground.brighter()
            val fg = scheme.defaultForeground

            g2.color = bg
            g2.fillRoundRect(
                targetRegion.x,
                targetRegion.y + 2,
                targetRegion.width.coerceAtLeast(10),
                targetRegion.height - 4,
                10,
                10
            )

            g2.color = fg
            val baseline = targetRegion.y + paddingY + fm.ascent
            g2.drawString(text, targetRegion.x + paddingX, baseline)
        } finally {
            g2.dispose()
        }
    }

    private fun getFont(editor: Editor): Font {
        val base = editor.colorsScheme.editorFontName
        val size = editor.colorsScheme.editorFontSize
        return Font(base, Font.PLAIN, size).deriveFont((size - 1).toFloat())
    }
}
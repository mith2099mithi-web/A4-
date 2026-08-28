package com.example.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.FontRes
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat
import com.example.R

/**
 * A single bundled document font, with real TTF resources for each style.
 * Used by the editor canvas, the font picker and the PDF exporter so that
 * what you see on screen is exactly what gets exported.
 */
data class DocumentFont(
    val id: String,
    val displayName: String,
    val sample: String,
    val composeFamily: FontFamily,
    @FontRes val regular: Int,
    @FontRes val bold: Int,
    @FontRes val italic: Int,
    @FontRes val boldItalic: Int
)

/**
 * Central registry of every font available inside documents.
 *
 * Fonts are bundled in `res/font`, so they render identically in the
 * editor canvas AND in exported PDFs (the old implementation mapped most
 * font names to the default system sans-serif, which made the font picker
 * appear to do nothing).
 */
object DocumentFonts {

    /** Default sans-serif — clean, modern, great for documents. */
    val Inter = DocumentFont(
        id = "inter",
        displayName = "Inter",
        sample = "The quick brown fox",
        composeFamily = FontFamily(
            Font(R.font.inter_regular, FontWeight.W400),
            Font(R.font.inter_italic, FontWeight.W400, FontStyle.Italic),
            Font(R.font.inter_bold, FontWeight.W700),
            Font(R.font.inter_bold_italic, FontWeight.W700, FontStyle.Italic)
        ),
        regular = R.font.inter_regular,
        bold = R.font.inter_bold,
        italic = R.font.inter_italic,
        boldItalic = R.font.inter_bold_italic
    )

    /** Elegant serif for formal documents. */
    val Serif = DocumentFont(
        id = "lora",
        displayName = "Lora Serif",
        sample = "The quick brown fox",
        composeFamily = FontFamily(
            Font(R.font.lora_regular, FontWeight.W400),
            Font(R.font.lora_italic, FontWeight.W400, FontStyle.Italic),
            Font(R.font.lora_bold, FontWeight.W700),
            Font(R.font.lora_bold_italic, FontWeight.W700, FontStyle.Italic)
        ),
        regular = R.font.lora_regular,
        bold = R.font.lora_bold,
        italic = R.font.lora_italic,
        boldItalic = R.font.lora_bold_italic
    )

    /** Monospace for code and technical documents. */
    val Mono = DocumentFont(
        id = "jetbrains_mono",
        displayName = "JetBrains Mono",
        sample = "The quick brown fox",
        composeFamily = FontFamily(
            Font(R.font.jetbrains_mono_regular, FontWeight.W400),
            Font(R.font.jetbrains_mono_italic, FontWeight.W400, FontStyle.Italic),
            Font(R.font.jetbrains_mono_bold, FontWeight.W700),
            Font(R.font.jetbrains_mono_bold_italic, FontWeight.W700, FontStyle.Italic)
        ),
        regular = R.font.jetbrains_mono_regular,
        bold = R.font.jetbrains_mono_bold,
        italic = R.font.jetbrains_mono_italic,
        boldItalic = R.font.jetbrains_mono_bold_italic
    )

    /** Playful handwriting style. */
    val Handwriting = DocumentFont(
        id = "caveat",
        displayName = "Caveat Handwriting",
        sample = "The quick brown fox",
        composeFamily = FontFamily(
            Font(R.font.caveat_regular, FontWeight.W400),
            Font(R.font.caveat_regular, FontWeight.W400, FontStyle.Italic),
            Font(R.font.caveat_bold, FontWeight.W700),
            Font(R.font.caveat_bold, FontWeight.W700, FontStyle.Italic)
        ),
        regular = R.font.caveat_regular,
        bold = R.font.caveat_bold,
        italic = R.font.caveat_regular,
        boldItalic = R.font.caveat_bold
    )

    val all = listOf(Inter, Serif, Mono, Handwriting)

    /** Default used for new / unknown fonts. */
    val default: DocumentFont = Inter

    /**
     * Resolve a persisted font id (e.g. "Inter", "serif", "monospace")
     * to a bundled font. Unknown ids fall back to Inter, and legacy ids
     * saved by older versions of the app are mapped to the closest match.
     */
    fun byId(id: String?): DocumentFont {
        val key = id?.trim()?.lowercase() ?: return default
        return when (key) {
            "lora", "serif", "times new roman", "georgia", "playfair" -> Serif
            "jetbrains mono", "jetbrainsmono", "monospace", "courier new", "code" -> Mono
            "caveat", "handwriting", "cursive" -> Handwriting
            "inter", "sans serif", "sans-serif", "arial", "helvetica",
            "roboto", "poppins", "open sans", "lato", "montserrat", "default" -> Inter
            else -> default
        }
    }

    /** Compose FontFamily for rendering a document text block on screen. */
    fun composeFamily(id: String?): FontFamily = byId(id).composeFamily

    /** Human readable name of a persisted font id. */
    fun displayName(id: String?): String = byId(id).displayName

    /**
     * Android Typeface for the PDF exporter. Loads the exact bundled TTF
     * for the requested style so exports match the editor pixel-for-pixel.
     */
    fun typeface(context: Context, id: String?, bold: Boolean, italic: Boolean): Typeface {
        val font = byId(id)
        val res = when {
            bold && italic -> font.boldItalic
            bold -> font.bold
            italic -> font.italic
            else -> font.regular
        }
        return try {
            ResourcesCompat.getFont(context, res) ?: syntheticTypeface(font, bold, italic)
        } catch (e: Exception) {
            syntheticTypeface(font, bold, italic)
        }
    }

    private fun syntheticTypeface(font: DocumentFont, bold: Boolean, italic: Boolean): Typeface {
        val base = when (font) {
            Serif -> Typeface.SERIF
            Mono -> Typeface.MONOSPACE
            else -> Typeface.SANS_SERIF
        }
        val style = when {
            bold && italic -> Typeface.BOLD_ITALIC
            bold -> Typeface.BOLD
            italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        return Typeface.create(base, style)
    }
}

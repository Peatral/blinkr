package xyz.peatral.blinkr.feature.glyph.data

sealed interface GlyphRenderCommand {
    data object Clear : GlyphRenderCommand
    data class RawFrame(val pixels: IntArray) : GlyphRenderCommand {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RawFrame

            return pixels.contentEquals(other.pixels)
        }

        override fun hashCode(): Int {
            return pixels.contentHashCode()
        }
    }

    data class AllOn(
        val brightness: Int,
    ) : GlyphRenderCommand

    data class Text(
        val text: String,
        val brightness: Int,
        val x: Int? = null,
        val y: Int? = null,
    ) : GlyphRenderCommand
}
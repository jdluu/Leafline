package com.jdluu.leafline.reader.font

import android.util.Base64
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubNavigatorFragment

/**
 * Injects CSS @font-face declarations for sideloaded custom fonts directly into Readium's WebView.
 */
object CustomFontInjector {

    private data class CachedFontScript(
        val key: List<String>,
        val script: String
    )

    @Volatile
    private var cachedScript: CachedFontScript? = null

    internal fun buildFontFacesCss(fonts: List<CustomFont>): String {
        if (fonts.isEmpty()) return ""
        val sb = StringBuilder()
        for (font in fonts) {
            if (!font.file.exists() || font.file.length() == 0L) continue
            val bytes = try {
                font.file.readBytes()
            } catch (_: Exception) {
                continue
            }
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            sb.append(
                """
                @font-face {
                    font-family: '${font.name.replace("'", "\\'")}';
                    src: url('data:${font.mimeType};charset=utf-8;base64,$base64');
                    font-weight: normal;
                    font-style: normal;
                }
                """.trimIndent()
            ).append("\n")
        }
        return sb.toString()
    }

    internal fun buildInjectionScript(fonts: List<CustomFont>): String? {
        if (fonts.isEmpty()) return null
        val key = fonts.map { font ->
            "${font.name}|${font.file.absolutePath}|${font.file.length()}|${font.file.lastModified()}"
        }
        cachedScript?.takeIf { it.key == key }?.let { return it.script }

        val css = buildFontFacesCss(fonts)
        if (css.isBlank()) return null
        val escapedCss = JSONObject.quote(css)
        val script = """
            (function() {
                var el = document.getElementById('leafline-custom-fonts');
                if (!el) {
                    el = document.createElement('style');
                    el.id = 'leafline-custom-fonts';
                    document.head.appendChild(el);
                }
                el.textContent = $escapedCss;
            })();
        """.trimIndent()
        cachedScript = CachedFontScript(key = key, script = script)
        return script
    }

    fun inject(navigator: EpubNavigatorFragment?, fonts: List<CustomFont>) {
        if (navigator == null || fonts.isEmpty()) return
        navigator.lifecycleScope.launch(Dispatchers.IO) {
            val js = buildInjectionScript(fonts) ?: return@launch
            withContext(Dispatchers.Main) {
                try {
                    navigator.evaluateJavascript(js)
                } catch (_: Exception) {}
            }
        }
    }
}

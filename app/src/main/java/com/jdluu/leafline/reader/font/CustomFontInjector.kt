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

    fun buildFontFacesCss(fonts: List<CustomFont>): String {
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

    fun inject(navigator: EpubNavigatorFragment?, fonts: List<CustomFont>) {
        if (navigator == null || fonts.isEmpty()) return
        navigator.lifecycleScope.launch(Dispatchers.IO) {
            val css = buildFontFacesCss(fonts)
            if (css.isBlank()) return@launch
            val escapedCss = JSONObject.quote(css)
            val js = """
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
            withContext(Dispatchers.Main) {
                try {
                    navigator.evaluateJavascript(js)
                } catch (_: Exception) {}
            }
        }
    }
}

package io.golos.golos.screens.editor

import org.junit.Test

/**
 * Created by yuri yurivladdurain@gmail.com on 25/10/2017.
 */
class EditorTextPartTest {
    @Test
    fun getMarkdownRepresentation() {
        var textPart = EditorTextPart("![dancing-banana.gif](https://images.golos.io/DQmZ12Qr5ezJBXuJSRR1cnRuEQeMrnnBBGU721cQX893afK/dancing-banana.gif)\n" +
                "(https://golos.io/submit.html)\n" +
                "<script>\n" +
                "  // JavaScript code here\n" +
                "</script>", null);
        assert(!textPart.markdownRepresentation.contains("<script>"))

        textPart = EditorTextPart("![dancing-banana.gif](https://images.golos.io/DQmZ12Qr5ezJBXuJSRR1cnRuEQeMrnnBBGU721cQX893afK/dancing-banana.gif)\n" +
                "https://images.golos.io/DQmZ12Qr5ezJBXuJSRR1cnRuEQeMrnnBBGU721cQX893afK/dancing-banana.exe)\\n\"" +
                "https://images.golos.io/DQmZ12Qr5ezJBXuJSRR1cnRuEQeMrnnBBGU721cQX893afK/dancing-banana.exe)\\n\"" +
                "https://images.golos.io/DQmZ12Qr5ezJBXuJSRR1cnRuEQeMrnnBBGU721cQX893afK/dancing-banana.bat)\\n\"" +
                "(https://golos.io/submit.html)\n" +
                "<script>\n" +
                "  // JavaScript code here\n" +
                "</script>", null)
        assert(textPart.markdownRepresentation.contains("link removed"))
    }

}
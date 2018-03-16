package io.golos.golos.utils

/**
 * Created by yuri on 28.02.18.
 */
object ImageUriResolver {
    fun resolveImageWithSize(originalUri: String,
                             wantedHeight: Int = 0,
                             wantedwidth: Int = 0): String {
        val wantedwidth = if (wantedwidth > 1200) 0 else wantedwidth
        val wantedHeight = if (wantedHeight > 1200) 0 else wantedHeight

        var workingUri = originalUri

        if (workingUri.startsWith("https://imgp")) {
            workingUri = originalUri.replace(Regexps.imgpFindRegexp, "")
        }
        if (workingUri.isEmpty()) return originalUri
        val out = "https://imgp.golos.io/${wantedHeight}x$wantedwidth/$workingUri"
        return out
    }
}
package io.golos.golos.utils

/**
 * Created by yuri on 28.02.18.
 */
object ImageUriResolver {
    fun resolveImageWithSize(originalUri: String,
                             wantedHeight: Int = 0,
                             wantedwidth: Int = 0): String {
        var workingUri = originalUri
        if (workingUri.startsWith("https://imgp")) {
            workingUri = originalUri.replace(Regexps.imgpFindRegexp, "")
        }
        return "https://imgp.golos.io/${wantedHeight}x$wantedwidth/$workingUri"
    }
}
package io.golos.golos.screens.stories.model

/**
 * Created by yuri on 19.02.18.
 */

data class NSFWStrategy(val showNSFWImages: Boolean,
                        val makeExceptionForUser: Pair<Boolean, String>)

package io.golos.golos.repository

import androidx.lifecycle.LiveData
import io.golos.golos.repository.model.PreparingState

/**
 * Created by yuri yurivladdurain@gmail.com on 16/11/2018.
 */
interface Preloadable {
    val loadingState: LiveData<PreparingState>
}
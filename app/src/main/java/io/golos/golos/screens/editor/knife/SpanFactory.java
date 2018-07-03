package io.golos.golos.screens.editor.knife;

import android.support.annotation.NonNull;

public interface SpanFactory {
    @NonNull <T> T produceOfType(@NonNull Class<?> type);
}

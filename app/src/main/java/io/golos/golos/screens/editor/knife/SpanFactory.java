package io.golos.golos.screens.editor.knife;

import androidx.annotation.NonNull;

public interface SpanFactory {
    @NonNull <T> T produceOfType(@NonNull Class<?> type);
}

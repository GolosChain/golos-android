package io.golos.golos.utils;

import android.widget.TextView;

public class StringTextView {
    private final TextView textView;

    public StringTextView(TextView textView) {
        this.textView = textView;
    }

    public String getText() {
        return textView.getText().toString();
    }

    public void setText(String text) {
        textView.setText(text);
    }
}

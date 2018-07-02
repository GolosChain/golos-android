/*
 * Copyright (C) 2015 Matthew Lee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.golos.golos.screens.editor.knife;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.style.LeadingMarginSpan;

public class NumberedMarginSpan implements LeadingMarginSpan {

    private final int gapWidth;
    private final int leadWidth;
    private final int index;

    public NumberedMarginSpan(int leadGap, int gapWidth, int index) {
        this.leadWidth = leadGap;
        this.gapWidth = gapWidth;
        this.index = index;
    }

    public NumberedMarginSpan nextIndex() {
        return new NumberedMarginSpan(leadWidth, gapWidth, index + 1);
    }

    public int getGapWidth() {
        return gapWidth;
    }

    public int getLeadWidth() {
        return leadWidth;
    }

    public int getIndex() {
        return index;
    }

    public int getLeadingMargin(boolean first) {
        return leadWidth + gapWidth;
    }

    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom, CharSequence text, int start, int end, boolean first, Layout l) {
        if (first) {

            Paint.Style orgStyle = p.getStyle();
            p.setStyle(Paint.Style.FILL);
            float width = p.measureText("4.");
            c.drawText(index + ".", (leadWidth + x - width / 2) * dir, bottom - p.descent(), p);
            p.setStyle(orgStyle);
        }
    }
}
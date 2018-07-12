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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.provider.Browser;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

public class KnifeURLSpan extends ClickableSpan {
    private int linkColor = 0;
    private boolean linkUnderline = true;
    private String mURL;

    public KnifeURLSpan(String url, int linkColor, boolean linkUnderline) {

        this.linkColor = linkColor;
        this.linkUnderline = linkUnderline;
        mURL = url;
    }

    public KnifeURLSpan(Parcel src) {

        this.linkColor = src.readInt();
        this.linkUnderline = src.readInt() != 0;
    }

    public KnifeURLSpan(String mURL) {
        this.mURL = mURL;
    }

    public String getURL() {
        return mURL;
    }

    public void setmURL(String mURL) {
        this.mURL = mURL;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setColor(linkColor != 0 ? linkColor : ds.linkColor);
        ds.setUnderlineText(linkUnderline);
    }

    @Override
    public void onClick(View widget) {
        Uri uri = Uri.parse(getURL());
        Context context = widget.getContext();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w("URLSpan", "Actvity was not found for intent, " + intent.toString());
        }
    }



    @Override
    public String toString() {
        return "KnifeURLSpan{" +
                "linkColor=" + linkColor +
                ", linkUnderline=" + linkUnderline +
                '}';
    }
}

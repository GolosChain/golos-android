/*
 * Copyright (C) 2015 Matthew Lee
 * Copyright (C) 2007 The Android Open Source Project
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

import android.graphics.Typeface;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BulletSpan;
import android.text.style.CharacterStyle;
import android.text.style.ImageSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.ParagraphStyle;
import android.text.style.QuoteSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;

import org.jetbrains.annotations.Nullable;

import io.golos.golos.screens.editor.UtilsKt;
import timber.log.Timber;

import static io.golos.golos.BuildConfig.DEBUG_EDITOR;
import static io.golos.golos.screens.editor.knife.KnifeTagHandler.HEADER;
import static io.golos.golos.screens.editor.knife.KnifeTagHandler.QUOTE;

public class KnifeParser {
    public static Spanned fromHtml(String source) {
        return fromHtml(source, null);
    }

    public static Spanned fromHtml(String source, @Nullable SpanFactory spanFactory) {
        source = source

                .replaceAll("blockquote>", QUOTE + ">")
                .replaceAll("h\\d>", HEADER + ">")
                .replaceAll("<li><br><p>", "<li>")
                .replaceAll("<ul>\\s+<br>", "<ul>")
                .replaceAll("</li>\\s+<br>", "</li>")
                .replaceAll("<li>\\s<br>", "<li>")
                .replaceAll("li>", KnifeTagHandler.LI + ">")
                .replaceAll("ul>", KnifeTagHandler.UNORDERED_LIST + ">")
                .replaceAll("<ol>\\s+<br>", "<ol>");
        return fromInnerHtmlFormat(source, spanFactory);
    }


    private static Spanned fromInnerHtmlFormat(String source, @Nullable SpanFactory spanFactory) {
        if (DEBUG_EDITOR) Timber.e("from html = " + source);
        Spanned s = Html
                .fromHtml(source.startsWith("<b></b>") ? source
                        : "<b></b>" + source, null, new KnifeTagHandler(spanFactory));
        if (DEBUG_EDITOR) Timber.e("after first = " + s);

        if (s instanceof SpannableStringBuilder) {
            SpannableStringBuilder sb = ((SpannableStringBuilder) s);
            UtilsKt.changeLeadingSpansFlagParagraphToInclusiveInclusive(sb);
            UtilsKt.printAllSpans(sb);

            URLSpan[] spans = sb.getSpans(0, sb.length(), URLSpan.class);
            for (URLSpan urlSpan : spans) {
                int spanStart = sb.getSpanStart(urlSpan);
                int spanEnd = sb.getSpanEnd(urlSpan);
                sb.removeSpan(urlSpan);
                sb.setSpan(new KnifeURLSpan(urlSpan.getURL()), spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return UtilsKt.trimStartAndEnd(s);

    }

    public static String toHtml(Spanned text) {
        if (DEBUG_EDITOR) Timber.e("from = " + text + ", its length is " + text.length());
        if (text instanceof SpannableStringBuilder) {
            SpannableStringBuilder sb = (SpannableStringBuilder) text;
            LeadingMarginSpan[] leadingMarginSpans = sb.getSpans(0, sb.length(), LeadingMarginSpan.class);
            for (LeadingMarginSpan leadingMarginSpan : leadingMarginSpans) {
                int spanEnd = sb.getSpanEnd(leadingMarginSpan);
                int spanStart = sb.getSpanStart(leadingMarginSpan);
                if (spanStart != spanEnd && spanEnd != 0) {
                    if (sb.charAt(spanEnd - 1) == '\n') {
                        sb.removeSpan(leadingMarginSpan);
                        sb.setSpan(leadingMarginSpan, spanStart, spanEnd - 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                    }
                }
            }
        }
        StringBuilder out = new StringBuilder();
        withinHtml(out, text);
        String htmlString = tidy(out.toString());
        htmlString = htmlString
                .replaceAll("</li></ul><ul><li>", "</li><li>")
                .replaceAll("</li></ol><ol><li>", "</li><li>");
        if (DEBUG_EDITOR) Timber.e("htmlstr = " + htmlString);
        return htmlString
                .replaceAll(QUOTE + ">", "blockquote>")
                .replaceAll(HEADER + ">", "h3>")
                .replaceAll("</li><li>", "</li>\n<li>")
                .replaceAll("</li></ul>", "</li>\n</ul>")
                .replaceAll("</li></ol>", "</li>\n</ol>")
                .replaceAll("<ul><li>", "<ul>\n<li>")
                .replaceAll("<ol><li>", "<ol>\n<li>")
                .replaceAll(KnifeTagHandler.LI + ">", "li>")
                .replaceAll(KnifeTagHandler.UNORDERED_LIST + ">", "ui>")
                .trim();
    }

    private static void withinHtml(StringBuilder out, Spanned text) {
        int next;

        for (int i = 0; i < text.length(); i = next) {
            next = text.nextSpanTransition(i, text.length(), ParagraphStyle.class);

            ParagraphStyle[] styles = text.getSpans(i, next, ParagraphStyle.class);
            if (styles.length == 2) {
                if (styles[0] instanceof BulletSpan && styles[1] instanceof QuoteSpan) {
                    // Let a <br> follow the BulletSpan or QuoteSpan end, so next++
                    withinBulletThenQuote(out, text, i, next++);
                } else if (styles[0] instanceof QuoteSpan) {
                    withinQuoteThenBullet(out, text, i, next++);
                } else {
                    withinContent(out, text, i, next);
                }
            } else if (styles.length == 1) {
                if (styles[0] instanceof BulletSpan) {
                    withinBullet(out, text, i, next++);
                }
                if (styles[0] instanceof KnifeBulletSpan) {
                    withinKnifeBulletBullet(out, text, i, next++);
                } else if (styles[0] instanceof NumberedMarginSpan) {
                    withinNumberedList(out, text, i, next++);
                } else if (styles[0] instanceof QuoteSpan) {
                    withinQuote(out, text, i, next++);
                } else if (styles[0] instanceof KnifeQuoteSpan) {
                    withinKnifeQuote(out, text, i, next++);
                } else {
                    withinContent(out, text, i, next);
                }
            } else {
                withinContent(out, text, i, next);
            }
        }
    }

    private static void withinBulletThenQuote(StringBuilder out, Spanned text, int start, int end) {
        out.append("<ul><li>");
        withinQuote(out, text, start, end);
        out.append("</li></ul>");
    }

    private static void withinQuoteThenBullet(StringBuilder out, Spanned text, int start, int end) {
        out.append("<blockquote>");
        withinBullet(out, text, start, end);
        out.append("</blockquote>");
    }

    private static void withinBullet(StringBuilder out, Spanned text, int start, int end) {
        out.append("<ul>");

        int next;

        for (int i = start; i < end; i = next) {
            next = text.nextSpanTransition(i, end, BulletSpan.class);

            BulletSpan[] spans = text.getSpans(i, next, BulletSpan.class);
            for (BulletSpan span : spans) {
                out.append("<li>");
            }

            withinContent(out, text, i, next);
            for (BulletSpan span : spans) {
                out.append("</li>");
            }
        }

        out.append("</ul>");
    }

    private static void withinKnifeBulletBullet(StringBuilder out, Spanned text, int start, int end) {
        out.append("<ul>");

        int next;

        for (int i = start; i < end; i = next) {
            next = text.nextSpanTransition(i, end, KnifeBulletSpan.class);

            KnifeBulletSpan[] spans = text.getSpans(i, next, KnifeBulletSpan.class);
            for (KnifeBulletSpan span : spans) {
                out.append("<li>");
            }

            withinContent(out, text, i, next);
            for (KnifeBulletSpan span : spans) {
                out.append("</li>");
            }
        }

        out.append("</ul>");
    }

    private static void withinNumberedList(StringBuilder out, Spanned text, int start, int end) {
        out.append("<ol>");

        int next;

        for (int i = start; i < end; i = next) {
            next = text.nextSpanTransition(i, end, NumberedMarginSpan.class);

            NumberedMarginSpan[] spans = text.getSpans(i, next, NumberedMarginSpan.class);
            for (NumberedMarginSpan span : spans) {
                out.append("<li>");
            }

            withinContent(out, text, i, next);
            for (NumberedMarginSpan span : spans) {
                out.append("</li>");
            }
        }

        out.append("</ol>");
    }

    private static void withinQuote(StringBuilder out, Spanned text, int start, int end) {
        int next;

        for (int i = start; i < end; i = next) {
            next = text.nextSpanTransition(i, end, QuoteSpan.class);

            QuoteSpan[] quotes = text.getSpans(i, next, QuoteSpan.class);
            for (QuoteSpan quote : quotes) {
                out.append("<").append(KnifeTagHandler.QUOTE).append(">");
            }

            withinContent(out, text, i, next);
            for (QuoteSpan quote : quotes) {

                out.append("</").append(KnifeTagHandler.QUOTE).append(">");
            }
        }
    }

    private static void withinKnifeQuote(StringBuilder out, Spanned text, int start, int end) {
        int next;

        for (int i = start; i < end; i = next) {
            next = text.nextSpanTransition(i, end, KnifeQuoteSpan.class);

            KnifeQuoteSpan[] quotes = text.getSpans(i, next, KnifeQuoteSpan.class);
            for (KnifeQuoteSpan quote : quotes) {
                out.append("<").append(KnifeTagHandler.QUOTE).append(">");
            }

            withinContent(out, text, i, next);
            for (KnifeQuoteSpan quote : quotes) {

                out.append("</").append(KnifeTagHandler.QUOTE).append(">");
            }
        }
    }

    private static void withinContent(StringBuilder out, Spanned text, int start, int end) {
        int next;

        for (int i = start; i < end; i = next) {
            next = TextUtils.indexOf(text, '\n', i, end);
            if (next < 0) {
                next = end;
            }

            int nl = 0;
            while (next < end && text.charAt(next) == '\n') {
                next++;
                nl++;
            }

            withinParagraph(out, text, i, next - nl, nl);
        }
    }

    // Copy from https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/text/Html.java,
    // remove some tag because we don't need them in Knife.
    private static void withinParagraph(StringBuilder out, Spanned text, int start, int end, int nl) {
        int next;

        for (int i = start; i < end; i = next) {
            next = text.nextSpanTransition(i, end, CharacterStyle.class);

            CharacterStyle[] spans = text.getSpans(i, next, CharacterStyle.class);
            for (int j = 0; j < spans.length; j++) {
                if (spans[j] instanceof StyleSpan) {
                    int style = ((StyleSpan) spans[j]).getStyle();

                    if ((style & Typeface.BOLD) != 0) {
                        out.append("<b>");
                    }

                    if ((style & Typeface.ITALIC) != 0) {
                        out.append("<i>");
                    }
                }

                if (spans[j] instanceof UnderlineSpan) {
                    out.append("<u>");
                }
                if (spans[j] instanceof AbsoluteSizeSpan) {
                    out.append("<" + KnifeTagHandler.HEADER + ">");
                }

                // Use standard strikethrough tag <del> rather than <s> or <strike>
                if (spans[j] instanceof StrikethroughSpan) {
                    out.append("<del>");
                }

                if (spans[j] instanceof URLSpan) {
                    out.append("<a href=\"");
                    out.append(((URLSpan) spans[j]).getURL());
                    out.append("\">");
                }
                if (spans[j] instanceof KnifeURLSpan) {
                    out.append("<a href=\"");
                    out.append(((KnifeURLSpan) spans[j]).getURL());
                    out.append("\">");
                }

                if (spans[j] instanceof ImageSpan) {
                    out.append("<img src=\"");
                    out.append(((ImageSpan) spans[j]).getSource());
                    out.append("\">");

                    // Don't output the dummy character underlying the image.
                    i = next;
                }
            }

            withinStyle(out, text, i, next);
            for (int j = spans.length - 1; j >= 0; j--) {
                if (spans[j] instanceof URLSpan || spans[j] instanceof KnifeURLSpan) {
                    out.append("</a>");
                }

                if (spans[j] instanceof StrikethroughSpan) {
                    out.append("</del>");
                }

                if (spans[j] instanceof UnderlineSpan) {
                    out.append("</u>");
                }
                if (spans[j] instanceof AbsoluteSizeSpan) {
                    out.append("</" + KnifeTagHandler.HEADER + ">");
                }

                if (spans[j] instanceof StyleSpan) {
                    int style = ((StyleSpan) spans[j]).getStyle();

                    if ((style & Typeface.BOLD) != 0) {
                        out.append("</b>");
                    }

                    if ((style & Typeface.ITALIC) != 0) {
                        out.append("</i>");
                    }
                }
            }
        }

        for (int i = 0; i < nl; i++) {
            out.append("<br>");
        }
    }

    private static void withinStyle(StringBuilder out, CharSequence text, int start, int end) {
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);

            if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else if (c == '&') {
                out.append("&amp;");
            } else if (c >= 0xD800 && c <= 0xDFFF) {
                if (c < 0xDC00 && i + 1 < end) {
                    char d = text.charAt(i + 1);
                    if (d >= 0xDC00 && d <= 0xDFFF) {
                        i++;
                        int codepoint = 0x010000 | (int) c - 0xD800 << 10 | (int) d - 0xDC00;
                        out.append("&#").append(codepoint).append(";");
                    }
                }
            } else if (c > 0x7E || c < ' ') {
                out.append("&#").append((int) c).append(";");
            } else if (c == ' ') {
                while (i + 1 < end && text.charAt(i + 1) == ' ') {
                    out.append("&nbsp;");
                    i++;
                }

                out.append(' ');
            } else {
                out.append(c);
            }
        }
    }

    private static String tidy(String html) {
        return html.replaceAll("</ul>(<br>)?", "</ul>").replaceAll("</blockquote>(<br>)?", "</blockquote>");
    }

}

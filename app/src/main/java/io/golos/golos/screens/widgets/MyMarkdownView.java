package io.golos.golos.screens.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mukesh.MarkdownView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yuri on 07.11.17.
 */

public class MyMarkdownView extends MarkdownView {
    private static final String TAG = MarkdownView.class.getSimpleName();
    private static final String IMAGE_PATTERN = "!\\[(.*)\\]\\((.*)\\)";
    private final Context mContext;
    private String mPreviewText;
    private boolean mIsOpenUrlInBrowser;


    public MyMarkdownView(Context context) {
        this(context, (AttributeSet) null);
    }

    public MyMarkdownView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyMarkdownView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        this.initialize();
    }

    @SuppressLint({"SetJavaScriptEnabled"})
    private void initialize() {
        this.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                if (Build.VERSION.SDK_INT < 19) {
                    MyMarkdownView.this.loadUrl(MyMarkdownView.this.mPreviewText);
                } else {
                    MyMarkdownView.this.evaluateJavascript(MyMarkdownView.this.mPreviewText, (ValueCallback) null);
                }

            }

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (MyMarkdownView.this.isOpenUrlInBrowser()) {
                    Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(url));
                    MyMarkdownView.this.mContext.startActivity(intent);
                    return true;
                } else {
                    return false;
                }
            }
        });
        this.loadUrl("file:///android_asset/html/preview.html");
        this.getSettings().setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= 16) {
            this.getSettings().setAllowUniversalAccessFromFileURLs(true);
        }

        if (Build.VERSION.SDK_INT >= 21) {
            this.getSettings().setMixedContentMode(0);
        }

    }

    /*private void init() {
        setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                try {
                    Uri uri = Uri.getLocalizedError("url");
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    getContext().startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
    }*/


    @Override
    public boolean canGoForward() {
        return false;
    }


    public void loadMarkdownFromFile(File markdownFile) {
        String mdText = "";

        try {
            FileInputStream fileInputStream = new FileInputStream(markdownFile);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();

            String readText;
            while ((readText = bufferedReader.readLine()) != null) {
                stringBuilder.append(readText);
                stringBuilder.append("\n");
            }

            fileInputStream.close();
            mdText = stringBuilder.toString();
        } catch (FileNotFoundException var8) {
            Log.e(TAG, "FileNotFoundException:" + var8);
        } catch (IOException var9) {
            Log.e(TAG, "IOException:" + var9);
        }

        this.setMarkDownText(mdText);
    }

    public void loadMarkdownFromAssets(String assetsFilePath) {
        try {
            StringBuilder buf = new StringBuilder();
            InputStream json = this.getContext().getAssets().open(assetsFilePath);
            BufferedReader in = new BufferedReader(new InputStreamReader(json, "UTF-8"));

            String str;
            while ((str = in.readLine()) != null) {
                buf.append(str).append("\n");
            }

            in.close();
            this.setMarkDownText(buf.toString());
        } catch (IOException var6) {
            var6.printStackTrace();
        }

    }

    public void setMarkDownText(String markdownText) {
        String bs64MdText = this.imgToBase64(markdownText);
        String escMdText = this.escapeForText(bs64MdText);
        if (Build.VERSION.SDK_INT < 19) {
            this.mPreviewText = String.format("javascript:preview('%s')", new Object[]{escMdText});
        } else {
            this.mPreviewText = String.format("preview('%s')", new Object[]{escMdText});
        }

        this.initialize();
    }

    private String escapeForText(String mdText) {
        String escText = mdText.replace("\n", "\\\\n");
        escText = escText.replace("'", "\\'");
        escText = escText.replace("\r", "");
        return escText;
    }

    private String imgToBase64(String mdText) {
        Pattern ptn = Pattern.compile("!\\[(.*)\\]\\((.*)\\)");
        Matcher matcher = ptn.matcher(mdText);
        if (!matcher.find()) {
            return mdText;
        } else {
            String imgPath = matcher.group(2);
            if (!this.isUrlPrefix(imgPath) && this.isPathExCheck(imgPath)) {
                String baseType = this.imgEx2BaseType(imgPath);
                if (baseType.equals("")) {
                    return mdText;
                } else {
                    File file = new File(imgPath);
                    byte[] bytes = new byte[(int) file.length()];

                    try {
                        BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                        buf.read(bytes, 0, bytes.length);
                        buf.close();
                    } catch (FileNotFoundException var9) {
                        Log.e(TAG, "FileNotFoundException:" + var9);
                    } catch (IOException var10) {
                        Log.e(TAG, "IOException:" + var10);
                    }

                    String base64Img = baseType + Base64.encodeToString(bytes, 2);
                    return mdText.replace(imgPath, base64Img);
                }
            } else {
                return mdText;
            }
        }
    }

    private boolean isUrlPrefix(String text) {
        return text.startsWith("http://") || text.startsWith("https://");
    }

    private boolean isPathExCheck(String text) {
        return text.endsWith(".png") || text.endsWith(".jpg") || text.endsWith(".jpeg") || text.endsWith(".gif");
    }

    private String imgEx2BaseType(String text) {
        return text.endsWith(".png") ? "data:image/png;base64," : (!text.endsWith(".jpg") && !text.endsWith(".jpeg") ? (text.endsWith(".gif") ? "data:image/gif;base64," : "") : "data:image/jpg;base64,");
    }

    public boolean isOpenUrlInBrowser() {
        return this.mIsOpenUrlInBrowser;
    }

    public void setOpenUrlInBrowser(boolean openUrlInBrowser) {
        this.mIsOpenUrlInBrowser = openUrlInBrowser;
    }
}

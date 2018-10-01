package io.golos.golos.utils;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;

import android.widget.ImageView;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.FitCenter;

import timber.log.Timber;

/**
 * Created by yuri on 27.12.17.
 */

public class FitCenterNoUpscale extends FitCenter {
    private ImageView mImageView;

    public FitCenterNoUpscale(ImageView mImageView) {
        this.mImageView = mImageView;
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        Timber.e("width is " + toTransform.getWidth());
        Timber.e("height is " + toTransform.getHeight());
        Timber.e("outWidth is " + outWidth);
        Timber.e("outHeight is " + outHeight);
        if (toTransform.getHeight() > outHeight || toTransform.getWidth() > outWidth) {

/*
            mImageView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
*/
            return super.transform(pool, toTransform, 1080, outHeight);
        } else {
          /*  mImageView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));*/
            return toTransform;
        }
    }
}

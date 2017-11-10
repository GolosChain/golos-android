package io.golos.golos.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView

class BarcodeScannerActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {
    private lateinit var mScannerView: ZXingScannerView

    public override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        mScannerView = ZXingScannerView(this)
        mScannerView.setFormats(listOf(BarcodeFormat.QR_CODE))
        setContentView(mScannerView)
    }

    public override fun onResume() {
        super.onResume()
        mScannerView.setResultHandler(this)
        mScannerView.startCamera()
    }

    public override fun onPause() {
        super.onPause()
        mScannerView.stopCamera()
    }

    override fun handleResult(rawResult: Result) {
        val intent = Intent()
        intent.putExtra(SCAN_RESULT_TAG, rawResult.text)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    companion object {
        val SCAN_RESULT_TAG = "SCAN_RESULT_TAG"
    }
}
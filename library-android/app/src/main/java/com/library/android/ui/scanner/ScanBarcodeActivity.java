package com.library.android.ui.scanner;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.Result;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.library.android.R;
import com.library.android.databinding.ActivityScanBarcodeBinding;

/**
 * 条码扫描 Activity — 使用 ZXing 库扫描 ISBN 条码（人员 B 主导）.
 *
 * <p>扫描结果可带回给借阅/编目入口使用。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class ScanBarcodeActivity extends AppCompatActivity {

    private ActivityScanBarcodeBinding binding;
    private String source;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScanBarcodeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        source = getIntent().getStringExtra("source");

        binding.barcodeScanner.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null) {
                    handleScanResult(result.getText());
                }
            }

            @Override
            public void possibleResultPoints(java.util.List<com.google.zxing.ResultPoint> resultPoints) {
                // 可选：显示可能的扫描点
            }
        });

        binding.barcodeScanner.setStatusText("");
    }

    private void handleScanResult(String isbn) {
        // 播放提示音或震动
        binding.barcodeScanner.pause();

        // 将扫描结果带回
        Intent resultIntent = new Intent();
        resultIntent.putExtra("isbn", isbn);
        resultIntent.putExtra("source", source);
        setResult(RESULT_OK, resultIntent);

        Toast.makeText(this, getString(R.string.scan_result_format, isbn), Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.barcodeScanner.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.barcodeScanner.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}

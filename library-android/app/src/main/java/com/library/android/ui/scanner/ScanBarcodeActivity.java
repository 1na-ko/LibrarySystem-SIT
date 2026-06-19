package com.library.android.ui.scanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.zxing.Result;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.library.android.R;
import com.library.android.databinding.ActivityScanBarcodeBinding;

/**
 * 条码扫描 Activity — 使用 ZXing 库扫描 ISBN 条码.
 *
 * <p>WP-9：补充运行时 CAMERA 权限请求（原版仅 Manifest 声明、Android 6.0+ 无运行时请求
 * 致摄像头无法打开、画面全黑无取景框）。授权后才 resume 扫描视图；拒绝则提示并关闭。
 *
 * <p>扫描结果可带回给借阅/编目入口使用. 标注 @AndroidEntryPoint 以便后续按 ISBN
 * 联动后端查图书时可注入 Repository.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@dagger.hilt.android.AndroidEntryPoint
public class ScanBarcodeActivity extends AppCompatActivity {

    private ActivityScanBarcodeBinding binding;
    private String source;

    /** WP-9：运行时 CAMERA 权限请求 launcher. */
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    binding.barcodeScanner.resume();
                } else {
                    Toast.makeText(this, R.string.scan_camera_permission_denied, Toast.LENGTH_LONG).show();
                    finish();
                }
            });

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

        binding.barcodeScanner.setStatusText(getString(R.string.scan_status_hint));
    }

    private void handleScanResult(String isbn) {
        binding.barcodeScanner.pause();

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
        // WP-9：先检查权限，已授权才 resume 扫描视图，否则请求运行时权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            binding.barcodeScanner.resume();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
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

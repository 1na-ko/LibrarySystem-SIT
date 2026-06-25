package com.library.android.network;

import android.util.Log;

import androidx.annotation.Nullable;

import com.library.android.BuildConfig;

import okhttp3.CertificatePinner;

/**
 * 证书锁定提供器（P2-07）— 当生产 HTTPS 上线后启用证书指纹绑定，防中间人攻击.
 *
 * <p>当前 HTTPS 证书未部署（{@code USE_HTTPS=false}），返回 {@code null}，
 * OkHttpClient 不启用证书锁定（与现网 HTTP 行为一致）。
 *
 * <p>HTTPS 上线后操作步骤：
 * <ol>
 *   <li>取得生产证书 SHA-256 指纹（可用 {@code openssl s_client -connect ...} 提取）</li>
 *   <li>在 {@link #PRIMARY_PIN} 填入主指纹（含备份指纹一同写入 backup pin，防证书续期断网）</li>
 *   <li>在 local.properties 设置 {@code api.use.https=true}</li>
 *   <li>{@link #provide()} 自动返回 CertificatePinner 实例</li>
 *   <li>建议在 release 构建中通过另一个 BuildConfig 字段 {@code CERT_PIN_ENABLED} 强制 require</li>
 * </ol>
 *
 * <p>NOTE: 仅在 HTTPS 模式下生效；HTTP 模式下 CertificatePinner 不会被 OkHttp 触发.
 *
 * @author LibrarySystem Team
 * @since 1.1.0
 */
public final class CertificatePinnerProvider {

    private static final String TAG = "CertPinner";

    /** 生产 HTTPS 主域名（与 BASE_URL 中 host 保持一致）. */
    private static final String PROD_HOST = "101.132.24.73";

    /**
     * TODO[P2-07]: HTTPS 部署后，从生产证书提取 SHA-256 指纹填入此处.
     * <p>提取命令示例：
     * <pre>{@code
     * echo | openssl s_client -servername 101.132.24.73 -connect 101.132.24.73:8443 2>/dev/null \
     *   | openssl x509 -pubkey -noout \
     *   | openssl pkey -pubin -outform der \
     *   | openssl dgst -sha256 -binary \
     *   | openssl enc -base64
     * }</pre>
     * 复制输出（不含末尾 =）填入 PRIMARY_PIN，格式 {@code "sha256/xxxxxxxxx="}.
     */
    private static final String PRIMARY_PIN = "";  // 例如 "sha256/AbCdEf..."
    /** 备份 pin — 防主证书续期前未及时更新 APK 导致全局断网. */
    private static final String BACKUP_PIN = "";

    private CertificatePinnerProvider() {
        // 工具类禁止实例化
    }

    /**
     * 提供 CertificatePinner，当未配置或 HTTPS 关闭时返回 null（OkHttp 不触发锁定）.
     */
    @Nullable
    public static CertificatePinner provide() {
        if (!BuildConfig.USE_HTTPS) {
            return null;  // HTTP 模式 — 不需要证书锁定
        }
        if (PRIMARY_PIN.isEmpty()) {
            Log.w(TAG, "USE_HTTPS=true 但 PRIMARY_PIN 未配置；CertificatePinner 跳过. "
                    + "生产环境部署前必须填入证书指纹.");
            return null;
        }
        CertificatePinner.Builder b = new CertificatePinner.Builder()
                .add(PROD_HOST, PRIMARY_PIN);
        if (!BACKUP_PIN.isEmpty()) {
            b.add(PROD_HOST, BACKUP_PIN);
        }
        Log.i(TAG, "CertificatePinner 已启用：host=" + PROD_HOST);
        return b.build();
    }
}

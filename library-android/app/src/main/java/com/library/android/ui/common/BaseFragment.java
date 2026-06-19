package com.library.android.ui.common;

import android.text.TextUtils;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.library.android.R;
import com.library.android.network.exception.ApiException;
import com.library.android.network.exception.BizConflictException;
import com.library.android.network.exception.NetworkException;
import com.library.android.network.exception.PermissionDeniedException;
import com.library.android.network.exception.ServiceUnavailableException;
import com.library.android.network.exception.SessionExpiredException;
import com.library.android.network.exception.ValidationException;

/**
 * Fragment 基类（可选继承）— 提供错误事件订阅与 Snackbar 展示工具方法.
 *
 * <p>WP-1：新增 {@link #setupToolbar} 系列方法，让二级页面绑定独立 toolbar
 * （返回按钮 + 居中标题 + 可选右侧 action），与 Activity 级全局 header 解耦.
 *
 * <p>典型用法:
 * <pre>{@code
 * @AndroidEntryPoint
 * public class MyFragment extends BaseFragment {
 *     @Override public void onViewCreated(View view, Bundle savedInstanceState) {
 *         super.onViewCreated(view, savedInstanceState);
 *         setupToolbar(view, R.string.page_title_xxx);
 *         observeError(viewModel.getErrorEvent());
 *     }
 * }
 * }</pre>
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public abstract class BaseFragment extends Fragment {

    /**
     * 订阅 ViewModel 的错误事件，按 {@link ApiException} 子类型展示分类提示.
     */
    protected void observeError(@NonNull LiveData<Throwable> errorEvent) {
        errorEvent.observe(getViewLifecycleOwner(), this::showErrorSnackbar);
    }

    /** 展示分类错误 Snackbar；调用方可用于自身 catch 路径. */
    protected void showErrorSnackbar(@Nullable Throwable throwable) {
        if (throwable == null) return;
        View root = getView();
        if (root == null) return;
        String msg = mapErrorMessage(throwable);
        Snackbar.make(root, msg, Snackbar.LENGTH_LONG).show();
    }

    /** 将异常映射为本地化文案. */
    @NonNull
    protected String mapErrorMessage(@NonNull Throwable throwable) {
        if (throwable instanceof SessionExpiredException) {
            return getString(R.string.session_expired_toast);
        }
        if (throwable instanceof PermissionDeniedException) {
            return getString(R.string.error_permission_denied);
        }
        if (throwable instanceof BizConflictException
                || throwable instanceof ValidationException) {
            String serverMsg = ((ApiException) throwable).getServerMessage();
            return !TextUtils.isEmpty(serverMsg) ? serverMsg : getString(R.string.error_validation);
        }
        if (throwable instanceof ServiceUnavailableException) {
            return getString(R.string.error_service_unavailable);
        }
        if (throwable instanceof NetworkException) {
            return getString(R.string.error_network);
        }
        if (throwable instanceof ApiException) {
            String serverMsg = ((ApiException) throwable).getServerMessage();
            return !TextUtils.isEmpty(serverMsg) ? serverMsg : getString(R.string.error_unknown);
        }
        return !TextUtils.isEmpty(throwable.getMessage())
                ? throwable.getMessage()
                : getString(R.string.error_unknown);
    }

    // ============================================================
    // WP-1：页面级 toolbar 绑定工具
    // ============================================================

    /**
     * 绑定二级页面 toolbar：返回键 → navigateUp，标题居中.
     * <p>前提：页面 layout 顶部 {@code <include layout="@layout/page_toolbar" />}.
     *
     * @param root      Fragment 根 View（onViewCreated 的 view 参数）
     * @param titleResId 页面标题资源 ID
     */
    protected void setupToolbar(@NonNull View root, @StringRes int titleResId) {
        setupToolbar(root, getString(titleResId));
    }

    /**
     * 同 {@link #setupToolbar(View, int)}，接受字符串.
     */
    protected void setupToolbar(@NonNull View root, @NonNull CharSequence title) {
        View backBtn = root.findViewById(R.id.btnPageBack);
        View titleTv = root.findViewById(R.id.tvPageTitle);
        if (titleTv instanceof android.widget.TextView) {
            ((android.widget.TextView) titleTv).setText(title);
        }
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> {
                try {
                    boolean navigated = NavHostFragment.findNavController(this).navigateUp();
                    if (!navigated) {
                        // startDestination：navigateUp 返回 false，回退交给 Activity
                        requireActivity().onBackPressed();
                    }
                } catch (IllegalStateException ignore) {
                    // Fragment 不在 NavHost 中（如 Dialog/独立测试），降级 popBackStack
                    if (!getParentFragmentManager().popBackStackImmediate()) {
                        requireActivity().onBackPressed();
                    }
                }
            });
        }
    }

    /**
     * 在页面级 toolbar 右侧显示一个操作按钮（可选）.
     *
     * @param root        Fragment 根 View
     * @param iconResId   图标资源
     * @param contentDesc 内容描述（无障碍）
     * @param onClick     点击回调
     */
    protected void setupToolbarAction(@NonNull View root,
                                      @DrawableRes int iconResId,
                                      @NonNull CharSequence contentDesc,
                                      @NonNull View.OnClickListener onClick) {
        android.widget.ImageButton actionBtn = root.findViewById(R.id.btnPageAction);
        if (actionBtn == null) return;
        actionBtn.setImageResource(iconResId);
        actionBtn.setContentDescription(contentDesc);
        actionBtn.setVisibility(View.VISIBLE);
        actionBtn.setOnClickListener(onClick);
    }
}

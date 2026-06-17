package com.library.android.ui.reservation;

import android.content.Context;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.library.android.R;
import com.library.android.model.QueuePositionVO;
import com.library.android.model.ReservationVO;
import com.library.android.viewmodel.ReservationViewModel;

/**
 * 预约状态卡片 — 可嵌入图书详情页的复用组件（人员 B 主导）.
 *
 * <p>显示当前排队位置 / 总等待人数，已通知时显示 48h 倒计时。
 *
 * <pre>
 * &lt;com.library.android.ui.reservation.ReservationStatusCard
 *     android:id="@+id/reservationCard"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content" /&gt;
 * </pre>
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class ReservationStatusCard extends MaterialCardView {

    private TextView textQueuePosition;
    private TextView textExpireHint;
    private MaterialButton btnCancel;
    private ReservationViewModel viewModel;
    private ReservationVO reservation;

    public ReservationStatusCard(Context context) {
        super(context);
        init(context);
    }

    public ReservationStatusCard(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ReservationStatusCard(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setCardElevation(0);
        setRadius(getResources().getDimensionPixelSize(R.dimen.radius_md));
        setStrokeWidth(1);
        setStrokeColor(ContextCompat.getColor(getContext(), R.color.border_light));

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp2px(16), dp2px(12), dp2px(16), dp2px(12));

        // 排队位置
        textQueuePosition = new TextView(context);
        textQueuePosition.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        textQueuePosition.setVisibility(View.GONE);
        root.addView(textQueuePosition);

        // 过期提示（48h 倒计时）
        textExpireHint = new TextView(context);
        textExpireHint.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
        textExpireHint.setTextColor(context.getColor(R.color.error));
        textExpireHint.setVisibility(View.GONE);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = dp2px(4);
        textExpireHint.setLayoutParams(hintParams);
        root.addView(textExpireHint);

        // 取消预约按钮
        btnCancel = new MaterialButton(context);
        btnCancel.setText(R.string.cancel_reservation);
        btnCancel.setTextColor(context.getColor(R.color.error));
        btnCancel.setVisibility(View.GONE);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.topMargin = dp2px(8);
        btnCancel.setLayoutParams(btnParams);
        root.addView(btnCancel);

        addView(root);
        setVisibility(View.GONE);
    }

    /** 绑定预约数据并初始化 ViewModel 观察. */
    public void bind(LifecycleOwner lifecycleOwner, ReservationViewModel vm, ReservationVO reservation) {
        this.viewModel = vm;
        this.reservation = reservation;

        if (reservation == null) {
            setVisibility(View.GONE);
            return;
        }

        setVisibility(View.VISIBLE);
        textQueuePosition.setVisibility(View.VISIBLE);
        textQueuePosition.setText(getContext().getString(
                R.string.queue_position_format, reservation.getQueuePosition()));

        // 查询详细排队信息
        viewModel.queryQueuePosition(reservation.getId());
        viewModel.getQueuePosition().observe(lifecycleOwner, this::displayQueueInfo);

        // 已被通知时显示倒计时提示
        if (reservation.isNotified()) {
            textExpireHint.setVisibility(View.VISIBLE);
            textExpireHint.setText(R.string.notified_expire_hint);
        } else {
            textExpireHint.setVisibility(View.GONE);
        }

        // 取消按钮
        if (reservation.canCancel()) {
            btnCancel.setVisibility(View.VISIBLE);
            btnCancel.setOnClickListener(v -> viewModel.cancelReservation(reservation.getId()));
        } else {
            btnCancel.setVisibility(View.GONE);
        }

        // 监听取消结果
        viewModel.getCancelResult().observe(lifecycleOwner, success -> {
            if (Boolean.TRUE.equals(success)) {
                setVisibility(View.GONE);
            }
        });
    }

    private void displayQueueInfo(QueuePositionVO queue) {
        if (queue != null) {
            textQueuePosition.setText(getContext().getString(
                    R.string.queue_detail_format, queue.getPosition(), queue.getTotalWaiting()));
        }
    }

    private int dp2px(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}

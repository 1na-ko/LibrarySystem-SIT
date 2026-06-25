package com.library.android.ui.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.library.android.databinding.FragmentAdvancedSearchBinding;

/**
 * 高级搜索 BottomSheet（WP-14：由独立页面改为半屏弹窗，节省栈深度 + 体验更轻量）.
 *
 * <p>SearchFragment 通过 {@code AdvancedSearchFragment.show(getChildFragmentManager(), tag)}
 * 唤出；提交时通过 {@code setFragmentResult} 回传参数给 SearchFragment.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class AdvancedSearchFragment extends BottomSheetDialogFragment {

    public static final String TAG = "AdvancedSearchBottomSheet";
    public static final String RESULT_KEY = "advanced_search_result";

    private FragmentAdvancedSearchBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdvancedSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnSearch.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("title", trim(binding.etTitle));
            args.putString("author", trim(binding.etAuthor));
            args.putString("isbn", trim(binding.etIsbn));
            args.putString("publisher", trim(binding.etPublisher));
            Integer yrFrom = parseIntOrNull(trim(binding.etYearFrom));
            Integer yrTo = parseIntOrNull(trim(binding.etYearTo));
            if (yrFrom != null) args.putInt("pubYearFrom", yrFrom);
            if (yrTo != null) args.putInt("pubYearTo", yrTo);
            args.putBoolean("onlyAvailable", binding.switchOnlyAvailable.isChecked());

            getParentFragmentManager().setFragmentResult(RESULT_KEY, args);
            dismiss();
        });
    }

    private String trim(android.widget.EditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Nullable
    private static Integer parseIntOrNull(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return Integer.parseInt(s); } catch (NumberFormatException ignore) { return null; }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

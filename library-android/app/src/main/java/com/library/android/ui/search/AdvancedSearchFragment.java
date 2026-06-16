package com.library.android.ui.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.library.android.databinding.FragmentAdvancedSearchBinding;

/**
 * 高级搜索 Fragment.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class AdvancedSearchFragment extends Fragment {

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
            String title = binding.etTitle.getText().toString().trim();
            String author = binding.etAuthor.getText().toString().trim();
            String isbn = binding.etIsbn.getText().toString().trim();
            String publisherText = binding.etPublisher.getText().toString().trim();
            String yearFrom = binding.etYearFrom.getText().toString().trim();
            String yearTo = binding.etYearTo.getText().toString().trim();
            boolean onlyAvailable = binding.switchOnlyAvailable.isChecked();

            Bundle args = new Bundle();
            args.putString("title", title);
            args.putString("author", author);
            args.putString("isbn", isbn);
            args.putString("publisher", publisherText);
            if (!yearFrom.isEmpty()) args.putInt("pubYearFrom", Integer.parseInt(yearFrom));
            if (!yearTo.isEmpty()) args.putInt("pubYearTo", Integer.parseInt(yearTo));
            args.putBoolean("onlyAvailable", onlyAvailable);

            Navigation.findNavController(requireView())
                    .navigate(com.library.android.R.id.action_advancedSearchFragment_to_searchFragment, args);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
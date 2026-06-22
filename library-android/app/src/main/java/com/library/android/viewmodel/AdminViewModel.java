package com.library.android.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.library.android.model.*;
import com.library.android.repository.AdminRepository;
import com.library.android.repository.BookRepository;
import com.library.android.ui.common.LoadingState;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 系统管理 ViewModel（人员 B 主导）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class AdminViewModel extends BaseViewModel {

    private final AdminRepository repository;
    private final BookRepository bookRepository;

    // 用户管理
    private final MutableLiveData<LoadingState> userLoadingState = new MutableLiveData<>(LoadingState.LOADING);
    private final MutableLiveData<List<UserManageVO>> userList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> statusUpdateResult = new MutableLiveData<>();
    // 图书编目
    private final MutableLiveData<com.library.android.model.BookDetailVO> createdBook = new MutableLiveData<>();
    private final MutableLiveData<com.library.android.model.BookDetailVO> updatedBook = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleteResult = new MutableLiveData<>();
    /** P1-01：分类树（图书编目分类选择器使用，原 BookEditActivity 直接注入 BookRepository 已下沉到此）. */
    private final MutableLiveData<List<CategoryVO>> categoryTree = new MutableLiveData<>();

    private int userPage = 1;
    private int userTotalPages = 0;
    private String userRoleFilter = null;
    private String userStatusFilter = null;
    private String userKeyword = null;
    private boolean isLoadingUsers = false;

    @Inject
    public AdminViewModel(AdminRepository repository, BookRepository bookRepository) {
        this.repository = repository;
        this.bookRepository = bookRepository;
    }

    // ---- 用户管理 ----
    public LiveData<LoadingState> getUserLoadingState() { return userLoadingState; }
    public LiveData<List<UserManageVO>> getUserList() { return userList; }
    public LiveData<Boolean> getStatusUpdateResult() { return statusUpdateResult; }
    public void loadUsers(String role, String status, String keyword) {
        userRoleFilter = role;
        userStatusFilter = status;
        userKeyword = keyword;
        userPage = 1;
        userLoadingState.setValue(LoadingState.LOADING);
        disposables.add(repository.listUsers(role, status, keyword, userPage, 20)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        PageResult<UserManageVO> page = result.getData();
                        List<UserManageVO> records = page.getRecords();
                        userList.setValue(records != null ? records : new ArrayList<>());
                        userTotalPages = page.getTotalPages();
                        userLoadingState.setValue(records == null || records.isEmpty()
                                ? LoadingState.EMPTY : LoadingState.CONTENT);
                    } else {
                        postError(new RuntimeException(result != null ? result.getMessage() : "加载失败"));
                        userLoadingState.setValue(LoadingState.ERROR);
                    }
                }, throwable -> {
                    postError(new RuntimeException(throwable.getMessage()));
                    userLoadingState.setValue(LoadingState.ERROR);
                }));
    }

    public void loadMoreUsers() {
        if (isLoadingUsers || userPage >= userTotalPages) return;
        isLoadingUsers = true;
        userPage++;
        disposables.add(repository.listUsers(userRoleFilter, userStatusFilter, userKeyword, userPage, 20)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    isLoadingUsers = false;
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        List<UserManageVO> records = result.getData().getRecords();
                        List<UserManageVO> current = new ArrayList<>(userList.getValue() != null
                                ? userList.getValue() : new ArrayList<>());
                        if (records != null) {
                            current.addAll(records);
                        }
                        userList.setValue(current);
                    }
                }, throwable -> isLoadingUsers = false));
    }

    public void updateUserStatus(long userId, String newStatus) {
        disposables.add(repository.updateUserStatus(userId, newStatus)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> statusUpdateResult.setValue(result != null && result.isSuccess()),
                        throwable -> statusUpdateResult.setValue(false)));
    }

    // ---- 图书编目 ----
    public LiveData<com.library.android.model.BookDetailVO> getCreatedBook() { return createdBook; }
    public LiveData<com.library.android.model.BookDetailVO> getUpdatedBook() { return updatedBook; }
    public LiveData<Boolean> getDeleteResult() { return deleteResult; }

    public void createBook(BookCreateRequest request) {
        disposables.add(repository.createBook(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        createdBook.setValue(result.getData());
                    } else {
                        postError(new RuntimeException(result != null ? result.getMessage() : "创建失败"));
                    }
                }, throwable -> postError(new RuntimeException(throwable.getMessage()))));
    }

    public void updateBook(long bookId, BookUpdateRequest request) {
        disposables.add(repository.updateBook(bookId, request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        updatedBook.setValue(result.getData());
                    } else {
                        postError(new RuntimeException(result != null ? result.getMessage() : "更新失败"));
                    }
                }, throwable -> postError(new RuntimeException(throwable.getMessage()))));
    }

    public void deleteBook(long bookId) {
        disposables.add(repository.deleteBook(bookId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> deleteResult.setValue(result != null && result.isSuccess()),
                        throwable -> deleteResult.setValue(false)));
    }

    // ---- P1-01：分类树（BookEditActivity 等管理端编目页使用） ----
    public LiveData<List<CategoryVO>> getCategoryTree() { return categoryTree; }

    public void loadCategoryTree() {
        disposables.add(bookRepository.getCategoryTree()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result != null && result.isSuccess() && result.getData() != null) {
                                categoryTree.setValue(result.getData());
                            } else {
                                postError(new RuntimeException(result != null ? result.getMessage() : "分类加载失败"));
                            }
                        },
                        throwable -> postError(new RuntimeException(
                                throwable.getMessage() != null ? throwable.getMessage() : "分类加载失败"))));
    }


}

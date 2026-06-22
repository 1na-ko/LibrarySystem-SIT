package com.library.android.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.library.android.model.BorrowRecordVO;
import com.library.android.model.BorrowStatsVO;
import com.library.android.model.PageResult;
import com.library.android.model.UserProfile;
import com.library.android.repository.AuthRepository;
import com.library.android.repository.UserRepository;
import com.library.android.testutil.PageResults;
import com.library.android.testutil.ResultFactory;
import com.library.android.testutil.RxJava3SchedulerRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import io.reactivex.rxjava3.core.Single;

public class ProfileViewModelTest {

    @Rule public InstantTaskExecutorRule instantTaskRule = new InstantTaskExecutorRule();
    @Rule public RxJava3SchedulerRule rxRule = new RxJava3SchedulerRule();

    private UserRepository userRepo;
    private AuthRepository authRepo;
    private ProfileViewModel viewModel;

    @Before
    public void setUp() {
        userRepo = mock(UserRepository.class);
        authRepo = mock(AuthRepository.class);
        viewModel = new ProfileViewModel(userRepo, authRepo);
    }

    @Test
    public void loadProfile_success_shouldExposeProfileLiveData() {
        UserProfile profile = mock(UserProfile.class);
        when(profile.getUsername()).thenReturn("alice");
        when(userRepo.getMyProfile()).thenReturn(Single.just(ResultFactory.success(profile)));

        viewModel.loadProfile();

        assertNotNull(viewModel.getUserProfile().getValue());
        assertEquals("alice", viewModel.getUserProfile().getValue().getUsername());
    }

    @Test
    public void loadProfile_apiFailure_shouldPostError() {
        when(userRepo.getMyProfile())
                .thenReturn(Single.just(ResultFactory.failure(401, "未登录")));

        viewModel.loadProfile();

        assertNull(viewModel.getUserProfile().getValue());
        assertNotNull(viewModel.getErrorEvent().getValue());
    }

    @Test
    public void loadBorrowStats_shouldExposeStats() {
        BorrowStatsVO stats = mock(BorrowStatsVO.class);
        when(userRepo.getMyStats()).thenReturn(Single.just(ResultFactory.success(stats)));

        viewModel.loadBorrowStats();
        assertNotNull(viewModel.getBorrowStats().getValue());
    }

    @Test
    public void loadBorrowHistory_shouldExposePagedRecords() {
        BorrowRecordVO rec = mock(BorrowRecordVO.class);
        PageResult<BorrowRecordVO> page = PageResults.single(Collections.singletonList(rec));
        when(userRepo.getMyHistory(any(), anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(page)));

        viewModel.loadBorrowHistory(2026, 1);

        assertNotNull(viewModel.getBorrowHistory().getValue());
        assertEquals(1, viewModel.getBorrowHistory().getValue().getRecords().size());
    }

    @Test
    public void loadBorrowHistoryMore_shouldAppendNotReplace() {
        BorrowRecordVO r1 = mock(BorrowRecordVO.class);
        BorrowRecordVO r2 = mock(BorrowRecordVO.class);
        // 第一页
        when(userRepo.getMyHistory(any(), anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(
                        PageResults.of(Collections.singletonList(r1), 1, 2))));
        viewModel.loadBorrowHistory(null, 1);
        assertEquals(1, viewModel.getBorrowHistory().getValue().getRecords().size());

        // 加载更多 → 追加 r2
        when(userRepo.getMyHistory(any(), anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(
                        PageResults.of(Collections.singletonList(r2), 2, 2))));
        viewModel.loadBorrowHistoryMore(null, 2);
        assertEquals(2, viewModel.getBorrowHistory().getValue().getRecords().size());
    }

    @Test
    public void updateProfile_success_shouldSetSaveSuccessAndReloadProfile() {
        when(userRepo.updateMyProfile(anyMap()))
                .thenReturn(Single.just(ResultFactory.success(null)));
        when(userRepo.getMyProfile())
                .thenReturn(Single.just(ResultFactory.success(mock(UserProfile.class))));

        viewModel.updateProfile("e@x.com", "13800138000");

        assertTrue(Boolean.TRUE.equals(viewModel.isSaveSuccess().getValue()));
        verify(userRepo, times(1)).getMyProfile();  // 内部触发 reload
    }

    @Test
    public void logout_shouldEmitLogoutCompletedRegardlessOfBackendResult() {
        // 后端成功
        when(authRepo.logout()).thenReturn(Single.just(ResultFactory.success(null)));
        viewModel.logout();
        assertNotNull(viewModel.getLogoutCompleted().getValue());
        assertEquals(Boolean.FALSE, viewModel.isLoggingOut().getValue());
    }

    @Test
    public void logout_apiError_shouldStillEmitLogoutCompleted() {
        when(authRepo.logout()).thenReturn(Single.error(new RuntimeException("backend down")));
        viewModel.logout();
        // 后端不可达也应触发完成事件（语义：本地仍要清退）
        assertNotNull(viewModel.getLogoutCompleted().getValue());
    }

    @Test
    public void updateProfile_nullEmail_shouldStillCallRepo() {
        when(userRepo.updateMyProfile(anyMap()))
                .thenReturn(Single.just(ResultFactory.success(null)));
        when(userRepo.getMyProfile())
                .thenReturn(Single.just(ResultFactory.success(mock(UserProfile.class))));

        viewModel.updateProfile(null, "13800138000");

        verify(userRepo, times(1)).updateMyProfile(anyMap());
    }
}

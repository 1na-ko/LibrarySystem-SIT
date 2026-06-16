package com.library.android.di;

import com.library.android.data.AppDatabase;
import com.library.android.network.LibraryApi;
import com.library.android.repository.AdminRepository;
import com.library.android.repository.AuthRepository;
import com.library.android.repository.BookRepository;
import com.library.android.repository.BorrowRepository;
import com.library.android.repository.KnowledgeGraphRepository;
import com.library.android.repository.ReservationRepository;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt Repository DI 模块 — 提供各业务域的 Repository 实例.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Module
@InstallIn(SingletonComponent.class)
public class RepositoryModule {

    @Provides
    @Singleton
    public AuthRepository provideAuthRepository(LibraryApi api) {
        return new AuthRepository(api);
    }

    @Provides
    @Singleton
    public BookRepository provideBookRepository(LibraryApi api) {
        return new BookRepository(api);
    }

    @Provides
    @Singleton
    public BorrowRepository provideBorrowRepository(LibraryApi api) {
        return new BorrowRepository(api);
    }

    @Provides
    @Singleton
    public ReservationRepository provideReservationRepository(LibraryApi api) {
        return new ReservationRepository(api);
    }

    @Provides
    @Singleton
    public KnowledgeGraphRepository provideKnowledgeGraphRepository(LibraryApi api) {
        return new KnowledgeGraphRepository(api);
    }

    @Provides
    @Singleton
    public AdminRepository provideAdminRepository(LibraryApi api) {
        return new AdminRepository(api);
    }
}

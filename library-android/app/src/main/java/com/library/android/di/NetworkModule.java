package com.library.android.di;

import android.content.Context;

import com.library.android.BuildConfig;
import com.library.android.network.AuthInterceptor;
import com.library.android.network.LibraryApi;
import com.library.android.network.TokenAuthenticator;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Hilt 网络层 DI 模块 — 提供 OkHttpClient / Retrofit / LibraryApi 实例.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(AuthInterceptor authInterceptor, TokenAuthenticator tokenAuthenticator) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);

        return new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .authenticator(tokenAuthenticator)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    public LibraryApi provideLibraryApi(Retrofit retrofit) {
        return retrofit.create(LibraryApi.class);
    }

    @Provides
    @Singleton
    public AuthInterceptor provideAuthInterceptor(@ApplicationContext Context context) {
        return new AuthInterceptor(context);
    }

    @Provides
    @Singleton
    public TokenAuthenticator provideTokenAuthenticator(
            @ApplicationContext Context context,
            TokenAuthenticator.LibraryApiProvider apiProvider) {
        return new TokenAuthenticator(context, apiProvider);
    }

    @Provides
    @Singleton
    public TokenAuthenticator.LibraryApiProvider provideLibraryApiProvider(LibraryApi api) {
        return () -> api;
    }
}

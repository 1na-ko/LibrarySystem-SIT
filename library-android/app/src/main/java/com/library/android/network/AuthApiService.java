package com.library.android.network;

import com.library.android.model.LoginRequest;
import com.library.android.model.LoginResponse;
import com.library.android.model.Result;

import io.reactivex.rxjava3.core.Single;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApiService {

    @POST("auth/login")
    Single<Result<LoginResponse>> login(@Body LoginRequest request);

    @POST("auth/register")
    Single<Result<Object>> register(@Body LoginRequest request);
}
package com.library.android.network;

import com.library.android.model.*;
import com.library.android.model.BorrowStatsVO;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.*;

/**
 * Retrofit API 接口 — 图书馆智能管理系统全量端点定义.
 *
 * <p>按功能模块分区，对应 OpenAPI 契约 library-api.yaml。
 * <p>统一响应体：{@link Result}，分页结果：{@link PageResult}。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface LibraryApi {

    // ======================== 认证模块（人员 A 主导） ========================

    @POST("auth/login")
    Call<Result<LoginResponse>> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<Result<LoginResponse>> register(@Body RegisterRequest request);

    @POST("auth/refresh")
    Call<Result<RefreshResponse>> refreshToken(@Body RefreshRequest request);

    @POST("auth/logout")
    Call<Result<Void>> logout();

    // ======================== 图书检索模块（人员 A 主导） ========================

    /** 后端实际返回 BookSimpleVO（列表摘要不含 description/location/totalCopies/borrowCount）. */
    @GET("books/search")
    Call<Result<PageResult<BookSimpleVO>>> searchBooks(
            @Query("keyword") String keyword,
            @Query("author") String author,
            @Query("categoryId") Long categoryId,
            @Query("sortBy") String sortBy,
            @Query("pageNum") int pageNum,
            @Query("pageSize") int pageSize
    );

    /** 后端实际返回 BookSimpleVO. */
    @GET("books/search/advanced")
    Call<Result<PageResult<BookSimpleVO>>> advancedSearch(
            @Query("title") String title,
            @Query("author") String author,
            @Query("isbn") String isbn,
            @Query("publisher") String publisher,
            @Query("pubYearFrom") Integer pubYearFrom,
            @Query("pubYearTo") Integer pubYearTo,
            @Query("categoryId") Long categoryId,
            @Query("onlyAvailable") Boolean onlyAvailable,
            @Query("pageNum") int pageNum,
            @Query("pageSize") int pageSize
    );

    /** WP-4 契约对齐：返回类型由 Map&lt;String,String&gt; 改为 SuggestVO（含 text + type 字段）. */
    @GET("books/suggest")
    Call<Result<List<SuggestVO>>> suggest(@Query("prefix") String prefix, @Query("limit") int limit);

    /** 后端实际返回 BookSimpleVO. */
    @GET("books/hot")
    Call<Result<List<BookSimpleVO>>> hotBooks(@Query("categoryId") Long categoryId, @Query("limit") int limit);

    @GET("books/{id}")
    Call<Result<BookDetailVO>> getBookDetail(@Path("id") long bookId);

    @GET("books/{id}/related")
    Call<Result<List<BookRecommendVO>>> getRelatedBooks(@Path("id") long bookId, @Query("limit") int limit);

    // ======================== 分类管理模块（人员 A 主导） ========================

    @GET("categories/tree")
    Call<Result<List<CategoryVO>>> getCategoryTree();

    @GET("categories")
    Call<Result<List<CategoryVO>>> listCategories(@Query("parentId") Long parentId);

    @GET("categories/{id}")
    Call<Result<CategoryVO>> getCategory(@Path("id") long categoryId);

    // ======================== 借阅管理模块（人员 B 主导） ========================

    @POST("borrows")
    Call<Result<BorrowResultVO>> borrowBook(@Body BorrowRequest request);

    /** 后端路径 /borrows（非 /borrows/my），认证用户自动限定本人数据. */
    @GET("borrows")
    Call<Result<PageResult<BorrowRecordVO>>> getMyBorrows(
            @Query("status") String status,
            @Query("pageNum") int pageNum,
            @Query("pageSize") int pageSize
    );

    @GET("borrows/{id}")
    Call<Result<BorrowRecordVO>> getBorrowDetail(@Path("id") long borrowId);

    @PUT("borrows/{id}/return")
    Call<Result<BorrowRecordVO>> returnBook(@Path("id") long borrowId);

    @PUT("borrows/{id}/renew")
    Call<Result<RenewResultVO>> renewBook(@Path("id") long borrowId);

    /** 需 LIBRARIAN/ADMIN 角色，路径在 /admin 下. */
    @GET("admin/borrows/overdue")
    Call<Result<PageResult<BorrowRecordVO>>> getOverdueRecords(
            @Query("pageNum") int pageNum,
            @Query("pageSize") int pageSize
    );

    // ======================== 预约管理模块（人员 B 主导） ========================

    @POST("reservations")
    Call<Result<ReservationVO>> reserveBook(@Body ReservationRequest request);

    /** 后端路径 /reservations（非 /reservations/my），认证用户自动限定本人数据. */
    @GET("reservations")
    Call<Result<PageResult<ReservationVO>>> getMyReservations(
            @Query("status") String status,
            @Query("pageNum") int pageNum,
            @Query("pageSize") int pageSize
    );

    @DELETE("reservations/{id}")
    Call<Result<Void>> cancelReservation(@Path("id") long reservationId);

    /** 后端当前返回 Result&lt;Integer&gt;（仅排队序号），非含 totalWaiting 的对象. */
    @GET("reservations/{id}/queue-position")
    Call<Result<Integer>> getQueuePosition(@Path("id") long reservationId);

    // ======================== 个人中心模块（人员 A 主导） ========================

    @GET("users/me")
    Call<Result<UserProfile>> getMyProfile();

    /** 后端返回 Result&lt;Void&gt;（data 为 null），编辑成功后需重新 GET /users/me 刷新. */
    @PUT("users/me")
    Call<Result<Void>> updateMyProfile(@Body Map<String, String> body);

    @GET("users/me/history")
    Call<Result<PageResult<BorrowRecordVO>>> getMyHistory(
            @Query("year") Integer year,
            @Query("pageNum") int pageNum,
            @Query("pageSize") int pageSize
    );

    @GET("users/me/stats")
    Call<Result<BorrowStatsVO>> getMyStats();

    @GET("users/me/recommendations")
    Call<Result<List<BookRecommendVO>>> getRecommendations(@Query("limit") int limit);

    /**
     * SSE 流式推荐（C.1 流式增强）：书目秒回 + LLM 导语逐 token.
     * <p>produces text/event-stream，事件序列：books / reason(多次) / done.
     * 用 @Streaming 避免 Retrofit 一次性读入 body，支持逐帧读取.
     */
    @Streaming
    @GET("users/me/recommendations/stream")
    Call<okhttp3.ResponseBody> streamRecommendations(@Query("limit") int limit);

    // ======================== 知识图谱模块（人员 B 主导） ========================

    /** 后端路径 /kg/book/{bookId}（无 /graph 后缀）. */
    @GET("kg/book/{bookId}")
    Call<Result<KnowledgeGraphVO>> getBookGraph(@Path("bookId") long bookId, @Query("depth") int depth);

    @GET("kg/book/{bookId}/trace")
    Call<Result<TraceGraph>> traceLiterature(
            @Path("bookId") long bookId,
            @Query("direction") String direction,
            @Query("maxDepth") int maxDepth
    );

    @GET("kg/subject/{name}")
    Call<Result<KnowledgeGraphVO>> getSubjectNetwork(@Path(value = "name", encoded = true) String name, @Query("topK") int topK);

    /** 后端返回 KnowledgeGraphVO（nodes+edges），Repository 层转换为 List&lt;EntitySearchResult&gt;. */
    @GET("kg/search")
    Call<Result<KnowledgeGraphVO>> searchEntities(
            @Query("entity") String entity,
            @Query("type") String type
    );

    /** 关键路径查询 — 两本图书之间的最短引用/关键词路径（C.3 新增）. */
    @GET("kg/book/{bookId}/keypath")
    Call<Result<TraceGraph>> getKeyPath(
            @Path("bookId") long bookId,
            @Query("targetBookId") long targetBookId
    );

    /** 单本图书图谱重建（管理员，kg:admin 权限，C.3 新增）. */
    @POST("admin/kg/rebuild/{bookId}")
    Call<Result<Void>> rebuildKgForBook(@Path("bookId") long bookId);

    /** 全量图谱重建（管理员，kg:admin 权限，返回处理图书数量，C.3 新增）. */
    @POST("admin/kg/rebuild-all")
    Call<Result<Integer>> rebuildKgAll();

    // ======================== 系统管理模块（人员 B 主导） ========================

    /** 后端分页参数名为 page / size（非 pageNum / pageSize）. */
    @GET("admin/users")
    Call<Result<PageResult<UserManageVO>>> listUsers(
            @Query("role") String role,
            @Query("status") String status,
            @Query("keyword") String keyword,
            @Query("page") int page,
            @Query("size") int size
    );

    @PUT("admin/users/{id}/status")
    Call<Result<Void>> updateUserStatus(@Path("id") long userId, @Body UserStatusUpdateRequest request);

    /** WP-4 契约对齐：后端 AdminBookController 返回 BookDetailVO（含 keywords/relatedBooks 完整字段）. */
    @POST("admin/books")
    Call<Result<BookDetailVO>> createBook(@Body BookCreateRequest request);

    @PUT("admin/books/{id}")
    Call<Result<BookDetailVO>> updateBook(@Path("id") long bookId, @Body BookUpdateRequest request);

    @DELETE("admin/books/{id}")
    Call<Result<Void>> deleteBook(@Path("id") long bookId);

    /** 流通统计 Dashboard（C.2 新增，需 LIBRARIAN/ADMIN 角色）. */
    @GET("admin/stats/dashboard")
    Call<Result<DashboardVO>> getDashboard();

    // ======================== 智能采编模块（人员 B·可选） ========================

    @GET("acquisition/predict")
    Call<Result<List<PurchasePredictionVO>>> predictDemand(@Query("subjectId") long subjectId, @Query("months") int months);

    @POST("acquisition/duplicate-check")
    Call<Result<DuplicateCheckResult>> checkDuplicate(@Body Map<String, String> body);

    @GET("acquisition/gap-analysis")
    Call<Result<GapAnalysisResult>> analyzeGap(@Query("subjectId") long subjectId);

    /** WP5：供应商列表（下拉选择用）. */
    @GET("acquisition/suppliers")
    Call<Result<List<SupplierVO>>> listSuppliers();

    /** WP5：电子资源列表（下拉选择用）. */
    @GET("acquisition/resources")
    Call<Result<List<ElectronicResourceVO>>> listResources();

    /**
     * 创建谈判会话 — 后端使用 @RequestParam（Query 参数）.
     *
     * <p>D.1 修复：原签名包含 negotiatorId 是 OpenAPI 描述与代码不符的产物，
     * 后端 AcquisitionController 实际从 SecurityUtils.getCurrentUserId() 取，
     * 客户端发送也会被忽略。移除该参数避免误导.
     *
     * <p>返回 {@link NegotiationVO}（会话元信息），单独通过
     * {@link #getNegotiationSuggestion} 获取 LLM 建议结果.
     */
    @POST("acquisition/negotiation")
    Call<Result<NegotiationVO>> createNegotiation(
            @Query("resourceId") long resourceId,
            @Query("supplierId") long supplierId
    );

    @GET("acquisition/negotiation/{id}/suggestion")
    Call<Result<NegotiationSuggestion>> getNegotiationSuggestion(@Path("id") long negotiationId);

    /** WP6：谈判建议 SSE 流式（priceRange JSON 秒回 + text 多次逐 token + done）. */
    @Streaming
    @GET("acquisition/negotiation/{id}/suggestion/stream")
    Call<okhttp3.ResponseBody> streamNegotiationSuggestion(@Path("id") long negotiationId);

}

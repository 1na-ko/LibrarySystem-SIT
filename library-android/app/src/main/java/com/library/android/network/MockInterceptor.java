package com.library.android.network;

import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 模拟拦截器 — 不依赖后端即可测试全部前端功能.
 *
 * <p>拦截所有 API 请求，返回模拟数据，使前端可以在无后端环境下独立验证。
 * <p>Debug 模式下自动启用，Release 模式下自动关闭。
 * <p>每个请求都会在 logcat 中输出 TAG=MockInterceptor 的日志。
 */
public class MockInterceptor implements Interceptor {

    private static final String TAG = "MockInterceptor";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private boolean enabled = false;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String path = chain.request().url().encodedPath();
        String method = chain.request().method();
        String query = chain.request().url().encodedQuery();
        String fullUrl = chain.request().url().toString();

        if (!enabled) {
            Log.d(TAG, "Mock 已禁用 → 放行: " + method + " " + fullUrl);
            return chain.proceed(chain.request());
        }

        Log.i(TAG, "============================================");
        Log.i(TAG, "Mock 拦截请求: " + method + " " + fullUrl);
        if (query != null) {
            Log.i(TAG, "  查询参数: " + query);
        }

        String responseBody = dispatch(method, path, query);

        if (responseBody != null) {
            Log.i(TAG, "Mock 返回成功 (200)");
            Log.i(TAG, "============================================");
            return new Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(responseBody, JSON))
                    .build();
        }

        Log.w(TAG, "Mock 未匹配，放行到真实后端: " + method + " " + path);
        Log.i(TAG, "============================================");
        return chain.proceed(chain.request());
    }

    private String dispatch(String method, String path, String query) {
        // ======================== 认证模块 ========================
        if (path.contains("/auth/login") && method.equals("POST")) {
            return mockLogin();
        }
        if (path.contains("/auth/register") && method.equals("POST")) {
            return mockRegister();
        }
        if (path.contains("/auth/refresh") && method.equals("POST")) {
            return mockRefreshToken();
        }
        if (path.contains("/auth/logout") && method.equals("POST")) {
            return mockSuccess("登出成功");
        }

        // ======================== 图书检索模块 ========================
        if (path.contains("/books/search/advanced") && method.equals("GET")) {
            return mockBookSearchResult();
        }
        if (path.contains("/books/search") && method.equals("GET")) {
            return mockBookSearchResult();
        }
        if (path.contains("/books/suggest") && method.equals("GET")) {
            return mockSuggest();
        }
        if (path.contains("/books/hot") && method.equals("GET")) {
            return mockHotBooks();
        }
        if (path.contains("/books/") && path.contains("/related") && method.equals("GET")) {
            return mockRelatedBooks();
        }
        if (path.contains("/books/") && method.equals("GET")) {
            return mockBookDetail();
        }

        // ======================== 分类管理模块 ========================
        if (path.contains("/categories/tree") && method.equals("GET")) {
            return mockCategoryTree();
        }
        if (path.contains("/categories/") && method.equals("GET")) {
            return mockCategoryDetail();
        }
        if (path.contains("/categories") && method.equals("GET")) {
            return mockCategoryList();
        }

        // ======================== 借阅管理模块 ========================
        if (path.contains("/borrows/overdue") && method.equals("GET")) {
            return mockOverdueRecords();
        }
        if (path.contains("/borrows/") && path.contains("/return") && method.equals("PUT")) {
            return mockSuccess("归还成功");
        }
        if (path.contains("/borrows/") && path.contains("/renew") && method.equals("PUT")) {
            return mockRenewResult();
        }
        if (path.contains("/borrows/") && method.equals("GET")) {
            return mockBorrowDetail();
        }
        if (path.contains("/borrows") && method.equals("POST")) {
            return mockBorrowResult();
        }
        if (path.contains("/borrows/my") && method.equals("GET")) {
            return mockMyBorrows();
        }

        // ======================== 预约管理模块 ========================
        if (path.contains("/reservations/") && path.contains("/queue-position") && method.equals("GET")) {
            return mockQueuePosition();
        }
        if (path.contains("/reservations/") && method.equals("DELETE")) {
            return mockSuccess("取消预约成功");
        }
        if (path.contains("/reservations") && method.equals("POST")) {
            return mockReservationCreated();
        }
        if (path.contains("/reservations/my") && method.equals("GET")) {
            return mockMyReservations();
        }

        // ======================== 个人中心模块 ========================
        if (path.contains("/users/me/stats") && method.equals("GET")) {
            return mockBorrowStats();
        }
        if (path.contains("/users/me/history") && method.equals("GET")) {
            return mockBorrowHistory();
        }
        if (path.contains("/users/me/recommendations") && method.equals("GET")) {
            return mockRecommendations();
        }
        if (path.contains("/users/me") && method.equals("PUT")) {
            return mockUpdateProfile();
        }
        if (path.contains("/users/me") && method.equals("GET")) {
            return mockMyProfile();
        }

        // ======================== 知识图谱模块 ========================
        if (path.contains("/kg/book/") && path.contains("/trace") && method.equals("GET")) {
            return mockLiteratureTrace();
        }
        if (path.contains("/kg/book/") && path.contains("/graph") && method.equals("GET")) {
            return mockKnowledgeGraph();
        }
        if (path.contains("/kg/subject/") && method.equals("GET")) {
            return mockSubjectNetwork();
        }
        if (path.contains("/kg/search") && method.equals("GET")) {
            return mockEntitySearch();
        }

        // ======================== 系统管理模块 ========================
        if (path.contains("/admin/users/") && path.contains("/status") && method.equals("PUT")) {
            return mockSuccess("状态变更成功");
        }
        if (path.contains("/admin/users") && method.equals("GET")) {
            return mockAdminUserList();
        }
        if (path.contains("/admin/books/") && method.equals("DELETE")) {
            return mockSuccess("删除成功");
        }
        if (path.contains("/admin/books/") && method.equals("PUT")) {
            return mockAdminBookUpdated();
        }
        if (path.contains("/admin/books") && method.equals("POST")) {
            return mockAdminBookCreated();
        }

        // ======================== 智能采编模块 ========================
        if (path.contains("/acquisition/predict") && method.equals("GET")) {
            return mockSuccess("{}");
        }
        if (path.contains("/acquisition/duplicate-check") && method.equals("POST")) {
            return mockSuccess("{}");
        }
        if (path.contains("/acquisition/gap-analysis") && method.equals("GET")) {
            return mockSuccess("{}");
        }
        if (path.contains("/acquisition/negotiation/") && path.contains("/suggestion") && method.equals("GET")) {
            return mockSuccess("{}");
        }
        if (path.contains("/acquisition/negotiation") && method.equals("POST")) {
            return mockSuccess("{}");
        }

        // ======================== 健康检查 ========================
        if (path.contains("/health") && method.equals("GET")) {
            return "{\"status\":\"UP\"}";
        }

        return null;
    }

    // ======================== 辅助方法 ========================

    private String mockSuccess(String message) {
        return "{\"code\":200,\"message\":\"" + message + "\",\"data\":null,\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String now() {
        return "2026-06-16";
    }

    // ================================================================
    // 认证模块 mock 数据
    // ================================================================

    private String mockLogin() {
        Log.d(TAG, "  → 模拟登录响应");
        return "{\"code\":200,\"message\":\"登录成功\",\"data\":{" +
                "\"accessToken\":\"mock_access_token_abc123\"," +
                "\"refreshToken\":\"mock_refresh_token_xyz789\"," +
                "\"tokenType\":\"Bearer\"," +
                "\"expiresIn\":7200," +
                "\"user\":{" +
                "\"id\":1,\"username\":\"2024001001\",\"realName\":\"模拟用户\"," +
                "\"role\":\"STUDENT\",\"email\":\"mock@university.edu.cn\"," +
                "\"status\":\"ACTIVE\"}}," +
                "\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockRegister() {
        Log.d(TAG, "  → 模拟注册响应");
        return "{\"code\":200,\"message\":\"注册成功\"," +
                "\"data\":null," +
                "\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockRefreshToken() {
        Log.d(TAG, "  → 模拟Token刷新响应");
        return "{\"code\":200,\"message\":\"刷新成功\",\"data\":{" +
                "\"accessToken\":\"mock_new_access_token_456\"," +
                "\"refreshToken\":\"mock_new_refresh_token_012\"," +
                "\"tokenType\":\"Bearer\"," +
                "\"expiresIn\":7200}," +
                "\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    // ================================================================
    // 图书检索模块 mock 数据
    // ================================================================

    private String mockBookSearchResult() {
        Log.d(TAG, "  → 模拟图书搜索结果");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":{" +
                "\"records\":[" +
                bookJson(1, "978-7-302-11111-1", "数据结构与算法分析", "Mark Allen Weiss", "清华大学出版社", "2020-01-15", "计算机科学", "经典数据结构教材，涵盖算法分析与设计", "A区3排5号", 3, 5, 128) + "," +
                bookJson(2, "978-7-111-22222-2", "深入理解Java虚拟机", "周志明", "机械工业出版社", "2019-06-01", "计算机科学", "JVM原理与性能调优", "A区2排1号", 0, 4, 256) + "," +
                bookJson(3, "978-7-121-33333-3", "人工智能：一种现代方法", "Stuart Russell", "电子工业出版社", "2021-03-10", "人工智能", "AI领域经典教材，涵盖机器学习与深度学习", "B区1排3号", 2, 3, 89) + "," +
                bookJson(4, "978-7-115-44444-4", "设计模式：可复用面向对象软件的基础", "GoF", "人民邮电出版社", "2018-09-20", "软件工程", "23种经典设计模式详解", "A区1排2号", 1, 2, 312) + "," +
                bookJson(5, "978-7-302-55555-5", "操作系统概念", "Abraham Silberschatz", "清华大学出版社", "2020-08-01", "计算机科学", "操作系统原理与实践", "A区3排8号", 4, 6, 95) + "," +
                bookJson(6, "978-7-111-66666-6", "计算机网络：自顶向下方法", "James Kurose", "机械工业出版社", "2021-01-15", "网络技术", "计算机网络经典教材", "B区2排4号", 0, 3, 178) + "," +
                bookJson(7, "978-7-121-77777-7", "Python编程：从入门到实践", "Eric Matthes", "人民邮电出版社", "2022-05-20", "编程语言", "Python入门经典", "A区4排2号", 5, 8, 420) + "," +
                bookJson(8, "978-7-302-88888-8", "数据库系统概念", "Abraham Silberschatz", "清华大学出版社", "2019-11-12", "计算机科学", "数据库系统原理", "A区3排9号", 2, 4, 156) + "," +
                bookJson(9, "978-7-111-99999-9", "Spring实战", "Craig Walls", "机械工业出版社", "2022-03-01", "软件工程", "Spring框架实战指南", "A区2排5号", 3, 5, 203) + "," +
                bookJson(10, "978-7-121-00000-1", "机器学习实战", "Peter Harrington", "电子工业出版社", "2020-12-10", "人工智能", "机器学习算法实践", "B区1排6号", 1, 3, 267) + "," +
                bookJson(11, "978-7-115-00000-2", "编译原理", "Alfred Aho", "机械工业出版社", "2018-05-15", "计算机科学", "编译器设计与实现", "A区1排7号", 2, 2, 45) + "," +
                bookJson(12, "978-7-302-00000-3", "线性代数及其应用", "David Lay", "人民邮电出版社", "2021-09-01", "数学", "线性代数理论与实践", "C区1排1号", 6, 10, 88) + "," +
                bookJson(13, "978-7-111-00000-4", "人月神话", "Frederick Brooks", "清华大学出版社", "2017-08-20", "软件工程", "软件工程管理经典", "A区1排9号", 1, 2, 134) + "," +
                bookJson(14, "978-7-121-00000-5", "统计学习方法", "李航", "清华大学出版社", "2019-04-15", "人工智能", "机器学习理论经典", "B区1排2号", 0, 5, 389) + "," +
                bookJson(15, "978-7-115-00000-6", "算法导论", "Thomas Cormen", "机械工业出版社", "2020-07-01", "计算机科学", "算法领域权威教材", "A区3排1号", 2, 4, 456) +
                "]," +
                "\"total\":24,\"pageNum\":1,\"pageSize\":15,\"totalPages\":2" +
                "},\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockSuggest() {
        Log.d(TAG, "  → 模拟搜索建议");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":[" +
                "{\"value\":\"数据结构\",\"label\":\"数据结构 — 图书\"}," +
                "{\"value\":\"数据科学\",\"label\":\"数据科学 — 图书\"}," +
                "{\"value\":\"数据库\",\"label\":\"数据库 — 图书\"}," +
                "{\"value\":\"数据挖掘\",\"label\":\"数据挖掘 — 图书\"}," +
                "{\"value\":\"数据分析\",\"label\":\"数据分析 — 图书\"}" +
                "],\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockHotBooks() {
        Log.d(TAG, "  → 模拟热门图书");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":[" +
                bookJson(1, "978-7-302-11111-1", "数据结构与算法分析", "Mark Allen Weiss", "清华大学出版社", "2020-01-15", "计算机科学", "经典数据结构教材", "A区3排5号", 3, 5, 128) + "," +
                bookJson(7, "978-7-121-77777-7", "Python编程：从入门到实践", "Eric Matthes", "人民邮电出版社", "2022-05-20", "编程语言", "Python入门经典", "A区4排2号", 5, 8, 420) + "," +
                bookJson(14, "978-7-121-00000-5", "统计学习方法", "李航", "清华大学出版社", "2019-04-15", "人工智能", "机器学习理论经典", "B区1排2号", 0, 5, 389) + "," +
                bookJson(15, "978-7-115-00000-6", "算法导论", "Thomas Cormen", "机械工业出版社", "2020-07-01", "计算机科学", "算法领域权威教材", "A区3排1号", 2, 4, 456) + "," +
                bookJson(4, "978-7-115-44444-4", "设计模式", "GoF", "人民邮电出版社", "2018-09-20", "软件工程", "23种经典设计模式", "A区1排2号", 1, 2, 312) + "," +
                bookJson(2, "978-7-111-22222-2", "深入理解Java虚拟机", "周志明", "机械工业出版社", "2019-06-01", "计算机科学", "JVM原理与性能调优", "A区2排1号", 0, 4, 256) + "," +
                bookJson(3, "978-7-121-33333-3", "人工智能：一种现代方法", "Stuart Russell", "电子工业出版社", "2021-03-10", "人工智能", "AI领域经典教材", "B区1排3号", 2, 3, 89) + "," +
                bookJson(6, "978-7-111-66666-6", "计算机网络", "James Kurose", "机械工业出版社", "2021-01-15", "网络技术", "计算机网络经典教材", "B区2排4号", 0, 3, 178) + "," +
                bookJson(8, "978-7-302-88888-8", "数据库系统概念", "Abraham Silberschatz", "清华大学出版社", "2019-11-12", "计算机科学", "数据库系统原理", "A区3排9号", 2, 4, 156) + "," +
                bookJson(10, "978-7-121-00000-1", "机器学习实战", "Peter Harrington", "电子工业出版社", "2020-12-10", "人工智能", "机器学习算法实践", "B区1排6号", 1, 3, 267) +
                "],\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockBookDetail() {
        Log.d(TAG, "  → 模拟图书详情");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":{" +
                "\"id\":1,\"isbn\":\"978-7-302-11111-1\"," +
                "\"title\":\"数据结构与算法分析\"," +
                "\"author\":\"Mark Allen Weiss\"," +
                "\"publisher\":\"清华大学出版社\"," +
                "\"pubDate\":\"2020-01-15\"," +
                "\"categoryName\":\"计算机科学\"," +
                "\"description\":\"经典数据结构教材，涵盖算法分析与设计，包括链表、栈、队列、树、图、排序算法等核心内容。\"," +
                "\"coverUrl\":\"https://img2.doubanio.com/view/subject/l/public/s1234567.jpg\"," +
                "\"location\":\"A区3排5号\"," +
                "\"availCopies\":3,\"totalCopies\":5,\"borrowCount\":128," +
                "\"keywords\":[\"数据结构\",\"算法\",\"C++\",\"编程\"]," +
                "\"relatedBooks\":[" +
                bookSimpleJson(15, "978-7-115-00000-6", "算法导论", "Thomas Cormen", "机械工业出版社", "计算机科学", 2, 4) + "," +
                bookSimpleJson(2, "978-7-111-22222-2", "深入理解Java虚拟机", "周志明", "机械工业出版社", "计算机科学", 0, 4) + "," +
                bookSimpleJson(8, "978-7-302-88888-8", "数据库系统概念", "Abraham Silberschatz", "清华大学出版社", "计算机科学", 2, 4) +
                "]," +
                "\"reservationCount\":2" +
                "},\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockRelatedBooks() {
        Log.d(TAG, "  → 模拟相关图书推荐");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":[" +
                recommendJson(15, "978-7-115-00000-6", "算法导论", "Thomas Cormen", "机械工业出版社", "计算机科学", 2, 4, 0.952, "您借阅过同类算法书籍，此书是算法领域权威教材") + "," +
                recommendJson(2, "978-7-111-22222-2", "深入理解Java虚拟机", "周志明", "机械工业出版社", "计算机科学", 0, 4, 0.871, "与您借阅过的书籍有相似的编程主题") + "," +
                recommendJson(4, "978-7-115-44444-4", "设计模式", "GoF", "人民邮电出版社", "软件工程", 1, 2, 0.834, "知识图谱显示此书与您研究方向有紧密关联") + "," +
                recommendJson(11, "978-7-115-00000-2", "编译原理", "Alfred Aho", "机械工业出版社", "计算机科学", 2, 2, 0.796, "与您借阅偏好相似的读者也喜欢此书") + "," +
                recommendJson(5, "978-7-302-55555-5", "操作系统概念", "Abraham Silberschatz", "清华大学出版社", "计算机科学", 4, 6, 0.753, "此书内容与您借阅过的书籍高度相关") +
                "],\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    // ================================================================
    // 分类管理 mock 数据
    // ================================================================

    private String mockCategoryTree() {
        Log.d(TAG, "  → 模拟分类树");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":[" +
                "{\"id\":1,\"name\":\"计算机科学\",\"parentId\":null,\"sortOrder\":1,\"children\":[" +
                "{\"id\":5,\"name\":\"编程语言\",\"parentId\":1,\"sortOrder\":1,\"children\":[" +
                "{\"id\":8,\"name\":\"Java\",\"parentId\":5,\"sortOrder\":1,\"children\":null}," +
                "{\"id\":9,\"name\":\"Python\",\"parentId\":5,\"sortOrder\":2,\"children\":null}," +
                "{\"id\":10,\"name\":\"C++\",\"parentId\":5,\"sortOrder\":3,\"children\":null}" +
                "]}," +
                "{\"id\":6,\"name\":\"软件工程\",\"parentId\":1,\"sortOrder\":2,\"children\":null}," +
                "{\"id\":7,\"name\":\"网络技术\",\"parentId\":1,\"sortOrder\":3,\"children\":null}" +
                "]}," +
                "{\"id\":2,\"name\":\"人工智能\",\"parentId\":null,\"sortOrder\":2,\"children\":[" +
                "{\"id\":11,\"name\":\"机器学习\",\"parentId\":2,\"sortOrder\":1,\"children\":null}," +
                "{\"id\":12,\"name\":\"深度学习\",\"parentId\":2,\"sortOrder\":2,\"children\":null}," +
                "{\"id\":13,\"name\":\"自然语言处理\",\"parentId\":2,\"sortOrder\":3,\"children\":null}" +
                "]}," +
                "{\"id\":3,\"name\":\"数学\",\"parentId\":null,\"sortOrder\":3,\"children\":[" +
                "{\"id\":14,\"name\":\"线性代数\",\"parentId\":3,\"sortOrder\":1,\"children\":null}," +
                "{\"id\":15,\"name\":\"概率统计\",\"parentId\":3,\"sortOrder\":2,\"children\":null}" +
                "]}," +
                "{\"id\":4,\"name\":\"文学\",\"parentId\":null,\"sortOrder\":4,\"children\":[" +
                "{\"id\":16,\"name\":\"中国文学\",\"parentId\":4,\"sortOrder\":1,\"children\":null}," +
                "{\"id\":17,\"name\":\"外国文学\",\"parentId\":4,\"sortOrder\":2,\"children\":null}" +
                "]}" +
                "],\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockCategoryList() {
        Log.d(TAG, "  → 模拟分类列表");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":[" +
                "{\"id\":1,\"name\":\"计算机科学\",\"parentId\":null,\"sortOrder\":1,\"children\":null}," +
                "{\"id\":2,\"name\":\"人工智能\",\"parentId\":null,\"sortOrder\":2,\"children\":null}," +
                "{\"id\":3,\"name\":\"数学\",\"parentId\":null,\"sortOrder\":3,\"children\":null}," +
                "{\"id\":4,\"name\":\"文学\",\"parentId\":null,\"sortOrder\":4,\"children\":null}," +
                "{\"id\":5,\"name\":\"编程语言\",\"parentId\":1,\"sortOrder\":1,\"children\":null}," +
                "{\"id\":6,\"name\":\"软件工程\",\"parentId\":1,\"sortOrder\":2,\"children\":null}," +
                "{\"id\":7,\"name\":\"网络技术\",\"parentId\":1,\"sortOrder\":3,\"children\":null}" +
                "],\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockCategoryDetail() {
        Log.d(TAG, "  → 模拟分类详情");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":{" +
                "\"id\":1,\"name\":\"计算机科学\",\"parentId\":null,\"sortOrder\":1,\"children\":[" +
                "{\"id\":5,\"name\":\"编程语言\",\"parentId\":1,\"sortOrder\":1,\"children\":null}," +
                "{\"id\":6,\"name\":\"软件工程\",\"parentId\":1,\"sortOrder\":2,\"children\":null}," +
                "{\"id\":7,\"name\":\"网络技术\",\"parentId\":1,\"sortOrder\":3,\"children\":null}" +
                "]},\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    // ================================================================
    // 借阅管理 mock 数据
    // ================================================================

    private String mockMyBorrows() {
        Log.d(TAG, "  → 模拟我的借阅");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":{" +
                "\"records\":[" +
                borrowRecordJson(1, 1, "数据结构与算法分析", "Mark Allen Weiss", "计算机科学", "2026-05-15", "2026-06-14", null, "BORROWED", 0, 0.0) + "," +
                borrowRecordJson(2, 3, "人工智能：一种现代方法", "Stuart Russell", "人工智能", "2026-05-20", "2026-06-19", null, "BORROWED", 1, 0.0) + "," +
                borrowRecordJson(3, 7, "Python编程：从入门到实践", "Eric Matthes", "编程语言", "2026-04-10", "2026-05-10", "2026-05-05", "RETURNED", 0, 0.0) + "," +
                borrowRecordJson(4, 4, "设计模式", "GoF", "软件工程", "2026-03-01", "2026-03-30", "2026-03-28", "RETURNED", 0, 0.0) + "," +
                borrowRecordJson(5, 6, "计算机网络", "James Kurose", "网络技术", "2026-04-15", "2026-05-15", null, "OVERDUE", 0, 5.5) + "," +
                borrowRecordJson(6, 10, "机器学习实战", "Peter Harrington", "人工智能", "2026-05-01", "2026-05-31", null, "RENEWED", 1, 0.0) +
                "]," +
                "\"total\":6,\"pageNum\":1,\"pageSize\":10,\"totalPages\":1" +
                "},\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockBorrowDetail() {
        Log.d(TAG, "  → 模拟借阅详情");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":" +
                borrowRecordJson(1, 1, "数据结构与算法分析", "Mark Allen Weiss", "计算机科学", "2026-05-15", "2026-06-14", null, "BORROWED", 0, 0.0) +
                ",\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockBorrowResult() {
        Log.d(TAG, "  → 模拟借书结果");
        return "{\"code\":200,\"message\":\"借书成功\",\"data\":{" +
                "\"borrowId\":100,\"bookTitle\":\"数据结构与算法分析\"," +
                "\"dueDate\":\"2026-07-16\"," +
                "\"status\":\"BORROWED\"" +
                "},\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockRenewResult() {
        Log.d(TAG, "  → 模拟续借结果");
        return "{\"code\":200,\"message\":\"续借成功\",\"data\":{" +
                "\"borrowId\":1,\"newDueDate\":\"2026-07-14\"," +
                "\"renewCount\":1,\"maxRenewReached\":false" +
                "},\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockOverdueRecords() {
        Log.d(TAG, "  → 模拟超期记录");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":{" +
                "\"records\":[" +
                borrowRecordJson(5, 6, "计算机网络", "James Kurose", "网络技术", "2026-04-15", "2026-05-15", null, "OVERDUE", 0, 5.5) + "," +
                borrowRecordJson(7, 14, "统计学习方法", "李航", "人工智能", "2026-03-20", "2026-04-19", null, "OVERDUE", 0, 15.0) +
                "]," +
                "\"total\":2,\"pageNum\":1,\"pageSize\":10,\"totalPages\":1" +
                "},\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    // ================================================================
    // 预约管理 mock 数据
    // ================================================================

    private String mockMyReservations() {
        Log.d(TAG, "  → 模拟我的预约");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":{" +
                "\"records\":[" +
                reservationJson(1, 2, "深入理解Java虚拟机", "周志明", "2026-06-10", "WAITING", 3, null) + "," +
                reservationJson(2, 6, "计算机网络", "James Kurose", "2026-06-08", "NOTIFIED", 1, "2026-06-18") + "," +
                reservationJson(3, 14, "统计学习方法", "李航", "2026-05-20", "COMPLETED", 0, null) +
                "]," +
                "\"total\":3,\"pageNum\":1,\"pageSize\":10,\"totalPages\":1" +
                "},\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockReservationCreated() {
        Log.d(TAG, "  → 模拟预约创建");
        return "{\"code\":200,\"message\":\"预约成功\",\"data\":{" +
                "\"id\":10,\"book\":{" +
                "\"id\":2,\"isbn\":\"978-7-111-22222-2\",\"title\":\"深入理解Java虚拟机\"," +
                "\"author\":\"周志明\",\"publisher\":\"机械工业出版社\"," +
                "\"categoryName\":\"计算机科学\",\"coverUrl\":\"\",\"availCopies\":0" +
                "}," +
                "\"reserveTime\":\"2026-06-16\",\"queuePosition\":4,\"status\":\"WAITING\"," +
                "\"expireTime\":null" +
                "},\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockQueuePosition() {
        Log.d(TAG, "  → 模拟排队位置");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":{" +
                "\"reservationId\":1,\"queuePosition\":3,\"totalWaiting\":5" +
                "},\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    // ================================================================
    // 个人中心 mock 数据
    // ================================================================

    private String mockMyProfile() {
        Log.d(TAG, "  → 模拟个人信息");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":{" +
                "\"id\":1,\"username\":\"2024001001\",\"realName\":\"模拟用户\"," +
                "\"role\":\"STUDENT\",\"email\":\"mock@university.edu.cn\"," +
                "\"phone\":\"13800138000\",\"maxBooks\":5,\"status\":\"ACTIVE\"," +
                "\"createTime\":\"2024-09-01\"" +
                "},\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockUpdateProfile() {
        Log.d(TAG, "  → 模拟更新个人信息");
        return "{\"code\":200,\"message\":\"更新成功\",\"data\":{" +
                "\"id\":1,\"username\":\"2024001001\",\"realName\":\"模拟用户\"," +
                "\"role\":\"STUDENT\",\"email\":\"newemail@university.edu.cn\"," +
                "\"phone\":\"13900139000\",\"maxBooks\":5,\"status\":\"ACTIVE\"," +
                "\"createTime\":\"2024-09-01\"" +
                "},\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockBorrowHistory() {
        Log.d(TAG, "  → 模拟借阅历史");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":{" +
                "\"records\":[" +
                borrowRecordJson(3, 7, "Python编程：从入门到实践", "Eric Matthes", "编程语言", "2026-04-10", "2026-05-10", "2026-05-05", "RETURNED", 0, 0.0) + "," +
                borrowRecordJson(4, 4, "设计模式", "GoF", "软件工程", "2026-03-01", "2026-03-30", "2026-03-28", "RETURNED", 0, 0.0) + "," +
                borrowRecordJson(8, 8, "数据库系统概念", "Abraham Silberschatz", "计算机科学", "2025-12-01", "2025-12-30", "2025-12-25", "RETURNED", 0, 0.0) + "," +
                borrowRecordJson(9, 9, "Spring实战", "Craig Walls", "软件工程", "2025-10-15", "2025-11-14", "2025-11-10", "RETURNED", 0, 0.0) + "," +
                borrowRecordJson(10, 15, "算法导论", "Thomas Cormen", "计算机科学", "2025-09-01", "2025-09-30", "2025-09-28", "RETURNED", 0, 0.0) +
                "]," +
                "\"total\":5,\"pageNum\":1,\"pageSize\":10,\"totalPages\":1" +
                "},\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockBorrowStats() {
        Log.d(TAG, "  → 模拟借阅统计");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":{" +
                "\"totalBorrows\":24,\"currentBorrows\":3,\"totalOverdue\":1,\"totalFines\":5.5," +
                "\"categoryDistribution\":[" +
                "{\"categoryName\":\"计算机科学\",\"count\":10}," +
                "{\"categoryName\":\"人工智能\",\"count\":6}," +
                "{\"categoryName\":\"软件工程\",\"count\":4}," +
                "{\"categoryName\":\"编程语言\",\"count\":2}," +
                "{\"categoryName\":\"网络技术\",\"count\":1}," +
                "{\"categoryName\":\"数学\",\"count\":1}" +
                "]," +
                "\"monthlyTrend\":[" +
                "{\"month\":\"2026-01\",\"count\":3}," +
                "{\"month\":\"2026-02\",\"count\":2}," +
                "{\"month\":\"2026-03\",\"count\":4}," +
                "{\"month\":\"2026-04\",\"count\":5}," +
                "{\"month\":\"2026-05\",\"count\":6}," +
                "{\"month\":\"2026-06\",\"count\":4}" +
                "]" +
                "},\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockRecommendations() {
        Log.d(TAG, "  → 模拟个性化推荐");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":[" +
                recommendJson(15, "978-7-115-00000-6", "算法导论", "Thomas Cormen", "机械工业出版社", "计算机科学", 2, 4, 0.952, "基于您的借阅历史，与您借阅过的数据结构类书籍高度相关") + "," +
                recommendJson(3, "978-7-121-33333-3", "人工智能：一种现代方法", "Stuart Russell", "电子工业出版社", "人工智能", 2, 3, 0.891, "知识图谱显示您的研究方向包含AI相关领域") + "," +
                recommendJson(5, "978-7-302-55555-5", "操作系统概念", "Abraham Silberschatz", "清华大学出版社", "计算机科学", 4, 6, 0.876, "与您借阅偏好相似的读者也喜欢此书") + "," +
                recommendJson(12, "978-7-302-00000-3", "线性代数及其应用", "David Lay", "人民邮电出版社", "数学", 6, 10, 0.843, "AI和机器学习的基础数学知识") + "," +
                recommendJson(4, "978-7-115-44444-4", "设计模式", "GoF", "人民邮电出版社", "软件工程", 1, 2, 0.812, "提升编程能力必读经典") + "," +
                recommendJson(10, "978-7-121-00000-1", "机器学习实战", "Peter Harrington", "电子工业出版社", "人工智能", 1, 3, 0.789, "与统计学习方法互补，注重实践") + "," +
                recommendJson(8, "978-7-302-88888-8", "数据库系统概念", "Abraham Silberschatz", "清华大学出版社", "计算机科学", 2, 4, 0.765, "计算机科学核心课程推荐") + "," +
                recommendJson(13, "978-7-111-00000-4", "人月神话", "Frederick Brooks", "清华大学出版社", "软件工程", 1, 2, 0.732, "软件工程管理经典读物") +
                "],\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    // ================================================================
    // 知识图谱 mock 数据
    // ================================================================

    private String mockKnowledgeGraph() {
        Log.d(TAG, "  → 模拟知识图谱");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":{" +
                "\"nodes\":[" +
                graphNodeJson(1, "数据结构与算法分析", "BOOK", "{\"pagerank\":0.85,\"borrowCount\":128}") + "," +
                graphNodeJson(2, "Mark Allen Weiss", "AUTHOR", "{\"pagerank\":0.72,\"publicationCount\":5}") + "," +
                graphNodeJson(3, "算法", "KEYWORD", "{\"pagerank\":0.91,\"frequency\":256}") + "," +
                graphNodeJson(4, "链表", "KEYWORD", "{\"pagerank\":0.45,\"frequency\":89}") + "," +
                graphNodeJson(5, "二叉树", "KEYWORD", "{\"pagerank\":0.38,\"frequency\":72}") + "," +
                graphNodeJson(6, "排序算法", "KEYWORD", "{\"pagerank\":0.52,\"frequency\":134}") + "," +
                graphNodeJson(7, "计算机科学", "SUBJECT", "{\"pagerank\":0.95,\"bookCount\":1200}") + "," +
                graphNodeJson(8, "算法导论", "BOOK", "{\"pagerank\":0.88,\"borrowCount\":456}") + "," +
                graphNodeJson(9, "Thomas Cormen", "AUTHOR", "{\"pagerank\":0.68,\"publicationCount\":3}") + "," +
                graphNodeJson(10, "时间复杂度", "KEYWORD", "{\"pagerank\":0.41,\"frequency\":98}") +
                "]," +
                "\"edges\":[" +
                graphEdgeJson(1, 2, "AUTHORED_BY", 1.0) + "," +
                graphEdgeJson(1, 3, "HAS_KEYWORD", 0.9) + "," +
                graphEdgeJson(1, 4, "HAS_KEYWORD", 0.5) + "," +
                graphEdgeJson(1, 5, "HAS_KEYWORD", 0.4) + "," +
                graphEdgeJson(1, 6, "HAS_KEYWORD", 0.6) + "," +
                graphEdgeJson(1, 7, "BELONGS_TO", 0.8) + "," +
                graphEdgeJson(8, 9, "AUTHORED_BY", 1.0) + "," +
                graphEdgeJson(8, 3, "HAS_KEYWORD", 0.95) + "," +
                graphEdgeJson(8, 10, "HAS_KEYWORD", 0.55) + "," +
                graphEdgeJson(8, 7, "BELONGS_TO", 0.8) + "," +
                graphEdgeJson(1, 8, "RELATED_TO", 0.75) + "," +
                graphEdgeJson(2, 7, "BELONGS_TO", 0.7) + "," +
                graphEdgeJson(3, 10, "RELATED_TO", 0.6) +
                "]" +
                "},\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockLiteratureTrace() {
        Log.d(TAG, "  → 模拟文献溯源");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":{" +
                "\"sourceBook\":{" +
                "\"id\":1,\"isbn\":\"978-7-302-11111-1\",\"title\":\"数据结构与算法分析\"," +
                "\"author\":\"Mark Allen Weiss\",\"publisher\":\"清华大学出版社\"," +
                "\"pubDate\":\"2020-01-15\",\"categoryName\":\"计算机科学\"," +
                "\"description\":\"经典数据结构教材\"," +
                "\"coverUrl\":\"\",\"location\":\"A区3排5号\"," +
                "\"availCopies\":3,\"totalCopies\":5,\"borrowCount\":128" +
                "}," +
                "\"paths\":[" +
                "{\"nodes\":[" +
                graphNodeJson(1, "数据结构与算法分析", "BOOK", "{}") + "," +
                graphNodeJson(3, "算法", "KEYWORD", "{}") + "," +
                graphNodeJson(8, "算法导论", "BOOK", "{}") + "," +
                graphNodeJson(9, "Thomas Cormen", "AUTHOR", "{}") +
                "],\"edges\":[" +
                graphEdgeJson(1, 3, "HAS_KEYWORD", 0.9) + "," +
                graphEdgeJson(8, 3, "HAS_KEYWORD", 0.95) + "," +
                graphEdgeJson(8, 9, "AUTHORED_BY", 1.0) + "," +
                graphEdgeJson(1, 8, "CITED_BY", 0.75) +
                "],\"depth\":3,\"totalWeight\":3.6}" +
                "]" +
                "},\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockSubjectNetwork() {
        Log.d(TAG, "  → 模拟学科网络");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":{" +
                "\"nodes\":[" +
                graphNodeJson(7, "计算机科学", "SUBJECT", "{\"pagerank\":0.95,\"bookCount\":1200}") + "," +
                graphNodeJson(20, "人工智能", "SUBJECT", "{\"pagerank\":0.89,\"bookCount\":450}") + "," +
                graphNodeJson(21, "软件工程", "SUBJECT", "{\"pagerank\":0.78,\"bookCount\":380}") + "," +
                graphNodeJson(22, "数学", "SUBJECT", "{\"pagerank\":0.82,\"bookCount\":520}") + "," +
                graphNodeJson(23, "网络技术", "SUBJECT", "{\"pagerank\":0.65,\"bookCount\":210}") + "," +
                graphNodeJson(3, "算法", "KEYWORD", "{\"pagerank\":0.91,\"frequency\":256}") + "," +
                graphNodeJson(24, "机器学习", "KEYWORD", "{\"pagerank\":0.87,\"frequency\":189}") + "," +
                graphNodeJson(25, "编程", "KEYWORD", "{\"pagerank\":0.76,\"frequency\":312}") +
                "]," +
                "\"edges\":[" +
                graphEdgeJson(7, 20, "RELATED_TO", 0.85) + "," +
                graphEdgeJson(7, 21, "RELATED_TO", 0.72) + "," +
                graphEdgeJson(7, 22, "RELATED_TO", 0.68) + "," +
                graphEdgeJson(7, 23, "RELATED_TO", 0.55) + "," +
                graphEdgeJson(20, 22, "RELATED_TO", 0.78) + "," +
                graphEdgeJson(20, 24, "HAS_KEYWORD", 0.92) + "," +
                graphEdgeJson(7, 3, "HAS_KEYWORD", 0.91) + "," +
                graphEdgeJson(21, 25, "HAS_KEYWORD", 0.74) + "," +
                graphEdgeJson(20, 25, "HAS_KEYWORD", 0.62) + "," +
                graphEdgeJson(3, 24, "RELATED_TO", 0.56) +
                "]" +
                "},\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockEntitySearch() {
        Log.d(TAG, "  → 模拟实体搜索");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":[" +
                "{\"entityId\":1,\"entityName\":\"数据结构与算法分析\",\"entityType\":\"BOOK\",\"pagerank\":0.85}," +
                "{\"entityId\":2,\"entityName\":\"Mark Allen Weiss\",\"entityType\":\"AUTHOR\",\"pagerank\":0.72}," +
                "{\"entityId\":3,\"entityName\":\"算法\",\"entityType\":\"KEYWORD\",\"pagerank\":0.91}," +
                "{\"entityId\":8,\"entityName\":\"算法导论\",\"entityType\":\"BOOK\",\"pagerank\":0.88}," +
                "{\"entityId\":24,\"entityName\":\"数据结构\",\"entityType\":\"KEYWORD\",\"pagerank\":0.67}" +
                "],\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    // ================================================================
    // 系统管理 mock 数据
    // ================================================================

    private String mockAdminUserList() {
        Log.d(TAG, "  → 模拟管理员用户列表");
        return "{\"code\":200,\"message\":\"查询成功\",\"data\":{" +
                "\"records\":[" +
                "{\"id\":1,\"username\":\"2024001001\",\"realName\":\"模拟用户\",\"role\":\"STUDENT\",\"email\":\"mock@university.edu.cn\",\"phone\":\"13800138000\",\"maxBooks\":5,\"status\":\"ACTIVE\",\"currentBorrows\":2,\"totalOverdue\":0,\"createTime\":\"2024-09-01\"}," +
                "{\"id\":2,\"username\":\"2024001002\",\"realName\":\"张三\",\"role\":\"STUDENT\",\"email\":\"zhangsan@university.edu.cn\",\"phone\":\"13800138001\",\"maxBooks\":5,\"status\":\"ACTIVE\",\"currentBorrows\":3,\"totalOverdue\":0,\"createTime\":\"2024-09-01\"}," +
                "{\"id\":3,\"username\":\"2024001003\",\"realName\":\"李四\",\"role\":\"STUDENT\",\"email\":\"lisi@university.edu.cn\",\"phone\":\"13800138002\",\"maxBooks\":5,\"status\":\"FROZEN\",\"currentBorrows\":0,\"totalOverdue\":2,\"createTime\":\"2024-09-01\"}," +
                "{\"id\":4,\"username\":\"admin\",\"realName\":\"系统管理员\",\"role\":\"ADMIN\",\"email\":\"admin@library.com\",\"phone\":\"13800138999\",\"maxBooks\":0,\"status\":\"ACTIVE\",\"currentBorrows\":0,\"totalOverdue\":0,\"createTime\":\"2024-01-01\"}," +
                "{\"id\":5,\"username\":\"librarian01\",\"realName\":\"图书管理员\",\"role\":\"LIBRARIAN\",\"email\":\"lib@library.com\",\"phone\":\"13800138888\",\"maxBooks\":0,\"status\":\"ACTIVE\",\"currentBorrows\":0,\"totalOverdue\":0,\"createTime\":\"2024-01-01\"}" +
                "]," +
                "\"total\":5,\"pageNum\":1,\"pageSize\":10,\"totalPages\":1" +
                "},\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockAdminBookCreated() {
        Log.d(TAG, "  → 模拟管理员新增图书");
        return "{\"code\":200,\"message\":\"新增成功\",\"data\":" +
                bookJson(100, "978-7-999-00001-0", "新编图书", "测试作者", "测试出版社", "2026-06-16", "计算机科学", "测试描述", "A区1排1号", 5, 5, 0) +
                ",\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    private String mockAdminBookUpdated() {
        Log.d(TAG, "  → 模拟管理员更新图书");
        return "{\"code\":200,\"message\":\"更新成功\",\"data\":" +
                bookJson(1, "978-7-302-11111-1", "数据结构与算法分析（第二版）", "Mark Allen Weiss", "清华大学出版社", "2020-01-15", "计算机科学", "经典数据结构教材，已更新第二版", "A区3排5号", 3, 5, 128) +
                ",\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    // ================================================================
    // JSON 构建辅助方法
    // ================================================================

    private String bookJson(long id, String isbn, String title, String author, String publisher,
                            String pubDate, String categoryName, String description, String location,
                            int availCopies, int totalCopies, int borrowCount) {
        return "{" +
                "\"id\":" + id + "," +
                "\"isbn\":\"" + isbn + "\"," +
                "\"title\":\"" + title + "\"," +
                "\"author\":\"" + author + "\"," +
                "\"publisher\":\"" + publisher + "\"," +
                "\"pubDate\":\"" + pubDate + "\"," +
                "\"categoryName\":\"" + categoryName + "\"," +
                "\"description\":\"" + description + "\"," +
                "\"coverUrl\":\"\"," +
                "\"location\":\"" + location + "\"," +
                "\"availCopies\":" + availCopies + "," +
                "\"totalCopies\":" + totalCopies + "," +
                "\"borrowCount\":" + borrowCount +
                "}";
    }

    private String bookSimpleJson(long id, String isbn, String title, String author, String publisher,
                                  String categoryName, int availCopies, int totalCopies) {
        return "{" +
                "\"id\":" + id + "," +
                "\"isbn\":\"" + isbn + "\"," +
                "\"title\":\"" + title + "\"," +
                "\"author\":\"" + author + "\"," +
                "\"publisher\":\"" + publisher + "\"," +
                "\"categoryName\":\"" + categoryName + "\"," +
                "\"coverUrl\":\"\"," +
                "\"availCopies\":" + availCopies +
                "}";
    }

    private String recommendJson(long id, String isbn, String title, String author, String publisher,
                                 String categoryName, int availCopies, int totalCopies,
                                 double score, String reason) {
        return "{" +
                "\"book\":{" +
                "\"id\":" + id + "," +
                "\"isbn\":\"" + isbn + "\"," +
                "\"title\":\"" + title + "\"," +
                "\"author\":\"" + author + "\"," +
                "\"publisher\":\"" + publisher + "\"," +
                "\"categoryName\":\"" + categoryName + "\"," +
                "\"coverUrl\":\"\"," +
                "\"availCopies\":" + availCopies +
                "}," +
                "\"score\":" + score + "," +
                "\"reason\":\"" + reason + "\"" +
                "}";
    }

    private String borrowRecordJson(long id, long bookId, String title, String author, String categoryName,
                                    String borrowDate, String dueDate, String returnDate,
                                    String status, int renewCount, double fineAmount) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(id).append(",");
        sb.append("\"book\":{");
        sb.append("\"id\":").append(bookId).append(",");
        sb.append("\"isbn\":\"978-7-xxx-xxxxx-x\",");
        sb.append("\"title\":\"").append(title).append("\",");
        sb.append("\"author\":\"").append(author).append("\",");
        sb.append("\"publisher\":\"测试出版社\",");
        sb.append("\"categoryName\":\"").append(categoryName).append("\",");
        sb.append("\"coverUrl\":\"\",");
        sb.append("\"availCopies\":1");
        sb.append("},");
        sb.append("\"borrowDate\":\"").append(borrowDate).append("\",");
        sb.append("\"dueDate\":\"").append(dueDate).append("\",");
        if (returnDate != null) {
            sb.append("\"returnDate\":\"").append(returnDate).append("\",");
        } else {
            sb.append("\"returnDate\":null,");
        }
        sb.append("\"renewCount\":").append(renewCount).append(",");
        sb.append("\"status\":\"").append(status).append("\",");
        sb.append("\"fineAmount\":").append(fineAmount);
        sb.append("}");
        return sb.toString();
    }

    private String reservationJson(long id, long bookId, String title, String author,
                                   String reserveTime, String status, int queuePosition, String expireTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(id).append(",");
        sb.append("\"book\":{");
        sb.append("\"id\":").append(bookId).append(",");
        sb.append("\"isbn\":\"978-7-xxx-xxxxx-x\",");
        sb.append("\"title\":\"").append(title).append("\",");
        sb.append("\"author\":\"").append(author).append("\",");
        sb.append("\"publisher\":\"测试出版社\",");
        sb.append("\"categoryName\":\"计算机科学\",");
        sb.append("\"coverUrl\":\"\",");
        sb.append("\"availCopies\":0");
        sb.append("},");
        sb.append("\"reserveTime\":\"").append(reserveTime).append("\",");
        sb.append("\"queuePosition\":").append(queuePosition).append(",");
        sb.append("\"status\":\"").append(status).append("\",");
        if (expireTime != null) {
            sb.append("\"expireTime\":\"").append(expireTime).append("\"");
        } else {
            sb.append("\"expireTime\":null");
        }
        sb.append("}");
        return sb.toString();
    }

    private String graphNodeJson(long id, String label, String type, String properties) {
        return "{" +
                "\"id\":" + id + "," +
                "\"label\":\"" + label + "\"," +
                "\"type\":\"" + type + "\"," +
                "\"properties\":" + properties +
                "}";
    }

    private String graphEdgeJson(long sourceId, long targetId, String relation, double weight) {
        return "{" +
                "\"sourceId\":" + sourceId + "," +
                "\"targetId\":" + targetId + "," +
                "\"relation\":\"" + relation + "\"," +
                "\"weight\":" + weight +
                "}";
    }
}
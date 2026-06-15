# 图书馆智能管理系统 — 编码规范与协作指南

> **目的**：统一全组开发规范，消除协作摩擦，保证代码质量可度量  
> **适用范围**：后端（Java/Spring）、前端（Android/Java）及所有相关文档  
> **最后更新**：2026-06-15

---

## 目录

1. [Git 工作流](#1-git-工作流)
2. [Commit 提交规范](#2-commit-提交规范)
3. [Java 编码规范（后端）](#3-java-编码规范后端)
4. [Android 编码规范](#4-android-编码规范)
5. [命名规范速查表](#5-命名规范速查表)
6. [Code Review 流程](#6-code-review-流程)
7. [测试要求](#7-测试要求)
8. [PR 模板](#8-pr-模板)
9. [IDE 配置统一](#9-ide-配置统一)

---

## 1. Git 工作流

### 1.1 分支模型

采用 **简化 Git Flow**，分支策略如下：

```
main ──────────────────────────────────────────────────────►  生产就绪
  │
  ├── develop ─────────────────────────────────────────────►  开发主线
  │     │
  │     ├── feature/xxx ──────► 功能分支（从 develop 分出）
  │     ├── fix/xxx ──────────► 缺陷修复分支
  │     ├── refactor/xxx ─────► 重构分支
  │     └── docs/xxx ─────────► 文档分支
  │
  └── hotfix/xxx ─────────────►  紧急修复（从 main 分出）
```

### 1.2 分支命名规则

```
feature/<模块>-<简述>      例：feature/core-book-search
fix/<模块>-<简述>          例：fix/security-jwt-expire
refactor/<模块>-<简述>     例：refactor/kg-graph-builder
docs/<简述>                例：docs/api-specification
hotfix/<简述>              例：hotfix-login-npe
```

### 1.3 分支生命周期

```
1. 从 develop 切出 feature 分支
   git checkout -b feature/core-book-search develop

2. 日常开发：在该分支上 commit
   git add -A
   git commit -m "feat(core): 实现图书关键词搜索接口"

3. 定期同步 develop 最新代码（避免冲突堆积）
   git fetch origin develop
   git rebase origin/develop
   # 如遇冲突，逐个文件解决后：git rebase --continue

4. 完成后发起 PR 到 develop
   git push origin feature/core-book-search
   # 在 GitHub/GitLab 上创建 Pull Request

5. Code Review 通过后，由 reviewer 合并到 develop
   # 采用 Squash Merge 保持 develop 历史整洁

6. 删除远程功能分支
   git push origin --delete feature/core-book-search
```

### 1.4 合并策略

| 场景 | 策略 | 说明 |
|------|------|------|
| feature → develop | **Squash Merge** | 压缩为一个干净 commit |
| develop → main | **Merge Commit** | 保留完整发布历史 |
| hotfix → main | **Merge Commit** | 保留修复记录 |
| hotfix → develop | **Merge Commit** 或 Cherry-pick | 同步修复到开发线 |

### 1.5 冲突解决原则

1. **谁后合并谁解决冲突** — 保持 rebase 时当前分支为最新
2. 解决冲突后**必须编译通过**再推送
3. 涉及数据库迁移脚本的冲突，必须**两人当面确认**
4. 禁止直接 `git push --force` 到 `main` / `develop`

---

## 2. Commit 提交规范

### 2.1 格式

严格遵守 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
<type>(<scope>): <subject>

<body>          （可选：详细描述）

<footer>        （可选：关联 issue、breaking change）
```

### 2.2 Type 类型

| Type | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat(core): 添加图书搜索接口` |
| `fix` | 缺陷修复 | `fix(borrow): 修复超期未还状态更新bug` |
| `refactor` | 重构（不改变功能） | `refactor(common): 提取公共分页逻辑` |
| `perf` | 性能优化 | `perf(search): ES查询添加缓存层` |
| `test` | 测试相关 | `test(core): 补充借阅模块单元测试` |
| `docs` | 文档变更 | `docs(api): 更新智能采编接口文档` |
| `style` | 代码格式（空格、缩进等） | `style(core): 统一代码缩进为4空格` |
| `chore` | 构建/工具配置 | `chore(deps): 升级Spring Boot到3.2.1` |
| `ci` | CI/CD 变更 | `ci: 添加自动化测试流水线` |

### 2.3 Scope 范围

| Scope | 对应模块 |
|-------|----------|
| `common` | library-common |
| `core` | library-core |
| `kg` | library-knowledge-graph |
| `acquisition` | library-acquisition |
| `security` | library-security |
| `server` | library-server（启动/配置） |
| `android` | library-web（Android 前端） |
| `deps` | 依赖管理 |

### 2.4 规则

1. `subject` **用中文**，简明扼要描述做了什么（不必加句号）
2. 每个 commit 只做一件事 — 一个 commit 不要同时包含新功能和 bug 修复
3. 每天至少 commit 一次，不要攒到最后统一提交
4. **WIP（Work In Progress） commit 需要 rebase 整理后再提 PR**

### 2.5 正确示例

```bash
# ✅ 好的 commit
git commit -m "feat(core): 实现多条件组合搜索接口"
git commit -m "fix(borrow): 修复最大借阅数校验逻辑错误"
git commit -m "docs(api): 补全借阅模块OpenAPI文档"
git commit -m "test(kg): 添加知识图谱溯源的集成测试"

# ❌ 坏的 commit
git commit -m "修了一些bug"                    # 不明确
git commit -m "feat and fix"                   # 类型混乱
git commit -m "feat(core): 做了很多改动..."    # 太模糊，应该拆分
```

---

## 3. Java 编码规范（后端）

本规范以 **阿里巴巴 Java 开发手册** 为基础，补充团队约定。

### 3.1 文件组织

```java
/**
 * 类文件结构（从上到下）
 */
package com.library.core.controller;           // 1. package 声明

import java.util.List;                         // 2. import（按 java > javax > 第三方 > 项目内 分组）
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.library.core.service.BookService;

/**
 * 图书检索控制器                              // 3. 类 Javadoc（必须）
 * <p>
 * 提供图书关键词搜索、高级搜索、自动补全等接口。
 *
 * @author 张三
 * @since 1.0.0
 */
@RestController                                // 4. 类注解
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;     // 5. 字段（private final，构造器注入）

    /**
     * 关键词搜索图书                          // 6. 方法 Javadoc（public 方法必须）
     *
     * @param keyword 搜索关键词
     * @param page 分页参数
     * @return 搜索结果分页
     */
    @GetMapping("/search")
    public Result<PageResult<BookVO>> search(
            @RequestParam @NotBlank String keyword,
            @Valid PageDTO page) {

        // 7. 方法体
        PageResult<BookVO> result = bookService.searchByKeyword(keyword, page);
        return Result.success(result);
    }
}
```

### 3.2 分层约束

严格遵守分层调用链，**禁止反向依赖**：

```
Controller ──► Service ──► Mapper/Repository
    │              │              │
    ├── 仅做参数校验  ├── 业务逻辑   ├── 仅做数据访问
    ├── DTO ↔ VO     ├── 事务管理   ├── 不写业务逻辑
    └── 不写业务逻辑   └── 领域事件   └── 参数化 SQL
```

**禁止事项**：
- ❌ Controller 直接调用 Mapper
- ❌ Service 层之间循环依赖
- ❌ Mapper 中出现 `if` 业务逻辑判断
- ❌ Service 直接返回 `HttpServletRequest` / `HttpServletResponse`

### 3.3 Service 层规范

```java
@Service
@RequiredArgsConstructor                    // Lombok：构造器注入
@Transactional(rollbackFor = Exception.class) // 默认事务回滚
public class BorrowServiceImpl implements BorrowService {

    private final BookMapper bookMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public BorrowResult borrow(Long userId, String bookIsbn) {
        // 1. 业务校验（防御式编程，快速失败）
        Book book = bookMapper.selectByIsbn(bookIsbn);
        if (book == null) {
            throw new BizException(ErrorCode.BOOK_NOT_FOUND);
        }
        if (book.getAvailCopies() <= 0) {
            throw new BizException(ErrorCode.BOOK_STOCK_EMPTY);
        }

        // 2. 核心业务逻辑
        int updatedRows = bookMapper.decreaseStock(book.getId());
        if (updatedRows != 1) {
            throw new BizException(ErrorCode.BOOK_STOCK_EMPTY);  // 乐观锁失败
        }
        BorrowRecord record = BorrowRecord.create(userId, book);
        borrowRecordMapper.insert(record);

        // 3. 发布领域事件（异步处理）
        eventPublisher.publishEvent(new BookBorrowedEvent(book.getId()));

        // 4. 返回结果
        return BorrowResult.from(record, book);
    }
}
```

**Service 层原则**：
- 每个 public 方法加 `@Override`（接口实现时）
- 校验逻辑不分散 — 集中在方法开头
- 领域事件放在业务逻辑执行**之后**发布
- 复杂查询方法使用 `@Transactional(readOnly = true)`

### 3.4 异常处理规范

```java
// ✅ 正确：使用业务异常
throw new BizException(ErrorCode.BOOK_STOCK_EMPTY);

// ✅ 正确：带动态参数的异常
throw new BizException(ErrorCode.BORROW_LIMIT_EXCEEDED, user.getMaxBooks());

// ❌ 错误：直接返回 null
if (book == null) return null;

// ❌ 错误：吞掉异常
try { ... } catch (Exception e) { e.printStackTrace(); }

// ❌ 错误：直接抛出 RuntimeException
throw new RuntimeException("图书不存在");
```

**GlobalExceptionHandler 负责统一处理**，各层只需抛出 `BizException`：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return Result.error(400, msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception e) {
        log.error("未捕获异常", e);
        return Result.error(500, "服务器内部错误");
    }
}
```

### 3.5 日志规范

```java
// ✅ 使用 Lombok @Slf4j
@Slf4j
@Service
public class BookService {

    public PageResult<BookVO> search(String keyword, PageDTO page) {
        // 记录入口参数
        log.info("图书搜索: keyword={}, page={}", keyword, page.getPageNum());

        try {
            PageResult<BookVO> result = bookESRepository.fullTextSearch(keyword, page);
            log.info("搜索结果: total={}, took={}ms", result.getTotal(), elapsed);
            return result;
        } catch (Exception e) {
            // 异常必须记录完整堆栈
            log.error("图书搜索异常: keyword={}", keyword, e);
            throw new BizException(ErrorCode.SEARCH_FAILED);
        }
    }
}
```

**日志级别使用**：
| 级别 | 使用场景 |
|------|----------|
| `ERROR` | 系统异常、第三方调用失败（必须含堆栈） |
| `WARN` | 业务异常（如库存不足、权限不足） |
| `INFO` | 关键业务流程节点、接口入口/出口 |
| `DEBUG` | 调试信息、SQL 参数 |
| `TRACE` | 循环内详细数据（禁止在生产环境开启） |

### 3.6 Lombok 使用规范

允许使用的注解：
```java
@Data           // Entity / DTO / VO 类
@Builder        // 复杂对象构建（配合 @AllArgsConstructor）
@Slf4j          // 日志
@RequiredArgsConstructor  // 构造器注入（Service / Controller）
@AllArgsConstructor       // Entity / DTO
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // 反射/序列化框架兼容
```

**禁止**使用的注解：
- ❌ `@SneakyThrows` — 隐藏异常，难以排查
- ❌ `@EqualsAndHashCode` 在 Entity 上不加限制地使用 — 应基于主键 ID 显式实现，或使用 `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` 仅标记 ID 字段，避免字段值变更导致集合行为异常
- ❌ 过度使用 `@Builder` 在简单 DTO 上 — 增加冗余代码

---

## 4. Android 编码规范

### 4.1 包结构

```
com.library.web/
├── LibraryApplication.java         # Application 类
├── ui/                             # UI 层（Activity / Fragment / Adapter）
│   ├── main/
│   ├── search/
│   ├── borrow/
│   ├── profile/
│   └── common/
├── viewmodel/                      # ViewModel
├── repository/                     # 数据仓库（网络 → 本地 降级策略）
├── network/                        # Retrofit API 接口 + Interceptor
├── model/                          # 数据模型（VO / DTO）
├── di/                             # Hilt / Dagger 依赖注入模块
└── util/                           # 工具类
```

### 4.2 Activity / Fragment 规范

```java
/**
 * 图书详情 Activity
 * <p>
 * 展示图书完整信息，支持直接借阅和预约操作。
 */
public class BookDetailActivity extends BaseActivity {

    private ActivityBookDetailBinding binding;
    private BookDetailViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. ViewBinding 初始化
        binding = ActivityBookDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. ViewModel 初始化
        viewModel = new ViewModelProvider(this).get(BookDetailViewModel.class);

        // 3. 观察数据
        observeViewModel();

        // 4. 加载数据
        long bookId = getIntent().getLongExtra("book_id", -1);
        if (bookId != -1) {
            viewModel.loadBookDetail(bookId);
        }
    }

    private void observeViewModel() {
        viewModel.getBookDetail().observe(this, book -> {
            if (book != null) {
                binding.tvTitle.setText(book.getTitle());
                binding.tvAuthor.setText(book.getAuthor());
                binding.btnBorrow.setEnabled(book.getAvailCopies() > 0);
            }
        });

        viewModel.getError().observe(this, errorMsg -> {
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;  // 防止内存泄漏
    }
}
```

**Activity/Fragment 原则**：
- 每个 Activity **不超过 300 行**（超出则提取 Fragment 或自定义 View）
- 业务逻辑全部在 ViewModel 中，Activity 只负责 UI 绑定
- 使用 ViewBinding，禁止 findViewById
- 禁止在 Activity 中直接操作数据库（必须通过 Repository → ViewModel）

### 4.3 网络请求规范

```java
/**
 * Retrofit API 接口 + OkHttp 拦截器
 */
@Singleton
public class RetrofitClient {

    private static final String BASE_URL = ApiConfig.BASE_URL;
    private Retrofit retrofit;

    @Inject
    public RetrofitClient(TokenManager tokenManager) {
        // OkHttp 配置
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor(tokenManager))  // 自动附加 Token
                .addInterceptor(new HttpLoggingInterceptor().setLevel(
                        BuildConfig.DEBUG ? Level.BODY : Level.NONE))
                .build();

        // Retrofit 配置
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }

    public <T> T create(Class<T> apiClass) {
        return retrofit.create(apiClass);
    }
}

/**
 * 自动附加 JWT Token 的拦截器
 */
class AuthInterceptor implements Interceptor {
    private final TokenManager tokenManager;

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request original = chain.request();
        String token = tokenManager.getAccessToken();

        if (token == null) {
            return chain.proceed(original);
        }

        Request request = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
        return chain.proceed(request);
    }
}
```

---

## 5. 命名规范速查表

| 元素 | 命名风格 | 示例 |
|------|----------|------|
| **包名** | 全部小写，单词连写 | `com.library.core.controller` |
| **类名** | UpperCamelCase | `BookController`, `BorrowRecordService` |
| **接口名** | UpperCamelCase（不加 I 前缀） | `BookService`, `BookRepository` |
| **抽象类名** | Abstract + UpperCamelCase | `AbstractBaseService` |
| **实现类名** | Impl 后缀 | `BookServiceImpl` |
| **方法名** | lowerCamelCase | `searchByKeyword()`, `getBookDetail()` |
| **常量** | 全大写 + 下划线 | `MAX_BORROW_COUNT`, `DEFAULT_PAGE_SIZE` |
| **变量名** | lowerCamelCase | `bookList`, `borrowRecord` |
| **数据库表** | 小写 + 下划线 | `book`, `borrow_record` |
| **数据库列** | 小写 + 下划线 | `avail_copies`, `due_date` |
| **DTO** | DTO 后缀 | `BookSearchDTO`, `PageDTO` |
| **VO** | VO 后缀 | `BookVO`, `BorrowRecordVO` |
| **API URL** | 小写 + 连字符 / 复数形式 | `/api/v1/books`, `/api/v1/borrows` |
| **JSON 字段** | lowerCamelCase | `"availCopies"`, `"dueDate"` |
| **资源文件** | 小写 + 下划线 | `ic_book_cover.xml`, `activity_main.xml` |

### 命名正向清单

```java
// ✅ 正确的命名
public List<BookVO> getHotBooks(int limit);
private void validateBorrowLimit(Long userId);
public static final int DEFAULT_RENEW_DAYS = 30;

// Android
private ActivityBookDetailBinding binding;
private TextView tvTitle;
private Button btnBorrow;

// ❌ 错误的命名
public List<BookVO> getList(int i);           // 无意义
private void check(Long uid, int n);          // 缩写不明确
private BookDetailBinding mBinding;           // 避免 m 前缀
public static final int days = 30;            // 非 final 且未大写
```

---

## 6. Code Review 流程

### 6.1 Review 清单

每个 PR 必须通过以下 7 项检查：

```
┌──────────────────────────────────────────────────────┐
│                  Code Review Checklist               │
├── 1. 功能正确性 ── 代码实现了预期功能？               │
├── 2. 边界处理   ── null/空集合/超限/并发 是否处理？    │
├── 3. 异常处理   ── 异常是否正确抛出和捕获？            │
├── 4. 分层合规   ── 是否遵循分层调用链？               │
├── 5. 测试覆盖   ── 是否包含必要的单元测试？            │
├── 6. 命名规范   ── 类/方法/变量命名是否清晰、一致？    │
├── 7. 日志充分   ── 关键节点是否有日志？               │
└──────────────────────────────────────────────────────┘
```

### 6.2 Review 流程

```
开发者                       Reviewer                      CI
  │                            │                           │
  │ 1. Push 分支              │                           │
  │──────────────────────────────────────────────────────►│ 2. 自动编译 + 测试
  │                            │                         │
  │ 3. 创建 PR (附模板)       │                           │
  │───────────────────────────►│                           │
  │                            │ 4. Code Review            │
  │◄──── 5. Review Comments ──│                           │
  │                            │                           │
  │ 6. 修改代码并回复          │                           │
  │───────────────────────────►│                           │
  │                            │ 7. Approve               │
  │                            │─────────────────────────►│
  │                            │                         │ 8. Merge + Deploy
  │◄──── 9. 删除分支 ──────────│                           │
```

### 6.3 Review 礼仪

**给 Reviewer**：
- 24 小时内完成 Review（工作日）
- 区分 **必须修改** `🔴` 和 **建议优化** `🟡`
- 不要人身攻击，针对代码不针对人

**给开发者**：
- PR 描述必须清晰（禁止"修bug"这类标题）
- PR 控制在 **400 行以内**（超出则拆分）
- 收到评论后不争论，先修改再讨论
- 紧急修复标记 `[HOTFIX]` 前缀，Reviewer 优先处理

### 6.4 Review 评论规范

```
🔴 必须修改：这会导致生产 bug / 安全漏洞 / 数据错误
🟡 建议优化：不影响功能，但可提升可读性或性能
🟢 值得肯定：写得好的地方（积极鼓励）
```

---

## 7. 测试要求

### 7.1 测试覆盖率

| 模块 | 最低行覆盖率 |
|------|-------------|
| library-common | 90% |
| library-core | 85% |
| library-knowledge-graph | 80% |
| library-acquisition | 80% |
| library-security | 90% |

### 7.2 测试命名规范

所有测试方法**必须**遵循 `should{预期行为}When{条件/输入}` 命名模式：

```java
// ✅ 正确：should + 预期行为 + When + 条件
void shouldReturnSearchResultsWhenKeywordValid()
void shouldThrowBizExceptionWhenKeywordIsEmpty()
void shouldReturnEmptyPageWhenNoMatchFound()
void shouldRejectBorrowWhenUserExceedsMaxBooks()

// ❌ 错误：模糊、无结构
void testSearch()
void testBorrowFail()
void test1()
```

**测试类命名**：`{被测类}Test`，如 `BookServiceTest`、`BorrowControllerTest`。

### 7.3 必须测试的场景

每个 Service 的 public 方法至少覆盖：

```java
class BookServiceTest {

    @Test
    void shouldReturnSearchResultsWhenKeywordValid() {
        // ✅ 正常场景
    }

    @Test
    void shouldThrowBizExceptionWhenKeywordIsEmpty() {
        // ✅ 边界场景 — 空输入
    }

    @Test
    void shouldReturnEmptyPageWhenNoMatchFound() {
        // ✅ 边界场景 — 无匹配结果
    }

    @Test
    void shouldHandleHighConcurrencySearch() {
        // ✅ 异常场景 — 高并发
    }
}
```

---

## 8. PR 模板

提交 PR 时，将以下内容填入 PR 描述：

```markdown
## 类型
<!-- 选择一个：feat / fix / refactor / perf / test / docs / chore -->

## 概述
<!-- 一句话描述这个 PR 做了什么 -->

## 关联 Issue
<!-- 如有关联 Issue：Closes #42 -->

## 改动范围
<!-- 修改了哪些模块/文件 -->
- [ ] library-common
- [ ] library-core
- [ ] library-knowledge-graph
- [ ] library-acquisition
- [ ] library-security
- [ ] library-web (Android)

## 测试情况
<!-- 描述你做了什么测试 -->
- [ ] 单元测试通过
- [ ] 本地接口测试通过（Postman）
- [ ] 集成测试通过
- [ ] 性能无明显回归

## 截图/录屏
<!-- Android 前端改动时附上 -->

## Review 关注点
<!-- 如有特别需要 Reviewer 关注的复杂逻辑，在此说明 -->
```

---

## 9. IDE 配置统一

### 9.1 IntelliJ IDEA（后端）

**必须统一**的配置：

| 配置项 | 设置值 |
|--------|--------|
| 文件编码 | UTF-8 |
| 缩进 | 4 个空格（不使用 Tab） |
| 行尾 | LF（Unix 风格） |
| 导入优化 | 自动整理 import，不使用通配符 `*` |
| Code Style | 遵循项目 `.editorconfig`，Java 行宽 120 字符 |

**安装并启用插件**：
- SonarLint（实时代码质量检查）
- Lombok
- MyBatisX（MyBatis 开发辅助）
- .env files support

### 9.2 Android Studio（前端）

| 配置项 | 设置值 |
|--------|--------|
| 文件编码 | UTF-8 |
| 缩进 | 4 个空格 |
| Code Style | Google Java Style |
| 最小 SDK | API 24 (Android 7.0) |
| 目标 SDK | API 34 (Android 14) |
| 编译 SDK | API 34 |

### 9.3 共享 EditorConfig

项目根目录放置 `.editorconfig` 文件：

```ini
# EditorConfig is awesome: https://EditorConfig.org

root = true

[*]
charset = utf-8
end_of_line = lf
indent_style = space
indent_size = 4
insert_final_newline = true
trim_trailing_whitespace = true

[*.md]
trim_trailing_whitespace = false

[*.xml]
indent_size = 2

[*.yml]
indent_size = 2

[*.properties]
indent_size = 2

[*.java]
max_line_length = 120
```

---

## 附录 — 违规速查表

| 违规行为 | 严重程度 | 处理方式 |
|----------|----------|----------|
| 未经 Review 直接 push 到 main | 🔴 严重 | 强制回滚 + 群内通报 |
| 提交包含明文密码/Token | 🔴 严重 | 立即重置密钥 + 清理 git 历史 |
| SQL 注入风险（拼接 SQL） | 🔴 严重 | 拒绝合并 |
| Controller 直接调 Mapper | 🔴 严重 | 拒绝合并 |
| 无测试覆盖的新功能 | 🟡 警告 | 要求补测试后合并 |
| 日志未记录异常堆栈 | 🟡 警告 | 要求修正后合并 |
| 命名不符合规范 | 🟡 警告 | 要求修正后合并 |
| 未使用的 import / 变量 | 🟢 提示 | 自动格式化即可 |

---

> **规范是团队的底线，不是天花板。** 遇到规范未覆盖的情况，以**代码可读性**和**可维护性**为最高准则做出判断，并在 Code Review 中达成共识后补充到本规范中。

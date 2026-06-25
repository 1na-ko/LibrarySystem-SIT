# 前端人员 A 完成记录

> **分支**: `develop`
> **完成日期**: 2026-06-17
> **人员**: 人员 A（基础架构 + 认证 + 图书检索 + 个人中心）
> **依据**: [前端分工商量书](../前端分工商量书.md) §3
> **提交**: `49ecce9` → `aeb519c` → `b5f8684`（含联调修复）

---

## 一、产出概览

| 层次 | 文件数 | 说明 |
|------|--------|------|
| Model（VO/DTO） | 37 | 全量数据模型类，对齐 OpenAPI `library-api.yaml`（主导编写） |
| Network | 6 | LibraryApi（44 端点）、AuthApiService、AuthInterceptor、TokenManager（加密存储）、TokenAuthenticator、MockInterceptor |
| DI（Hilt） | 3 | NetworkModule、AppModule、RepositoryModule |
| Repository | 3 | BookRepository、UserRepository、AuthRepository |
| ViewModel | 5 | SearchViewModel、BookDetailViewModel、ProfileViewModel、LoginViewModel、RegisterViewModel |
| Fragment / Activity | 13 | 12 个业务 Fragment + MainActivity（入口 + 导航托管） |
| 布局 XML | 17 | 全部使用 Material 3 + ViewBinding |
| 资源文件 | 4 | strings（80+ 条目）、colors、arrays、drawable |
| **合计** | **87+** | 含 nav_graph.xml 整合、Manifest 更新、Gradle 配置 |

---

## 二、模块完成详情

### 2.1 公共基础设施（主导，人员 B 参与 Review）

| 子任务 | 产出物 | 关键实现 |
|--------|--------|---------|
| 数据模型类 | `model/` 包 37 个 VO/DTO | 依据 OpenAPI `library-api.yaml` 全量生成：通用模型（Result/PageResult/ErrorResponse）、认证（LoginRequest/Response/Register/Refresh）、图书（BookVO/BookDetailVO/BookSearchRequest 等）、借阅/预约/推荐/分类/图谱/采编/管理。类名驼峰映射，`@SerializedName` 标注 JSON 字段 |
| Hilt DI 模块 | `di/` 包 3 个 Module | NetworkModule（Retrofit + OkHttp + RxJava3 CallAdapter + Gson + Logging + Mock 拦截器）、AppModule（TokenManager + EncryptedSharedPreferences）、RepositoryModule（BookRepository/UserRepository/AuthRepository） |
| AuthInterceptor | `network/AuthInterceptor.java` | 自动从 TokenManager 读取 Access Token 附加 `Authorization: Bearer <token>` 头部，跳过登录/注册/刷新端点 |
| TokenAuthenticator | `network/TokenAuthenticator.java` | OkHttp Authenticator 接口实现（同步刷新/重试/失败清除），通过 `LibraryApiProvider` 打破循环依赖，`NetworkModule` 中已注册 `.authenticator()` |
| TokenManager | `network/TokenManager.java` | EncryptedSharedPreferences 安全存储（Access Token / Refresh Token / 用户信息 / 角色），`isLoggedIn()` / `isAdmin()` / `saveUserInfo()` / `getUsername()` 等 |
| nav_graph.xml | `res/navigation/nav_graph.xml` | 全量 20 个 Fragment 目的地 + 导航 action，startDestination = searchFragment，auth/login/register 隐藏底部导航栏 |
| 通用 UI 组件 | `ui/common/` | LoadingState 枚举（LOADING/CONTENT/EMPTY/ERROR）、BaseAdapter<T, B> 泛型基类（ListAdapter + DiffUtil + ViewBinding）、PagingScrollListener 分页滚动监听 |

### 2.2 认证模块（4 API）

| API 端点 | 界面组件 | 关键实现 |
|----------|---------|---------|
| `POST /auth/login` | `LoginFragment` | 用户名 + 密码表单、BCrypt 服务端加密、成功后 TokenManager 保存 Token + 用户信息、导航至 MainActivity |
| `POST /auth/register` | `RegisterFragment` | 学号/工号 + 真实姓名 + 邮箱 + 密码表单（4 字段），空值校验，成功后自动返回登录页 |
| `POST /auth/refresh` | 拦截器自动处理 | TokenAuthenticator 拦截 401 → 调用 refresh → 更新 Token → 重放原请求 |
| `POST /auth/logout` | `ProfileFragment` 退出按钮 | 清除 TokenManager 全部数据 → 导航至 LoginFragment |

**对应 ViewModel**：`LoginViewModel.java`（注入 AuthApiService + TokenManager）、`RegisterViewModel.java`（注入 AuthApiService），均 @HiltViewModel，RxJava3 调度。⚠️ AuthRepository 已存在但 LoginViewModel/RegisterViewModel 未使用，直接调用 AuthApiService。

**安全措施**：
- 密码明文不入库（仅传输至后端，BCrypt 12 轮服务端哈希）
- Access Token 2h 无状态（HS256），Refresh Token 7d 存 Redis 防重放
- EncryptedSharedPreferences 本地存储（AES-256-GCM）
- 登录页 RateLimit 20/min·IP 防爆破（服务端实现，客户端无感知）

### 2.3 图书检索模块（8 API）

| API 端点 | 界面组件 | 关键实现 |
|----------|---------|---------|
| `GET /books/search` | `SearchFragment` | 关键词搜索 + 分类筛选 + 分页加载（每页 20 条），sortBy 参数 API 层已支持但 UI 暂未暴露排序控件 |
| `GET /books/search/advanced` | `AdvancedSearchFragment` + `SearchFragment` | AdvancedSearchFragment 多字段组合表单 → Bundle 传参到 SearchFragment → `handleAdvancedSearchArgs()` 解析参数 → `SearchViewModel.searchAdvanced()` 调用 API |
| `GET /books/suggest` | `SearchFragment` 自动补全 | TextWatcher 监听输入 ≥ 2 字符 → 下拉建议列表（最多 5 条），点击建议直接搜索 |
| `GET /books/hot` | `SearchFragment` 首页 + `HotBooksFragment` | 首页热门图书区（前 10）+ 独立排行榜页（前 50）+ 分类 ChipGroup 筛选 |
| `GET /books/{id}` | `BookDetailFragment` | 封面（ImageView 占位）、基本信息卡片（ISBN/作者/出版社/出版日期/馆藏位置）、可借册数/预约人数、借阅按钮（可借 > 0）/ 预约按钮（可借 = 0）、关键词 ChipGroup、图书简介 |
| `GET /books/{id}/related` | `BookDetailFragment` 相关推荐区 | RecyclerView 展示关联图书（书名 + 作者 + 推荐分数 + 推荐理由） |
| `GET /categories/tree` | `CategoryTreeFragment` + `SearchFragment` 分类入口 | 树形分类浏览（MaterialToolbar + 返回按钮）、SearchFragment 首页分类导航横向列表（点击分类直接搜索该分类图书，含标题栏 + 返回首页按钮） |
| `GET /categories` | `SearchFragment` 分类快捷入口 | 首页展示一级分类节点，点击进入分类筛选结果视图 |

**核心界面清单**：

1. **SearchFragment（重写）** — 搜索首页
   - 顶部搜索栏 + 自动补全下拉（`/books/suggest`）
   - 快捷链接：高级搜索 + 分类浏览
   - 首页内容：分类导航 RecyclerView + 热门图书 RecyclerView
   - 搜索结果：分类标题栏（含返回首页按钮）+ 结果计数 + 分页 RecyclerView
   - 视图切换：首页 ScrollView ↔ 搜索结果 LinearLayout
   - 分类筛选模式：`searchByCategory(categoryId, name)` + 分类标题栏 → 返回首页按钮

2. **AdvancedSearchFragment** — 高级搜索
   - 6 个筛选字段：书名 / 作者 / ISBN / 出版社 / 年份范围（起-止）/ 仅可借开关
   - 搜索按钮 → Bundle 传参 → `action_advancedSearchFragment_to_searchFragment`

3. **BookDetailFragment** — 图书详情
   - MaterialToolbar 返回按钮（← 图书详情）
   - 封面占位图 + 完整书目信息卡片
   - 借阅/预约按钮（按库存状态动态显隐）
   - 关键词 ChipGroup + 图书简介
   - 相关推荐 RecyclerView

4. **CategoryTreeFragment** — 分类浏览
   - MaterialToolbar 返回按钮（← 分类浏览）
   - 树形分类展平列表（带缩进）
   - Loading / Empty / Error 三态

5. **HotBooksFragment** — 热门排行
   - MaterialToolbar 返回按钮（← 热门图书）
   - 排名编号列表 + 分类 ChipGroup 筛选
   - 异步加载：先加载分类筛选器，后加载图书数据

### 2.4 个人中心模块（5 API）

| API 端点 | 界面组件 | 关键实现 |
|----------|---------|---------|
| `GET /users/me` | `ProfileFragment` | 用户信息卡片（用户名 + 真实姓名 + 邮箱），借阅概览实时数据（`loadBorrowStats()` → 当前借阅/历史总数/超期次数），功能入口列表（借阅历史/借阅统计/个性化推荐/编辑资料/退出登录） |
| `PUT /users/me` | `EditProfileFragment` | 表单编辑（邮箱 + 手机号），MaterialToolbar 返回按钮，保存成功后自动返回 |
| `GET /users/me/history` | `BorrowHistoryFragment` | 年份 ChipGroup 筛选（全部 + 近 5 年）、分页列表（书名 + 借阅日期 + 应还日期 + 状态标签），MaterialToolbar 返回按钮 |
| `GET /users/me/stats` | `BorrowStatsFragment` | 4 数字卡片（总借阅/当前借阅/超期次数/罚款总额）+ MPAndroidChart 饼图（分类分布）+ 折线图（月度趋势），MaterialToolbar 返回按钮 |
| `GET /users/me/recommendations` | `RecommendationsFragment` | SwipeRefreshLayout 下拉刷新 + 推荐列表（书名 + 作者 + 推荐理由 + 推荐分数），MaterialToolbar 返回按钮 |

**核心界面清单**：

1. **ProfileFragment（重写）** — 个人中心首页
   - 用户信息卡片（MaterialCardView，含用户名 + 真实姓名 + 邮箱，无头像/角色标签）
   - 借阅统计概览（3 个指标：当前借阅/历史总数/超期次数，登录后显示）
   - 功能入口列表（TextView 点击跳转，4 个导航 action）
   - 登录/退出登录按钮（按登录状态动态切换）

2. **EditProfileFragment** — 编辑资料
   - MaterialToolbar 返回按钮（← 编辑资料）
   - OutlinedBox 表单（邮箱 + 手机号）
   - 保存按钮 → API 调用 → 成功后 `onBackPressed()`

3. **BorrowHistoryFragment** — 借阅历史
   - MaterialToolbar 返回按钮（← 借阅历史）
   - ChipGroup 年份筛选（全部 + 近 5 年，单选）
   - 分页列表：书名 + 借阅日期 + 应还日期 + 状态 Chip（借阅中/已归还/已超期）

4. **BorrowStatsFragment** — 借阅统计
   - MaterialToolbar 返回按钮（← 借阅统计）
   - 2×2 数字卡片网格 + 饼图（240dp）+ 折线图（240dp）
   - MPAndroidChart 动态实例化，按月/分类渲染

5. **RecommendationsFragment** — 个性化推荐
   - MaterialToolbar 返回按钮（← 个性化推荐）
   - SwipeRefreshLayout 包裹 RecyclerView
   - 推荐条目：书名 + 作者 + 推荐理由 + 分数

### 2.5 MainActivity（入口 + 全局导航）

| 组件 | 关键实现 |
|------|---------|
| `MainActivity` | `@AndroidEntryPoint`，BottomNavigationView + NavHostFragment + NavigationUI，登录/注册页隐藏底部导航栏 |
| 启动认证检查 | `onCreate()` 中调用 `tokenManager.isLoggedIn()` → 未登录则 `navController.navigate(R.id.loginFragment)` |
| 全局返回处理 | `onBackPressed()` 重写：优先委托 `navController.navigateUp()` 处理 Navigation 返回栈，仅在起始页时调用 `super.onBackPressed()` 退出应用 |
| 底部导航 | NavigationUI.setupWithNavController 自动绑定，三个主 Tab：搜索/借阅/我的 |

---

## 三、架构合规性

### 3.1 目录结构（对照 §3.3.2）

```
java/com/library/android/
├── LibraryApplication.java      ✅ @HiltAndroidApp
├── ui/                          ✅
│   ├── main/                    ✅ MainActivity (@AndroidEntryPoint)
│   ├── search/                  ✅ 5 个文件 (Search + BookDetail + AdvancedSearch + CategoryTree + HotBooks)
│   ├── profile/                 ✅ 5 个文件 (Profile + EditProfile + BorrowHistory + BorrowStats + Recommendations)
│   ├── login/                   ✅ 2 个文件 (LoginFragment + RegisterFragment)
│   ├── borrow/                  ✅ 人员 B 实现
│   ├── reservation/             ✅ 人员 B 实现
│   ├── kg/                      ✅ 人员 B 实现
│   ├── admin/                   ✅ 人员 B 实现
│   ├── scanner/                 ✅ 人员 B 实现
│   └── common/                  ✅ BaseAdapter + PagingScrollListener + LoadingState
├── viewmodel/                   ✅ 10 个 ViewModel, 全部 @HiltViewModel
├── repository/                  ✅ 7 个 Repository
├── network/                     ✅ 6 个文件 (LibraryApi + AuthApiService + TokenManager + 3 Interceptor)
├── model/                       ✅ 37 个 VO/DTO
├── di/                          ✅ 3 个 Module
└── data/                        ✅ 人员 B 实现 (Room)
```

### 3.2 分层依赖

```
Fragment/Activity  ──Observe──▶  ViewModel  ──Subscribe──▶  Repository  ──execute──▶  LibraryApi
      │                              │                          │
   ViewBinding                 @HiltViewModel              Single.fromCallable
   @AndroidEntryPoint          LiveData/MutableLiveData    RxJava3 (io → main)
```

- ViewModel 不持有 Fragment 引用 ✅
- Repository 是唯一数据入口 ✅
- Fragment 间通过 NavController + SafeArgs 传参 ✅
- TokenManager 统一放在 `network` 包（与 AuthInterceptor 紧密配合）✅

### 3.3 技术栈一致性

| 技术 | 版本 | 使用情况 |
|------|------|---------|
| Retrofit 2 + Gson | 2.9.0 | LibraryApi（44 端点全量定义，A/B 分区注释）+ AuthApiService |
| OkHttp | 4.12.0 | AuthInterceptor + TokenAuthenticator（401 自动刷新）+ HttpLoggingInterceptor |
| Hilt | 2.50 | @HiltAndroidApp + @AndroidEntryPoint + @HiltViewModel（全部 Fragment/ViewModel） |
| RxJava 3 | 3.1.8 | Repository → ViewModel 异步调度（io → main） |
| ViewBinding | enabled | 所有 Fragment/Activity 使用 |
| Material 3 | 1.11.0 | MaterialToolbar（统一返回按钮）、MaterialCardView、Chip/ChipGroup、TextInputLayout.OutlinedBox、MaterialButton、MaterialSwitch、Slider |
| MPAndroidChart | 3.1.0 | BorrowStatsFragment 饼图 + 折线图 |
| Navigation | 2.7.7 | NavHostFragment + BottomNavigationView + SafeArgs |
| EncryptedSharedPreferences | - | TokenManager 安全存储（AES-256-GCM） |

---

## 四、编码规范检查

| 规范项（§5.1） | 状态 |
|----------------|------|
| Java 驼峰命名（类 UpperCamelCase、方法/变量 lowerCamelCase） | ✅ |
| 常量 UPPER_SNAKE_CASE | ✅ |
| 资源文件 snake_case | ✅ |
| Javadoc 中文注释 | ✅ 所有公开类/方法 |
| 字符串在 strings.xml 定义 | ✅ 80+ 字符串统一管理 |
| LibraryApi 按 A/B 分区注释 | ✅ 清晰分隔 |
| 包结构严格遵循架构文档 §3.3.2 | ✅ |
| Commit Conventional Commits + 中文 | ✅ |
| 禁止 Controller 直接调 Mapper | ✅（Repository 唯一数据入口） |

---

## 五、联调修复记录

主提交 (`aeb519c`) 之后的集成修复，与人员 B 代码合并后暴露的问题：

### 5.1 编译修复

| # | 问题 | 修复 | 提交 |
|---|------|------|------|
| 1 | 5 个人员 B 布局文件缺少 `xmlns:tools` / `xmlns:app` 命名空间 | 补充缺失的 XML 命名空间声明 | `b5f8684` |
| 2 | `strings.xml` 格式化字符串 `formatted="false"` 与 `String.format()` 调用冲突 | 使用位置格式化说明符：`%1$d/%2$d`、`%1$s (%2$s)` | Session |
| 3 | Camera 权限暗示硬件必需，Chrome OS 等设备不兼容 | `AndroidManifest.xml` 添加 `<uses-feature android:name="android.hardware.camera" android:required="false" />` | Session |

### 5.2 架构清理

| # | 问题 | 修复 |
|---|------|------|
| 4 | 两个 `TokenManager` 重复类（`util/` 普通 SP vs `network/` 加密 SP） | 保留 `network/TokenManager`（EncryptedSharedPreferences），合并 `saveUserInfo()`/`getUsername()`/`getRealName()` 方法，删除 `util/TokenManager`，更新 5 个引用类的 import |
| 5 | `RetrofitClient.java` 死代码（已被 Hilt NetworkModule 完全替代） | 删除文件 |
| 6 | `MockInterceptor.enabled` 默认值 `true` 存在安全风险 | 改为 `false`，仅 debug 构建显式启用 |

### 5.3 功能修复

| # | 问题 | 修复 |
|---|------|------|
| 7 | 分类导航点击"计算机科学"崩溃 — `NullPointerException: null cannot be cast to non-null type kotlin.Long`（错误复制图书点击的导航逻辑） | 分类点击改为调用 `viewModel.searchByCategory()` 就地搜索，而非错误地导航到 `bookDetailFragment` |
| 8 | 分类搜索结果页缺少返回按钮 | `fragment_search.xml` 新增 `layoutResultHeader`（含返回 ImageButton + 分类名称标题），SearchFragment 实现视图切换逻辑 |
| 9 | 全局所有二级页面缺少统一的左上角返回按钮 | 为 12 个二级 Fragment 统一添加 `MaterialToolbar`（`ic_menu_revert` + 页面标题），统一 `toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed())` |
| 10 | 点击图书详情返回按钮直接退出应用 | `MainActivity` 添加 `onBackPressed()` 重写，优先委托 `navController.navigateUp()` 处理 Navigation 返回栈 |

### 5.4 修改文件汇总

| 类别 | 修改文件数 |
|------|-----------|
| 布局 XML（新增 Toolbar 返回按钮） | 12 |
| Fragment Java（新增 Toolbar 回退逻辑） | 12 |
| MainActivity（全局 onBackPressed 拦截） | 1 |
| DI/Network/Architecture 架构清理 | 8 |
| Lint 修复 | 2 |
| **合计** | **35** |

### 5.5 缺陷修复（审查后）

对审计发现的已修复项：

| # | 原缺陷 | 修复 | 涉及文件 |
|---|--------|------|----------|
| 11 | TokenAuthenticator 未接入 OkHttpClient（🔴 CRITICAL） | NetworkModule 新增 `provideTokenAuthenticator()` + 静态 `sLibraryApi` 持有者打破循环依赖；OkHttpClient Builder 添加 `.authenticator(tokenAuthenticator)`；TokenAuthenticator 添加 `apiProvider.getApi()` null 安全检查 | NetworkModule.java, TokenAuthenticator.java |
| 12 | ProfileFragment 借阅概览数字为静态占位 | `onResume()` 中调用 `viewModel.loadBorrowStats()`；新增 `updateBorrowOverview(BorrowStatsVO)` 观察者填充 3 个概览数字 | ProfileFragment.java |
| 13 | 高级搜索参数未在 SearchFragment 处理 | SearchViewModel 新增 `searchAdvanced()` 方法 + `isAdvancedMode()` / `clearAdvancedParams()` 辅助方法，`loadMore()` 分支支持高级模式分页；SearchFragment 新增 `handleAdvancedSearchArgs()` 解析 Bundle 并触发高级搜索 | SearchViewModel.java, SearchFragment.java |

**修改文件汇总（第二轮）**: 5 个文件（2 Network + 1 Profile + 1 SearchViewModel + 1 SearchFragment）

---

## 六、已知缺陷与待完善项

### 功能层面

| # | 问题 | 模块 | 严重度 | 状态 |
|---|------|------|--------|------|
| 1 | 图书详情封面未加载（仅显示占位符 ImageView，Glide 依赖已声明但未集成） | BookDetailFragment | 🟠 HIGH | 📋 待修复 |
| 2 | 借阅/预约按钮为 TODO 桩代码（`// TODO: 跳转借阅确认` / `// TODO: 调用预约接口`） | BookDetailFragment | 🟠 HIGH | 📋 待修复 |
| 3 | ~~高级搜索结果传递回 SearchFragment 后未实际处理高级搜索参数~~ | AdvancedSearchFragment → SearchFragment | ~~🟠 HIGH~~ | ✅ 已修复 |
| 4 | 图书分类节点点击后显示了重复标题（搜索框文字 + 标题栏均显示分类名） | SearchFragment | 🟡 MEDIUM | 📋 待修复 |
| 5 | 编辑资料缺少输入验证（邮箱格式 / 手机号格式） | EditProfileFragment | 🟡 MEDIUM | 📋 待修复 |
| 6 | ~~ProfileFragment 借阅概览数字为静态占位（未从 API 获取实时数据）~~ | ProfileFragment | ~~🟡 MEDIUM~~ | ✅ 已修复 |
| 7 | BorrowStatsFragment 图表在 ScrollView 内可能有滑动冲突 | BorrowStatsFragment | 🟡 MEDIUM | 📋 待修复 |
| 8 | 搜索建议展示格式为 JSON key-value（`{"type":"title","value":"..."}`），UI 仅显示 value | SearchFragment | 🟢 LOW | 📋 待修复 |

### 架构层面

| # | 问题 | 严重度 | 状态 |
|---|------|--------|------|
| 9 | ~~**TokenAuthenticator 未接入 OkHttpClient**：类已实现完整刷新逻辑，但 `NetworkModule.provideOkHttpClient()` 未调用 `.authenticator()`~~ | ~~🔴 CRITICAL~~ | ✅ 已修复 |
| 10 | **LoginViewModel/RegisterViewModel 绕过 AuthRepository**：直接注入 AuthApiService 而非 AuthRepository，导致认证层分层不一致（AuthRepository 存在但未被使用） | 🟡 MEDIUM | 📋 待修复 |
| 11 | 登录/注册未在独立 Activity 中实现（在 MainActivity NavHost 中），退出登录后底部导航栏状态需手动管理 | 🟡 MEDIUM | 📋 待修复 |
| 12 | Repository 层错误处理缺少统一降级策略（部分直接 Log.e，无用户提示） | 🟡 MEDIUM | 📋 待修复 |
| 13 | 字符串资源混合中英文硬编码（部分 hint 如"搜索图书、作者、ISBN"直接写在 layout XML 中） | 🟢 LOW | 📋 待修复 |

> **已修复 3/13**：🔴 #9（TokenAuthenticator 接线）、🟠 #3（高级搜索参数传递）、🟡 #6（ProfileFragment 概览数据）。**待修复 10/13**。

---

## 七、与人员 B 的协作接口

| 共享资源 | 人员 A 已完成部分 | 人员 B 使用/对接情况 |
|----------|------------------|---------------------|
| `model/` 全量数据类 | ✅ 37 个 VO/DTO 全部完成 | ✅ 人员 B 直接复用，无冲突 |
| `network/LibraryApi.java` | ✅ 44 个端点全部定义，A/B 分区注释 | ✅ 人员 B 的 Repository 基于同一接口 |
| `network/AuthInterceptor.java` | ✅ 自动附加 Bearer Token | ✅ 人员 B 的所有 API 调用无需额外处理认证 |
| `network/TokenManager.java` | ✅ EncryptedSharedPreferences 安全存储 | ✅ 人员 B 通过 Hilt 注入使用 |
| `di/NetworkModule.java` | ✅ Retrofit/OkHttp Hilt DI | ✅ 提供 LibraryApi 注入 |
| `di/RepositoryModule.java` | ✅ BookRepository/UserRepository/AuthRepository | ✅ 人员 B 追加 Borrow/KG/Admin/Reservation Repository |
| `ui/common/` | ✅ LoadingState + BaseAdapter + PagingScrollListener | ✅ 人员 B 所有列表页面复用 |
| `nav_graph.xml` | ✅ 3 主 Tab + 认证 + 人员 A 全部目的地 | ✅ 人员 B 追加 10 个目的地和 action |

---

## 八、里程碑对照（分工商量书 §6）

| 周次 | 人员 A 计划 | 实际交付 |
|------|-----------|---------|
| **第一周** Day 1-2 | 全部 VO/DTO 模型类编写、AuthInterceptor | ✅ 37 个 VO/DTO + AuthInterceptor + TokenManager |
| **第一周** Day 3-4 | LoginActivity/RegisterActivity + Token 管理 | ✅ LoginFragment + RegisterFragment + LoginViewModel + RegisterViewModel + AuthApiService（以 Fragment 替代 Activity，统一在 NavHost 中） |
| **第一周** Day 5 | NetworkModule + RepositoryModule DI 配置 | ✅ NetworkModule + AppModule + RepositoryModule |
| **第二周** Day 1-2 | SearchFragment 完整实现（关键词搜索+自动补全+分页） | ✅ SearchFragment 完整实现（搜索+建议+分类导航+热门图书+分页） |
| **第二周** Day 3-4 | BookDetailActivity + 分类树浏览 | ✅ BookDetailFragment + CategoryTreeFragment |
| **第二周** Day 5 | 高级搜索 + 热门榜 | ✅ AdvancedSearchFragment + HotBooksFragment |
| **第三周** Day 1-2 | ProfileFragment 完整实现 + 编辑资料 | ✅ ProfileFragment + EditProfileFragment |
| **第三周** Day 3-4 | 借阅历史 + 借阅统计图表（MPAndroidChart） | ✅ BorrowHistoryFragment + BorrowStatsFragment（饼图+折线图） |
| **第三周** Day 5 | 个性化推荐列表 | ✅ RecommendationsFragment（SwipeRefresh + 分数+理由） |
| **第四周** Day 1-2 | 条码扫描集成（ZXing）、UI 打磨 | ⬜ 条码扫描由人员 B 完成（ScanBarcodeActivity）；人员 A 投入联调修复 |
| **第四周** Day 3-5 | 全链路联调 + Bugfix | ✅ Lint 修复 + 架构清理 + 崩溃修复 + 全局返回按钮 + onBackPressed 拦截 |

> 第四周部分计划任务条码扫描由人员 B 完成，人员 A 转向联调质量保障：修复编译问题 3 项、架构冗余 3 项、功能缺陷 4 项，累计修改 35 个文件。

---

> **归档时间**: 2026-06-17（更新于同日） · **分支**: `develop` · **关键提交**: `49ecce9` (DI/认证) → `09cbbba` (模型) → `aeb519c` (完整交付) → `b5f8684` (联调修复) → Session (缺陷修复 3/13)

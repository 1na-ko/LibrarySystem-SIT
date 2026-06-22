# CLAUDE.md — Android 前端模块开发指引

> **项目**: 图书馆智能管理系统 Android 前端
> **技术栈**: Java 17 + Gradle Kotlin DSL + MVVM + Hilt + Retrofit + RxJava3 + ViewBinding
> **设计系统**: Material Design 3 + 朱砂红学术美学（v3.0）
> **生产服务**: `http://101.132.24.73:8080/api/v1/`
> **最后更新**: 2026-06-19（阶段 14 系统化重构 + 真机走查修复）

---

## 1. 架构概览

```
┌─────────────────────────────────────────────┐
│  UI Layer (Fragment / Activity)              │
│  BaseFragment ← observeError / observeLoading │
├─────────────────────────────────────────────┤
│  ViewModel Layer                             │
│  BaseViewModel ← errorEvent / loadingState   │
├─────────────────────────────────────────────┤
│  Repository Layer                            │
│  ApiCallExecutor ← HTTP→异常翻译              │
├─────────────────────────────────────────────┤
│  Network Layer (Retrofit + OkHttp + RxJava3) │
│  AuthInterceptor / TokenAuthenticator        │
└─────────────────────────────────────────────┘
```

### 依赖方向

```
Fragment → ViewModel → Repository → LibraryApi (Retrofit)
                  ↑
              Hilt DI
```

---

## 2. 目录结构

```
library-android/app/src/main/java/com/library/android/
├── LibraryApplication.java          # @HiltAndroidApp 入口
├── di/
│   ├── AppModule.java               # TokenManager 提供
│   ├── NetworkModule.java           # OkHttp/Retrofit/LibraryApi
│   └── RepositoryModule.java        # 8 个 Repository
├── model/                           # 30+ VO/Request/Response
├── network/
│   ├── LibraryApi.java              # Retrofit 接口（50 端点）
│   ├── ApiCallExecutor.java         # HTTP→异常统一翻译
│   ├── AuthInterceptor.java         # Bearer Token 注入
│   ├── TokenAuthenticator.java      # 401 自动刷新
│   ├── TokenManager.java            # EncryptedSharedPreferences
│   ├── SessionManager.java          # 全局会话失效广播
│   └── exception/                   # 7 个业务异常类
├── repository/                      # 8 个 Repository
├── viewmodel/                       # 14 个 ViewModel
│   ├── BaseViewModel.java           # loadingState + errorEvent
│   ├── HomeViewModel.java           # 推荐流式（WP4.1 新建）
│   ├── ProfileViewModel.java        # 个人中心（WP4.1 精简）
│   ├── OverdueViewModel.java        # 超期管理（WP2.2 新建）
│   └── ...                          # 其他 ViewModel
└── ui/
    ├── main/                        # MainActivity + HomeFragment
    ├── login/                       # 登录/注册
    ├── search/                      # 搜索 + 详情 + 分类 + 热门
    ├── borrow/                      # 借阅管理 + 超期管理
    ├── profile/                     # 个人中心
    ├── reservation/                 # 预约管理
    ├── kg/                          # 知识图谱（4 页）
    ├── admin/                       # 用户管理 + Dashboard + 图书编目
    ├── acquisition/                 # 智能采编（6 页）
    └── common/                      # BaseFragment/BaseAdapter/工具
```

---

## 3. 导航结构

### 底部 Tab（3 个）

| Tab | Fragment | 说明 |
|-----|----------|------|
| 首页 | `HomeFragment` | 混合首页（AI 推荐流式 + 热门 + 分类） |
| 借阅 | `BorrowFragment` | 借阅管理（全部/借阅中/已归还/已超期 Tab） |
| 我的 | `ProfileFragment` | 个人中心 + 管理工具入口 |

### 全局入口

- **搜索按钮**（全局头部右侧）→ `SearchFragment`（二级页）
- **会话过期**→ 自动跳登录并清栈

### 管理端入口（ProfileFragment → 角色可见）

- Librarian/Admin → 用户管理 + Dashboard + 超期管理
- Acquisitor → 智能采编（采购预测/查重/缺口/谈判）

---

## 4. 设计系统

### 配色

| 用途 | 颜色值 | 变量 |
|------|--------|------|
| 主背景 | `#F7F4EB` | `bg_primary` |
| 卡片背景 | `#FDFCF8` | `bg_card` |
| 主强调色 | `#C93756` | `accent_cta` |
| 可用色 | `#4A7C59` | `accent_available` |
| 预约色 | `#D4A24C` | `accent_reserve` |
| 深色背景 | `#1A1A1A` | `dark_bg_primary` |
| 深色卡片 | `#2C2C2E` | `dark_bg_card` |

### 暗色模式页面

- 知识图谱（4 页）：`knowledgeGraphFragment`, `literatureTraceFragment`, `subjectNetworkFragment`, `entitySearchFragment`
- 管理端（7 页）：`adminUserListFragment`, `adminDashboardFragment`, `acquisitionFragment`, `purchasePredictFragment`, `duplicateCheckFragment`, `gapAnalysisFragment`, `negotiationCreateFragment`, `negotiationDetailFragment`
- 图书编目：`BookEditActivity`（通过 AndroidManifest theme 静态声明）

### 排版

- 标题：衬线字体 (serif)，28sp
- 正文：无衬线字体，15sp
- 说明文字：12sp

---

## 5. 关键模式

### BaseFragment

提供 `observeError(LiveData<Throwable>)` — 按异常类型映射到 `strings.xml` 的文案并 Snackbar 展示。

### BaseViewModel

提供 `loadingState` (IDLE/LOADING/CONTENT/EMPTY/ERROR) 和 `errorEvent` (SingleLiveEvent 防重复 Snackbar)，以及 `disposables` (CompositeDisposable 自动清理)。

### BaseAdapter

泛型适配器基类，支持 DiffUtil 差异更新。

### ApiCallExecutor

静态方法，执行 Retrofit Call → 按 HTTP 状态码转换为类型化异常。

---

## 6. 构建与运行

```bash
# 调试构建
./gradlew assembleDebug

# 安装到真机
adb install app/build/outputs/apk/debug/app-debug.apk

# Lint 检查
./gradlew lint
```

---

## 7. 当前状态

### 阶段 14 系统化重构（2026-06-18）

**后端**（WP-0）：
- ✅ 采编三功能修复 + 部署上线：subjectId 递归子分类 / 查重 LIKE 前置 / 预测降级 / DB 字符集修复

**前端**（WP-1 ∼ WP-12）：
- ✅ 导航治本（page_toolbar.xml + BaseFragment.setupToolbar / 标题屏幕居中 / 搜索仅首页）
- ✅ 搜索页返回修复（删内部状态机，一次返回回首页）+ 布局 NestedScrollView
- ✅ 首页分类横向+浏览全部+错误提示+资源化
- ✅ 契约对齐（DashboardVO / BorrowStatsVO / BookDetailVO / TraceGraph / AdminBook / suggest / RenewResult / Reservation）
- ✅ ViewModel 遮蔽清理（BorrowVM/ReservationVM）+ KG keyPath 接入
- ✅ KG 模块 Loading 修复 + observeError + EntitySearch 空态
- ✅ 3 处死按钮修复（推荐/热门/分类）+ HotBooks 标签横向滚动+单选 + 推荐页滚动+颜色统一
- ✅ 预约取消按钮可见（朱砂红#C93756）+ BorrowHistory 分页+可点击 + EditProfile 校验
- ✅ 扫码权限运行时请求 + 暗色 7 Fragment 补 wrapContext
- ✅ 管理端饼图数据修复 + 重建图谱进度反馈 + 用户组合筛选
- ✅ 采编 DuplicateCheck/GapAnalysis 结构化卡片 UI + 表单 AutoComplete + 谈判返回不重触发

### 已知限制（阶段 14 未覆盖）
- ECharts 本地化（离线不可用，当前 CDN 在线可用）
- 无障碍/横屏适配

### 阶段 14 后真机走查修复（2026-06-19）

> 针对真机走查发现的 9 项问题 + 3 轮回归修复，覆盖搜索页/谈判流式/取消预约。

**搜索页修复**：
- ✅ 搜索页 titlebar — `fragment_search.xml` 引入 `page_toolbar`，`SearchFragment` 改继承 `BaseFragment`
- ✅ 清空按钮 icon — 新建 `ic_close_vector.xml`（标准 X 形），替换旧的 `ic_search_vector` + `rotation=45` hack
- ✅ 分类导航箭头 — `strings.xml` 中 `category_nav` 删除 Unicode `▸` 字符（此前误改 `item_category.xml` 的 `ivExpand`）
- ✅ 热门搜索词 — 从 8 个硬编码扩展到 23 个，覆盖更多学科领域
- ✅ 搜索结果展示 — 新增 `searchMethodLabel`（关键词搜索/ISBN搜索/分类浏览/高级搜索），格式 `关键词搜索 "机器学习" 找到 5 条结果`
- ✅ 删除"继续搜索"按钮 — `tvSearchAgain` 与 `page_toolbar` 返回按钮功能重复
- ✅ ISBN 扫码返回 — `removeExtra("isbn")` 防止 `navigateUp` 重新触发导航死循环
- ✅ 返回按钮 — `BaseFragment.setupToolbar` 中 `navigateUp()` 返回 false（startDestination）时兜底调 `onBackPressed()`

**谈判流式防闪退**：
- ✅ SSE `done` 事件识别 — 后端 `event:done` 后 `data:` 为空，`dataBuf.length()==0` 导致帧被跳过的边界 bug
- ✅ `body.close()` IOException — `parseSseStream` 返回 `boolean`（done→true），`fromCallable` 捕获后按 `completed` 抑制 close 异常
- ✅ 流式取消跨 VM 污染 — `AcquisitionViewModel.onCleared()` 移除 `repository.disposeStreams()`（repository 是全局单例，创建页 VM 清理会错误取消详情页活跃流）
- ✅ 流式生命周期 — `NegotiationDetailFragment.onDestroyView` 主动调 `viewModel.disposeStreams()`；Repository 新增 `volatile streamCancelled` 合作取消标志

**取消预约修复**：
- ✅ 后端终态扩展 — `cancel()` 从仅允许 `WAITING` 改为拒绝 RESERVED/COMPLETED/EXPIRED/CANCELLED 四种终态，允许 NOTIFIED 取消
- ✅ V6 唯一约束根治 — 新增 `ReservationMapper.physicalCleanStaleWaiting` 物理 DELETE（绕过 MyBatis-Plus 逻辑删除），`reserve()` 前置清理 + `cancel()` 约束冲突时 `deleteById`→`physicalClean`
- ✅ 前端错误可见 — `ReservationListFragment` 改继承 `BaseFragment` + `observeError()`；取消成功 `!ok` 时 `postError` 透传服务端消息
- ✅ 取消反馈 — 取消失败时显示具体错误 Snackbar（替代静默吞没）

# 前端人员 B 完成记录

> **分支**: `feature/frontend-borrow`
> **完成日期**: 2026-06-16
> **人员**: 人员 B（借阅管理 + 预约管理 + 知识图谱 + 后台管理）
> **依据**: [前端分工商量书](../前端分工商量书.md) §4
> **提交**: `927e3b5` — `feat(android): 实现人员B全部前端模块`

---

## 一、产出概览

| 层次 | 文件数 | 说明 |
|------|--------|------|
| Model（VO/DTO） | 28 | 全量数据模型类，对齐 OpenAPI `library-api.yaml` |
| Network | 4 | LibraryApi（44 端点）、TokenManager、AuthInterceptor、TokenAuthenticator |
| DI（Hilt） | 3 | NetworkModule、DatabaseModule、RepositoryModule |
| Room 数据库 | 5 | AppDatabase + 2 Entity + 2 DAO（本地缓存层） |
| Repository | 6 | Borrow、Reservation、KnowledgeGraph、Admin、Auth、Book |
| ViewModel | 5 | Borrow、BorrowDetail、Reservation、KnowledgeGraph、Admin |
| Fragment / Activity | 13 | 11 个业务界面 + 2 个公共组件 |
| 布局 XML | 14 | 全部使用 Material 3 + ViewBinding |
| 资源文件 | 6 | strings（80+）、colors、arrays、drawable×2 |
| **合计** | **87+** | 含 nav_graph 更新、Manifest 更新、Gradle 配置 |

---

## 二、模块完成详情

### 2.1 借阅管理（6 API）

| API 端点 | 界面组件 | 关键实现 |
|----------|---------|---------|
| `POST /borrows` | `BorrowConfirmDialog` | BottomSheetDialogFragment，图书摘要展示 + 最大借阅数提示 + 确认调用 API |
| `GET /borrows/my` | `BorrowFragment`（重写） | TabLayout 四状态切换（全部/借阅中/已归还/已超期）、分页加载、下拉刷新、DiffUtil 增量更新 |
| `GET /borrows/{id}` | `BorrowDetailFragment` | 图书信息卡片（封面+书名+作者+ISBN）、借阅/应还/归还日期、续借次数（≤1）+ 续借按钮、归还按钮、罚款信息 |
| `PUT /borrows/{id}/return` | `BorrowDetailFragment` 内归还按钮 | MaterialAlertDialog 确认 → API 调用 → 成功后隐藏操作区 |
| `PUT /borrows/{id}/renew` | `BorrowDetailFragment` 内续借按钮 | 确认弹窗 → API 调用 → 刷新详情 |
| `GET /borrows/overdue` | `OverdueFragment` | 管理员超期列表（分页），复用 `item_borrow_record` 布局 |

### 2.2 预约管理（4 API）

| API 端点 | 界面组件 | 关键实现 |
|----------|---------|---------|
| `POST /reservations` | 图书详情页按钮触发 | API 已定义，界面入口由人员 A 实现 |
| `GET /reservations/my` | `ReservationListFragment` | 6 状态 Tab 筛选（全部/等待中/已通知/已锁定/已完成/已取消）、分页、下拉刷新排队位置 |
| `DELETE /reservations/{id}` | 左滑取消 | ItemTouchHelper.LEFT → 确认弹窗 → API 调用 |
| `GET /reservations/{id}/queue-position` | `ReservationStatusCard` | 复用组件：排队位置/总等待人数、已通知 48h 提示、取消按钮 |

### 2.3 知识图谱（4 API）

| API 端点 | 界面组件 | 可视化方案 |
|----------|---------|-----------|
| `GET /kg/book/{id}/graph` | `KnowledgeGraphFragment` | **WebView + ECharts 5.5 力导向图**：5 类节点着色（图书/作者/关键词/学科/出版物）、深度 Slider（1-3 跳）、手势缩放拖拽 |
| `GET /kg/book/{id}/trace` | `LiteratureTraceFragment` | 有向引用链图：方向 Spinner（前向/后向/双向）+ 深度 Spinner（1-5 跳）、箭头边符号 |
| `GET /kg/subject/{name}` | `SubjectNetworkFragment` | 学科关键词关联网络：输入框搜索学科名、WebView+ECharts 渲染 |
| `GET /kg/search` | `EntitySearchFragment` | 实体搜索列表：ChipGroup 类型筛选（全部/图书/作者/关键词/学科）、结果含 PageRank 中心度 |

### 2.4 系统管理（5 API）

| API 端点 | 界面组件 | 关键实现 |
|----------|---------|---------|
| `GET /admin/users` | `AdminUserListFragment` | 搜索栏（IME_ACTION_SEARCH）+ 角色筛选弹窗 + 分页列表（用户名+姓名+角色+状态+借阅数+超期数） |
| `PUT /admin/users/{id}/status` | 点击用户 → 操作弹窗 | 按当前状态动态选项：ACTIVE→冻结/禁用、FROZEN→解冻/禁用、DISABLED→解冻 |
| `POST /admin/books` | `BookEditActivity`（新增模式） | 完整表单 9 字段：ISBN/书名/作者/出版社/出版日期/总册数/馆藏位置/简介/关键词 |
| `PUT /admin/books/{id}` | `BookEditActivity`（编辑模式） | 同上表单，预填现有数据，新增删除按钮 |
| `DELETE /admin/books/{id}` | 确认对话框 → API | 确认弹窗 → 删除 → 返回 |

### 2.5 条码扫描

| 组件 | 实现 |
|------|------|
| `ScanBarcodeActivity` | ZXing `DecoratedBarcodeView` + `decodeContinuous()`，扫描后通过 `Intent` 回传 ISBN，支持 `source` 参数区分借书/编目入口 |

### 2.6 智能采编（可选）

| 状态 | 说明 |
|------|------|
| API 接口已定义 | `LibraryApi` 中 5 个端点（predict/duplicate-check/gap-analysis/negotiation/suggestion）已完整定义 |
| UI 未实现 | 按分工商量书 §4.6 标记为"优先级较低，若进度允许则实现"，当前不做 |

---

## 三、公共基础设施（B 主导部分）

按分工商量书 §5.5 公共资源分配表，人员 B 主导以下公共资源：

| 公共资源 | 产出物 | 说明 |
|----------|--------|------|
| `di/DatabaseModule.java` | ✅ | Room 数据库 + CachedBookDao + CachedBorrowDao 注入 |
| `ui/common/BaseAdapter.java` | ✅ | 通用 RecyclerView Adapter 基类（ListAdapter + DiffUtil + ViewBinding 泛型） |
| `ui/common/PagingScrollListener.java` | ✅ | 分页滚动监听器 |
| `ui/common/LoadingState.java` | ✅ | UI 四态枚举（LOADING/CONTENT/EMPTY/ERROR） |
| Room 数据库 | ✅ | `AppDatabase` + `CachedBookEntity` + `CachedBorrowEntity` + 2 DAO |
| `nav_graph.xml`（补充） | ✅ | 新增 10 个人员 B 目的地 + action |

---

## 四、架构合规性

### 4.1 目录结构（对照 §3.3.2）

```
java/com/library/android/
├── LibraryApplication.java      ✅ @HiltAndroidApp
├── ui/                          ✅
│   ├── main/                    ✅ MainActivity (@AndroidEntryPoint)
│   ├── search/                  ✅ SearchFragment (stub, 人员A待实现)
│   ├── borrow/                  ✅ 4 个文件 (BorrowFragment + Detail + ConfirmDialog + Overdue)
│   ├── profile/                 ✅ ProfileFragment (stub, 人员A待实现)
│   ├── common/                  ✅ BaseAdapter + PagingScrollListener + LoadingState
│   ├── kg/                      🆕 4 个文件 (知识图谱, 合理扩展)
│   ├── reservation/             🆕 2 个文件 (预约管理, 合理扩展)
│   ├── admin/                   🆕 2 个文件 (系统管理, 合理扩展)
│   └── scanner/                 🆕 1 个文件 (条码扫描, 合理扩展)
├── viewmodel/                   ✅ 5 个 ViewModel, 全部 @HiltViewModel
├── repository/                  ✅ 6 个 Repository
├── network/                     ✅ 4 个文件 (LibraryApi + TokenManager + 2 Interceptor)
├── model/                       ✅ 28 个 VO/DTO
├── di/                          ✅ 3 个 Module
├── data/                        🆕 Room 数据库 (AppDatabase + entity + dao, 合理扩展)
└── util/                        ⚠️ 空目录，待后续补充工具类
```

### 4.2 分层依赖

```
Fragment/Activity  ──Observe──▶  ViewModel  ──Subscribe──▶  Repository  ──execute──▶  LibraryApi
      │                              │                          │
   ViewBinding                 @HiltViewModel              Single.fromCallable
   @AndroidEntryPoint          LiveData/MutableLiveData    RxJava3 (io → main)
```

- ViewModel 不持有 Fragment 引用 ✅
- Repository 是唯一数据入口 ✅
- Fragment 间通过 NavController + SafeArgs 跳转 ✅

### 4.3 技术栈一致性

| 技术 | 版本 | 使用情况 |
|------|------|---------|
| Retrofit 2 + Gson | 2.9.0 | LibraryApi + GsonConverterFactory |
| OkHttp | 4.12.0 | AuthInterceptor + TokenAuthenticator + Logging |
| Hilt | 2.50 | @HiltAndroidApp + @AndroidEntryPoint + @HiltViewModel |
| Room | 2.6.1 | AppDatabase + 2 Entity + 2 DAO + RxJava3 桥接 |
| RxJava 3 | 3.1.8 | Repository → ViewModel 异步调度 |
| ViewBinding | enabled | 所有 Fragment/Activity 使用 |
| Glide | 4.16.0 | 依赖已声明（封面加载待人员 A 在图书详情实现） |
| ZXing | 4.3.0 | ScanBarcodeActivity |
| Material 3 | 1.11.0 | 全部布局使用 M3 组件 |

---

## 五、编码规范检查

| 规范项（§5.1） | 状态 |
|----------------|------|
| Java 驼峰命名（类 UpperCamelCase、方法/变量 lowerCamelCase） | ✅ |
| 常量 UPPER_SNAKE_CASE | ✅ |
| 资源文件 snake_case | ✅ |
| Javadoc 中文注释 | ✅ 所有公开类/方法 |
| 字符串在 strings.xml 定义 | ✅ 80+ 字符串统一管理 |
| LibraryApi 按 A/B 分区注释 | ✅ 9 个分区清晰分隔 |
| Commit Conventional Commits + 中文 | ✅ |

---

## 六、已知缺陷与待完善项

审查发现的遗留问题，建议在联调阶段（第四周）修复：

### 功能层面

| # | 问题 | 模块 | 严重度 |
|---|------|------|--------|
| 1 | 封面图片未用 Glide 加载（仅显示占位符） | BorrowDetailFragment | 🟠 HIGH |
| 2 | ReservationStatusCard 倒计时为静态文本，非实时 CountDownTimer | 预约管理 | 🟠 HIGH |
| 3 | 节点大小未按 PageRank 值缩放（硬编码 25/40） | KnowledgeGraphFragment | 🟠 HIGH |
| 4 | 边粗细未按 weight 映射 lineStyle | KnowledgeGraphFragment | 🟠 HIGH |
| 5 | 文献溯源缺少路径高亮与权重标注 | LiteratureTraceFragment | 🟠 HIGH |
| 6 | 学科网络缺少 Top-K 调节控件（硬编码 100） | SubjectNetworkFragment | 🟠 HIGH |
| 7 | 用户管理缺少状态下拉筛选 | AdminUserListFragment | 🟠 HIGH |
| 8 | 管理员权限判断缺失（未做入口显隐/拦截） | AdminUserListFragment / OverdueFragment | 🟠 HIGH |
| 9 | 图书编目缺少分类下拉选择（硬编码 categoryId=1） | BookEditActivity | 🟠 HIGH |
| 10 | ISBN 格式校验缺失（仅空值检查，无 ISBN-10/13 正则） | BookEditActivity | 🟡 MEDIUM |

### 架构层面

| # | 问题 | 严重度 |
|---|------|--------|
| 11 | `util/` 包缺失（目标目录结构 §3.3.2 明确要求） | 🟡 MEDIUM |
| 12 | `OverdueFragment` 绕过 ViewModel 直接注入 Repository | 🟡 MEDIUM |

---

## 七、与人员 A 的协作接口

| 共享资源 | 人员 B 已完成部分 | 人员 A 需要做的 |
|----------|------------------|----------------|
| `model/` 全量数据类 | ✅ 28 个 VO/DTO 全部完成 | Review + 按需调整 |
| `network/LibraryApi.java` | ✅ 44 个端点全部定义，A/B 分区注释 | 按 §3.1-3.4 实现 A 区 UI |
| `network/AuthInterceptor.java` | ✅ 自动附加 Bearer Token | 实现 LoginActivity/Token 存储 |
| `di/NetworkModule.java` | ✅ Retrofit/OkHttp Hilt DI | 无需额外工作 |
| `ui/common/` | ✅ BaseAdapter + PagingScrollListener + LoadingState | 直接复用 |
| `nav_graph.xml` | ✅ 3 主 Tab + 10 个人员 B 目的地 | 补充 A 方目的地和 action |

---

## 八、里程碑对照（分工商量书 §6）

| 周次 | 人员 B 计划 | 实际交付 |
|------|-----------|---------|
| **第一周** Day 1-2 | Room 数据库 + DatabaseModule + BaseAdapter | ✅ AppDatabase + 2 Entity + 2 DAO + DatabaseModule + BaseAdapter |
| **第一周** Day 5 | BorrowFragment 框架 | ✅ BorrowFragment 完整实现（Tab + 分页 + FAB） |
| **第二周** Day 1-2 | BorrowFragment 完整实现 | ✅ |
| **第二周** Day 3-4 | 借阅详情 + 归还/续借 | ✅ BorrowDetailFragment + OverdueFragment + BorrowConfirmDialog |
| **第二周** Day 5 | 预约列表 + 取消 + 排队 | ✅ ReservationListFragment + ReservationStatusCard |
| **第三周** Day 1-5 | 知识图谱可视化（4 页面） | ✅ WebView+ECharts 方案（4 个 Fragment） |
| **第四周** Day 1-2 | 系统管理模块 | ✅ AdminUserListFragment + BookEditActivity |
| **第四周** Day 1-2 | 条码扫描 ZXing | ✅ ScanBarcodeActivity |

> 按分工商量书第四周计划，人员 B 剩余的 "全链路联调、权限判断（管理员入口显隐）" 和 "智能采编（若进度允许）" 需与人员 A 协作推进。

---

> **归档时间**: 2026-06-16 · **分支**: `feature/frontend-borrow` · **提交**: `927e3b5`

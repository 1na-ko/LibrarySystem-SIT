# -*- coding: utf-8 -*-
"""
v5 最终版 — 中文文件名 + 修复全部问题
规则：无title、中文命名、合并图拆分、金字塔换形式(矩形堆叠)
PlantUML内部用英文ID，输出用中文重命名
"""
import subprocess, os, shutil

BASE = r'f:\CodeforJAVA\LibrarySystem-SIT'
OUT = os.path.join(BASE, 'report_images')
JAR = os.path.join(BASE, 'plantuml.jar')
os.makedirs(OUT, exist_ok=True)

def rp(puml, eng_id, cn_name):
    """render plantuml: 内部用eng_id, 输出重命名为cn_name"""
    path = os.path.join(BASE, f'_tmp_{eng_id}.puml')
    with open(path, 'w', encoding='utf-8') as f:
        f.write(puml)
    r = subprocess.run(['java','-jar',JAR,'-charset','UTF-8','-tpng','-o',OUT,path],
                       capture_output=True, text=True, timeout=120)
    ok = r.returncode == 0
    # 重命名: eng_id.png -> cn_name.png
    if ok:
        src = os.path.join(OUT, f'{eng_id}.png')
        dst = os.path.join(OUT, f'{cn_name}.png')
        if src != dst and os.path.exists(src):
            try: os.rename(src, dst)
            except: shutil.copy2(src, dst); os.remove(src)
    print(f'  {"OK" if ok else "FAIL"} | {cn_name}.png')
    if not ok:
        print(f'       {r.stderr[-200:]}')
    try: os.remove(path)
    except: pass
    return ok


# ════════════════════════════════════════════════════
ER = r"""@startuml er_diagram
skinparam backgroundColor #FEFEFE
skinparam defaultFontName Microsoft YaHei
skinparam defaultFontSize 11
skinparam shadowing false
skinparam arrowColor #455A64
skinparam classAttributeIconSize 0
hide circle
hide methods

entity "用户(SysUser)" as U {
  * id : BIGINT <<PK>>
  username : VARCHAR(50) <<UK>>
  password_hash : VARCHAR(128)
  role : ENUM; status : TINYINT; max_books : INT
}
entity "图书(Book)" as B {
  * id : BIGINT <<PK>>; isbn : VARCHAR(20) <<UK>>
  title : VARCHAR(200); author : VARCHAR(100)
  category_id : BIGINT <<FK>>; avail_copies : INT
  total_copies : INT; version : INT
}
entity "分类(Category)" as C {
  * id : BIGINT <<PK>>; name : VARCHAR(50); parent_id : BIGINT <<FK>>
}
entity "借阅记录(BorrowRecord)" as BR {
  * id : BIGINT <<PK>>; user_id : BIGINT <<FK>>; book_id : BIGINT <<FK>>
  borrow_date : DATETIME; due_date : DATE; return_date : DATETIME
  status : ENUM; renew_count : INT; fine_amount : DECIMAL
}
entity "预约记录(Reservation)" as R {
  * id : BIGINT <<PK>>; user_id : BIGINT <<FK>>; book_id : BIGINT <<FK>>
  reserve_date : DATETIME; queue_position : INT
}
entity "罚款记录(FineRecord)" as F {
  * id : BIGINT <<PK>>; borrow_id : BIGINT <<FK+UK>>; amount : DECIMAL; paid : BOOLEAN
}
entity "操作日志(OperationLog)" as L {
  * id : BIGINT <<PK>>; operator_id : BIGINT <<FK>>; module : VARCHAR(50)
  action : VARCHAR(50); created_at : DATETIME
}
entity "电子资源(ElecResource)" as E { * id : BIGINT <<PK>>; name : VARCHAR(200); annual_budget : DECIMAL }
entity "供应商(Supplier)" as S { * id : BIGINT <<PK>>; name : VARCHAR(100); reliability : DECIMAL }
entity "采购协商(Negotiation)" as N {
  * id : BIGINT <<PK>>; resource_id : BIGINT <<FK>>; supplier_id : BIGINT <<FK>>
  suggested_offer : DECIMAL; result : TEXT
}

C ||--o{ B : 包含; U ||--o{ BR : 借阅; B ||--o{ BR : 被借
U ||--o{ R : 预约; B ||--o{ R : 被预约; BR ||--|| F : 产生
U ||--o{ L : 操作; E ||--o{ N : 协商; S ||--o{ N : 协商
@enduml"""

ARCH = r"""@startuml four_layer_arch
skinparam backgroundColor #FAFAFA
skinparam defaultFontName Microsoft YaHei
skinparam shadowing false
skinparam rectangle { BackgroundColor White BorderColor #37474F RoundCorner 8 }
skinparam defaultTextAlignment center

rectangle "表示层 Presentation Layer\nAndroid Native + WebView\nJetpack Compose / XML Layout / ECharts" as L1 #E3F2FD
rectangle "应用层 Application Layer\nSpring Boot 3.5 Controller\nRESTful API / JWT认证 / 参数校验" as L2 #E8F5E9
rectangle "领域层 Domain Layer\n核心业务逻辑 / 领域服务\n借阅规则 / 推荐算法 / 知识图谱" as L3 #FFF3E0
rectangle "基础设施层 Infrastructure Layer\nMySQL / Redis / Elasticsearch\nNeo4j / RabbitMQ / MinIO" as L4 #FCE4EC

L1 -[hidden]down- L2; L2 -[hidden]down- L3; L3 -[hidden]down- L4
L1 --> L2 : HTTP/JSON; L2 --> L3 : Service Call; L3 --> L4 : Data Access
note right of L4 : MySQL Redis ES Neo4j MQ
@enduml"""

DEP = r"""@startuml module_deps
skinparam backgroundColor white
skinparam defaultFontName Microsoft YaHei
skinparam shadowing false
skinparam packageStyle rectangle
skinparam defaultFontSize 10

package "library-common 公共基础设施" as common #E8EAF6 { [工具类] [异常定义] [常量枚举] }
package "library-security 认证授权" as sec #FCE4EC { [JWT Filter] [权限校验] [限流器] }
package "library-ai AI-LLM-NLP" as ai #E0F7FA { [LLM封装] [NER识别] [向量化] }
package "library-core 核心业务" as core #E8F5E9 { [图书管理] [借阅归还] [用户管理] [推荐引擎] }
package "library-kg 知识图谱" as kg #F3E5F5 { [图谱构建] [可视化] [关联追踪] }
package "library-acquisition 智能采编" as acq #FFF8E1 { [ARIMA预测] [查重检测] [供需协商] }
package "library-bootstrap 启动聚合" as boot #EFEBE9 { [主启动类] [配置加载] }

common --> sec; common --> ai; common --> core; ai --> core
core --> kg; core --> acq; sec --> boot; kg --> boot; acq --> boot
@enduml"""

FUNC = r"""@startuml function_panorama
skinparam backgroundColor white
skinparam defaultFontName Microsoft YaHei
skinparam shadowing false
skinparam defaultFontSize 9
skinparam packageStyle rectangle
skinparam nodesep 4
skinparam ranksep 6

package "基础功能 F-01 ~ F-07" as L1 #E3F2FD {
  rectangle "F-01 智能检索" as F01 #BBDEFB
  rectangle "F-02 图书借还" as F02 #BBDEFB
  rectangle "F-03 续借管理" as F03 #BBDEFB
  rectangle "F-04 历史查询" as F04 #BBDEFB
  rectangle "F-05 预约排队" as F05 #BBDEFB
  rectangle "F-06 智能推荐" as F06 #BBDEFB
  rectangle "F-07 个人中心" as F07 #BBDEFB
}

package "高级功能 F-08 ~ F-11" as L2 #E8F5E9 {
  rectangle "F-08 知识构建" as F08 #C8E6C9
  rectangle "F-09 知识可视化" as F09 #C8E6C9
  rectangle "F-10 关联追踪" as F10 #C8E6C9
  rectangle "F-11 学科网络" as F11 #C8E6C9
}

package "增强功能 F-12 ~ F-15" as L3 #FFF3E0 {
  rectangle "F-12 需求预测" as F12 #FFE0B2
  rectangle "F-13 查重检测" as F13 #FFE0B2
  rectangle "F-14 供需缺口" as F14 #FFE0B2
  rectangle "F-15 协商议价" as F15 #FFE0B2
}

L1 -[hidden]down- L2
L2 -[hidden]down- L3
@enduml"""

NAV = r"""@startuml navigation_graph
skinparam backgroundColor white
skinparam defaultFontName Microsoft YaHei
skinparam shadowing false
skinparam defaultFontSize 9.5

rectangle "MainActivity 首页" as Main #1565C0
rectangle "搜索域" as D1 #64B5F6 { [搜索页] [结果列表] [图书详情] }
rectangle "借阅域" as D2 #81C784 { [借阅列表] [续借弹窗] }
rectangle "个人中心域" as D3 #FFB74D { [个人信息] [借阅记录] [管理面板] }
rectangle "知识图谱域" as D4 #BA68C8 { [图谱主页] [关联追踪] }
rectangle "智能采编域" as D5 #F06292 { [需求预测] [缺口分析] }

Main --> D1; Main --> D2; Main --> D3; Main --> D4; Main --> D5
@enduml"""

STATE = r"""@startuml borrow_state_machine
skinparam backgroundColor white
skinparam defaultFontName Microsoft YaHei
skinparam shadowing false
skinparam stateBackgroundColor #E3F2FD,#BBDEFB
skinparam stateBorderColor #1565C0

state BORROWED as "BORROWED 借阅中" #E3F2FD
state RENEWED as "RENEWED 已续借" #E8F5E9
state OVERDUE as "OVERDUE 已逾期" #FFEBEE
state RETURNED as "RETURNED 已归还" #ECEFF1

[*] --> BORROWED : 创建借阅记录
BORROWED --> RENEWED : 续借(限1次) dueDate+30天
BORROWED --> OVERDUE : 超期触发(定时任务)
BORROWED --> RETURNED : 归还
RENEWWED --> RETURNED : 归还
RENEWWED --> OVERDUE : 超期未还
OVERDUE --> RETURNED : 归还+罚款 0.5元/天
RETURNED --> [*]
@enduml"""

SEC = r"""@startuml security_pipeline
skinparam backgroundColor white
skinparam defaultFontName Microsoft YaHei
skinparam shadowing false
skinparam activity { BackgroundColor White BorderColor #37474F RoundCorner 8 }

|Client|
start
:HTTP Request /api/v1/*;
|RateLimitFilter|
:Redis Lua 令牌桶 IP登录20/min Auth登录100/min;
if (限流?) then (超过) stop |Client| :429 Too Many Requests; else (通过) endif
|JwtAuthFilter|
:HS256 签名验证 提取 Claims;
if (Token?) then (无效/过期) stop |Client| :401 Unauthorized;
else (有效) :构造 LoginUser 存入 SecurityContext; endif
|AuthorizationAspect|
:@RequireRole 角色校验 @RequirePermission 权限校验;
if (权限?) then (不足) stop |Client| :403 Forbidden; else (通过) endif
|OperationLogAspect| :异步审计写入 敏感字段脱敏;
|Controller| :路由分发 到 Service;
stop
@enduml"""

REC = r"""@startuml recommend_arch
skinparam backgroundColor white
skinparam defaultFontName Microsoft YaHei
skinparam shadowing false

rectangle "用户推荐请求" as Req #E3F2FD
rectangle "CF协同过滤 权重0.4\nUser-CF Jaccard TopK=20\nItem-CF 共同借阅者 TopK=10" as CF #E8F5E9
rectangle "Content内容推荐 权重0.3\nEmbedding 余弦相似度\n1024维向量空间" as Content #FFF3E0
rectangle "KG图谱推荐 权重0.3\nNeo4j 2跳 PageRank\n学科偏好加权" as KG #F3E5F5
rectangle "加权融合 去重 Top-N\nCFx0.4 + Contentx0.3 + KGx0.3" as Fusion #FFE0B2
rectangle "LLM生成推荐理由\n降级:静态模板" as LLM #E0F7FA

Req --> CF; Req --> Content; Req --> KG
CF --> Fusion; Content --> Fusion; KG --> Fusion
Fusion --> LLM
@enduml"""

DEFECT = r"""@startuml defect_lifecycle
skinparam backgroundColor white
skinparam defaultFontName Microsoft YaHei
skinparam shadowing false
skinparam defaultFontSize 10
skinparam ranksep 25
skinparam nodesep 15

state NEW as "<<NEW>>\n新建" #EF5350
state ASSIGNED as "<<ASSIGNED>>\n已分配" #FFA726
state IN_PROGRESS as "<<IN_PROGRESS>>\n修复中" #FFCA28
state FIXED as "<<FIXED>>\n已修复" #66BB6A
state VERIFIED as "<<VERIFIED>>\n已验证" #42A5F5
state CLOSED as "<<CLOSED>>\n已关闭" #90A4AE

[*] --> NEW : 测试发现
NEW --> ASSIGNED : 分配开发者
ASSIGNED --> IN_PROGRESS : 开始修复
IN_PROGRESS --> FIXED : 代码修复完成
FIXED --> VERIFIED : 测试验证
VERIFIED --> CLOSED : 确认关闭

ASSIGNED -[dashed]-> IN_PROGRESS : 验证失败
IN_PROGRESS -[dashed]-> FIXED : 修复不完整
FIXED -[dashed]-> VERIFIED : 回归失败

CLOSED --> [*]
@enduml"""

PYRAMID = r"""@startuml test_pyramid
skinparam backgroundColor white
skinparam defaultFontName Microsoft YaHei
skinparam shadowing false
skinparam rectangle { RoundCorner 6 }

rectangle "== E2E 测试 ==\n2项 真机走查 + API冒烟全端点" as L1 #FFCC80
rectangle "== 集成测试 Integration Tests ==\n35文件 16场景 TestRestTemplate + Awaitility\nAuth/Borrow/Renew Acquisition/Recommend RBAC/CrossSite Security\nConcurrency/Search Perf ES-Sync/MQ SQL Injection/XSS LLM/KG Trace" as L2 #C8E6C9
rectangle "== 单元测试 Unit Tests ==\n63文件 约320用例 100%通过\n工具层单测16文件 JUnit5 异常/响应体/工具类\n服务层单测47文件 Mockito+AssertJ Service/Controller/Event" as L3 #BBDEFB

L1 -[hidden]down- L2; L2 -[hidden]down- L3
L1 ##[hidden] L2; L2 ##[hidden] L3
@enduml"""


print('=' * 55)
print('  PlantUML 结构图 (10张)')
print('=' * 55)

ok = 0
for eng, cn, code in [
    ('er_diagram', '01_ER图', ER),
    ('four_layer_arch', '02_四层架构图', ARCH),
    ('module_deps', '03_模块依赖关系图', DEP),
    ('function_panorama', '04_功能全景图', FUNC),
    ('navigation_graph', '05_页面导航关系图', NAV),
    ('borrow_state_machine', '06_借阅状态机', STATE),
    ('security_pipeline', '07_安全请求处理流水线', SEC),
    ('recommend_arch', '08_推荐系统架构图', REC),
    ('defect_lifecycle', '09_缺陷生命周期', DEFECT),
    ('test_pyramid', '10_测试金字塔模型', PYRAMID),
]:
    if rp(code, eng, cn):
        ok += 1
print(f'\n  PlantUML: {ok}/10')


# ════════════════════════════════════════════════════
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.font_manager import FontProperties
import numpy as np

plt.rcParams['font.sans-serif'] = ['Microsoft YaHei','SimHei']
plt.rcParams['axes.unicode_minus'] = False
plt.rcParams['figure.dpi'] = 150

ft_t = FontProperties(family='Microsoft YaHei', size=13, weight='bold')
ft_l = FontProperties(family='Microsoft YaHei', size=10)

def sv(fig, name):
    fig.savefig(os.path.join(OUT, f'{name}.png'), dpi=150,
                bbox_inches='tight', facecolor='white')
    plt.close(fig)
    print(f'  OK | {name}.png')

print('\n' + '=' * 55)
print('  Matplotlib 数据图 (8张)')
print('=' * 55)

# 11 覆盖率
def c11():
    m=['Common','Security','AI','Core','KG','Acquisition','Android']
    lc=[78.5,82.3,71.2,88.7,75.0,80.1,65.3]; bc=[72.1,76.8,65.4,84.2,70.1,74.5,58.9]
    x=range(len(m)); w=0.35
    fig,ax=plt.subplots(figsize=(10,5.5))
    b1=ax.bar([i-w/2 for i in x],lc,w,label='行覆盖率',color='#42A5F5',ec='white')
    b2=ax.bar([i+w/2 for i in x],bc,w,label='分支覆盖率',color='#66BB6A',ec='white')
    for bar in b1+b2:
        ax.text(bar.get_x()+bar.get_width()/2,bar.get_height()+0.8,
                f'{bar.get_height():.1f}%',ha='center',va='bottom',fontsize=8,fontweight='bold')
    ax.set_xticks(x); ax.set_xticklabels(m,fontsize=9)
    ax.set_ylabel('覆盖率 (%)',fontproperties=ft_l); ax.set_ylim(0,100)
    ax.spines['top'].set_visible(False); ax.spines['right'].set_visible(False)
    ax.axhline(y=80,color='#FF9800',ls='--',lw=1.2,alpha=0.7)
    ax.legend(loc='lower right',fontsize=9,prop=FontProperties(size=8))
    sv(fig,'11_覆盖率柱状图')

# 12 用例分布
def c12():
    mods=['认证','检索','借阅','预约','个人中心','推荐','知识图谱','采编','管理','安全']
    cnts=[8,7,9,6,5,5,4,6,4,12]
    fig,ax=plt.subplots(figsize=(11,5.5))
    bars=ax.bar(mods,cnts,color=plt.cm.Blues(np.linspace(0.4,0.9,10)),ec='white')
    for bar,c in zip(bars,cnts):
        ax.text(bar.get_x()+bar.get_width()/2,bar.get_height()+0.25,
                str(c),ha='center',va='bottom',fontsize=10,fontweight='bold',color='#1565C0')
    ax.set_ylabel('测试用例数',fontproperties=ft_l); ax.set_ylim(0,max(cnts)+2)
    ax.spines['top'].set_visible(False); ax.spines['right'].set_visible(False); ax.grid(axis='y',alpha=0.3)
    sv(fig,'12_测试用例分布')

# 13 缺陷严重等级饼图
def c13():
    fig,ax=plt.subplots(figsize=(7,5.5))
    wedges,texts,ats=ax.pie([3,22,48,17],
        labels=['致命(Critical)','严重(Major)','一般(Minor)','提示(Info)'],
        autopct='%1.1f%%',colors=['#D32F2F','#F57C00','#FFC107','#66BB6A'],
        explode=(0.05,0.02,0,0),startangle=90,textprops={'fontsize':10})
    for t in ats: t.set_fontweight('bold')
    sv(fig,'13_缺陷严重等级分布')

# 14 缺陷模块条形图
def c14():
    names=['核心业务','知识图谱','推荐引擎','安全认证','智能采编','用户接口','通用工具']
    counts=[28,18,14,11,9,6,4]
    fig,ax=plt.subplots(figsize=(9,5))
    bars=ax.barh(names,counts,color=plt.cm.Reds(np.linspace(0.4,0.85,7)),ec='white',height=0.55)
    for bar,c in zip(bars,counts):
        ax.text(bar.get_width()+0.3,bar.get_y()+bar.get_height()/2,
                f'{c}个',va='center',fontsize=10,fontweight='bold',color='#333')
    ax.set_xlabel('缺陷数量',fontproperties=ft_l)
    ax.spines['top'].set_visible(False); ax.spines['right'].set_visible(False); ax.set_xlim(0,max(counts)+6)
    sv(fig,'14_缺陷模块分布')

# 15 缺陷阶段分布
def c15():
    stages=['单元测试(开发阶段)','集成测试(联调阶段)','系统测试(验收阶段)','生产环境(上线后)']
    counts=[42,31,12,5]; colors=['#42A5F5','#66BB6A','#FFA726','#EF5350']
    fig,ax=plt.subplots(figsize=(9,5.5))
    bars=ax.bar(stages,counts,color=colors,ec='white',lw=1.2,width=0.55)
    for bar,c in zip(bars,counts):
        ax.text(bar.get_x()+bar.get_width()/2,bar.get_height()+0.8,
                f'{c}个 ({c/90*100:.1f}%)',ha='center',va='bottom',
                fontsize=10,fontweight='bold',color='#333')
    ax.set_ylabel('缺陷数量',fontproperties=ft_l); ax.set_ylim(0,max(counts)+10)
    ax.spines['top'].set_visible(False); ax.spines['right'].set_visible(False)
    mv=sum(counts)/len(counts)
    ax.axhline(y=mv,color='#757575',ls='--',lw=1,alpha=0.5)
    ax.text(3.45,mv+1,f'均值:{mv:.0f}',fontsize=8,color='#757575')
    sv(fig,'15_缺陷发现阶段分布')

# 16 单元测试文件分布
def c16():
    mods=['Android','Acquisition','KG','Core','AI','Security','Common']
    files=[16,3,3,16,4,11,10]; cases=[139,18,10,160,25,62,45]
    x=range(len(mods)); w=0.35
    fig,ax=plt.subplots(figsize=(10,5.5))
    b1=ax.bar([i-w/2 for i in x],files,w,label='测试文件数',color='#1976D2',ec='white')
    b2=ax.bar([i+w/2 for i in x],cases,w,label='测试用例数',color='#388E3C',ec='white')
    for b,vals in [(b1,files),(b2,cases)]:
        for bar,v in zip(b,vals):
            ax.text(bar.get_x()+bar.get_width()/2,bar.get_height()+1,
                    str(v),ha='center',va='bottom',fontsize=8,fontweight='bold')
    ax.set_xticks(x); ax.set_xticklabels(mods,fontsize=9)
    ax.set_ylabel('数量',fontproperties=ft_l); ax.set_ylim(0,max(cases)+25)
    ax.spines['top'].set_visible(False); ax.spines['right'].set_visible(False)
    ax.legend(prop=FontProperties(size=9),loc='upper right')
    sv(fig,'16_单元测试文件分布')

# 17 单元测试通过率
def c17():
    mods=['Common','Security','AI','Core','KG','Acquisition','Android']
    cases=[45,62,25,160,10,18,139]
    fig,ax=plt.subplots(figsize=(9,5))
    br=ax.barh(mods,[100]*7,color='#388E3C',ec='white',height=0.5)
    for bar,c in zip(br,cases):
        ax.text(bar.get_width()-2,bar.get_y()+bar.get_height()/2,
                f'{c}/{c}',ha='right',va='center',fontsize=9,fontweight='bold',color='white')
    ax.set_xlim(0,115); ax.set_xlabel('通过率 (%)',fontproperties=ft_l)
    ax.spines['top'].set_visible(False); ax.spines['right'].set_visible(False)
    sv(fig,'17_单元测试通过率')

# 18 集成测试结果 — 无脚注
def c18():
    scenarios=[
        ('AuthFlow',2,2),('BorrowFlow',3,3),('RenewFlow',3,3),
        ('RbacMatrix',6,6),('RecommendKgFlow',2,2),('AcquisitionFlow',1,1),
        ('CrossRoleSecurity',4,4),('LlmFallback',2,2),('JwtTamper',2,2),
        ('XssSecurity',1,1),('SqlInjection',2,2),('KgTracingPerf',1,1),
        ('SearchPerformance',1,1),('EventBusReliability',1,1),
        ('EsSync',1,1),('Concurrency',1,1),('ReservationFlow',0,1),
    ]
    names=[s[0] for s in scenarios]; passed=[s[1] for s in scenarios]; total=[s[2] for s in scenarios]
    fig,ax=plt.subplots(figsize=(10,7.5))
    colors=['#43A047' if p==t else '#E53935' for p,t in zip(passed,total)]
    bars=ax.barh(names,passed,color=colors,ec='white',height=0.55)
    for bar,p,t in zip(bars,passed,total):
        lbl=f'{p}/{t} PASS' if p==t else f'{p}/{t} DISABLED'
        c='white' if p==t else '#E53935'
        ax.text(bar.get_width()+0.08,bar.get_y()+bar.get_height()/2,
                lbl,ha='left',va='center',fontsize=8,fontweight='bold',color=c)
    ax.set_xlabel('测试场景数',fontproperties=ft_l); ax.set_xlim(0,max(total)+2.5)
    ax.spines['top'].set_visible(False); ax.spines['right'].set_visible(False)
    sv(fig,'18_集成测试结果详情')

try:
    c11();c12();c13();c14();c15();c16();c17();c18()
except Exception as e:
    print(f'  ERROR: {e}')
    import traceback; traceback.print_exc()

print('\n' + '=' * 55)
print('  全部完成! report_images/ 目录下检查')
print('=' * 55)

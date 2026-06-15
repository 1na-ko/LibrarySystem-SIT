import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  // 公开路由
  { path: '/', name: 'home', component: () => import('@/views/home/Index.vue'), meta: { title: '首页' } },
  { path: '/login', name: 'login', component: () => import('@/views/auth/Login.vue'), meta: { title: '登录' } },
  { path: '/register', name: 'register', component: () => import('@/views/auth/Register.vue'), meta: { title: '注册' } },
  { path: '/search', name: 'search', component: () => import('@/views/search/Index.vue'), meta: { title: '图书检索' } },
  { path: '/book/:id', name: 'book-detail', component: () => import('@/views/book/Detail.vue'), meta: { title: '图书详情' } },

  // 图谱探索（公开）
  { path: '/graph/explore', name: 'graph-explore', component: () => import('@/views/graph/Explore.vue'), meta: { title: '知识图谱' } },
  { path: '/graph/trace', name: 'graph-trace', component: () => import('@/views/graph/Trace.vue'), meta: { title: '文献溯源' } },
  { path: '/graph/hotspot', name: 'graph-hotspot', component: () => import('@/views/graph/Hotspot.vue'), meta: { title: '学科热点' } },

  // 读者路由（需登录）
  {
    path: '/user',
    meta: { requiresAuth: true },
    children: [
      { path: 'borrows', name: 'my-borrows', component: () => import('@/views/user/BorrowList.vue'), meta: { title: '当前借阅' } },
      { path: 'history', name: 'my-history', component: () => import('@/views/user/History.vue'), meta: { title: '借阅历史' } },
      { path: 'reserves', name: 'my-reserves', component: () => import('@/views/user/ReserveList.vue'), meta: { title: '我的预约' } },
      { path: 'recommend', name: 'my-recommend', component: () => import('@/views/user/Recommend.vue'), meta: { title: '推荐' } },
      { path: 'ai-chat', name: 'ai-chat', component: () => import('@/views/ai/ChatAssistant.vue'), meta: { title: 'AI助手' } },
      { path: 'notifications', name: 'notifications', component: () => import('@/views/notification/NotificationList.vue'), meta: { title: '通知' } },
    ]
  },

  // 馆员路由
  {
    path: '/librarian',
    meta: { requiresAuth: true, roles: ['LIBRARIAN', 'ADMIN'] },
    children: [
      { path: 'dashboard', name: 'librarian-dashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '工作台' } },
    ]
  },

  // 采编路由（提高版）
  {
    path: '/acquisition',
    meta: { requiresAuth: true, roles: ['ACQUISITION', 'ADMIN'] },
    children: [
      { path: 'predictions', name: 'predictions', component: () => import('@/views/acquisition/PredictionList.vue'), meta: { title: '采购预测' } },
      { path: 'predictions/:id', name: 'prediction-detail', component: () => import('@/views/acquisition/PredictionDetail.vue'), meta: { title: '预测详情' } },
      { path: 'dedup', name: 'dedup-check', component: () => import('@/views/acquisition/DedupCheck.vue'), meta: { title: '查重查缺' } },
      { path: 'suppliers', name: 'suppliers', component: () => import('@/views/acquisition/SupplierList.vue'), meta: { title: '供应商管理' } },
      { path: 'contracts/:id', name: 'contract-detail', component: () => import('@/views/acquisition/ContractDetail.vue'), meta: { title: '合同详情' } },
      { path: 'orders', name: 'orders', component: () => import('@/views/acquisition/OrderList.vue'), meta: { title: '采购订单' } },
      { path: 'budget', name: 'budget', component: () => import('@/views/acquisition/Budget.vue'), meta: { title: '预算管理' } },
    ]
  },

  // 管理员路由
  {
    path: '/admin',
    meta: { requiresAuth: true, roles: ['ADMIN'] },
    children: [
      { path: 'users', name: 'admin-users', component: () => import('@/views/admin/UserManage.vue'), meta: { title: '用户管理' } },
      { path: 'logs', name: 'admin-logs', component: () => import('@/views/admin/SystemLog.vue'), meta: { title: '操作日志' } },
    ]
  },

  // 404
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/home/NotFound.vue'), meta: { title: '404' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

// 全局前置守卫
router.beforeEach((to, _from, next) => {
  document.title = `${to.meta.title || '图书馆'} - 图书馆智能管理系统`

  if (to.meta.requiresAuth) {
    const token = localStorage.getItem('accessToken')
    if (!token) {
      return next({ name: 'login', query: { redirect: to.fullPath } })
    }
    // 角色校验简化实现
    if (to.meta.roles && Array.isArray(to.meta.roles)) {
      const role = localStorage.getItem('role') || 'READER'
      if (!(to.meta.roles as string[]).includes(role)) {
        return next({ name: 'home' })
      }
    }
  }
  next()
})

export default router

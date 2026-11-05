import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/login',
    component: () => import('../views/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/register',
    component: () => import('../views/RegisterView.vue'),
    meta: { public: true },
  },
  {
    path: '/user/login',
    component: () => import('../views/ConsumerAuthView.vue'),
    meta: { public: true, consumerAuth: true },
  },
  {
    path: '/user/register',
    component: () => import('../views/ConsumerAuthView.vue'),
    meta: { public: true, consumerAuth: true },
  },
  {
    path: '/user',
    component: () => import('../views/ConsumerCenterView.vue'),
    meta: { consumer: true },
  },
  {
    path: '/trace/:traceCode?',
    component: () => import('../views/PublicTraceView.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('../layouts/AppLayout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: () => import('../views/DashboardView.vue') },
      { path: 'batches', component: () => import('../views/BatchListView.vue') },
      { path: 'batches/new', component: () => import('../views/BatchCreateView.vue') },
      { path: 'batches/:id', component: () => import('../views/BatchDetailView.vue') },
      { path: 'team', component: () => import('../views/OrgUsersView.vue') },
      { path: 'submissions', component: () => import('../views/MySubmissionsView.vue') },
      { path: 'reviews', component: () => import('../views/ReviewView.vue') },
      { path: 'admin', component: () => import('../views/AdminView.vue') },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  if (to.meta.consumer) {
    return localStorage.getItem('consumer_token') ? true : { path: '/user/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.consumerAuth && localStorage.getItem('consumer_token')) {
    return '/user'
  }
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if ((to.path === '/login' || to.path === '/register') && auth.isLoggedIn) {
    return '/dashboard'
  }
  if (to.path === '/admin' && !auth.user?.orgType?.includes('ADMIN')) {
    return '/dashboard'
  }
  if (to.path === '/team' && !auth.roles.includes('ORG_MANAGER')) {
    return '/dashboard'
  }
  if (to.path === '/reviews' && !auth.roles.includes('ORG_MANAGER')) {
    return '/dashboard'
  }
})

export default router

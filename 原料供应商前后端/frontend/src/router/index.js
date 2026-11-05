import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/login',
    component: () => import('../views/LoginView.vue'),
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
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && auth.isLoggedIn) {
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

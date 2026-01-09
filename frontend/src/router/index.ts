import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            component: () => import('../layout/MainLayout.vue'),
            redirect: '/alarm/list',
            children: [
                {
                    path: '/alarm/list',
                    name: 'AlarmList',
                    component: () => import('../views/alarm/AlarmList.vue'),
                    meta: { requiresAuth: true, title: '告警管理' }
                },
                {
                    path: '/alarm/detail/:id',
                    name: 'AlarmDetail',
                    component: () => import('../views/alarm/AlarmDetail.vue'),
                    meta: { requiresAuth: true, title: '告警详情' }
                }
            ]
        },
        {
            path: '/login',
            name: 'Login',
            component: () => import('../views/Login.vue')
        }
    ]
})

// Navigation Guard
router.beforeEach((to, from, next) => {
    const customToken = localStorage.getItem('accessToken')
    if (to.meta.requiresAuth && !customToken) {
        next('/login')
    } else {
        next()
    }
})

export default router

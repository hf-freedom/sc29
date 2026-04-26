import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/sites',
    name: 'Sites',
    component: () => import('../views/Sites.vue')
  },
  {
    path: '/reservation',
    name: 'Reservation',
    component: () => import('../views/Reservation.vue')
  },
  {
    path: '/my-reservations',
    name: 'MyReservations',
    component: () => import('../views/MyReservations.vue')
  },
  {
    path: '/charging',
    name: 'Charging',
    component: () => import('../views/Charging.vue')
  },
  {
    path: '/records',
    name: 'Records',
    component: () => import('../views/Records.vue')
  },
  {
    path: '/user',
    name: 'User',
    component: () => import('../views/User.vue')
  },
  {
    path: '/reports/daily',
    name: 'DailyReports',
    component: () => import('../views/DailyReports.vue')
  },
  {
    path: '/reports/abnormal',
    name: 'AbnormalOrders',
    component: () => import('../views/AbnormalOrders.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

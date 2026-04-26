import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.response.use(
  response => response.data,
  error => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export default {
  getSites() {
    return api.get('/sites')
  },
  getSite(id) {
    return api.get(`/sites/${id}`)
  },
  getSitePiles(siteId) {
    return api.get(`/sites/${siteId}/piles`)
  },
  getPile(pileId) {
    return api.get(`/sites/piles/${pileId}`)
  },
  getPriceRules(siteId) {
    return api.get(`/sites/${siteId}/price-rules`)
  },
  getCurrentPrice(siteId) {
    return api.get(`/sites/${siteId}/current-price`)
  },
  checkPileAvailability(pileId, startTime, endTime) {
    return api.get(`/sites/piles/${pileId}/availability`, {
      params: { startTime, endTime }
    })
  },

  getUsers() {
    return api.get('/users')
  },
  getUser(userId) {
    return api.get(`/users/${userId}`)
  },
  recharge(userId, amount) {
    return api.post(`/users/${userId}/recharge`, null, {
      params: { amount }
    })
  },
  getUserVehicles(userId) {
    return api.get(`/users/${userId}/vehicles`)
  },
  addVehicle(userId, vehicle) {
    return api.post(`/users/${userId}/vehicles`, vehicle)
  },
  deleteVehicle(userId, vehicleId) {
    return api.delete(`/users/${userId}/vehicles/${vehicleId}`)
  },

  getUserReservations(userId) {
    return api.get(`/reservations/user/${userId}`)
  },
  getReservation(id) {
    return api.get(`/reservations/${id}`)
  },
  getActiveReservations() {
    return api.get('/reservations/active')
  },
  createReservation(data) {
    return api.post('/reservations', data)
  },
  cancelReservation(reservationId, userId) {
    return api.post(`/reservations/${reservationId}/cancel`, null, {
      params: { userId }
    })
  },
  checkIn(reservationId) {
    return api.post(`/reservations/${reservationId}/checkin`)
  },

  getUserChargingRecords(userId) {
    return api.get(`/charging/user/${userId}`)
  },
  getChargingRecord(id) {
    return api.get(`/charging/records/${id}`)
  },
  getActiveSessions() {
    return api.get('/charging/sessions/active')
  },
  getActiveSession(reservationId) {
    return api.get(`/charging/sessions/${reservationId}`)
  },
  startCharging(reservationId) {
    return api.post('/charging/start', { reservationId })
  },
  stopCharging(reservationId) {
    return api.post(`/charging/stop/${reservationId}`)
  },
  reportFault(reservationId, description) {
    return api.post(`/charging/fault/${reservationId}`, null, {
      params: { description }
    })
  },

  getDailyReports() {
    return api.get('/reports/daily')
  },
  getSiteDailyReports(siteId) {
    return api.get(`/reports/daily/site/${siteId}`)
  },
  getAllAbnormalOrders() {
    return api.get('/reports/abnormal')
  },
  getPendingAbnormalOrders() {
    return api.get('/reports/abnormal/pending')
  },
  resolveAbnormalOrder(abnormalOrderId, compensate, remark) {
    return api.post(`/reports/abnormal/${abnormalOrderId}/resolve`, null, {
      params: { compensate, remark }
    })
  }
}

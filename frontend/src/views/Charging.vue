<template>
  <div>
    <el-card>
      <template #header>
        <span>充电管理</span>
      </template>

      <el-empty v-if="sessions.length === 0" description="暂无进行中的充电">
        <el-button type="primary" @click="$router.push('/my-reservations')">查看我的预约</el-button>
      </el-empty>

      <div v-else class="charging-list">
        <el-card v-for="session in sessions" :key="session.reservationId" class="charging-card">
          <div class="charging-header">
            <div class="charging-status">
              <el-icon :size="24" color="#67c23a"><Lightning /></el-icon>
              <span class="status-text">充电中</span>
            </div>
            <el-tag type="success">进行中</el-tag>
          </div>

          <el-row :gutter="20" class="charging-info">
            <el-col :span="8">
              <div class="info-item">
                <span class="info-label">充电桩</span>
                <span class="info-value">{{ getPileName(session.pileId) }}</span>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="info-item">
                <span class="info-label">开始时间</span>
                <span class="info-value">{{ formatDateTime(session.startTime) }}</span>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="info-item">
                <span class="info-label">充电时长</span>
                <span class="info-value">{{ chargingDuration(session) }}</span>
              </div>
            </el-col>
          </el-row>

          <el-row :gutter="20" class="charging-stats">
            <el-col :span="6">
              <div class="stat-box">
                <div class="stat-value-large">{{ session.totalKwh?.toFixed(2) || '0.00' }}</div>
                <div class="stat-label">充电量 (kWh)</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-box">
                <div class="stat-value-large">{{ calculateCost(session) }}</div>
                <div class="stat-label">预估费用 (元)</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-box">
                <div class="stat-value-large">{{ currentPeriod }}</div>
                <div class="stat-label">当前时段</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-box">
                <div class="stat-value-large">{{ currentPrice }}</div>
                <div class="stat-label">当前电价 (元/度)</div>
              </div>
            </el-col>
          </el-row>

          <div class="charging-actions">
            <el-button type="danger" @click="stopCharging(session)" :loading="stopping">
              结束充电
            </el-button>
            <el-button type="warning" @click="reportFault(session)">
              上报故障
            </el-button>
          </div>
        </el-card>
      </div>
    </el-card>

    <el-dialog v-model="faultDialogVisible" title="上报故障" width="500px">
      <el-form label-width="80px">
        <el-form-item label="故障描述">
          <el-input v-model="faultDescription" type="textarea" :rows="4" placeholder="请描述故障情况" />
        </el-form-item>
        <el-alert title="注意" type="warning" :closable="false">
          上报故障后系统将停止计费并生成异常单，符合条件的将给予补偿
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="faultDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReportFault" :loading="reportingFault">确认上报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import api from '../api'

const router = useRouter()

const sessions = ref([])
const piles = ref([])
const stopping = ref(false)
const faultDialogVisible = ref(false)
const faultDescription = ref('')
const reportingFault = ref(false)
const selectedSession = ref(null)
let refreshInterval = null

const currentPeriod = computed(() => {
  const hour = dayjs().hour()
  if (hour >= 10 && hour < 14 || hour >= 18 && hour < 21) return '峰时'
  if (hour >= 7 && hour < 10 || hour >= 14 && hour < 18) return '平时'
  return '谷时'
})

const currentPrice = computed(() => {
  const hour = dayjs().hour()
  if (hour >= 18 && hour < 21) return '1.50'
  if (hour >= 10 && hour < 14) return '1.20'
  if (hour >= 7 && hour < 10 || hour >= 14 && hour < 18) return '0.80'
  return '0.40'
})

const formatDateTime = (dt) => {
  if (!dt) return ''
  return dayjs(dt).format('MM-DD HH:mm')
}

const getPileName = (pileId) => {
  const pile = piles.value.find(p => p.id === pileId)
  return pile?.name || `桩${pileId}`
}

const chargingDuration = (session) => {
  if (!session.startTime) return '0分钟'
  const minutes = dayjs().diff(dayjs(session.startTime), 'minute')
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  if (hours > 0) return `${hours}小时${mins}分钟`
  return `${mins}分钟`
}

const calculateCost = (session) => {
  const kwh = session.totalKwh || 0
  const price = parseFloat(currentPrice.value)
  const serviceFee = 0.8
  return (kwh * price + kwh * serviceFee).toFixed(2)
}

const fetchSessions = async () => {
  try {
    const res = await api.getActiveSessions()
    if (res.success) {
      sessions.value = res.data
      if (res.data.length > 0) {
        const siteIds = [...new Set(res.data.map(s => s.siteId))]
        const pilePromises = siteIds.map(siteId => api.getSitePiles(siteId))
        const pileResults = await Promise.all(pilePromises)
        piles.value = pileResults.filter(r => r.success).flatMap(r => r.data)
      }
    }
  } catch (e) {
    console.error('获取充电会话失败', e)
  }
}

const stopCharging = async (session) => {
  try {
    await ElMessageBox.confirm('确认结束充电？结束后将结算费用。', '结束充电', {
      confirmButtonText: '确认结束',
      cancelButtonText: '取消',
      type: 'warning'
    })
    stopping.value = true
    const res = await api.stopCharging(session.reservationId)
    if (res.success) {
      ElMessage.success('充电已结束')
      fetchSessions()
      router.push('/records')
    } else {
      ElMessage.error(res.message || '结束充电失败')
    }
  } catch {
    // 用户取消
  } finally {
    stopping.value = false
  }
}

const reportFault = (session) => {
  selectedSession.value = session
  faultDescription.value = ''
  faultDialogVisible.value = true
}

const confirmReportFault = async () => {
  reportingFault.value = true
  try {
    const res = await api.reportFault(selectedSession.value.reservationId, faultDescription.value)
    if (res.success) {
      ElMessage.success('故障已上报，充电已停止')
      faultDialogVisible.value = false
      fetchSessions()
    } else {
      ElMessage.error(res.message || '上报失败')
    }
  } catch (e) {
    ElMessage.error('上报失败')
  } finally {
    reportingFault.value = false
  }
}

onMounted(() => {
  fetchSessions()
  refreshInterval = setInterval(fetchSessions, 30000)
})

onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
  }
})
</script>

<style scoped>
.charging-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.charging-card {
  border: 1px solid #67c23a;
}

.charging-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.charging-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-text {
  font-size: 18px;
  font-weight: 600;
  color: #67c23a;
}

.charging-info {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px dashed #e6e6e6;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #909399;
}

.info-value {
  font-size: 14px;
  font-weight: 500;
}

.charging-stats {
  margin-bottom: 20px;
}

.stat-box {
  text-align: center;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.stat-value-large {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.charging-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>

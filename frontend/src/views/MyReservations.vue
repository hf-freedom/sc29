<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的预约</span>
          <el-radio-group v-model="statusFilter" size="small" @change="fetchReservations">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="RESERVED">待签到</el-radio-button>
            <el-radio-button label="CHECKED_IN">已签到</el-radio-button>
            <el-radio-button label="CHARGING">充电中</el-radio-button>
            <el-radio-button label="COMPLETED">已完成</el-radio-button>
            <el-radio-button label="CANCELLED">已取消</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table :data="filteredReservations" style="width: 100%" v-loading="loading">
        <el-table-column prop="reservationNo" label="预约编号" width="200" />
        <el-table-column prop="startTime" label="预约时间" width="200">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }}<br />
            至 {{ formatDateTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="充电桩">
          <template #default="{ row }">
            {{ getPileName(row.pileId) }}
          </template>
        </el-table-column>
        <el-table-column prop="depositAmount" label="保证金">
          <template #default="{ row }">
            ¥{{ row.depositAmount?.toFixed(2) || '0.00' }}
            <el-tag v-if="row.depositDeducted > 0" type="danger" size="small" style="margin-left: 8px">
              已扣 ¥{{ row.depositDeducted?.toFixed(2) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <template v-if="row.status === 'RESERVED'">
              <el-button type="primary" link @click="checkIn(row)">签到</el-button>
              <el-button type="danger" link @click="cancelReservation(row)">取消预约</el-button>
            </template>
            <template v-else-if="row.status === 'CHECKED_IN'">
              <el-button type="success" link @click="startCharging(row)">开始充电</el-button>
            </template>
            <template v-else-if="row.status === 'CHARGING'">
              <el-button type="warning" link @click="goToCharging(row)">查看充电</el-button>
              <el-button type="danger" link @click="reportFault(row)">报故障</el-button>
            </template>
            <template v-else-if="row.status === 'ABNORMAL'">
              <el-tag type="warning">异常中</el-tag>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="filteredReservations.length === 0 && !loading" description="暂无预约记录">
        <el-button type="primary" @click="$router.push('/reservation')">立即预约</el-button>
      </el-empty>
    </el-card>

    <el-dialog v-model="faultDialogVisible" title="上报故障" width="500px">
      <el-form label-width="80px">
        <el-form-item label="故障描述">
          <el-input v-model="faultDescription" type="textarea" :rows="4" placeholder="请描述故障情况" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="faultDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReportFault" :loading="reportingFault">确认上报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import api from '../api'

const router = useRouter()

const loading = ref(false)
const statusFilter = ref('')
const reservations = ref([])
const piles = ref([])
const currentUser = ref(null)

const faultDialogVisible = ref(false)
const faultDescription = ref('')
const reportingFault = ref(false)
const selectedReservation = ref(null)

const statusMap = {
  PENDING: '待处理',
  RESERVED: '待签到',
  CHECKED_IN: '已签到',
  CHARGING: '充电中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  TIMEOUT_CANCELLED: '超时取消',
  ABNORMAL: '异常'
}

const statusTypeMap = {
  PENDING: 'info',
  RESERVED: 'warning',
  CHECKED_IN: 'primary',
  CHARGING: 'success',
  COMPLETED: 'success',
  CANCELLED: 'info',
  TIMEOUT_CANCELLED: 'danger',
  ABNORMAL: 'danger'
}

const filteredReservations = computed(() => {
  if (!statusFilter.value) return reservations.value
  return reservations.value.filter(r => r.status === statusFilter.value)
})

const formatDateTime = (dt) => {
  if (!dt) return ''
  return dayjs(dt).format('MM-DD HH:mm')
}

const getStatusText = (status) => statusMap[status] || status
const getStatusType = (status) => statusTypeMap[status] || 'info'

const getPileName = (pileId) => {
  const pile = piles.value.find(p => p.id === pileId)
  return pile?.name || `桩${pileId}`
}

const fetchReservations = async () => {
  loading.value = true
  try {
    const userRes = await api.getUser(1)
    if (userRes.success) {
      currentUser.value = userRes.data
      const res = await api.getUserReservations(userRes.data.id)
      if (res.success) {
        reservations.value = res.data
        const siteIds = [...new Set(res.data.map(r => r.siteId))]
        const pilePromises = siteIds.map(siteId => api.getSitePiles(siteId))
        const pileResults = await Promise.all(pilePromises)
        piles.value = pileResults.filter(r => r.success).flatMap(r => r.data)
      }
    }
  } catch (e) {
    ElMessage.error('获取预约列表失败')
  } finally {
    loading.value = false
  }
}

const checkIn = async (row) => {
  try {
    await ElMessageBox.confirm('确认签到？', '签到确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'info'
    })
    const res = await api.checkIn(row.id)
    if (res.success) {
      ElMessage.success('签到成功！')
      fetchReservations()
    } else {
      ElMessage.error(res.message || '签到失败')
    }
  } catch {
    // 用户取消
  }
}

const cancelReservation = async (row) => {
  try {
    await ElMessageBox.confirm('确认取消预约？预约开始前30分钟内取消将扣除50%保证金', '取消预约', {
      confirmButtonText: '确认取消',
      cancelButtonText: '不取消',
      type: 'warning'
    })
    const res = await api.cancelReservation(row.id, currentUser.value.id)
    if (res.success) {
      ElMessage.success('取消成功')
      fetchReservations()
    } else {
      ElMessage.error(res.message || '取消失败')
    }
  } catch {
    // 用户取消
  }
}

const startCharging = async (row) => {
  try {
    await ElMessageBox.confirm('确认开始充电？', '开始充电', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'success'
    })
    const res = await api.startCharging(row.id)
    if (res.success) {
      ElMessage.success('开始充电！')
      router.push('/charging')
    } else {
      ElMessage.error(res.message || '开始充电失败')
    }
  } catch {
    // 用户取消
  }
}

const goToCharging = (row) => {
  router.push('/charging')
}

const reportFault = (row) => {
  selectedReservation.value = row
  faultDescription.value = ''
  faultDialogVisible.value = true
}

const confirmReportFault = async () => {
  reportingFault.value = true
  try {
    const res = await api.reportFault(selectedReservation.value.id, faultDescription.value)
    if (res.success) {
      ElMessage.success('故障已上报，充电已停止')
      faultDialogVisible.value = false
      fetchReservations()
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
  fetchReservations()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

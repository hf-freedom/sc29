<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>异常单管理</span>
          <el-radio-group v-model="statusFilter" size="small" @change="fetchOrders">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="PENDING">待处理</el-radio-button>
            <el-radio-button label="PROCESSING">处理中</el-radio-button>
            <el-radio-button label="RESOLVED">已解决</el-radio-button>
            <el-radio-button label="COMPENSATED">已补偿</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table :data="filteredOrders" style="width: 100%" v-loading="loading">
        <el-table-column prop="abnormalNo" label="异常单编号" width="200" />
        <el-table-column prop="type" label="异常类型">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)">{{ getTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="站点">
          <template #default="{ row }">
            {{ getSiteName(row.siteId) }}
          </template>
        </el-table-column>
        <el-table-column label="充电桩">
          <template #default="{ row }">
            {{ getPileName(row.pileId) }}
          </template>
        </el-table-column>
        <el-table-column prop="occurTime" label="发生时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.occurTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="chargedKwh" label="已充电量(kWh)">
          <template #default="{ row }">
            {{ row.chargedKwh?.toFixed(2) || '0.00' }}
          </template>
        </el-table-column>
        <el-table-column prop="compensatedAmount" label="补偿金额(元)">
          <template #default="{ row }">
            <span v-if="row.compensatedAmount > 0" style="color: #67c23a">
              +¥{{ row.compensatedAmount?.toFixed(2) || '0.00' }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
            <template v-if="row.status === 'PENDING' || row.status === 'PROCESSING'">
              <el-button type="success" link @click="resolveOrder(row, true)">补偿并解决</el-button>
              <el-button type="warning" link @click="resolveOrder(row, false)">仅解决</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="filteredOrders.length === 0 && !loading" description="暂无异常单" />
    </el-card>

    <el-dialog v-model="detailVisible" title="异常单详情" width="70%">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="异常单编号">{{ currentOrder?.abnormalNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(currentOrder?.status)">{{ getStatusText(currentOrder?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="异常类型">
          <el-tag :type="getTypeTagType(currentOrder?.type)">{{ getTypeText(currentOrder?.type) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发生时间">{{ formatDateTime(currentOrder?.occurTime) }}</el-descriptions-item>
        <el-descriptions-item label="解决时间">
          {{ currentOrder?.resolveTime ? formatDateTime(currentOrder.resolveTime) : '未解决' }}
        </el-descriptions-item>
        <el-descriptions-item label="已充电量">
          {{ currentOrder?.chargedKwh?.toFixed(2) || '0.00' }} kWh
        </el-descriptions-item>
        <el-descriptions-item label="补偿金额" :span="2">
          <span v-if="currentOrder?.compensatedAmount > 0" style="color: #67c23a; font-size: 18px; font-weight: 600">
            +¥{{ currentOrder?.compensatedAmount?.toFixed(2) || '0.00' }}
          </span>
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ currentOrder?.description || '无' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentOrder?.remark || '无' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import api from '../api'

const loading = ref(false)
const statusFilter = ref('')
const orders = ref([])
const sites = ref([])
const piles = ref([])
const detailVisible = ref(false)
const currentOrder = ref(null)

const typeMap = {
  PILE_FAULT: '充电桩故障',
  POWER_OUTAGE: '停电',
  NETWORK_ERROR: '网络错误',
  OTHER: '其他'
}

const statusMap = {
  PENDING: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已解决',
  COMPENSATED: '已补偿'
}

const typeTagTypeMap = {
  PILE_FAULT: 'danger',
  POWER_OUTAGE: 'warning',
  NETWORK_ERROR: 'warning',
  OTHER: 'info'
}

const statusTagTypeMap = {
  PENDING: 'danger',
  PROCESSING: 'warning',
  RESOLVED: 'success',
  COMPENSATED: 'success'
}

const filteredOrders = computed(() => {
  if (!statusFilter.value) return orders.value
  return orders.value.filter(o => o.status === statusFilter.value)
})

const formatDateTime = (dt) => {
  if (!dt) return ''
  return dayjs(dt).format('MM-DD HH:mm')
}

const getTypeText = (type) => typeMap[type] || type
const getStatusText = (status) => statusMap[status] || status
const getTypeTagType = (type) => typeTagTypeMap[type] || 'info'
const getStatusTagType = (status) => statusTagTypeMap[status] || 'info'

const getSiteName = (siteId) => {
  const site = sites.value.find(s => s.id === siteId)
  return site?.name || `站点${siteId}`
}

const getPileName = (pileId) => {
  const pile = piles.value.find(p => p.id === pileId)
  return pile?.name || `桩${pileId}`
}

const fetchOrders = async () => {
  loading.value = true
  try {
    const [sitesRes, ordersRes] = await Promise.all([
      api.getSites(),
      api.getAllAbnormalOrders()
    ])
    if (sitesRes.success) {
      sites.value = sitesRes.data
      const pilePromises = sitesRes.data.map(s => api.getSitePiles(s.id))
      const pileResults = await Promise.all(pilePromises)
      piles.value = pileResults.filter(r => r.success).flatMap(r => r.data)
    }
    if (ordersRes.success) orders.value = ordersRes.data
  } catch (e) {
    ElMessage.error('获取异常单失败')
  } finally {
    loading.value = false
  }
}

const viewDetail = (row) => {
  currentOrder.value = row
  detailVisible.value = true
}

const resolveOrder = async (row, compensate) => {
  const action = compensate ? '补偿并解决' : '仅解决'
  try {
    await ElMessageBox.confirm(`确定${action}该异常单？${compensate ? '将给予用户电费50%的补偿。' : ''}`, `确认${action}`, {
      type: 'warning'
    })
    const res = await api.resolveAbnormalOrder(row.id, compensate, `${action}处理`)
    if (res.success) {
      ElMessage.success(`${action}成功`)
      fetchOrders()
    } else {
      ElMessage.error(res.message || '处理失败')
    }
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

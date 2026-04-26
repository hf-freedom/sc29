<template>
  <div>
    <el-card>
      <template #header>
        <span>充电记录</span>
      </template>

      <el-table :data="records" style="width: 100%" v-loading="loading">
        <el-table-column prop="recordNo" label="记录编号" width="200" />
        <el-table-column prop="startTime" label="充电时间">
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
        <el-table-column prop="totalKwh" label="充电量(kWh)">
          <template #default="{ row }">
            <el-tag type="primary">{{ row.totalKwh?.toFixed(2) || '0.00' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="electricityFee" label="电费(元)">
          <template #default="{ row }">
            ¥{{ row.electricityFee?.toFixed(2) || '0.00' }}
          </template>
        </el-table-column>
        <el-table-column prop="serviceFee" label="服务费(元)">
          <template #default="{ row }">
            ¥{{ row.serviceFee?.toFixed(2) || '0.00' }}
          </template>
        </el-table-column>
        <el-table-column prop="totalFee" label="总费用(元)">
          <template #default="{ row }">
            <el-tag type="danger" size="large">¥{{ row.totalFee?.toFixed(2) || '0.00' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="records.length === 0 && !loading" description="暂无充电记录" />
    </el-card>

    <el-dialog v-model="detailVisible" title="充电记录详情" width="80%">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="记录编号">{{ currentRecord?.recordNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentRecord?.status)">{{ getStatusText(currentRecord?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatDateTime(currentRecord?.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ formatDateTime(currentRecord?.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="充电量">
          <el-tag type="primary">{{ currentRecord?.totalKwh?.toFixed(2) || '0.00' }} kWh</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ currentRecord?.remark || '无' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">费用明细</el-divider>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card shadow="hover">
            <div class="fee-item">
              <div class="fee-label">电费</div>
              <div class="fee-value">¥{{ currentRecord?.electricityFee?.toFixed(2) || '0.00' }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover">
            <div class="fee-item">
              <div class="fee-label">服务费</div>
              <div class="fee-value">¥{{ currentRecord?.serviceFee?.toFixed(2) || '0.00' }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #fff">
            <div class="fee-item">
              <div class="fee-label">总费用</div>
              <div class="fee-value">¥{{ currentRecord?.totalFee?.toFixed(2) || '0.00' }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-divider content-position="left" v-if="currentRecord?.segments?.length > 0">分段计费明细</el-divider>
      <el-table :data="currentRecord?.segments" style="width: 100%" v-if="currentRecord?.segments?.length > 0">
        <el-table-column prop="period" label="时段">
          <template #default="{ row }">
            <span :class="['status-badge', `period-${row.period.toLowerCase()}`]">
              {{ getPeriodText(row.period) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="时间">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }} - {{ formatDateTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="kwh" label="电量(kWh)">
          <template #default="{ row }">
            {{ row.kwh?.toFixed(2) || '0.00' }}
          </template>
        </el-table-column>
        <el-table-column prop="pricePerKwh" label="电价(元/度)">
          <template #default="{ row }">
            ¥{{ row.pricePerKwh?.toFixed(2) || '0.00' }}
          </template>
        </el-table-column>
        <el-table-column prop="segmentFee" label="费用(元)">
          <template #default="{ row }">
            <el-tag type="warning">¥{{ row.segmentFee?.toFixed(2) || '0.00' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import api from '../api'

const loading = ref(false)
const records = ref([])
const piles = ref([])
const detailVisible = ref(false)
const currentRecord = ref(null)

const statusMap = {
  CHARGING: '充电中',
  COMPLETED: '已完成',
  ABNORMAL_STOPPED: '异常停止'
}

const statusTypeMap = {
  CHARGING: 'success',
  COMPLETED: 'success',
  ABNORMAL_STOPPED: 'warning'
}

const periodMap = {
  PEAK: '峰时',
  FLAT: '平时',
  VALLEY: '谷时'
}

const formatDateTime = (dt) => {
  if (!dt) return ''
  return dayjs(dt).format('MM-DD HH:mm')
}

const getStatusText = (status) => statusMap[status] || status
const getStatusType = (status) => statusTypeMap[status] || 'info'
const getPeriodText = (period) => periodMap[period] || period

const getPileName = (pileId) => {
  const pile = piles.value.find(p => p.id === pileId)
  return pile?.name || `桩${pileId}`
}

const fetchRecords = async () => {
  loading.value = true
  try {
    const userRes = await api.getUser(1)
    if (userRes.success) {
      const res = await api.getUserChargingRecords(userRes.data.id)
      if (res.success) {
        records.value = res.data
        const siteIds = [...new Set(res.data.map(r => r.siteId))]
        const pilePromises = siteIds.map(siteId => api.getSitePiles(siteId))
        const pileResults = await Promise.all(pilePromises)
        piles.value = pileResults.filter(r => r.success).flatMap(r => r.data)
      }
    }
  } catch (e) {
    ElMessage.error('获取充电记录失败')
  } finally {
    loading.value = false
  }
}

const viewDetail = (row) => {
  currentRecord.value = row
  detailVisible.value = true
}

onMounted(() => {
  fetchRecords()
})
</script>

<style scoped>
.fee-item {
  text-align: center;
}

.fee-label {
  font-size: 14px;
  opacity: 0.8;
}

.fee-value {
  font-size: 24px;
  font-weight: 600;
  margin-top: 8px;
}
</style>

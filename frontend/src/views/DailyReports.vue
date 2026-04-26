<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>日报表</span>
          <el-button type="primary" @click="fetchReports">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <el-table :data="reports" style="width: 100%" v-loading="loading">
        <el-table-column prop="reportDate" label="报表日期" width="120" />
        <el-table-column label="站点">
          <template #default="{ row }">
            {{ getSiteName(row.siteId) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalReservations" label="总预约数" />
        <el-table-column prop="completedReservations" label="完成数">
          <template #default="{ row }">
            <el-tag type="success">{{ row.completedReservations }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cancelledReservations" label="取消数">
          <template #default="{ row }">
            <el-tag type="info">{{ row.cancelledReservations }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalChargingRecords" label="充电记录数" />
        <el-table-column prop="totalKwh" label="总充电量(kWh)">
          <template #default="{ row }">
            {{ row.totalKwh?.toFixed(2) || '0.00' }}
          </template>
        </el-table-column>
        <el-table-column prop="totalElectricityFee" label="电费(元)">
          <template #default="{ row }">
            ¥{{ row.totalElectricityFee?.toFixed(2) || '0.00' }}
          </template>
        </el-table-column>
        <el-table-column prop="totalServiceFee" label="服务费(元)">
          <template #default="{ row }">
            ¥{{ row.totalServiceFee?.toFixed(2) || '0.00' }}
          </template>
        </el-table-column>
        <el-table-column prop="totalRevenue" label="总收入(元)">
          <template #default="{ row }">
            <el-tag type="danger" size="large">¥{{ row.totalRevenue?.toFixed(2) || '0.00' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="abnormalCount" label="异常数">
          <template #default="{ row }">
            <el-tag v-if="row.abnormalCount > 0" type="warning">{{ row.abnormalCount }}</el-tag>
            <span v-else>0</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalCompensation" label="补偿金额(元)">
          <template #default="{ row }">
            <span v-if="row.totalCompensation > 0" style="color: #f56c6c">
              -¥{{ row.totalCompensation?.toFixed(2) || '0.00' }}
            </span>
            <span v-else>¥0.00</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="reports.length === 0 && !loading" description="暂无日报表数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const loading = ref(false)
const reports = ref([])
const sites = ref([])

const getSiteName = (siteId) => {
  const site = sites.value.find(s => s.id === siteId)
  return site?.name || `站点${siteId}`
}

const fetchReports = async () => {
  loading.value = true
  try {
    const [sitesRes, reportsRes] = await Promise.all([
      api.getSites(),
      api.getDailyReports()
    ])
    if (sitesRes.success) sites.value = sitesRes.data
    if (reportsRes.success) reports.value = reportsRes.data
  } catch (e) {
    ElMessage.error('获取报表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchReports()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

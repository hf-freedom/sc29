<template>
  <div>
    <el-card>
      <template #header>
        <span>站点列表</span>
      </template>
      <el-table :data="sites" style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="站点名称" />
        <el-table-column prop="area" label="区域" />
        <el-table-column prop="address" label="地址" show-overflow-tooltip />
        <el-table-column label="营业时间">
          <template #default="{ row }">
            {{ formatTime(row.openingTime) }} - {{ formatTime(row.closingTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="serviceFeePerKwh" label="服务费(元/度)">
          <template #default="{ row }">
            <el-tag type="info">¥{{ row.serviceFeePerKwh }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewPiles(row)">查看充电桩</el-button>
            <el-button type="primary" link @click="viewPriceRules(row)">电价规则</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="pilesDialogVisible" :title="`${selectedSite?.name} - 充电桩列表`" width="90%">
      <el-table :data="sitePiles" style="width: 100%">
        <el-table-column prop="pileNo" label="桩编号" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="maxPower" label="最大功率(kW)" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <span :class="['status-badge', `status-${row.status.toLowerCase()}`]">
              {{ getStatusText(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              link 
              :disabled="row.status !== 'IDLE'"
              @click="goToReservation(selectedSite, row)"
            >
              预约
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="priceRulesDialogVisible" :title="`${selectedSite?.name} - 电价规则`" width="80%">
      <el-table :data="sitePriceRules" style="width: 100%">
        <el-table-column prop="ruleName" label="规则名称" />
        <el-table-column prop="period" label="时段">
          <template #default="{ row }">
            <span :class="['status-badge', `period-${row.period.toLowerCase()}`]">
              {{ getPeriodText(row.period) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="适用日期">
          <template #default="{ row }">
            {{ formatApplicableDays(row.applicableDays) }}
          </template>
        </el-table-column>
        <el-table-column label="时间段">
          <template #default="{ row }">
            {{ formatTime(row.startTime) }} - {{ formatTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="pricePerKwh" label="电价(元/度)">
          <template #default="{ row }">
            <el-tag type="warning">¥{{ row.pricePerKwh }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()

const loading = ref(false)
const sites = ref([])
const selectedSite = ref(null)
const sitePiles = ref([])
const sitePriceRules = ref([])
const pilesDialogVisible = ref(false)
const priceRulesDialogVisible = ref(false)

const statusMap = {
  IDLE: '空闲',
  RESERVED: '预约中',
  CHARGING: '充电中',
  FAULT: '故障',
  OFFLINE: '离线'
}

const periodMap = {
  PEAK: '峰时',
  FLAT: '平时',
  VALLEY: '谷时'
}

const dayMap = {
  MONDAY: '周一',
  TUESDAY: '周二',
  WEDNESDAY: '周三',
  THURSDAY: '周四',
  FRIDAY: '周五',
  SATURDAY: '周六',
  SUNDAY: '周日'
}

const formatTime = (time) => {
  if (!time) return ''
  return time.substring(0, 5)
}

const getStatusText = (status) => statusMap[status] || status
const getPeriodText = (period) => periodMap[period] || period

const formatApplicableDays = (days) => {
  if (!days || days.length === 0) return ''
  return days.map(d => dayMap[d]).join('、')
}

const fetchSites = async () => {
  loading.value = true
  try {
    const res = await api.getSites()
    if (res.success) {
      sites.value = res.data
    } else {
      ElMessage.error(res.message || '获取站点列表失败')
    }
  } catch (e) {
    ElMessage.error('获取站点列表失败')
  } finally {
    loading.value = false
  }
}

const viewPiles = async (site) => {
  selectedSite.value = site
  try {
    const res = await api.getSitePiles(site.id)
    if (res.success) {
      sitePiles.value = res.data
      pilesDialogVisible.value = true
    }
  } catch (e) {
    ElMessage.error('获取充电桩列表失败')
  }
}

const viewPriceRules = async (site) => {
  selectedSite.value = site
  try {
    const res = await api.getPriceRules(site.id)
    if (res.success) {
      sitePriceRules.value = res.data
      priceRulesDialogVisible.value = true
    }
  } catch (e) {
    ElMessage.error('获取电价规则失败')
  }
}

const goToReservation = (site, pile) => {
  pilesDialogVisible.value = false
  router.push({
    path: '/reservation',
    query: { siteId: site.id, pileId: pile.id }
  })
}

onMounted(() => {
  fetchSites()
})
</script>

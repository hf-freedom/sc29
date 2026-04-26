<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
              <el-icon :size="30"><OfficeBuilding /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ siteCount }}</div>
              <div class="stat-label">站点数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
              <el-icon :size="30"><ChargingPile /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ pileCount }}</div>
              <div class="stat-label">充电桩数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
              <el-icon :size="30"><Lightning /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ chargingCount }}</div>
              <div class="stat-label">充电中</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
              <el-icon :size="30"><Wallet /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">¥{{ balance }}</div>
              <div class="stat-label">账户余额</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>站点列表</span>
              <el-button type="primary" link @click="$router.push('/sites')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="sites" style="width: 100%">
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
                ¥{{ row.serviceFeePerKwh }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>快速操作</span>
            </div>
          </template>
          <div class="quick-actions">
            <el-button type="primary" size="large" @click="$router.push('/reservation')" style="width: 100%; margin-bottom: 12px">
              <el-icon><Calendar /></el-icon>
              预约充电桩
            </el-button>
            <el-button type="success" size="large" @click="$router.push('/my-reservations')" style="width: 100%; margin-bottom: 12px">
              <el-icon><Document /></el-icon>
              我的预约
            </el-button>
            <el-button type="warning" size="large" @click="$router.push('/charging')" style="width: 100%">
              <el-icon><Lightning /></el-icon>
              充电管理
            </el-button>
          </div>
        </el-card>

        <el-card style="margin-top: 20px">
          <template #header>
            <span>充电桩状态统计</span>
          </template>
          <div class="pile-stats">
            <div class="pile-stat-item">
              <span class="status-badge status-idle">空闲</span>
              <span class="stat-number">{{ statusCount.IDLE || 0 }}</span>
            </div>
            <div class="pile-stat-item">
              <span class="status-badge status-reserved">预约中</span>
              <span class="stat-number">{{ statusCount.RESERVED || 0 }}</span>
            </div>
            <div class="pile-stat-item">
              <span class="status-badge status-charging">充电中</span>
              <span class="stat-number">{{ statusCount.CHARGING || 0 }}</span>
            </div>
            <div class="pile-stat-item">
              <span class="status-badge status-fault">故障</span>
              <span class="stat-number">{{ statusCount.FAULT || 0 }}</span>
            </div>
            <div class="pile-stat-item">
              <span class="status-badge status-offline">离线</span>
              <span class="stat-number">{{ statusCount.OFFLINE || 0 }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import api from '../api'

const sites = ref([])
const piles = ref([])
const user = ref(null)

const siteCount = computed(() => sites.value.length)
const pileCount = computed(() => piles.value.length)
const chargingCount = computed(() => piles.value.filter(p => p.status === 'CHARGING').length)
const balance = computed(() => user.value?.balance?.toFixed(2) || '0.00')

const statusCount = computed(() => {
  const counts = {}
  piles.value.forEach(p => {
    counts[p.status] = (counts[p.status] || 0) + 1
  })
  return counts
})

const formatTime = (time) => {
  if (!time) return ''
  return time.substring(0, 5)
}

const fetchData = async () => {
  try {
    const [sitesRes, userRes] = await Promise.all([
      api.getSites(),
      api.getUser(1)
    ])
    if (sitesRes.success) {
      sites.value = sitesRes.data
      const pilePromises = sitesRes.data.map(s => api.getSitePiles(s.id))
      const pileResults = await Promise.all(pilePromises)
      piles.value = pileResults.filter(r => r.success).flatMap(r => r.data)
    }
    if (userRes.success) {
      user.value = userRes.data
    }
  } catch (e) {
    console.error('获取数据失败', e)
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.stat-card {
  margin-bottom: 20px;
}

.stat-content {
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-info {
  margin-left: 16px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.quick-actions {
  display: flex;
  flex-direction: column;
}

.pile-stats {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pile-stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-number {
  font-size: 18px;
  font-weight: 600;
}
</style>

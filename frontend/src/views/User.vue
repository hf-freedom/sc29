<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>个人信息</span>
          </template>
          <div class="user-info">
            <el-avatar :size="80" :icon="UserFilled" />
            <h3 style="margin-top: 16px">{{ user?.username }}</h3>
            <p style="color: #909399">{{ user?.phone }}</p>
          </div>
          <el-divider />
          <div class="account-info">
            <div class="account-item">
              <span class="account-label">账户余额</span>
              <span class="account-value balance">¥{{ user?.balance?.toFixed(2) || '0.00' }}</span>
            </div>
            <div class="account-item">
              <span class="account-label">冻结金额</span>
              <span class="account-value frozen">¥{{ user?.frozenAmount?.toFixed(2) || '0.00' }}</span>
            </div>
            <div class="account-item">
              <span class="account-label">信用状态</span>
              <el-tag :type="getCreditType(user?.creditStatus)">{{ getCreditText(user?.creditStatus) }}</el-tag>
            </div>
            <div class="account-item">
              <span class="account-label">逾期次数</span>
              <span class="account-value" :class="{ overdue: user?.overdueCount > 0 }">
                {{ user?.overdueCount || 0 }} 次
              </span>
            </div>
          </div>
          <el-button type="primary" style="width: 100%; margin-top: 16px" @click="showRecharge = true">
            充值
          </el-button>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>我的车辆</span>
              <el-button type="primary" link @click="showAddVehicle = true">+ 添加车辆</el-button>
            </div>
          </template>

          <el-table :data="vehicles" style="width: 100%" v-loading="loading">
            <el-table-column prop="plateNumber" label="车牌号" width="150">
              <template #default="{ row }">
                <el-tag type="primary" size="large">{{ row.plateNumber }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="brand" label="品牌" />
            <el-table-column prop="model" label="型号" />
            <el-table-column prop="batteryCapacity" label="电池容量(kWh)">
              <template #default="{ row }">
                {{ row.batteryCapacity }} kWh
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="danger" link @click="deleteVehicle(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="vehicles.length === 0 && !loading" description="暂无车辆信息">
            <el-button type="primary" @click="showAddVehicle = true">添加车辆</el-button>
          </el-empty>
        </el-card>

        <el-card style="margin-top: 20px">
          <template #header>
            <span>信用等级说明</span>
          </template>
          <el-table :data="creditLevels" style="width: 100%">
            <el-table-column prop="level" label="等级">
              <template #default="{ row }">
                <el-tag :type="row.type">{{ row.level }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="overdueCount" label="逾期次数" />
            <el-table-column prop="description" label="说明" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="showRecharge" title="账户充值" width="400px">
      <el-form label-width="80px">
        <el-form-item label="当前余额">
          <el-tag type="success" size="large">¥{{ user?.balance?.toFixed(2) || '0.00' }}</el-tag>
        </el-form-item>
        <el-form-item label="充值金额">
          <el-radio-group v-model="rechargeAmount">
            <el-radio :value="50">50元</el-radio>
            <el-radio :value="100">100元</el-radio>
            <el-radio :value="200">200元</el-radio>
            <el-radio :value="500">500元</el-radio>
            <el-radio :value="1000">1000元</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRecharge = false">取消</el-button>
        <el-button type="primary" @click="handleRecharge" :loading="recharging">确认充值</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showAddVehicle" title="添加车辆" width="500px">
      <el-form :model="newVehicle" label-width="100px">
        <el-form-item label="车牌号">
          <el-input v-model="newVehicle.plateNumber" placeholder="请输入车牌号" />
        </el-form-item>
        <el-form-item label="品牌">
          <el-input v-model="newVehicle.brand" placeholder="请输入品牌" />
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="newVehicle.model" placeholder="请输入型号" />
        </el-form-item>
        <el-form-item label="电池容量(kWh)">
          <el-input-number v-model="newVehicle.batteryCapacity" :min="0" :precision="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddVehicle = false">取消</el-button>
        <el-button type="primary" @click="handleAddVehicle" :loading="addingVehicle">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import api from '../api'

const loading = ref(false)
const user = ref(null)
const vehicles = ref([])
const showRecharge = ref(false)
const showAddVehicle = ref(false)
const recharging = ref(false)
const addingVehicle = ref(false)
const rechargeAmount = ref(100)

const newVehicle = reactive({
  plateNumber: '',
  brand: '',
  model: '',
  batteryCapacity: 50
})

const creditLevels = [
  { level: '优秀', type: 'success', overdueCount: '0次', description: '可正常使用所有功能' },
  { level: '良好', type: 'primary', overdueCount: '1次', description: '可正常使用，累计2次将降级' },
  { level: '一般', type: 'warning', overdueCount: '2次', description: '可正常使用，累计3次将降级' },
  { level: '较差', type: 'danger', overdueCount: '3-4次', description: '部分功能受限' },
  { level: '黑名单', type: 'info', overdueCount: '5次及以上', description: '无法预约充电' }
]

const creditMap = {
  EXCELLENT: '优秀',
  GOOD: '良好',
  NORMAL: '一般',
  POOR: '较差',
  BLACKLIST: '黑名单'
}

const creditTypeMap = {
  EXCELLENT: 'success',
  GOOD: 'primary',
  NORMAL: 'warning',
  POOR: 'danger',
  BLACKLIST: 'info'
}

const getCreditText = (status) => creditMap[status] || status
const getCreditType = (status) => creditTypeMap[status] || 'info'

const fetchUserData = async () => {
  loading.value = true
  try {
    const userRes = await api.getUser(1)
    if (userRes.success) {
      user.value = userRes.data
      const vehiclesRes = await api.getUserVehicles(userRes.data.id)
      if (vehiclesRes.success) {
        vehicles.value = vehiclesRes.data
      }
    }
  } catch (e) {
    ElMessage.error('获取用户信息失败')
  } finally {
    loading.value = false
  }
}

const handleRecharge = async () => {
  if (!rechargeAmount.value) {
    ElMessage.warning('请选择充值金额')
    return
  }
  recharging.value = true
  try {
    const res = await api.recharge(user.value.id, rechargeAmount.value)
    if (res.success) {
      ElMessage.success('充值成功')
      user.value = res.data
      showRecharge.value = false
    } else {
      ElMessage.error(res.message || '充值失败')
    }
  } catch (e) {
    ElMessage.error('充值失败')
  } finally {
    recharging.value = false
  }
}

const handleAddVehicle = async () => {
  if (!newVehicle.plateNumber) {
    ElMessage.warning('请输入车牌号')
    return
  }
  addingVehicle.value = true
  try {
    const res = await api.addVehicle(user.value.id, newVehicle)
    if (res.success) {
      ElMessage.success('添加车辆成功')
      vehicles.value.push(res.data)
      showAddVehicle.value = false
      Object.assign(newVehicle, { plateNumber: '', brand: '', model: '', batteryCapacity: 50 })
    } else {
      ElMessage.error(res.message || '添加失败')
    }
  } catch (e) {
    ElMessage.error('添加车辆失败')
  } finally {
    addingVehicle.value = false
  }
}

const deleteVehicle = async (vehicle) => {
  try {
    await ElMessageBox.confirm(`确定删除车辆 ${vehicle.plateNumber}？`, '确认删除', {
      type: 'warning'
    })
    const res = await api.deleteVehicle(user.value.id, vehicle.id)
    if (res.success) {
      ElMessage.success('删除成功')
      vehicles.value = vehicles.value.filter(v => v.id !== vehicle.id)
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  fetchUserData()
})
</script>

<style scoped>
.user-info {
  text-align: center;
  padding: 20px 0;
}

.account-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.account-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.account-label {
  color: #909399;
}

.account-value {
  font-weight: 600;
  font-size: 16px;
}

.account-value.balance {
  color: #67c23a;
}

.account-value.frozen {
  color: #e6a23c;
}

.account-value.overdue {
  color: #f56c6c;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

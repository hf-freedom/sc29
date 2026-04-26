<template>
  <div>
    <el-card>
      <template #header>
        <span>预约充电桩</span>
      </template>
      
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="选择站点" prop="siteId">
              <el-select v-model="form.siteId" placeholder="请选择站点" style="width: 100%" @change="handleSiteChange">
                <el-option v-for="site in sites" :key="site.id" :label="site.name" :value="site.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="选择充电桩" prop="pileId">
              <el-select v-model="form.pileId" placeholder="请选择充电桩" style="width: 100%" :disabled="!form.siteId">
                <el-option 
                  v-for="pile in availablePiles" 
                  :key="pile.id" 
                  :label="`${pile.name} (${getStatusText(pile.status)})`" 
                  :value="pile.id"
                  :disabled="pile.status !== 'IDLE'"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="选择车辆" prop="vehicleId">
              <el-select v-model="form.vehicleId" placeholder="请选择车辆" style="width: 100%">
                <el-option v-for="v in vehicles" :key="v.id" :label="`${v.plateNumber} - ${v.brand} ${v.model}`" :value="v.id" />
              </el-select>
              <el-button type="primary" link @click="showAddVehicle = true">+ 添加车辆</el-button>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="预约开始时间" prop="startTime">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                placeholder="选择开始时间"
                style="width: 100%"
                :disabled-date="disabledDate"
                :disabled-time="disabledStartTime"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预约结束时间" prop="endTime">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                placeholder="选择结束时间"
                style="width: 100%"
                :disabled-date="disabledEndDate"
                :disabled-time="disabledEndTime"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注信息（选填）" />
        </el-form-item>

        <el-form-item>
          <el-alert title="预约须知" type="info" :closable="false" show-icon style="margin-bottom: 20px">
            <template #default>
              <ul style="margin: 0; padding-left: 20px">
                <li>预约需冻结 ¥50 保证金</li>
                <li>预约开始前30分钟内取消，扣除50%保证金</li>
                <li>超时15分钟未签到，预约自动取消，扣除30%保证金</li>
                <li>预约时长最短30分钟，最长8小时</li>
              </ul>
            </template>
          </el-alert>
          <el-button type="primary" @click="submitReservation" :loading="submitting">
            确认预约
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="form.siteId" style="margin-top: 20px">
      <template #header>
        <span>站点电价规则</span>
      </template>
      <el-table :data="priceRules" style="width: 100%">
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
    </el-card>

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
        <el-button type="primary" @click="addVehicle" :loading="addingVehicle">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import api from '../api'

const route = useRoute()
const router = useRouter()

const formRef = ref(null)
const submitting = ref(false)
const addingVehicle = ref(false)
const showAddVehicle = ref(false)

const sites = ref([])
const piles = ref([])
const vehicles = ref([])
const priceRules = ref([])
const currentUser = ref(null)

const availablePiles = computed(() => {
  return piles.value.filter(p => p.status === 'IDLE')
})

const newVehicle = reactive({
  plateNumber: '',
  brand: '',
  model: '',
  batteryCapacity: 50
})

const form = reactive({
  siteId: null,
  pileId: null,
  vehicleId: null,
  startTime: null,
  endTime: null,
  remark: ''
})

const rules = {
  siteId: [{ required: true, message: '请选择站点', trigger: 'change' }],
  pileId: [{ required: true, message: '请选择充电桩', trigger: 'change' }],
  vehicleId: [{ required: true, message: '请选择车辆', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

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

const disabledDate = (time) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return time.getTime() < today.getTime()
}

const disabledEndDate = (time) => {
  if (form.startTime) {
    const startDate = new Date(form.startTime)
    startDate.setHours(0, 0, 0, 0)
    return time.getTime() < startDate.getTime()
  }
  return disabledDate(time)
}

const disabledStartTime = () => {
  const now = new Date()
  const hours = now.getHours()
  const minutes = now.getMinutes()
  const seconds = now.getSeconds()
  
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  
  if (form.startTime) {
    const selectedDate = new Date(form.startTime)
    selectedDate.setHours(0, 0, 0, 0)
    
    if (selectedDate.getTime() === today.getTime()) {
      return {
        disabledHours: () => Array.from({ length: hours }, (_, i) => i),
        disabledMinutes: (h) => {
          if (h === hours) {
            return Array.from({ length: minutes }, (_, i) => i)
          }
          return []
        },
        disabledSeconds: (h, m) => {
          if (h === hours && m === minutes) {
            return Array.from({ length: seconds }, (_, i) => i)
          }
          return []
        }
      }
    }
  }
  
  return {
    disabledHours: () => [],
    disabledMinutes: () => [],
    disabledSeconds: () => []
  }
}

const disabledEndTime = () => {
  if (form.startTime && form.endTime) {
    const startDate = new Date(form.startTime)
    const endDate = new Date(form.endTime)
    
    startDate.setHours(0, 0, 0, 0)
    endDate.setHours(0, 0, 0, 0)
    
    if (startDate.getTime() === endDate.getTime()) {
      const start = new Date(form.startTime)
      const hours = start.getHours()
      const minutes = start.getMinutes()
      const seconds = start.getSeconds()
      
      return {
        disabledHours: () => Array.from({ length: hours }, (_, i) => i),
        disabledMinutes: (h) => {
          if (h === hours) {
            return Array.from({ length: minutes }, (_, i) => i)
          }
          return []
        },
        disabledSeconds: (h, m) => {
          if (h === hours && m === minutes) {
            return Array.from({ length: seconds }, (_, i) => i)
          }
          return []
        }
      }
    }
  }
  
  return {
    disabledHours: () => [],
    disabledMinutes: () => [],
    disabledSeconds: () => []
  }
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

const fetchInitialData = async () => {
  try {
    const [sitesRes, userRes] = await Promise.all([
      api.getSites(),
      api.getUser(1)
    ])
    if (sitesRes.success) sites.value = sitesRes.data
    if (userRes.success) {
      currentUser.value = userRes.data
      const vehiclesRes = await api.getUserVehicles(userRes.data.id)
      if (vehiclesRes.success) vehicles.value = vehiclesRes.data
    }
  } catch (e) {
    console.error('获取数据失败', e)
  }
}

const handleSiteChange = async (siteId) => {
  form.pileId = null
  try {
    const [pilesRes, priceRes] = await Promise.all([
      api.getSitePiles(siteId),
      api.getPriceRules(siteId)
    ])
    if (pilesRes.success) piles.value = pilesRes.data
    if (priceRes.success) priceRules.value = priceRes.data
  } catch (e) {
    ElMessage.error('获取站点数据失败')
  }
}

const addVehicle = async () => {
  if (!newVehicle.plateNumber) {
    ElMessage.warning('请输入车牌号')
    return
  }
  addingVehicle.value = true
  try {
    const res = await api.addVehicle(currentUser.value.id, newVehicle)
    if (res.success) {
      ElMessage.success('添加车辆成功')
      vehicles.value.push(res.data)
      form.vehicleId = res.data.id
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

const submitReservation = async () => {
  await formRef.value.validate()
  
  if (form.startTime >= form.endTime) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }

  const minutes = dayjs(form.endTime).diff(dayjs(form.startTime), 'minute')
  if (minutes < 30) {
    ElMessage.warning('预约时长至少为30分钟')
    return
  }
  if (minutes > 480) {
    ElMessage.warning('预约时长最多为8小时')
    return
  }

  try {
    const availableRes = await api.checkPileAvailability(
      form.pileId,
      dayjs(form.startTime).format('YYYY-MM-DDTHH:mm:ss'),
      dayjs(form.endTime).format('YYYY-MM-DDTHH:mm:ss')
    )
    if (!availableRes.success || !availableRes.data) {
      ElMessage.warning('该时间段已被预约，请选择其他时间')
      return
    }
  } catch (e) {
    console.error('检查可用性失败', e)
  }

  try {
    await ElMessageBox.confirm(
      `确认预约该充电桩？\n预约将冻结 ¥50 保证金`,
      '确认预约',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }

  submitting.value = true
  try {
    const res = await api.createReservation({
      userId: currentUser.value.id,
      vehicleId: form.vehicleId,
      pileId: form.pileId,
      startTime: dayjs(form.startTime).format('YYYY-MM-DDTHH:mm:ss'),
      endTime: dayjs(form.endTime).format('YYYY-MM-DDTHH:mm:ss'),
      remark: form.remark
    })
    if (res.success) {
      ElMessage.success('预约成功！')
      router.push('/my-reservations')
    } else {
      ElMessage.error(res.message || '预约失败')
    }
  } catch (e) {
    ElMessage.error('预约失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchInitialData()
  
  if (route.query.siteId) {
    form.siteId = Number(route.query.siteId)
    handleSiteChange(form.siteId)
  }
  if (route.query.pileId) {
    form.pileId = Number(route.query.pileId)
  }
})
</script>

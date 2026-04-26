<template>
  <el-config-provider :locale="zhCn">
    <el-container style="height: 100vh">
      <el-aside width="220px" style="background-color: #001529">
        <div class="logo">
          <el-icon :size="30" color="#409EFF"><ChargingPile /></el-icon>
          <span class="logo-text">充电桩预约系统</span>
        </div>
        <el-menu
          background-color="#001529"
          text-color="#ffffffa6"
          active-text-color="#409EFF"
          router
          :default-active="$route.path"
        >
          <el-menu-item index="/">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/sites">
            <el-icon><OfficeBuilding /></el-icon>
            <span>站点管理</span>
          </el-menu-item>
          <el-menu-item index="/reservation">
            <el-icon><Calendar /></el-icon>
            <span>预约充电桩</span>
          </el-menu-item>
          <el-menu-item index="/my-reservations">
            <el-icon><Document /></el-icon>
            <span>我的预约</span>
          </el-menu-item>
          <el-menu-item index="/charging">
            <el-icon><Lightning /></el-icon>
            <span>充电中</span>
          </el-menu-item>
          <el-menu-item index="/records">
            <el-icon><List /></el-icon>
            <span>充电记录</span>
          </el-menu-item>
          <el-menu-item index="/user">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </el-menu-item>
          <el-sub-menu index="/reports">
            <template #title>
              <el-icon><DataAnalysis /></el-icon>
              <span>报表管理</span>
            </template>
            <el-menu-item index="/reports/daily">日报表</el-menu-item>
            <el-menu-item index="/reports/abnormal">异常单</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header style="background-color: #fff; border-bottom: 1px solid #e6e6e6">
          <div class="header-content">
            <span style="font-size: 16px; font-weight: 500">{{ currentPageTitle }}</span>
            <div class="user-info">
              <el-badge :value="abnormalCount" :hidden="abnormalCount === 0" class="item">
                <el-button type="text" @click="$router.push('/reports/abnormal')">
                  <el-icon :size="20"><Bell /></el-icon>
                </el-button>
              </el-badge>
              <el-dropdown @command="handleCommand">
                <span class="el-dropdown-link">
                  <el-avatar :size="32" :icon="UserFilled" />
                  <span style="margin-left: 8px">{{ currentUser?.username || '用户' }}</span>
                  <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="user">个人中心</el-dropdown-item>
                    <el-dropdown-item command="recharge">充值</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </el-header>
        <el-main style="background-color: #f0f2f5; overflow-y: auto">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
    <el-dialog v-model="rechargeDialogVisible" title="账户充值" width="400px">
      <el-form :model="rechargeForm" label-width="80px">
        <el-form-item label="当前余额">
          <el-tag type="success" size="large">
            ¥{{ currentUser?.balance?.toFixed(2) || '0.00' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="充值金额">
          <el-select v-model="rechargeForm.amount" placeholder="请选择充值金额" style="width: 100%">
            <el-option :value="50" label="50元" />
            <el-option :value="100" label="100元" />
            <el-option :value="200" label="200元" />
            <el-option :value="500" label="500元" />
            <el-option :value="1000" label="1000元" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRecharge" :loading="rechargeLoading">确认充值</el-button>
      </template>
    </el-dialog>
  </el-config-provider>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import api from './api'

const router = useRouter()
const route = useRoute()

const currentUser = ref(null)
const rechargeDialogVisible = ref(false)
const rechargeForm = ref({ amount: 100 })
const rechargeLoading = ref(false)
const abnormalCount = ref(0)

const pageTitles = {
  '/': '首页',
  '/sites': '站点管理',
  '/reservation': '预约充电桩',
  '/my-reservations': '我的预约',
  '/charging': '充电中',
  '/records': '充电记录',
  '/user': '个人中心',
  '/reports/daily': '日报表',
  '/reports/abnormal': '异常单管理'
}

const currentPageTitle = computed(() => pageTitles[route.path] || '充电桩预约系统')

const fetchUser = async () => {
  try {
    const res = await api.getUser(1)
    if (res.success) {
      currentUser.value = res.data
    }
  } catch (e) {
    console.error('获取用户信息失败', e)
  }
}

const fetchAbnormalCount = async () => {
  try {
    const res = await api.getPendingAbnormalOrders()
    if (res.success) {
      abnormalCount.value = res.data?.length || 0
    }
  } catch (e) {
    console.error('获取异常单数量失败', e)
  }
}

onMounted(() => {
  fetchUser()
  fetchAbnormalCount()
})

watch(route.path, () => {
  if (route.path === '/reports/abnormal') {
    fetchAbnormalCount()
  }
})

const handleCommand = (command) => {
  if (command === 'user') {
    router.push('/user')
  } else if (command === 'recharge') {
    rechargeDialogVisible.value = true
  }
}

const handleRecharge = async () => {
  if (!rechargeForm.value.amount) {
    ElMessage.warning('请选择充值金额')
    return
  }
  rechargeLoading.value = true
  try {
    const res = await api.recharge(currentUser.value.id, rechargeForm.value.amount)
    if (res.success) {
      ElMessage.success('充值成功')
      currentUser.value = res.data
      rechargeDialogVisible.value = false
    } else {
      ElMessage.error(res.message || '充值失败')
    }
  } catch (e) {
    ElMessage.error('充值失败')
  } finally {
    rechargeLoading.value = false
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #1f3043;
}

.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  margin-left: 10px;
}

.header-content {
  height: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.el-dropdown-link {
  cursor: pointer;
  display: flex;
  align-items: center;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.page-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.status-idle { background: #e6f7ff; color: #1890ff; }
.status-reserved { background: #fff7e6; color: #fa8c16; }
.status-charging { background: #f6ffed; color: #52c41a; }
.status-fault { background: #fff2f0; color: #ff4d4f; }
.status-offline { background: #f0f0f0; color: #8c8c8c; }

.period-peak { background: #fff1f0; color: #cf1322; }
.period-flat { background: #e6f7ff; color: #1890ff; }
.period-valley { background: #f6ffed; color: #389e0d; }
</style>

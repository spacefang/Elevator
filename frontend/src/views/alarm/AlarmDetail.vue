<template>
  <div v-loading="loading">
    <!-- 头部信息 -->
    <div class="bg-white p-6 rounded-lg shadow-sm mb-4 flex justify-between items-start">
      <div>
        <div class="flex items-center space-x-4 mb-2">
          <h1 class="text-2xl font-bold text-gray-900">{{ alarmData.title || '加载中...' }}</h1>
          <el-tag :type="getLevelTagType(alarmData.level)" effect="dark" size="large">
            {{ getLevelText(alarmData.level) }}
          </el-tag>
          <el-tag :type="getStatusTagType(alarmData.status)" effect="plain" size="large">
            {{ getStatusText(alarmData.status) }}
          </el-tag>
        </div>
        <p class="text-gray-500 text-sm flex items-center">
          <el-icon class="mr-1"><Clock /></el-icon>
          触发时间: {{ alarmData.triggerTime }}
          <span class="mx-4 text-gray-300">|</span>
          <el-icon class="mr-1"><Location /></el-icon>
          {{ alarmData.location }} ({{ alarmData.deviceNo }})
        </p>
      </div>
      <div class="text-right">
        <el-button @click="router.back()">返回列表</el-button>
      </div>
    </div>

    <!-- 主体布局 -->
    <el-row :gutter="20">
      <!-- 左侧信息区 60% -->
      <el-col :span="14">
        <!-- 告警详情 -->
        <el-card class="mb-4 shadow-sm !border-none" header="告警详情">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="告警类型">{{ alarmData.type }}</el-descriptions-item>
            <el-descriptions-item label="持续时长">{{ alarmData.duration || '-' }}</el-descriptions-item>
            <el-descriptions-item label="触发值/阈值" :span="2">
              <span class="text-red-500 font-mono">{{ alarmData.value || 'N/A' }}</span> / 
              <span class="text-gray-500 font-mono">{{ alarmData.threshold || 'N/A' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="告警描述" :span="2">
              {{ alarmData.content }}
            </el-descriptions-item>
            <el-descriptions-item label="可能与原因" :span="2">
              {{ alarmData.possibleCause || '系统自动分析中...' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 设备档案 -->
        <el-card class="mb-4 shadow-sm !border-none" header="关联设备档案">
          <el-descriptions :column="2">
             <el-descriptions-item label="品牌型号">
              <el-tag size="small">奥的斯</el-tag> Gen2-Regen
             </el-descriptions-item>
             <el-descriptions-item label="维保负责人">
              张三 (13800138000)
             </el-descriptions-item>
             <el-descriptions-item label="上次维保">2025-12-15</el-descriptions-item>
             <el-descriptions-item label="设备状态"><span class="text-green-500">运行中</span></el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 实时监控 (占位) -->
        <el-card class="shadow-sm !border-none" header="实时监控">
          <div class="bg-gray-900 rounded-lg h-64 flex items-center justify-center text-gray-500 flex-col">
            <el-icon class="text-4xl mb-2"><VideoCamera /></el-icon>
            <p>实时视频信号连接中...</p>
            <p class="text-xs mt-2">Device ID: {{ alarmData.deviceNo }}</p>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧处理区 40% -->
      <el-col :span="10">
        <!-- 操作面板 -->
        <el-card class="mb-4 shadow-sm !border-none" header="快捷操作">
          <div class="flex space-x-4 mb-4" v-if="alarmData.status !== 'CLOSED'">
            <el-button v-permission="['alarm:handle']" type="primary" size="large" class="flex-1" @click="handleAction('process')">
              立即接单
            </el-button>
            <el-button v-permission="['alarm:handle']" type="warning" size="large" class="flex-1" @click="handleAction('transfer')">
              转工单
            </el-button>
            <el-button v-permission="['alarm:supervise']" type="danger" size="large" class="flex-1" @click="handleAction('supervise')">
              督办
            </el-button>
          </div>
          <div class="flex space-x-4 mb-4" v-else>
             <el-alert title="该告警已关闭" type="success" :closable="false" show-icon class="w-full"/>
          </div>
          
          <el-input
            v-if="alarmData.status !== 'CLOSED'"
            v-model="remark"
            type="textarea"
            :rows="3"
            placeholder="请输入处理备注/跟进情况..."
          />
          <div class="mt-4 text-right" v-if="alarmData.status !== 'CLOSED'">
             <el-button v-permission="['alarm:handle']" type="success" @click="handleAction('complete')">完成处理</el-button>
          </div>
        </el-card>

        <!-- 处理记录 -->
        <el-card class="shadow-sm !border-none" header="处理记录">
          <el-timeline>
            <el-timeline-item
              v-for="(activity, index) in activities"
              :key="index"
              :type="activity.type"
              :color="activity.color"
              :timestamp="activity.timestamp"
              placement="top"
            >
              <h4 class="font-medium">{{ activity.title }}</h4>
              <p class="text-gray-500 text-sm mt-1">{{ activity.content }}</p>
              <p class="text-gray-400 text-xs mt-1" v-if="activity.operator">操作人: {{ activity.operator }}</p>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Clock, Location, VideoCamera } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { closeAlarm, getAlarmDetail, processAlarm, superviseAlarm, transferAlarm } from '../../api/alarm'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const remark = ref('')

const alarmData = ref<any>({})
const activities = ref<any[]>([])
const authStore = useAuthStore()

const toTimelineItem = (action: any) => {
  const actionType = action.actionType
  const map: any = {
    PROCESS: { title: '接单响应', type: 'primary' },
    TRANSFER: { title: '转工单', type: 'warning' },
    CLOSE: { title: '关闭告警', type: 'success' },
    SUPERVISE: { title: '督办', type: 'danger' }
  }
  const meta = map[actionType] || { title: actionType || '操作', type: 'info' }
  return {
    title: meta.title,
    type: meta.type,
    content: action.note || '-',
    timestamp: action.createdAt ? new Date(action.createdAt).toLocaleString() : '',
    operator: action.operatorName || ''
  }
}

const applyDetail = (res: any) => {
  alarmData.value = {
    id: res.id,
    title: `${res.type || '告警'} - ${res.deviceId}`,
    level: res.level || 'RED',
    status: res.status || 'PENDING',
    type: res.type,
    deviceNo: res.deviceId,
    location: res.location || '未知位置',
    triggerTime: res.occurredAt,
    content: res.description,
    duration: '-',
    value: '-',
    threshold: '-',
    possibleCause: '-'
  }

  activities.value = (res.actions || []).map(toTimelineItem)
}

const loadData = async () => {
  loading.value = true
  const id = route.params.id
  
  try {
    const res: any = await getAlarmDetail(id as string)
    applyDetail(res)
    
  } catch (error) {
    console.error(error)
    ElMessage.error('获取详情失败')
  } finally {
    loading.value = false
  }
}

const handleAction = async (type: string) => {
  const id = route.params.id as string
  try {
    if (type === 'process') {
      if (!authStore.hasPermission('alarm:handle')) {
        ElMessage.warning('无权限操作')
        return
      }
      const res: any = await processAlarm(id)
      applyDetail(res)
      ElMessage.success('接单成功')
      return
    }

    if (type === 'complete') {
      if (!authStore.hasPermission('alarm:handle')) {
        ElMessage.warning('无权限操作')
        return
      }
      if (!remark.value) {
        ElMessage.warning('请填写处理备注')
        return
      }
      const res: any = await closeAlarm(id, { note: remark.value })
      applyDetail(res)
      remark.value = ''
      ElMessage.success('处理已完成')
      return
    }

    if (type === 'transfer') {
      if (!authStore.hasPermission('alarm:handle')) {
        ElMessage.warning('无权限操作')
        return
      }
      const res: any = await transferAlarm(id, remark.value ? { note: remark.value } : undefined)
      applyDetail(res)
      ElMessage.success('已记录转工单动作（demo）')
      return
    }

    if (type === 'supervise') {
      if (!authStore.hasPermission('alarm:supervise')) {
        ElMessage.warning('无权限操作')
        return
      }
      const res: any = await superviseAlarm(id, remark.value ? { note: remark.value } : undefined)
      applyDetail(res)
      ElMessage.success('督办已提交')
      return
    }

    ElMessage.info('功能开发中')
  } catch (e: any) {
    console.error(e)
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

// Helpers
const getLevelTagType = (level: string) => {
  const map: any = { RED: 'danger', ORANGE: 'warning', YELLOW: 'warning' }
  return map[level] || 'info'
}

const getLevelText = (level: string) => {
  const map: any = { RED: '红色告警', ORANGE: '橙色告警', YELLOW: '黄色告警' }
  return map[level] || level
}

const getStatusTagType = (status: string) => {
  const map: any = { PENDING: 'danger', PROCESSING: 'primary', CLOSED: 'success' }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: any = { PENDING: '待处理', PROCESSING: '处理中', CLOSED: '已关闭' }
  return map[status] || status
}

onMounted(() => {
  loadData()
})
</script>

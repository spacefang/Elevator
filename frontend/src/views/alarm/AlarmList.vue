<template>
  <div>
    <!-- 筛选卡片 -->
    <el-card class="mb-4 shadow-sm !border-none" :body-style="{ padding: '16px 20px' }">
      <el-form :inline="true" :model="searchForm" class="!mb-0">
        <el-form-item label="设备">
          <el-input v-model="searchForm.deviceId" placeholder="输入设备编号/位置" clearable prefix-icon="Search" />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="searchForm.level" placeholder="全部级别" clearable style="width: 140px">
            <el-option label="红色告警" value="RED" />
            <el-option label="橙色告警" value="ORANGE" />
            <el-option label="黄色告警" value="YELLOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="shadow-sm !border-none" :body-style="{ padding: '0' }">
      <el-table :data="tableData" v-loading="loading" style="width: 100%" size="large">
        <el-table-column prop="level" label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="getLevelTagType(row.level)" effect="dark" class="font-medium">
              {{ getLevelText(row.level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="deviceNo" label="设备编号" width="150" font-family="monospace" />
        <el-table-column prop="location" label="位置" min-width="180" show-overflow-tooltip />
        <el-table-column prop="content" label="告警内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="triggerTime" label="触发时间" width="180" sortable />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small" effect="plain">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row.id)">详情</el-button>
            <el-button 
              v-permission="['alarm:handle']"
              v-if="row.status !== 'CLOSED'"
              link 
              type="primary" 
              @click="handleAlarm(row)"
            >
              处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="p-4 flex justify-end border-t border-gray-100">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getAlarmList } from '../../api/alarm'

const router = useRouter()
const loading = ref(false)

const searchForm = reactive({
  deviceId: '',
  level: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const tableData = ref([])

// Mock Data Load (Replace with API)
const loadData = async () => {
  loading.value = true
  try {
    const res: any = await getAlarmList({ 
      ...searchForm, 
      page: pagination.page - 1, 
      size: pagination.size 
    })

    tableData.value = (res.content || []).map((a: any) => ({
      id: a.id,
      level: a.level,
      type: a.type,
      deviceNo: a.deviceId,
      location: a.location || '-',
      content: a.description,
      triggerTime: a.occurredAt,
      status: a.status
    }))
    pagination.total = res.totalElements
    loading.value = false
  } catch (error) {
    console.error(error)
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const resetSearch = () => {
  searchForm.deviceId = ''
  searchForm.level = ''
  searchForm.status = ''
  handleSearch()
}

const handleSizeChange = (val: number) => {
  pagination.size = val
  loadData()
}

const handleCurrentChange = (val: number) => {
  pagination.page = val
  loadData()
}

const viewDetail = (id: number) => {
  router.push(`/alarm/detail/${id}`)
}

const handleAlarm = (row: any) => {
  console.log('Update status for', row)
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

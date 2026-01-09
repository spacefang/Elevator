<template>
  <div class="common-layout h-screen flex">
    <!-- Sidebar -->
    <div class="bg-gray-900 text-white flex flex-col transition-all duration-300" :style="{ width: isCollapse ? '64px' : '240px' }">
      <div class="h-16 flex items-center justify-center border-b border-gray-800">
        <el-icon v-if="isCollapse" class="text-xl text-primary"><Monitor /></el-icon>
        <span v-else class="text-lg font-bold tracking-wider text-primary truncate px-4">智能电梯运维</span>
      </div>
      
      <el-menu
        active-text-color="#409EFF"
        background-color="#111827"
        class="el-menu-vertical flex-1 border-r-0"
        :default-active="activeMenu"
        text-color="#fff"
        :collapse="isCollapse"
        router
      >
        <el-menu-item index="/alarm/list">
          <el-icon><Bell /></el-icon>
          <template #title>告警管理</template>
        </el-menu-item>
        <!-- 占位菜单 -->
        <el-menu-item index="/device/list" disabled>
          <el-icon><Cpu /></el-icon>
          <template #title>设备管理 (P1)</template>
        </el-menu-item>
      </el-menu>

      <div class="h-12 border-t border-gray-800 flex items-center justify-center cursor-pointer hover:bg-gray-800" @click="toggleCollapse">
        <el-icon color="#909399"><component :is="isCollapse ? 'Expand' : 'Fold'" /></el-icon>
      </div>
    </div>

    <!-- Main Container -->
    <div class="flex-1 flex flex-col min-w-0 bg-page">
      <!-- Header -->
      <div class="h-16 bg-white shadow-sm flex items-center justify-between px-6 z-10">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item>首页</el-breadcrumb-item>
          <el-breadcrumb-item>{{ currentRouteName }}</el-breadcrumb-item>
        </el-breadcrumb>
        
        <div class="flex items-center space-x-4">
          <el-badge :value="3" class="item cursor-pointer">
            <el-icon class="text-xl text-gray-600 hover:text-primary"><Bell /></el-icon>
          </el-badge>
          <el-dropdown command="handleCommand">
            <span class="flex items-center cursor-pointer text-gray-700 hover:text-primary">
              <el-avatar :size="32" class="mr-2">A</el-avatar>
              Admin
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- Content -->
      <div class="flex-1 p-6 overflow-auto">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Monitor, Bell, Cpu, Expand, Fold, ArrowDown } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const authStore = useAuthStore()
const isCollapse = ref(false)

const activeMenu = computed(() => route.path)
const currentRouteName = computed(() => route.meta.title || '当前页面')

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const handleCommand = (command: string) => {
  if (command === 'logout') {
    authStore.logout()
  }
}
</script>

<style scoped>
.el-menu-vertical:not(.el-menu--collapse) {
  width: 240px;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

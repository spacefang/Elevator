<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-900 relative overflow-hidden">
    <!-- 背景装饰 -->
    <div class="absolute inset-0 z-0">
      <div class="absolute inset-0 bg-gradient-to-br from-gray-900 via-gray-800 to-black opacity-90"></div>
      <div class="absolute top-0 left-0 w-full h-full bg-[url('https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?q=80&w=2670&auto=format&fit=crop')] bg-cover bg-center opacity-10 blur-sm"></div>
    </div>

    <!-- 登录卡片 -->
    <div class="relative z-10 w-full max-w-md bg-white/10 backdrop-blur-md rounded-2xl shadow-2xl border border-white/20 p-8 transform transition-all hover:scale-[1.01]">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-white mb-2 tracking-wide">智能电梯运维平台</h1>
        <p class="text-gray-300 text-sm">Smart Elevator Operation Platform</p>
      </div>

      <el-form :model="loginForm" :rules="rules" ref="loginFormRef" class="space-y-6" size="large">
        <el-form-item prop="username">
          <el-input 
            v-model="loginForm.username" 
            placeholder="请输入用户名" 
            :prefix-icon="User"
            class="custom-input"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="请输入密码" 
            :prefix-icon="Lock"
            show-password
            class="custom-input"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-button type="primary" :loading="loading" class="w-full !h-12 !text-lg !font-medium !rounded-lg !bg-primary hover:!bg-blue-500 !border-none transition-colors duration-300 shadow-lg" @click="handleLogin">
          登 录
        </el-button>
      </el-form>

      <div class="mt-6 text-center text-gray-400 text-xs">
        &copy; 2026 Smart Elevator Co., Ltd. All rights reserved.
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const loginFormRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = reactive<FormRules>({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
})

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await authStore.login(loginForm)
        ElMessage.success('登录成功')
        router.push('/')
      } catch (error) {
        ElMessage.error('登录失败，请检查用户名或密码')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
/* 自定义 Element Plus Input 样式以适应深色背景 */
:deep(.custom-input .el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.1);
  box-shadow: none !important;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: #fff;
}

:deep(.custom-input .el-input__wrapper:hover),
:deep(.custom-input .el-input__wrapper.is-focus) {
  border-color: var(--el-color-primary);
  background-color: rgba(255, 255, 255, 0.15);
}

:deep(.custom-input .el-input__inner) {
  color: #fff;
  height: 48px;
}

:deep(.custom-input .el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.5);
}

:deep(.custom-input .el-input__icon) {
  color: rgba(255, 255, 255, 0.7);
}
</style>

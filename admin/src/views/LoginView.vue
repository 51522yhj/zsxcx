<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="login-brand" aria-hidden="true">
        <img v-if="logoUrl" :src="logoUrl" alt="" />
        <svg v-else viewBox="0 0 48 48" role="img">
          <path class="mark-bg" d="M10 7h28c2.76 0 5 2.24 5 5v24c0 2.76-2.24 5-5 5H10c-2.76 0-5-2.24-5-5V12c0-2.76 2.24-5 5-5Z" />
          <path class="mark-cloth" d="M13 18c4.8-5.7 10.7 5.8 16.4.2 2.8-2.8 5.8-3.5 9.1-.5v6.4c-3.3-3.1-6.3-2.4-9.1.4-5.7 5.7-11.6-5.8-16.4-.1V18Z" />
          <path class="mark-thread" d="M13 31.5c5.2-4.1 10.2 2.7 16.2-.9 3.2-1.9 6.1-2.1 9.3.3" />
        </svg>
      </div>
      <h1>{{ siteName }}</h1>
      <p>商品展示与公告配置后台</p>
      <el-form :model="form" @keyup.enter="login">
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" size="large" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" style="width: 100%" @click="login">
          登录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '../api/http'

defineProps({
  siteName: {
    type: String,
    default: '小于印染'
  },
  logoUrl: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['logged-in'])
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })

async function login() {
  loading.value = true
  try {
    const result = await http.post('/api/admin/auth/login', form)
    localStorage.setItem('xy_token', result.token)
    ElMessage.success('登录成功')
    emit('logged-in', result.token)
  } finally {
    loading.value = false
  }
}
</script>

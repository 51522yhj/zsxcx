<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="login-brand">小</div>
      <h1>小于印染</h1>
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

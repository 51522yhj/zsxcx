<template>
  <section>
    <div class="page-head">
      <div class="page-title">
        <h1>站点配置</h1>
        <p>维护小程序名称、统一联系方式和客服按钮</p>
      </div>
    </div>
    <div class="panel">
      <el-form label-width="120px" style="max-width: 640px">
        <el-form-item label="小程序名称"><el-input v-model="form.siteName" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.contactPhone" /></el-form-item>
        <el-form-item label="微信号"><el-input v-model="form.contactWechat" /></el-form-item>
        <el-form-item label="首页模块标题"><el-input v-model="form.homeSectionTitle" /></el-form-item>
        <el-form-item label="启用客服"><el-switch v-model="form.customerServiceEnabled" /></el-form-item>
        <el-form-item label="客服按钮文案"><el-input v-model="form.customerServiceText" /></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="save">保存配置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '../api/http'

const form = reactive({ siteName: '小于印染', contactPhone: '', contactWechat: '', customerServiceEnabled: true, customerServiceText: '咨询客服', homeSectionTitle: '精选面料' })

async function load() {
  Object.assign(form, await http.get('/api/admin/settings'))
}

async function save() {
  await http.put('/api/admin/settings', form)
  ElMessage.success('已保存')
}

onMounted(load)
</script>


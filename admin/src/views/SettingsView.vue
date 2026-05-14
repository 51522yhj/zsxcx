<template>
  <section>
    <div class="page-head">
      <div class="page-title">
        <h1>站点配置</h1>
        <p>维护小程序名称、后台图标、统一联系方式和客服按钮</p>
      </div>
    </div>
    <div class="panel">
      <el-form label-width="120px" style="max-width: 640px">
        <el-form-item label="网站/小程序名称">
          <el-input v-model="form.siteName" placeholder="例如 小于印染" />
        </el-form-item>
        <el-form-item label="后台图标">
          <el-upload
            :http-request="uploadLogo"
            list-type="picture-card"
            :file-list="logoFiles"
            :limit="1"
            :on-remove="removeLogo"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { http, uploadImage } from '../api/http'

const emit = defineEmits(['saved'])
const logoFiles = ref([])
const form = reactive({
  siteName: '小于印染',
  contactPhone: '',
  contactWechat: '',
  logoUrl: '',
  customerServiceEnabled: true,
  customerServiceText: '咨询客服',
  homeSectionTitle: '精选面料'
})

function syncLogoFiles() {
  logoFiles.value = form.logoUrl ? [{ name: '后台图标', url: form.logoUrl }] : []
}

async function load() {
  const settings = await http.get('/api/admin/settings')
  Object.assign(form, settings)
  syncLogoFiles()
  emit('saved', settings)
}

async function uploadLogo(option) {
  const result = await uploadImage(option.file)
  form.logoUrl = result.url
  syncLogoFiles()
  option.onSuccess(result)
}

function removeLogo() {
  form.logoUrl = ''
  syncLogoFiles()
}

async function save() {
  const settings = await http.put('/api/admin/settings', form)
  Object.assign(form, settings)
  syncLogoFiles()
  emit('saved', settings)
  ElMessage.success('已保存')
}

onMounted(load)
</script>

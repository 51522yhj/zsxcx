<template>
  <section>
    <div class="page-head">
      <div class="page-title">
        <h1>公告管理</h1>
        <p>配置首页顶部滚动通知和弹窗内容</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="open()">新增公告</el-button>
    </div>

    <div class="panel">
      <el-table :data="rows">
        <el-table-column prop="tickerText" label="滚动内容" min-width="220" />
        <el-table-column prop="title" label="弹窗标题" width="160" />
        <el-table-column label="图片" width="100">
          <template #default="{ row }">
            <img v-if="row.imageUrl" class="cover" :src="row.imageUrl" alt="" />
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column label="启用" width="90">
          <template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="open(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑公告' : '新增公告'" width="640px">
      <el-form label-width="96px">
        <el-form-item label="滚动内容"><el-input v-model="form.tickerText" /></el-form-item>
        <el-form-item label="弹窗标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="正文"><el-input v-model="form.content" type="textarea" :rows="5" /></el-form-item>
        <el-form-item label="图片">
          <el-upload :http-request="handleImageUpload" list-type="picture-card" :file-list="imageList" :limit="1" :on-remove="removeImage">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { http, uploadImage } from '../api/http'

const rows = ref([])
const dialog = ref(false)
const imageList = ref([])
const form = reactive({ id: null, title: '公告', tickerText: '', content: '', imageUrl: '', enabled: true, sortOrder: 0 })

async function load() {
  rows.value = await http.get('/api/admin/announcements')
}

function open(row) {
  Object.assign(form, { id: null, title: '公告', tickerText: '', content: '', imageUrl: '', enabled: true, sortOrder: 0 }, row || {})
  imageList.value = form.imageUrl ? [{ name: '公告图', url: form.imageUrl }] : []
  dialog.value = true
}

async function handleImageUpload(option) {
  const data = await uploadImage(option.file)
  form.imageUrl = data.url
  imageList.value = [{ name: option.file.name, url: data.url }]
  option.onSuccess(data)
}

function removeImage() {
  form.imageUrl = ''
  imageList.value = []
}

async function save() {
  if (form.id) await http.put(`/api/admin/announcements/${form.id}`, form)
  else await http.post('/api/admin/announcements', form)
  dialog.value = false
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除「${row.title}」？`, '删除公告')
  await http.delete(`/api/admin/announcements/${row.id}`)
  load()
}

onMounted(load)
</script>


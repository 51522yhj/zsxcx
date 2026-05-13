<template>
  <section>
    <div class="page-head">
      <div class="page-title">
        <h1>分类管理</h1>
        <p>配置小程序分类页左侧分类和二级细分</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="open()">新增分类</el-button>
    </div>

    <div class="panel">
      <el-table :data="rows" row-key="id" default-expand-all>
        <el-table-column prop="name" label="分类名称" />
        <el-table-column label="封面" width="100">
          <template #default="{ row }">
            <img v-if="row.coverUrl" class="cover" :src="row.coverUrl" alt="" />
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="120" />
        <el-table-column prop="enabled" label="启用" width="120">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="open(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑分类' : '新增分类'" width="560px">
      <el-form label-width="88px">
        <el-form-item label="上级分类">
          <el-select v-model="form.parentId" clearable style="width: 100%">
            <el-option v-for="item in flatRows" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="封面">
          <el-upload :http-request="handleCoverUpload" list-type="picture-card" :file-list="coverList" :limit="1" :on-remove="removeCover">
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { http, uploadImage } from '../api/http'

const rows = ref([])
const dialog = ref(false)
const coverList = ref([])
const form = reactive({ id: null, parentId: null, name: '', coverUrl: '', iconUrl: '', sortOrder: 0, enabled: true })
const flatRows = computed(() => rows.value.flatMap((item) => [item, ...(item.children || [])]).filter((item) => item.id !== form.id))

async function load() {
  rows.value = await http.get('/api/admin/categories/tree')
}

function open(row) {
  Object.assign(form, { id: null, parentId: null, name: '', coverUrl: '', iconUrl: '', sortOrder: 0, enabled: true }, row || {})
  coverList.value = form.coverUrl ? [{ name: '封面', url: form.coverUrl }] : []
  dialog.value = true
}

async function handleCoverUpload(option) {
  const data = await uploadImage(option.file)
  form.coverUrl = data.url
  coverList.value = [{ name: option.file.name, url: data.url }]
  option.onSuccess(data)
}

function removeCover() {
  form.coverUrl = ''
  coverList.value = []
}

async function save() {
  if (form.id) await http.put(`/api/admin/categories/${form.id}`, form)
  else await http.post('/api/admin/categories', form)
  dialog.value = false
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除「${row.name}」？`, '删除分类')
  await http.delete(`/api/admin/categories/${row.id}`)
  load()
}

onMounted(load)
</script>

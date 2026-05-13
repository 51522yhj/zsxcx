<template>
  <section>
    <div class="page-head">
      <div class="page-title">
        <h1>标签管理</h1>
        <p>标签会参与小程序模糊搜索</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="open()">新增标签</el-button>
    </div>
    <div class="panel">
      <el-table :data="rows">
        <el-table-column prop="name" label="标签名称" />
        <el-table-column prop="sortOrder" label="排序" width="120" />
        <el-table-column label="启用" width="120">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" disabled />
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
    <el-dialog v-model="dialog" :title="form.id ? '编辑标签' : '新增标签'" width="420px">
      <el-form label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
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
import { http } from '../api/http'

const rows = ref([])
const dialog = ref(false)
const form = reactive({ id: null, name: '', sortOrder: 0, enabled: true })

async function load() {
  rows.value = await http.get('/api/admin/tags')
}

function open(row) {
  Object.assign(form, { id: null, name: '', sortOrder: 0, enabled: true }, row || {})
  dialog.value = true
}

async function save() {
  if (form.id) await http.put(`/api/admin/tags/${form.id}`, form)
  else await http.post('/api/admin/tags', form)
  dialog.value = false
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除「${row.name}」？`, '删除标签')
  await http.delete(`/api/admin/tags/${row.id}`)
  load()
}

onMounted(load)
</script>

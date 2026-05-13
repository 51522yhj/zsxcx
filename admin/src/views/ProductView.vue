<template>
  <section>
    <div class="page-head">
      <div class="page-title">
        <h1>商品管理</h1>
        <p>维护面料商品、分类、标签、图片和上下架状态</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增商品</el-button>
    </div>

    <div class="panel">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索名称、标签、分类" clearable style="width: 260px" @keyup.enter="load" />
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px">
          <el-option label="已上架" value="PUBLISHED" />
          <el-option label="草稿" value="DRAFT" />
        </el-select>
        <el-button :icon="Search" @click="load">查询</el-button>
      </div>

      <el-table :data="rows" v-loading="loading">
        <el-table-column label="图片" width="92">
          <template #default="{ row }">
            <img class="cover" :src="row.coverUrl || row.images?.[0]?.imageUrl" alt="" />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column label="标签" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="tag in row.tags" :key="tag.id" size="small" style="margin-right: 6px">{{ tag.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">{{ row.status === 'PUBLISHED' ? '已上架' : '草稿' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="edit(row)">编辑</el-button>
            <el-button size="small" @click="toggle(row)">{{ row.status === 'PUBLISHED' ? '下架' : '上架' }}</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        layout="sizes, prev, pager, next, jumper, total"
        :page-sizes="[10, 12, 20, 30]"
        :total="total"
        v-model:page-size="query.size"
        v-model:current-page="query.page"
        @size-change="load"
        @current-change="load"
        style="margin-top: 16px"
      />
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑商品' : '新增商品'" width="720px">
      <el-form label-width="92px" :model="form">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="例如 5030#" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" clearable style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="form.tagIds" multiple style="width: 100%">
            <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" placeholder="例如 价格面议" />
        </el-form-item>
        <el-form-item label="详情">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="图片">
          <el-upload :http-request="handleUpload" list-type="picture-card" :file-list="fileList" :on-remove="removeImage">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button label="DRAFT">草稿</el-radio-button>
            <el-radio-button label="PUBLISHED">上架</el-radio-button>
          </el-radio-group>
        </el-form-item>
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
import { Plus, Search } from '@element-plus/icons-vue'
import { http, uploadImage } from '../api/http'

const loading = ref(false)
const dialog = ref(false)
const rows = ref([])
const total = ref(0)
const categories = ref([])
const tags = ref([])
const fileList = ref([])
const query = reactive({ page: 1, size: 12, keyword: '', status: '' })
const form = reactive(emptyForm())

function emptyForm() {
  return { id: null, name: '', categoryId: null, summary: '', description: '', coverUrl: '', status: 'DRAFT', sortOrder: 0, tagIds: [], images: [] }
}

async function loadMeta() {
  categories.value = await http.get('/api/admin/categories')
  tags.value = await http.get('/api/admin/tags')
}

async function load() {
  loading.value = true
  try {
    const data = await http.get('/api/admin/products', { params: query })
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function assignForm(data) {
  Object.assign(form, emptyForm(), data)
  form.tagIds = data.tags?.map((item) => item.id) || []
  form.images = data.images?.map((item) => ({ imageUrl: item.imageUrl, objectKey: item.objectKey, sortOrder: item.sortOrder, isCover: item.isCover })) || []
  fileList.value = form.images.map((image, index) => ({ name: `图片${index + 1}`, url: image.imageUrl }))
}

function openCreate() {
  assignForm(emptyForm())
  dialog.value = true
}

async function edit(row) {
  const data = await http.get(`/api/admin/products/${row.id}`)
  assignForm(data)
  dialog.value = true
}

async function handleUpload(option) {
  const data = await uploadImage(option.file)
  form.images.push({ imageUrl: data.url, objectKey: data.objectKey, sortOrder: form.images.length, isCover: form.images.length === 0 })
  form.coverUrl = form.coverUrl || data.url
  fileList.value.push({ name: option.file.name, url: data.url })
  option.onSuccess(data)
}

function removeImage(file) {
  form.images = form.images.filter((image) => image.imageUrl !== file.url)
  form.coverUrl = form.images[0]?.imageUrl || ''
}

async function save() {
  form.coverUrl = form.coverUrl || form.images[0]?.imageUrl || ''
  if (form.id) await http.put(`/api/admin/products/${form.id}`, form)
  else await http.post('/api/admin/products', form)
  dialog.value = false
  load()
}

async function toggle(row) {
  await http.patch(`/api/admin/products/${row.id}/status`, { status: row.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED' })
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除「${row.name}」？`, '删除商品')
  await http.delete(`/api/admin/products/${row.id}`)
  load()
}

onMounted(() => {
  loadMeta()
  load()
})
</script>


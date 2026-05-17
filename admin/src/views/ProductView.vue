<template>
  <section>
    <div class="page-head">
      <div class="page-title">
        <h1>商品管理</h1>
        <p>维护商品、分类、标签、轮播图片/视频和上下架状态</p>
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
        <el-table-column label="封面" width="104">
          <template #default="{ row }">
            <div v-if="row.coverUrl || mediaCover(row.images?.[0])" class="cover-box">
              <img class="cover" :src="row.coverUrl || mediaCover(row.images?.[0])" alt="" />
            </div>
            <div v-else class="cover-box video-cover-placeholder">MP4</div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column label="标签" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="tag in row.tags" :key="tag.id" size="small" class="tag">{{ tag.name }}</el-tag>
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

    <el-dialog v-model="dialog" :title="form.id ? '编辑商品' : '新增商品'" width="920px">
      <el-form label-width="112px" :model="form">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="例如 5030#" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" clearable style="width: 100%">
            <el-option v-for="cat in flatCategories" :key="cat.id" :label="cat.name" :value="cat.id" />
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
        <el-form-item label="详情文字">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>

        <el-form-item label="轮播设置">
          <div class="carousel-setting">
            <el-switch v-model="form.carouselAutoplayEnabled" active-text="自动轮播" inactive-text="关闭自动轮播" />
            <el-input-number
              v-model="form.carouselIntervalSeconds"
              :min="1"
              :max="20"
              :step="1"
              :disabled="!form.carouselAutoplayEnabled"
            />
            <span class="setting-tip">每张停留秒数</span>
          </div>
        </el-form-item>

        <el-form-item label="轮播媒体">
          <div class="media-editor" v-loading="isUploadingMedia" element-loading-text="Uploading...">
            <div class="upload-actions">
              <el-upload
                :http-request="handleImageUpload"
                accept="image/*"
                multiple
                :limit="5"
                v-model:file-list="imageUploadFiles"
                :before-upload="beforeUploadFile"
                :on-exceed="handleImageExceed"
                :show-file-list="false"
                :disabled="isUploadingMedia"
              >
                <el-button :icon="Picture" :loading="uploading.image > 0" :disabled="isUploadingMedia">批量上传图片</el-button>
              </el-upload>
              <el-upload
                :http-request="handleVideoUpload"
                accept="video/mp4"
                :before-upload="beforeUploadFile"
                :show-file-list="false"
                :disabled="isUploadingMedia"
              >
                <el-button :icon="VideoCamera" :loading="uploading.video > 0" :disabled="isUploadingMedia">上传视频</el-button>
              </el-upload>
              <span class="upload-tip">图片单次最多 5 张，单个文件最大 100MB</span>
            </div>
            <div class="media-list" v-if="form.images.length">
              <div class="media-item" v-for="(media, index) in sortedMedia" :key="media.localKey">
                <div class="media-preview">
                  <img v-if="media.mediaType !== 'VIDEO'" :src="media.imageUrl" alt="" />
                  <template v-else>
                    <img v-if="media.posterUrl" :src="media.posterUrl" alt="" />
                    <div v-else class="video-placeholder">MP4</div>
                  </template>
                  <span class="media-badge">{{ media.mediaType === 'VIDEO' ? '视频' : '图片' }}</span>
                </div>
                <div class="media-name">{{ media.objectKey || media.imageUrl }}</div>
                <div class="media-actions">
                  <el-button size="small" :disabled="index === 0 || isUploadingMedia" @click="moveCarousel(index, -1)">上移</el-button>
                  <el-button size="small" :disabled="index === sortedMedia.length - 1 || isUploadingMedia" @click="moveCarousel(index, 1)">下移</el-button>
                  <el-button size="small" :disabled="isUploadingMedia" @click="setCover(index)">设封面</el-button>
                  <el-upload
                    v-if="media.mediaType === 'VIDEO'"
                    :http-request="(option) => uploadPoster(option, index)"
                    accept="image/*"
                    :show-file-list="false"
                    :disabled="isUploadingMedia"
                  >
                    <el-button size="small" :loading="uploading.poster > 0" :disabled="isUploadingMedia">封面图</el-button>
                  </el-upload>
                  <el-button size="small" type="danger" :disabled="isUploadingMedia" @click="removeMedia(index)">删除</el-button>
                </div>
                <el-tag v-if="media.isCover" size="small" type="success">当前封面</el-tag>
              </div>
            </div>
            <el-empty v-else description="请上传商品图片或视频" :image-size="80" />
          </div>
        </el-form-item>

        <el-form-item label="图文详情图片">
          <div class="detail-editor">
            <div class="detail-item" v-for="(media, index) in detailImages" :key="media.localKey">
              <img :src="media.imageUrl" alt="" />
              <el-switch v-model="media.showInDetail" active-text="展示" inactive-text="隐藏" />
              <el-button size="small" :disabled="index === 0" @click="moveDetail(index, -1)">上移</el-button>
              <el-button size="small" :disabled="index === detailImages.length - 1" @click="moveDetail(index, 1)">下移</el-button>
            </div>
            <el-empty v-if="!detailImages.length" description="图文详情只展示图片，视频不会出现在这里" :image-size="80" />
          </div>
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
        <el-button type="primary" :disabled="isUploadingMedia" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Plus, Search, VideoCamera } from '@element-plus/icons-vue'
import { http, uploadImage, uploadVideo } from '../api/http'

const loading = ref(false)
const MAX_UPLOAD_SIZE = 100 * 1024 * 1024
const MAX_BATCH_IMAGE_COUNT = 5

const uploading = reactive({ image: 0, video: 0, poster: 0 })
const dialog = ref(false)
const imageUploadFiles = ref([])
const rows = ref([])
const total = ref(0)
const categories = ref([])
const tags = ref([])
const query = reactive({ page: 1, size: 12, keyword: '', status: '' })
const form = reactive(emptyForm())

const flatCategories = computed(() => flattenCategories(categories.value))
const sortedMedia = computed(() => form.images.slice().sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0)))
const isUploadingMedia = computed(() => uploading.image > 0 || uploading.video > 0 || uploading.poster > 0)
const detailImages = computed(() => form.images
  .filter((item) => item.mediaType !== 'VIDEO')
  .sort((a, b) => (a.detailSortOrder ?? 0) - (b.detailSortOrder ?? 0)))

function emptyForm() {
  return {
    id: null,
    name: '',
    categoryId: null,
    summary: '',
    description: '',
    coverUrl: '',
    status: 'DRAFT',
    sortOrder: 0,
    carouselAutoplayEnabled: true,
    carouselIntervalSeconds: 3,
    tagIds: [],
    images: []
  }
}

function flattenCategories(list, prefix = '') {
  return list.flatMap((item) => [
    { ...item, name: `${prefix}${item.name}` },
    ...flattenCategories(item.children || [], `${prefix}${item.name} / `)
  ])
}

async function loadMeta() {
  categories.value = await http.get('/api/admin/categories/tree')
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
  form.carouselAutoplayEnabled = data.carouselAutoplayEnabled !== false
  form.carouselIntervalSeconds = Math.min(Math.max(Number(data.carouselIntervalSeconds || 3), 1), 20)
  form.tagIds = data.tags?.map((item) => item.id) || []
  form.images = (data.images || []).map((item, index) => normalizeMedia(item, index))
  normalizeOrders()
}

function normalizeMedia(item, index) {
  const mediaType = item.mediaType === 'VIDEO' ? 'VIDEO' : 'IMAGE'
  return {
    localKey: `${Date.now()}-${index}-${Math.random()}`,
    mediaType,
    imageUrl: item.imageUrl,
    objectKey: item.objectKey,
    posterUrl: item.posterUrl || '',
    width: item.width,
    height: item.height,
    sortOrder: item.sortOrder ?? index,
    isCover: Boolean(item.isCover),
    showInDetail: mediaType === 'IMAGE' ? item.showInDetail !== false : false,
    detailSortOrder: item.detailSortOrder ?? index
  }
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

async function handleImageUpload(option) {
  uploading.image += 1
  try {
    const data = await uploadImage(option.file)
    form.images.push(normalizeMedia({ mediaType: 'IMAGE', imageUrl: data.url, objectKey: data.objectKey, showInDetail: true }, form.images.length))
    if (!form.images.some((item) => item.isCover)) {
      form.images[0].isCover = true
    }
    normalizeOrders()
    option.onSuccess(data)
  } catch (error) {
    option.onError?.(error)
  } finally {
    uploading.image = Math.max(uploading.image - 1, 0)
    if (uploading.image === 0) {
      imageUploadFiles.value = []
    }
  }
}

async function handleVideoUpload(option) {
  uploading.video += 1
  try {
    const data = await uploadVideo(option.file)
    form.images.push(normalizeMedia({ mediaType: 'VIDEO', imageUrl: data.url, objectKey: data.objectKey, showInDetail: false }, form.images.length))
    normalizeOrders()
    option.onSuccess(data)
  } catch (error) {
    option.onError?.(error)
  } finally {
    uploading.video = Math.max(uploading.video - 1, 0)
  }
}

async function uploadPoster(option, sortedIndex) {
  uploading.poster += 1
  try {
    const data = await uploadImage(option.file)
    sortedMedia.value[sortedIndex].posterUrl = data.url
    option.onSuccess(data)
  } catch (error) {
    option.onError?.(error)
  } finally {
    uploading.poster = Math.max(uploading.poster - 1, 0)
  }
}

function beforeUploadFile(file) {
  if (file.size > MAX_UPLOAD_SIZE) {
    ElMessage.error('文件最大不得超过100MB')
    return false
  }
  return true
}

function handleImageExceed() {
  ElMessage.error(`一次最多上传${MAX_BATCH_IMAGE_COUNT}张图片`)
}

function moveCarousel(index, step) {
  const list = sortedMedia.value
  const current = list[index]
  const next = list[index + step]
  if (!current || !next) return
  const temp = current.sortOrder
  current.sortOrder = next.sortOrder
  next.sortOrder = temp
  normalizeOrders()
}

function moveDetail(index, step) {
  const list = detailImages.value
  const current = list[index]
  const next = list[index + step]
  if (!current || !next) return
  const temp = current.detailSortOrder
  current.detailSortOrder = next.detailSortOrder
  next.detailSortOrder = temp
  normalizeDetailOrders()
}

function setCover(sortedIndex) {
  const target = sortedMedia.value[sortedIndex]
  form.images.forEach((item) => { item.isCover = item === target })
}

function removeMedia(sortedIndex) {
  const target = sortedMedia.value[sortedIndex]
  form.images = form.images.filter((item) => item !== target)
  if (!form.images.some((item) => item.isCover) && form.images.length) {
    form.images[0].isCover = true
  }
  normalizeOrders()
}

function normalizeOrders() {
  sortedMedia.value.forEach((item, index) => { item.sortOrder = index })
  normalizeDetailOrders()
}

function normalizeDetailOrders() {
  detailImages.value.forEach((item, index) => { item.detailSortOrder = index })
}

function mediaCover(media) {
  if (!media) return ''
  return media.mediaType === 'VIDEO' ? media.posterUrl || '' : media.imageUrl
}

async function save() {
  normalizeOrders()
  const cover = form.images.find((item) => item.isCover) || form.images.find((item) => item.mediaType !== 'VIDEO') || form.images[0]
  form.coverUrl = mediaCover(cover)
  const payload = {
    ...form,
    images: form.images.map(({ localKey, ...item }) => ({
      ...item,
      showInDetail: item.mediaType === 'IMAGE' ? Boolean(item.showInDetail) : false
    }))
  }
  if (form.id) await http.put(`/api/admin/products/${form.id}`, payload)
  else await http.post('/api/admin/products', payload)
  ElMessage.success('已保存')
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

<style scoped>
.tag {
  margin-right: 6px;
}

.cover-box,
.cover {
  width: 64px;
  height: 64px;
}

.cover {
  object-fit: cover;
}

.cover-box {
  border-radius: 8px;
  background: #f2f3f5;
  overflow: hidden;
}

.video-cover-placeholder {
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, #2b2f36, #111318);
  font-size: 13px;
  font-weight: 800;
}

.media-editor,
.detail-editor {
  width: 100%;
}

.carousel-setting {
  display: flex;
  align-items: center;
  gap: 14px;
}

.setting-tip {
  color: #909399;
  font-size: 13px;
}

.upload-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.upload-tip {
  color: #909399;
  font-size: 13px;
}

.media-list {
  display: grid;
  gap: 12px;
}

.media-item {
  display: grid;
  grid-template-columns: 96px 1fr auto;
  gap: 14px;
  align-items: center;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fff;
}

.media-preview {
  position: relative;
  width: 96px;
  height: 72px;
  overflow: hidden;
  border-radius: 8px;
  background: #f2f3f5;
}

.media-preview img,
.detail-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: #fff;
  background: #1f2329;
  font-weight: 700;
}

.media-badge {
  position: absolute;
  right: 6px;
  bottom: 6px;
  padding: 2px 6px;
  border-radius: 999px;
  color: #fff;
  background: #ef3f3a;
  font-size: 12px;
}

.media-name {
  color: #606266;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.media-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-editor {
  display: grid;
  gap: 10px;
}

.detail-item {
  display: grid;
  grid-template-columns: 96px 150px 64px 64px;
  gap: 10px;
  align-items: center;
}

.detail-item img {
  width: 96px;
  height: 72px;
  border-radius: 8px;
  background: #f2f3f5;
}
</style>

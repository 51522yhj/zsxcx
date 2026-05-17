<template>
  <LoginView v-if="!token" :site-name="siteName" :logo-url="logoUrl" @logged-in="handleLoggedIn" />
  <div v-else class="shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark" aria-hidden="true">
          <img v-if="logoUrl" :src="logoUrl" alt="" />
          <BrandLogo v-else />
        </div>
        <div class="brand-copy">
          <strong>{{ adminTitle }}</strong>
          <span>运营后台</span>
        </div>
      </div>
      <button
        v-for="item in nav"
        :key="item.key"
        class="nav-item"
        :class="{ active: current === item.key }"
        @click="changeCurrent(item.key)"
      >
        <component :is="item.icon" />
        {{ item.label }}
      </button>
      <button class="nav-item logout" @click="logout">
        <SwitchButton />
        退出登录
      </button>
    </aside>
    <main class="workspace">
      <ProductView v-if="current === 'products'" />
      <CategoryView v-if="current === 'categories'" />
      <TagView v-if="current === 'tags'" />
      <AnnouncementView v-if="current === 'announcements'" />
      <MonitoringView v-if="current === 'monitoring'" />
      <SettingsView v-if="current === 'settings'" @saved="applySettings" />
    </main>
  </div>
</template>

<script setup>
import { computed, h, onMounted, ref } from 'vue'
import { Bell, DataAnalysis, Goods, Menu, PriceTag, Setting, SwitchButton } from '@element-plus/icons-vue'
import { http } from './api/http'
import LoginView from './views/LoginView.vue'
import ProductView from './views/ProductView.vue'
import CategoryView from './views/CategoryView.vue'
import TagView from './views/TagView.vue'
import AnnouncementView from './views/AnnouncementView.vue'
import MonitoringView from './views/MonitoringView.vue'
import SettingsView from './views/SettingsView.vue'

const defaultSiteName = '小于印染'
const defaultIconSvg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48"><path fill="#ff2d2d" d="M10 7h28c2.76 0 5 2.24 5 5v24c0 2.76-2.24 5-5 5H10c-2.76 0-5-2.24-5-5V12c0-2.76 2.24-5 5-5Z"/><path fill="#fff" fill-opacity=".92" d="M13 18c4.8-5.7 10.7 5.8 16.4.2 2.8-2.8 5.8-3.5 9.1-.5v6.4c-3.3-3.1-6.3-2.4-9.1.4-5.7 5.7-11.6-5.8-16.4-.1V18Z"/><path fill="none" stroke="#fff" stroke-width="2.4" stroke-linecap="round" opacity=".72" d="M13 31.5c5.2-4.1 10.2 2.7 16.2-.9 3.2-1.9 6.1-2.1 9.3.3"/></svg>`
const defaultIconHref = `data:image/svg+xml,${encodeURIComponent(defaultIconSvg)}`
const BrandLogo = {
  name: 'BrandLogo',
  render: () => h('span', { innerHTML: defaultIconSvg })
}

const token = ref(localStorage.getItem('xy_token'))
const current = ref('products')
const siteName = ref(defaultSiteName)
const logoUrl = ref('')
const adminTitle = computed(() => `${siteName.value || defaultSiteName}后台`)
const nav = [
  { key: 'products', label: '商品管理', icon: Goods },
  { key: 'categories', label: '分类管理', icon: Menu },
  { key: 'tags', label: '标签管理', icon: PriceTag },
  { key: 'announcements', label: '公告管理', icon: Bell },
  { key: 'monitoring', label: '数据监控', icon: DataAnalysis },
  { key: 'settings', label: '站点配置', icon: Setting }
]

function changeCurrent(key) {
  current.value = key
  localStorage.setItem('xy_admin_source', key)
}

function setFavicon(href) {
  let link = document.querySelector('link[rel="icon"]')
  if (!link) {
    link = document.createElement('link')
    link.rel = 'icon'
    document.head.appendChild(link)
  }
  link.type = href?.startsWith('data:image/svg+xml') ? 'image/svg+xml' : ''
  link.href = href || defaultIconHref
}

function applySettings(settings) {
  siteName.value = settings?.siteName || defaultSiteName
  logoUrl.value = settings?.logoUrl || ''
  document.title = adminTitle.value
  setFavicon(logoUrl.value || defaultIconHref)
}

async function loadPublicSettings() {
  try {
    applySettings(await http.get('/api/public/settings'))
  } catch {
    applySettings({ siteName: defaultSiteName, logoUrl: '' })
  }
}

function handleLoggedIn(newToken) {
  token.value = newToken
  loadPublicSettings()
}

function logout() {
  localStorage.removeItem('xy_token')
  token.value = ''
}

onMounted(() => {
  localStorage.setItem('xy_admin_source', current.value)
  loadPublicSettings()
})
</script>

<template>
  <LoginView v-if="!token" @logged-in="handleLoggedIn" />
  <div v-else class="shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">小</div>
        <div>
          <strong>小于印染</strong>
          <span>运营后台</span>
        </div>
      </div>
      <button
        v-for="item in nav"
        :key="item.key"
        class="nav-item"
        :class="{ active: current === item.key }"
        @click="current = item.key"
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
      <SettingsView v-if="current === 'settings'" />
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Goods, Menu, PriceTag, Bell, Setting, SwitchButton } from '@element-plus/icons-vue'
import LoginView from './views/LoginView.vue'
import ProductView from './views/ProductView.vue'
import CategoryView from './views/CategoryView.vue'
import TagView from './views/TagView.vue'
import AnnouncementView from './views/AnnouncementView.vue'
import SettingsView from './views/SettingsView.vue'

const token = ref(localStorage.getItem('xy_token'))
const current = ref('products')
const nav = [
  { key: 'products', label: '商品管理', icon: Goods },
  { key: 'categories', label: '分类管理', icon: Menu },
  { key: 'tags', label: '标签管理', icon: PriceTag },
  { key: 'announcements', label: '公告管理', icon: Bell },
  { key: 'settings', label: '站点配置', icon: Setting }
]

function handleLoggedIn(newToken) {
  token.value = newToken
}

function logout() {
  localStorage.removeItem('xy_token')
  token.value = ''
}
</script>

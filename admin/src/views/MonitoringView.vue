<template>
  <section>
    <div class="page-head">
      <div class="page-title">
        <h1>数据监控</h1>
        <p>统计小程序访问来源、访问量和新用户增长</p>
      </div>
      <div class="toolbar compact">
        <el-select v-model="days" style="width: 132px" @change="load">
          <el-option label="最近7天" :value="7" />
          <el-option label="最近30天" :value="30" />
          <el-option label="最近90天" :value="90" />
        </el-select>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <div v-loading="loading" class="monitoring">
      <div class="metric-row">
        <div class="metric">
          <span>今日访问量</span>
          <strong>{{ number(summary.today.visits) }}</strong>
          <em>新用户 {{ number(summary.today.newUsers) }}</em>
        </div>
        <div class="metric">
          <span>本周访问量</span>
          <strong>{{ number(summary.week.visits) }}</strong>
          <em>访客 {{ number(summary.week.users) }}</em>
        </div>
        <div class="metric">
          <span>本月访问量</span>
          <strong>{{ number(summary.month.visits) }}</strong>
          <em>新用户 {{ number(summary.month.newUsers) }}</em>
        </div>
        <div class="metric">
          <span>平均响应</span>
          <strong>{{ number(summary.today.avgCostMs) }}ms</strong>
          <em>今日小程序接口</em>
        </div>
      </div>

      <div class="chart-grid">
        <div class="panel chart-panel">
          <div class="panel-title">
            <h2>访问趋势</h2>
            <span>访问量 / 新用户</span>
          </div>
          <svg class="line-chart" viewBox="0 0 720 260" preserveAspectRatio="none">
            <line x1="28" y1="222" x2="700" y2="222" class="axis" />
            <polyline :points="linePoints('visits')" class="line visits-line" />
            <polyline :points="linePoints('newUsers')" class="line new-line" />
            <g v-for="item in trendPoints" :key="item.label">
              <circle :cx="item.x" :cy="item.visitsY" r="3.5" class="dot visits-dot" />
              <circle :cx="item.x" :cy="item.newUsersY" r="3" class="dot new-dot" />
            </g>
          </svg>
          <div class="legend">
            <span><i class="legend-red"></i>访问量</span>
            <span><i class="legend-dark"></i>新用户</span>
          </div>
        </div>

        <div class="panel chart-panel">
          <div class="panel-title">
            <h2>请求来源</h2>
            <span>按小程序页面来源统计</span>
          </div>
          <div class="source-list">
            <div v-for="source in sources" :key="source.source" class="source-item">
              <div class="source-label">
                <strong>{{ sourceName(source.source) }}</strong>
                <span>{{ number(source.visits) }} 次</span>
              </div>
              <div class="source-bar">
                <i :style="{ width: `${sourcePercent(source.visits)}%` }"></i>
              </div>
            </div>
            <el-empty v-if="!sources.length" description="暂无来源数据" :image-size="80" />
          </div>
        </div>
      </div>

      <div class="table-grid">
        <div class="panel">
          <div class="panel-title">
            <h2>接口热度</h2>
            <span>最近 {{ days }} 天</span>
          </div>
          <el-table :data="paths" size="small">
            <el-table-column prop="path" label="接口" min-width="240" show-overflow-tooltip />
            <el-table-column prop="visits" label="访问" width="86" />
            <el-table-column prop="users" label="访客" width="86" />
            <el-table-column prop="avgCostMs" label="平均耗时" width="100">
              <template #default="{ row }">{{ number(row.avgCostMs) }}ms</template>
            </el-table-column>
          </el-table>
        </div>

        <div class="panel">
          <div class="panel-title">
            <h2>最近访问</h2>
            <span>实时记录</span>
          </div>
          <el-table :data="recent" size="small">
            <el-table-column prop="visitedAt" label="时间" width="150" />
            <el-table-column label="来源" width="128" show-overflow-tooltip>
              <template #default="{ row }">{{ sourceName(row.source) }}</template>
            </el-table-column>
            <el-table-column prop="path" label="目标接口" min-width="180" show-overflow-tooltip />
            <el-table-column prop="statusCode" label="状态" width="72" />
            <el-table-column label="新用户" width="76">
              <template #default="{ row }">
                <el-tag v-if="isTrue(row.isNew)" size="small" type="success">是</el-tag>
                <span v-else>否</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { http } from '../api/http'

const loading = ref(false)
const days = ref(30)
const summary = ref({
  today: {},
  week: {},
  month: {}
})
const trend = ref([])
const sources = ref([])
const paths = ref([])
const recent = ref([])

const maxTrendValue = computed(() => Math.max(1, ...trend.value.flatMap((item) => [
  Number(item.visits || 0),
  Number(item.newUsers || 0)
])))

const trendPoints = computed(() => {
  const count = Math.max(trend.value.length - 1, 1)
  return trend.value.map((item, index) => {
    const x = 28 + (672 * index) / count
    return {
      label: item.label,
      x,
      visitsY: y(Number(item.visits || 0)),
      newUsersY: y(Number(item.newUsers || 0))
    }
  })
})

const maxSourceVisits = computed(() => Math.max(1, ...sources.value.map((item) => Number(item.visits || 0))))

async function load() {
  loading.value = true
  try {
    const data = await http.get('/api/admin/monitoring/overview', {
      params: {
        days: days.value
      }
    })
    summary.value = {
      today: data.today || {},
      week: data.week || {},
      month: data.month || {}
    }
    trend.value = data.trend || []
    sources.value = data.sources || []
    paths.value = data.paths || []
    recent.value = data.recent || []
  } finally {
    loading.value = false
  }
}

function y(value) {
  return 222 - (value / maxTrendValue.value) * 188
}

function linePoints(key) {
  return trendPoints.value.map((item) => `${item.x},${key === 'visits' ? item.visitsY : item.newUsersY}`).join(' ')
}

function sourcePercent(value) {
  return Math.max(4, Math.round((Number(value || 0) / maxSourceVisits.value) * 100))
}

function number(value) {
  const num = Number(value || 0)
  return Number.isInteger(num) ? num.toLocaleString() : num.toFixed(0)
}

function sourceName(value) {
  const map = {
    'pages/index/index': '首页',
    'pages/category/category': '分类页',
    'pages/search/search': '搜索页',
    'pages/detail/detail': '详情页',
    'pages/mine/mine': '我的页',
    app: '启动阶段',
    unknown: '未知来源'
  }
  return map[value] || value || '未知来源'
}

function isTrue(value) {
  return value === true || value === 1 || value === '1' || value === 'Y' || value === 'true'
}

onMounted(load)
</script>

<style scoped>
.compact {
  margin: 0;
}

.monitoring {
  display: grid;
  gap: 18px;
}

.metric-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric {
  min-height: 132px;
  padding: 20px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.metric span,
.metric em,
.panel-title span {
  color: var(--text-muted);
  font-size: 13px;
  font-style: normal;
}

.metric strong {
  color: var(--text-main);
  font-size: 34px;
  line-height: 1;
}

.chart-grid,
.table-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(360px, 0.65fr);
  gap: 18px;
}

.chart-panel {
  min-height: 360px;
}

.panel-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-title h2 {
  margin: 0;
  font-size: 18px;
}

.line-chart {
  width: 100%;
  height: 260px;
  background: linear-gradient(180deg, #fff 0%, #fff8f8 100%);
  border-radius: 8px;
}

.axis {
  stroke: #e8e8e8;
  stroke-width: 1;
}

.line {
  fill: none;
  stroke-width: 3;
  stroke-linejoin: round;
  stroke-linecap: round;
}

.visits-line {
  stroke: var(--brand-red);
}

.new-line {
  stroke: #222;
}

.visits-dot {
  fill: var(--brand-red);
}

.new-dot {
  fill: #222;
}

.legend {
  display: flex;
  gap: 18px;
  margin-top: 12px;
  color: var(--text-muted);
  font-size: 13px;
}

.legend i {
  width: 18px;
  height: 3px;
  display: inline-block;
  margin-right: 6px;
  vertical-align: middle;
  border-radius: 3px;
}

.legend-red {
  background: var(--brand-red);
}

.legend-dark {
  background: #222;
}

.source-list {
  display: grid;
  gap: 16px;
}

.source-label {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  color: var(--text-main);
}

.source-label span {
  color: var(--text-muted);
}

.source-bar {
  height: 10px;
  border-radius: 999px;
  overflow: hidden;
  background: #f2f2f2;
}

.source-bar i {
  height: 100%;
  display: block;
  border-radius: inherit;
  background: var(--brand-red);
  transition: width 0.28s ease;
}

@media (max-width: 1180px) {
  .metric-row,
  .chart-grid,
  .table-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 760px) {
  .metric-row,
  .chart-grid,
  .table-grid {
    grid-template-columns: 1fr;
  }
}
</style>

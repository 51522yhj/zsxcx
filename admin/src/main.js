import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as Icons from '@element-plus/icons-vue'
import App from './App.vue'
import './assets/styles.css'

const app = createApp(App)
Object.entries(Icons).forEach(([name, component]) => app.component(name, component))
app.use(ElementPlus)
app.mount('#app')


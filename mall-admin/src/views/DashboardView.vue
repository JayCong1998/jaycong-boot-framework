<script setup lang="ts">
import { computed, ref } from 'vue'
import { Plus, Right, InfoFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import SalesChart from '@/components/SalesChart.vue'
import { metrics, todos, orders, bestsellers } from '@/data/dashboard'

const auth = useAuthStore()
const days = ref(7)
const date = new Intl.DateTimeFormat('sv-SE').format(new Date())
const greeting = computed(() =>
  new Date().getHours() < 12
    ? '早上好'
    : new Date().getHours() < 18
      ? '下午好'
      : '晚上好',
)
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <h1>工作台</h1>
        <p>
          {{ greeting }}，{{ auth.user?.nickname }}。今天也让好生意持续发生。
        </p>
      </div>
      <div class="heading-actions">
        <span class="date-label">{{ date }}</span
        ><el-button
          type="primary"
          :icon="Plus"
          @click="$router.push('/products')"
          >商品管理</el-button
        >
      </div>
    </div>
    <div class="demo-banner">
      <el-icon><InfoFilled /></el-icon><strong>示例工作台</strong
      ><span>当前经营数据仅用于页面展示；商品和会员管理已接入，订单管理尚未接入。</span
      ><router-link to="/settings"
        >账号信息 <el-icon><Right /></el-icon
      ></router-link>
    </div>
    <div class="metric-grid">
      <article
        v-for="metric in metrics"
        :key="metric.title"
        class="panel metric-card"
      >
        <div>
          <span class="muted">{{ metric.title }}</span>
          <h2>{{ metric.value }}</h2>
          <p :class="metric.up ? 'change-up' : 'change-down'">
            较昨日 {{ metric.up ? '↑' : '↓' }} {{ metric.change }}
          </p>
        </div>
        <span class="metric-icon" :class="metric.color"
          ><el-icon><component :is="metric.icon" /></el-icon
        ></span>
      </article>
    </div>
    <div class="dashboard-grid">
      <section class="panel trend-panel">
        <div class="panel-heading">
          <div>
            <h2>销售趋势</h2>
            <p>成交金额走势 · 示例数据</p>
          </div>
          <el-radio-group v-model="days" size="small"
            ><el-radio-button :value="7">近7天</el-radio-button
            ><el-radio-button :value="30"
              >近30天</el-radio-button
            ></el-radio-group
          >
        </div>
        <SalesChart :days="days" />
      </section>
      <section class="panel todo-panel">
        <div class="panel-heading">
          <h2>待办事项</h2>
          <router-link to="/orders">查看全部</router-link>
        </div>
        <div class="todo-grid">
          <router-link
            v-for="todo in todos"
            :key="todo.title"
            :to="{ path: todo.path, query: { status: todo.status } }"
            class="todo-card"
            ><span>{{ todo.title }}</span>
            <div :class="`text-${todo.color}`">
              <strong>{{ todo.value }}</strong
              ><el-icon><Right /></el-icon></div
          ></router-link>
        </div>
      </section>
      <section class="panel order-panel">
        <div class="panel-heading">
          <h2>最新订单</h2>
          <router-link to="/orders"
            >全部订单 <el-icon><Right /></el-icon
          ></router-link>
        </div>
        <el-table :data="[...orders]" class="orders-table"
          ><el-table-column
            prop="id"
            label="订单编号"
            min-width="160"
          /><el-table-column
            prop="product"
            label="商品 / 数量"
            min-width="185"
          /><el-table-column label="实付金额" min-width="112"
            ><template #default="{ row }"
              ><strong>¥ {{ row.amount }}</strong></template
            ></el-table-column
          ><el-table-column label="订单状态" min-width="100"
            ><template #default="{ row }"
              ><el-tag :type="row.type" effect="light" size="small">{{
                row.status
              }}</el-tag></template
            ></el-table-column
          ></el-table
        >
      </section>
      <section class="panel bestsellers-panel">
        <div class="panel-heading">
          <h2>热销商品</h2>
          <span class="muted">本周 · 示例</span>
        </div>
        <div
          v-for="(product, index) in bestsellers"
          :key="product.name"
          class="bestseller"
        >
          <span class="rank" :class="{ first: index === 0 }"
            >0{{ index + 1 }}</span
          ><span class="product-icon" :class="product.color"
            ><el-icon><component :is="product.icon" /></el-icon
          ></span>
          <div class="product-info">
            <strong>{{ product.name }}</strong
            ><small>{{ product.category }}</small>
          </div>
          <div class="product-sales">
            <strong>{{ product.sales }}</strong
            ><small>销量</small>
          </div>
        </div>
      </section>
    </div>
  </section>
</template>

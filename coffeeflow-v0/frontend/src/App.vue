<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

type Status = 'NEW' | 'IN_PROGRESS' | 'READY' | 'COMPLETED' | 'CANCELLED'
interface Order {
  id: string
  storeName: string
  customerName: string
  productCode: string
  drinkName: string
  items: string[]
  size?: string
  quantity: number
  status: Status
  createdAt: string
  estimatedReadyAt: string
  note: string
}

const orders = ref<Order[]>([])
const selected = ref<Order | null>(null)
const keyword = ref('')
const status = ref('ALL')
const store = ref('ALL')
const loading = ref(true)
const error = ref('')
const operationError = ref('')
const updating = ref('')

const labels: Record<Status, string> = {
  NEW: '待制作',
  IN_PROGRESS: '制作中',
  READY: '待取餐',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}
const next: Partial<Record<Status, Status>> = {
  NEW: 'IN_PROGRESS',
  IN_PROGRESS: 'READY',
  READY: 'COMPLETED',
}
const stores = computed(() => [...new Set(orders.value.map(order => order.storeName))])
const visible = computed(() => orders.value.filter(order => {
  const query = keyword.value.trim().toLowerCase()
  return (!query || [order.id, order.customerName, order.drinkName]
    .some(value => value.toLowerCase().includes(query)))
    && (status.value === 'ALL' || order.status === status.value)
    && (store.value === 'ALL' || order.storeName === store.value)
}))
const summary = computed(() => ({
  total: orders.value.length,
  overdue: orders.value.filter(order =>
    !['COMPLETED', 'CANCELLED'].includes(order.status)
    && new Date(order.estimatedReadyAt).getTime() < Date.now()).length,
  brewing: orders.value.filter(order => order.status === 'IN_PROGRESS').length,
  ready: orders.value.filter(order => order.status === 'READY').length,
}))

async function json<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init)
  if (!response.ok) {
    let message = `请求失败（${response.status}）`
    try {
      const body = await response.json() as { message?: string }
      if (body.message) message = body.message
    } catch {
      // Keep the status-based fallback when the response is not JSON.
    }
    throw new Error(message)
  }
  return response.json() as Promise<T>
}
async function load() {
  loading.value = true
  error.value = ''
  operationError.value = ''
  try {
    const result = await json<{ orders: Order[] }>('/api/v1/orders')
    orders.value = result.orders
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '订单加载失败'
  } finally {
    loading.value = false
  }
}
async function openDetail(id: string) {
  operationError.value = ''
  try {
    selected.value = await json<Order>(`/api/v1/orders/${id}`)
  } catch (reason) {
    operationError.value = reason instanceof Error ? reason.message : '详情加载失败'
  }
}
async function advance(order: Order) {
  const target = next[order.status]
  if (!target) return
  updating.value = order.id
  operationError.value = ''
  try {
    const updated = await json<Order>(`/api/v1/orders/${order.id}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: target }),
    })
    orders.value = orders.value.map(item => item.id === updated.id ? updated : item)
    if (selected.value?.id === updated.id) selected.value = updated
  } catch (reason) {
    operationError.value = reason instanceof Error ? reason.message : '订单状态更新失败'
  } finally {
    updating.value = ''
  }
}
function reset() {
  keyword.value = ''
  status.value = 'ALL'
  store.value = 'ALL'
}
onMounted(load)
</script>

<template>
  <main>
    <header>
      <p class="eyebrow">COFFEEFLOW · V0 ORDER BOARD</p>
      <h1>门店订单看板</h1>
      <p>查看、筛选并推进现有门店订单。</p>
    </header>
    <section class="summary">
      <article class="panel"><span>总订单</span><b>{{ summary.total }}</b></article>
      <article class="panel"><span>超时订单</span><b>{{ summary.overdue }}</b></article>
      <article class="panel"><span>制作中</span><b>{{ summary.brewing }}</b></article>
      <article class="panel"><span>待取餐</span><b>{{ summary.ready }}</b></article>
    </section>
    <section class="panel filters">
      <input v-model="keyword" data-testid="keyword-filter" placeholder="订单号 / 顾客 / 饮品">
      <select v-model="status" data-testid="status-filter">
        <option value="ALL">全部状态</option>
        <option v-for="(_, value) in labels" :key="value" :value="value">{{ labels[value] }}</option>
      </select>
      <select v-model="store" data-testid="store-filter">
        <option value="ALL">全部门店</option>
        <option v-for="name in stores" :key="name">{{ name }}</option>
      </select>
      <button class="ghost" @click="reset">重置</button>
    </section>
    <p v-if="operationError" class="panel error" role="alert">
      {{ operationError }}
      <button @click="operationError = ''">关闭</button>
    </p>
    <p v-if="loading" class="panel">订单数据加载中</p>
    <p v-else-if="error" class="panel error">{{ error }} <button @click="load">重试</button></p>
    <section v-else class="orders">
      <article
        v-for="order in visible"
        :key="order.id"
        class="panel order"
        :data-testid="`order-card-${order.id}`"
        @click="openDetail(order.id)"
      >
        <div><small>{{ order.id }} · {{ order.storeName }}</small><h2>{{ order.customerName }}</h2></div>
        <span class="badge">{{ labels[order.status] }}</span>
        <p>{{ order.drinkName }} · {{ order.quantity }} 杯</p>
        <p>{{ order.note }}</p>
        <button v-if="next[order.status]" class="primary" :disabled="updating === order.id" @click.stop="advance(order)">
          {{ updating === order.id ? '更新中…' : '推进状态' }}
        </button>
      </article>
      <p v-if="!visible.length" class="panel">当前筛选条件下没有订单</p>
    </section>
    <div v-if="selected" class="drawer-layer" @click.self="selected = null">
      <aside class="panel drawer">
        <button class="close" @click="selected = null">关闭</button>
        <p class="eyebrow">订单详情</p>
        <h2>{{ selected.id }} · {{ selected.customerName }}</h2>
        <img :src="`/drinks/${selected.productCode}.png`" :alt="selected.drinkName">
        <dl>
          <dt>饮品</dt><dd>{{ selected.drinkName }}</dd>
          <dt>商品明细</dt><dd>{{ selected.items.join('、') }}</dd>
          <dt>规格</dt><dd>{{ selected.size }}</dd>
          <dt>门店</dt><dd>{{ selected.storeName }}</dd>
          <dt>状态</dt><dd>{{ labels[selected.status] }}</dd>
          <dt>备注</dt><dd>{{ selected.note }}</dd>
        </dl>
      </aside>
    </div>
  </main>
</template>

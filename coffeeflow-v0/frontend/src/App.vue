<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

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
// 取餐码查询 / 取消返回顾客侧订单详情：明细为对象数组，金额为 totalAmount
interface PickupItem {
  productCode?: string
  drinkName?: string
  size?: string
  quantity?: number
  unitPrice?: number
  subtotal?: number
}
interface PickupOrder {
  orderId: string
  pickupCode?: string
  storeName: string
  customerName: string
  status: Status
  totalAmount?: number
  note?: string
  items?: (PickupItem | string)[]
}
interface Store { storeId: string; storeName: string }
interface Product { productCode: string; name: string; price: number; available: boolean }
interface NewOrderResult { orderId: string; pickupCode: string; totalAmount: number; status: Status }
interface CartLine { productCode: string; name: string; price: number; size: string; quantity: number }

// 规格只作为展示与明细属性，不参与金额计算（design D6）
const sizes = ['小杯', '中杯', '大杯']
const defaultSize = '中杯'

// 视图切换：默认停留在门店看板，不引入 vue-router（design D8）
const tab = ref<'board' | 'customer'>('board')

// ---------- 门店看板（V0 既有行为保持不变） ----------
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

// ---------- 门店列表（顾客端与停售管理共用，按需加载） ----------
const storeList = ref<Store[]>([])
const storeListError = ref('')
const storeListLoading = ref(false)

// ---------- 门店看板侧：商品停售 / 恢复 ----------
const stockPanelOpen = ref(false)
const stockStoreId = ref('')
const stockProducts = ref<Product[]>([])
const stockLoading = ref(false)
const stockUpdating = ref('')
const stockError = ref('')

// ---------- 顾客端：门店选择与商品列表 ----------
const customerStoreId = ref('')
const products = ref<Product[]>([])
const productsLoading = ref(false)
const productsError = ref('')
const customerError = ref('')
const cart = ref<CartLine[]>([])
const form = reactive({ customerName: '', phoneLast4: '', note: '' })
const submitting = ref(false)
const placed = ref<NewOrderResult | null>(null)

// ---------- 顾客端：取餐码查询与取消 ----------
const pickupCode = ref('')
const pickupOrder = ref<PickupOrder | null>(null)
const pickupLoading = ref(false)
const pickupError = ref('')
const cancelPhone = ref('')
const cancelError = ref('')
const cancelling = ref(false)

const cartTotal = computed(() =>
  cart.value.reduce((total, line) => total + line.price * line.quantity, 0))

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
function reasonText(reason: unknown, fallback: string) {
  return reason instanceof Error && reason.message ? reason.message : fallback
}
function money(value?: number) {
  return typeof value === 'number' ? value.toFixed(2) : '—'
}
// 明细统一成一行展示文案（后端返回明细对象，兼容纯名称数组）
function itemText(item: PickupItem | string) {
  if (typeof item === 'string') return item
  const name = item.drinkName ?? item.productCode ?? ''
  return `${name}（${item.size ?? '—'}）× ${item.quantity ?? 0} · ￥${money(item.subtotal)}`
}
async function load() {
  loading.value = true
  error.value = ''
  operationError.value = ''
  try {
    const result = await json<{ orders: Order[] }>('/api/v1/orders')
    orders.value = result.orders
  } catch (reason) {
    error.value = reasonText(reason, '订单加载失败')
  } finally {
    loading.value = false
  }
}
async function openDetail(id: string) {
  operationError.value = ''
  try {
    selected.value = await json<Order>(`/api/v1/orders/${id}`)
  } catch (reason) {
    operationError.value = reasonText(reason, '详情加载失败')
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
    operationError.value = reasonText(reason, '订单状态更新失败')
  } finally {
    updating.value = ''
  }
}
function reset() {
  keyword.value = ''
  status.value = 'ALL'
  store.value = 'ALL'
}

async function ensureStoreList() {
  if (storeList.value.length) return
  storeListLoading.value = true
  storeListError.value = ''
  try {
    const result = await json<{ stores: Store[] }>('/api/v1/stores')
    storeList.value = result.stores ?? []
  } catch (reason) {
    storeListError.value = reasonText(reason, '门店列表加载失败')
  } finally {
    storeListLoading.value = false
  }
}
async function switchTab(target: 'board' | 'customer') {
  tab.value = target
  if (target === 'customer') await ensureStoreList()
}

async function toggleStockPanel() {
  stockPanelOpen.value = !stockPanelOpen.value
  if (!stockPanelOpen.value) return
  await ensureStoreList()
  if (!storeList.value.length) return
  // 默认对齐看板当前筛选的门店，未指定时取门店列表第一项
  const matched = storeList.value.find(item => item.storeName === store.value)
  const target = matched ?? storeList.value[0]
  stockStoreId.value = target.storeId
  await loadStockProducts()
}
async function loadStockProducts() {
  stockError.value = ''
  if (!stockStoreId.value) {
    stockProducts.value = []
    return
  }
  stockLoading.value = true
  try {
    const result = await json<{ products: Product[] }>(`/api/v1/stores/${stockStoreId.value}/products`)
    stockProducts.value = result.products ?? []
  } catch (reason) {
    stockProducts.value = []
    stockError.value = reasonText(reason, '商品列表加载失败')
  } finally {
    stockLoading.value = false
  }
}
async function toggleAvailability(product: Product) {
  stockUpdating.value = product.productCode
  stockError.value = ''
  try {
    const updated = await json<Product>(
      `/api/v1/stores/${stockStoreId.value}/products/${product.productCode}/availability`,
      {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ available: !product.available }),
      })
    stockProducts.value = stockProducts.value
      .map(item => item.productCode === updated.productCode ? updated : item)
    // 顾客端正停在当前门店时同步刷新，保证「恢复后立即可选」
    if (customerStoreId.value === stockStoreId.value && products.value.length) {
      await loadCustomerProducts()
    }
  } catch (reason) {
    stockError.value = reasonText(reason, '停售状态更新失败')
    await loadStockProducts()
  } finally {
    stockUpdating.value = ''
  }
}

async function loadCustomerProducts() {
  productsError.value = ''
  productsLoading.value = true
  try {
    const result = await json<{ products: Product[] }>(`/api/v1/stores/${customerStoreId.value}/products`)
    products.value = result.products ?? []
  } catch (reason) {
    products.value = []
    productsError.value = reasonText(reason, '商品列表加载失败')
  } finally {
    productsLoading.value = false
  }
}
async function selectCustomerStore() {
  // 切换门店后清空购物车与下单结果，避免提交其他门店的商品
  cart.value = []
  placed.value = null
  customerError.value = ''
  productsError.value = ''
  if (!customerStoreId.value) {
    products.value = []
    return
  }
  await loadCustomerProducts()
}
function addToCart(product: Product) {
  if (!product.available) return
  const existing = cart.value.find(line =>
    line.productCode === product.productCode && line.size === defaultSize)
  if (existing) {
    existing.quantity += 1
    return
  }
  cart.value.push({
    productCode: product.productCode,
    name: product.name,
    price: product.price,
    size: defaultSize,
    quantity: 1,
  })
}
function changeQuantity(line: CartLine, delta: number) {
  const quantity = line.quantity + delta
  if (quantity < 1) {
    removeLine(line)
    return
  }
  line.quantity = quantity
}
function removeLine(line: CartLine) {
  cart.value = cart.value.filter(item =>
    !(item.productCode === line.productCode && item.size === line.size))
}
async function submitOrder() {
  customerError.value = ''
  if (!customerStoreId.value) {
    customerError.value = '请先选择取餐门店'
    return
  }
  if (!form.customerName.trim()) {
    customerError.value = '请填写顾客姓名'
    return
  }
  if (!/^\d{4}$/.test(form.phoneLast4)) {
    customerError.value = '请填写手机号后四位（4 位数字）'
    return
  }
  if (!cart.value.length) {
    customerError.value = '请先选择商品'
    return
  }
  submitting.value = true
  try {
    const result = await json<NewOrderResult>('/api/v1/orders', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        storeId: customerStoreId.value,
        customerName: form.customerName.trim(),
        phoneLast4: form.phoneLast4,
        items: cart.value.map(line => ({
          productCode: line.productCode,
          size: line.size,
          quantity: line.quantity,
        })),
        note: form.note,
      }),
    })
    placed.value = result
    pickupCode.value = result.pickupCode
    cart.value = []
    form.customerName = ''
    form.phoneLast4 = ''
    form.note = ''
    // 下单成功后刷新门店看板，新订单立即出现在列表中
    await load()
  } catch (reason) {
    customerError.value = reasonText(reason, '下单失败')
  } finally {
    submitting.value = false
  }
}

async function queryPickup() {
  pickupError.value = ''
  cancelError.value = ''
  cancelPhone.value = ''
  const code = pickupCode.value.trim()
  if (!code) {
    pickupError.value = '请输入取餐码'
    return
  }
  pickupLoading.value = true
  try {
    pickupOrder.value = await json<PickupOrder>(`/api/v1/orders/pickup/${code}`)
  } catch (reason) {
    pickupOrder.value = null
    const message = reasonText(reason, '取餐码查询失败')
    pickupError.value = message === '请求失败（404）'
      ? '未找到该取餐码对应的当日订单，请核对后重试'
      : message
  } finally {
    pickupLoading.value = false
  }
}
async function cancelOrder() {
  cancelError.value = ''
  if (!pickupOrder.value) return
  if (!/^\d{4}$/.test(cancelPhone.value)) {
    cancelError.value = '请填写手机号后四位（4 位数字）'
    return
  }
  cancelling.value = true
  try {
    // 取消需要同时提供取餐码与手机号后四位（双因子校验）
    const updated = await json<PickupOrder>(`/api/v1/orders/${pickupOrder.value.orderId}/cancel`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        pickupCode: pickupOrder.value.pickupCode ?? pickupCode.value.trim(),
        phoneLast4: cancelPhone.value,
      }),
    })
    pickupOrder.value = updated
    cancelPhone.value = ''
    await load()
  } catch (reason) {
    cancelError.value = reasonText(reason, '取消失败')
  } finally {
    cancelling.value = false
  }
}

onMounted(load)
</script>

<template>
  <main>
    <header>
      <p class="eyebrow">COFFEEFLOW · V0 ORDER BOARD</p>
      <h1>{{ tab === 'board' ? '门店订单看板' : '顾客点单' }}</h1>
      <p>{{ tab === 'board' ? '查看、筛选并推进现有门店订单。' : '选择门店与商品下单，凭取餐码查询或取消订单。' }}</p>
    </header>
    <nav class="tabs">
      <button data-testid="tab-board" :class="{ active: tab === 'board' }" @click="switchTab('board')">门店看板</button>
      <button data-testid="tab-customer" :class="{ active: tab === 'customer' }" @click="switchTab('customer')">顾客点单</button>
    </nav>
    <template v-if="tab === 'board'">
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
          <button v-if="next[order.status]" class="primary" :data-testid="`advance-${order.id}`" :disabled="updating === order.id" @click.stop="advance(order)">
            {{ updating === order.id ? '更新中…' : '推进状态' }}
          </button>
        </article>
        <p v-if="!visible.length" class="panel">当前筛选条件下没有订单</p>
      </section>
      <section class="panel stock">
        <div class="stock-head">
          <h2>商品停售管理</h2>
          <button class="ghost" data-testid="stock-panel-toggle" @click="toggleStockPanel">
            {{ stockPanelOpen ? '收起' : '展开' }}
          </button>
        </div>
        <template v-if="stockPanelOpen">
          <p>停售只作用于所选门店，不影响其他门店。</p>
          <select v-model="stockStoreId" data-testid="stock-store-select" @change="loadStockProducts">
            <option v-for="item in storeList" :key="item.storeId" :value="item.storeId">{{ item.storeName }}</option>
          </select>
          <p v-if="storeListError" class="error" data-testid="store-list-error">{{ storeListError }}</p>
          <p v-if="stockError" class="error" data-testid="stock-error">{{ stockError }}</p>
          <p v-if="stockLoading" class="stock-row">商品加载中…</p>
          <div v-for="product in stockProducts" :key="product.productCode" class="stock-row" :data-testid="`stock-product-${product.productCode}`">
            <span>{{ product.name }} · ￥{{ money(product.price) }}</span>
            <label>
              <input type="checkbox" :checked="product.available" :disabled="stockUpdating === product.productCode" :data-testid="`product-availability-${product.productCode}`" @change="toggleAvailability(product)">
              {{ product.available ? '可售' : '已停售' }}
            </label>
          </div>
        </template>
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
    </template>
    <template v-else>
      <section class="panel">
        <h2>1. 选择取餐门店</h2>
        <label class="field">
          <span>取餐门店</span>
          <select v-model="customerStoreId" data-testid="customer-store-select" @change="selectCustomerStore">
            <option value="">请选择门店</option>
            <option v-for="item in storeList" :key="item.storeId" :value="item.storeId">{{ item.storeName }}</option>
          </select>
        </label>
        <p v-if="storeListLoading" class="muted">门店列表加载中…</p>
        <p v-else-if="storeListError" class="error" data-testid="store-list-error">
          {{ storeListError }}
          <button class="ghost" @click="ensureStoreList">重试</button>
        </p>
      </section>
      <section class="product-grid">
        <p v-if="!customerStoreId" class="panel">请先选择门店，再挑选商品。</p>
        <p v-else-if="productsLoading" class="panel">商品加载中…</p>
        <p v-else-if="productsError" class="panel error" data-testid="products-error">{{ productsError }}</p>
        <p v-else-if="!products.length" class="panel">该门店暂无可展示商品。</p>
        <article v-for="product in products" :key="product.productCode" class="panel product" :class="{ unavailable: !product.available }" :data-testid="`product-card-${product.productCode}`">
          <img :src="`/drinks/${product.productCode}.png`" :alt="product.name">
          <h3>{{ product.name }}</h3>
          <p>单价 ￥{{ money(product.price) }}</p>
          <span v-if="!product.available" class="badge" :data-testid="`unavailable-${product.productCode}`">不可售</span>
          <button class="primary" :data-testid="`add-to-cart-${product.productCode}`" :disabled="!product.available" @click="addToCart(product)">
            {{ product.available ? '加入购物车' : '不可售' }}
          </button>
        </article>
      </section>
      <section class="panel cart">
        <h2>2. 购物车</h2>
        <p v-if="!cart.length">购物车还是空的。</p>
        <div v-for="line in cart" :key="`${line.productCode}-${line.size}`" class="cart-line" :data-testid="`cart-line-${line.productCode}`">
          <span>{{ line.name }}</span>
          <select v-model="line.size" :data-testid="`cart-size-${line.productCode}`">
            <option v-for="size in sizes" :key="size">{{ size }}</option>
          </select>
          <button class="ghost" :data-testid="`cart-minus-${line.productCode}`" @click="changeQuantity(line, -1)">−</button>
          <b :data-testid="`cart-quantity-${line.productCode}`">{{ line.quantity }}</b>
          <button class="ghost" :data-testid="`cart-plus-${line.productCode}`" @click="changeQuantity(line, 1)">＋</button>
          <span>￥{{ money(line.price * line.quantity) }}</span>
          <button class="ghost" :data-testid="`cart-remove-${line.productCode}`" @click="removeLine(line)">移除</button>
        </div>
        <p>合计金额：<b data-testid="cart-total">￥{{ money(cartTotal) }}</b></p>
      </section>
      <section class="panel">
        <h2>3. 填写下单信息</h2>
        <label class="field">
          <span>顾客姓名</span>
          <input v-model="form.customerName" data-testid="customer-name" placeholder="顾客姓名">
        </label>
        <label class="field">
          <span>手机号后四位</span>
          <input v-model="form.phoneLast4" data-testid="customer-phone" maxlength="4" placeholder="如 8888">
        </label>
        <label class="field">
          <span>备注</span>
          <input v-model="form.note" data-testid="order-note" placeholder="如：少冰">
        </label>
        <button class="primary" data-testid="submit-order" :disabled="submitting" @click="submitOrder">
          {{ submitting ? '提交中…' : '提交订单' }}
        </button>
        <p v-if="customerError" class="error" data-testid="customer-error">{{ customerError }}</p>
        <div v-if="placed" class="result" data-testid="order-result">
          <h3>下单成功</h3>
          <p>订单号：{{ placed.orderId }}</p>
          <p>订单金额：<b data-testid="placed-total">￥{{ money(placed.totalAmount) }}</b></p>
          <p>取餐码：<b data-testid="placed-pickup-code">{{ placed.pickupCode }}</b></p>
        </div>
      </section>
      <section class="panel pickup">
        <h2>4. 取餐码查询</h2>
        <div class="field">
          <span>取餐码</span>
          <input v-model="pickupCode" data-testid="pickup-code-input" placeholder="如 1001">
          <button class="ghost" data-testid="query-pickup" @click="queryPickup">查询</button>
        </div>
        <p v-if="pickupLoading">查询中…</p>
        <p v-else-if="pickupError" class="error" data-testid="pickup-error">{{ pickupError }}</p>
        <div v-else-if="pickupOrder" data-testid="pickup-result">
          <h3>{{ pickupOrder.orderId }} · {{ pickupOrder.storeName }}</h3>
          <p>状态：<b data-testid="pickup-status">{{ labels[pickupOrder.status] }}</b></p>
          <p>顾客：{{ pickupOrder.customerName }}</p>
          <ul class="pickup-items">
            <li v-for="(item, index) in pickupOrder.items ?? []" :key="index">{{ itemText(item) }}</li>
          </ul>
          <p>订单金额：<b data-testid="pickup-total">￥{{ money(pickupOrder.totalAmount) }}</b></p>
          <p>备注：{{ pickupOrder.note || '无' }}</p>
          <template v-if="pickupOrder.status === 'NEW'">
            <div class="field">
              <span>手机号后四位</span>
              <input v-model="cancelPhone" data-testid="cancel-phone" maxlength="4" placeholder="如 8888">
              <button class="ghost" data-testid="cancel-order" :disabled="cancelling" @click="cancelOrder">
                {{ cancelling ? '取消中…' : '取消订单' }}
              </button>
            </div>
            <p v-if="cancelError" class="error" data-testid="cancel-error">{{ cancelError }}</p>
          </template>
          <p v-else class="muted">仅「待制作」状态的订单可以取消，当前状态不可取消。</p>
        </div>
        <p v-else class="muted">输入取餐码查询当日订单的最新状态。</p>
      </section>
    </template>
  </main>
</template>

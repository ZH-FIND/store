import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App.vue'

const orders = [
  { id: 'CF-1001', storeName: '国贸店', customerName: 'Alice', productCode: 'CF-BEV-001',
    drinkName: '燕麦拿铁', items: ['燕麦拿铁'], size: '大杯', quantity: 1, status: 'NEW',
    createdAt: '2020-09-10T09:00:00', estimatedReadyAt: '2020-09-10T09:10:00', note: '先做燕麦拿铁' },
  { id: 'CF-1002', storeName: '望京店', customerName: 'Bob', productCode: 'CF-BEV-002',
    drinkName: '美式', items: ['美式', '冷萃'], size: '中杯', quantity: 2, status: 'READY',
    createdAt: '2020-09-10T09:00:00', estimatedReadyAt: '2020-09-10T09:10:00', note: '柜台自取' },
]

function response(body: unknown, ok = true, status = 200) {
  return { ok, status, json: async () => body }
}

function successfulFetch() {
  return vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input)
    if (init?.method === 'PATCH') return response({ ...orders[0], status: 'IN_PROGRESS' })
    if (url.endsWith('/orders')) return response({ total: 2, orders })
    return response(url.endsWith('CF-1002') ? orders[1] : orders[0])
  })
}

async function flush() {
  await Promise.resolve()
  await Promise.resolve()
  await nextTick()
  await new Promise(resolve => setTimeout(resolve, 0))
  await nextTick()
}

afterEach(() => vi.unstubAllGlobals())

describe('CoffeeFlow V0', () => {
  it('展示汇总和订单列表', async () => {
    vi.stubGlobal('fetch', successfulFetch())
    const wrapper = mount(App)
    await flush()

    expect(wrapper.text()).toContain('门店订单看板')
    expect(wrapper.findAll('.summary .panel').map(panel => panel.text())).toEqual([
      '总订单2', '超时订单2', '制作中0', '待取餐1',
    ])
    expect(wrapper.text()).toContain('CF-1001')
    expect(wrapper.text()).toContain('CF-1002')
  })

  it('支持筛选和重置', async () => {
    vi.stubGlobal('fetch', successfulFetch())
    const wrapper = mount(App)
    await flush()

    await wrapper.get('[data-testid="keyword-filter"]').setValue('alice')
    expect(wrapper.text()).toContain('CF-1001')
    expect(wrapper.text()).not.toContain('CF-1002')
    await wrapper.get('[data-testid="status-filter"]').setValue('READY')
    expect(wrapper.text()).toContain('当前筛选条件下没有订单')
    await wrapper.get('.filters .ghost').trigger('click')
    expect(wrapper.text()).toContain('CF-1002')
  })

  it('打开详情并展示混合商品明细', async () => {
    const fetchMock = successfulFetch()
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(App)
    await flush()

    await wrapper.get('[data-testid="order-card-CF-1002"]').trigger('click')
    await flush()
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/orders/CF-1002', undefined)
    expect(wrapper.text()).toContain('美式、冷萃')
  })

  it('推进订单状态', async () => {
    const fetchMock = successfulFetch()
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(App)
    await flush()

    await wrapper.get('[data-testid="order-card-CF-1001"] .primary').trigger('click')
    await flush()
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/orders/CF-1001/status',
      expect.objectContaining({ method: 'PATCH', body: JSON.stringify({ status: 'IN_PROGRESS' }) }))
    expect(wrapper.text()).toContain('制作中')
  })

  it('列表加载失败时展示后端错误并可重试', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(response({ message: '数据库暂不可用' }, false, 503))
      .mockResolvedValueOnce(response({ total: 2, orders }))
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(App)
    await flush()

    expect(wrapper.text()).toContain('数据库暂不可用')
    await wrapper.get('.error button').trigger('click')
    await flush()
    expect(wrapper.text()).toContain('CF-1001')
  })

  it('状态更新失败时保留订单看板并显示错误', async () => {
    const fetchMock = successfulFetch()
    fetchMock.mockResolvedValueOnce(response({ total: 2, orders }))
      .mockResolvedValueOnce(response({}, false, 500))
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(App)
    await flush()

    await wrapper.get('[data-testid="order-card-CF-1001"] .primary').trigger('click')
    await flush()
    expect(wrapper.text()).toContain('请求失败（500）')
    expect(wrapper.text()).toContain('CF-1001')
  })
})

// ---------------- V1：顾客点单 / 取餐码查询与取消 / 门店停售 ----------------

const storeFixtures = [
  { storeId: 'S001', storeName: '国贸店' },
  { storeId: 'S002', storeName: '望京店' },
]
const productFixtures = [
  { productCode: 'CF-BEV-001', name: '燕麦拿铁', price: 32, available: true },
  { productCode: 'CF-BEV-002', name: '美式', price: 22, available: false },
]
function pickupFixture(status: string) {
  return {
    orderId: 'CF-1013', pickupCode: '1001', storeName: '国贸店', customerName: '顾客13',
    status, totalAmount: 64, note: '少冰',
    items: [{ productCode: 'CF-BEV-001', drinkName: '燕麦拿铁', size: '大杯', quantity: 2,
      unitPrice: 32, subtotal: 64 }],
  }
}

interface OrderingMockOptions {
  pickupStatus?: string
  pickupMissing?: boolean
  cancelFails?: boolean
}

function orderingFetch(options: OrderingMockOptions = {}) {
  const stock: Record<string, boolean> = {}
  productFixtures.forEach(product => { stock[product.productCode] = product.available })
  let pickupStatus = options.pickupStatus ?? 'NEW'
  return vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input)
    const method = init?.method ?? 'GET'
    if (url === '/api/v1/stores') return response({ stores: storeFixtures })
    const availability = url.match(/^\/api\/v1\/stores\/([^/]+)\/products\/([^/]+)\/availability$/)
    if (availability) {
      const code = availability[2]
      const body = JSON.parse(String(init?.body)) as { available: boolean }
      stock[code] = body.available
      const product = productFixtures.find(item => item.productCode === code) ?? productFixtures[0]
      return response({ ...product, available: body.available })
    }
    if (url.endsWith('/products')) {
      return response({
        products: productFixtures.map(item => ({ ...item, available: stock[item.productCode] })),
      })
    }
    if (url === '/api/v1/orders' && method === 'POST') {
      return response({ orderId: 'CF-1013', pickupCode: '1001', totalAmount: 64, status: 'NEW' })
    }
    if (url === '/api/v1/orders') return response({ total: 2, orders })
    if (url.startsWith('/api/v1/orders/pickup/')) {
      if (options.pickupMissing) return response({ message: '取餐码不存在或不属于当日订单' }, false, 404)
      return response(pickupFixture(pickupStatus))
    }
    if (url.endsWith('/cancel')) {
      if (options.cancelFails) return response({ message: '订单已开始制作，无法取消' }, false, 409)
      pickupStatus = 'CANCELLED'
      return response(pickupFixture('CANCELLED'))
    }
    return response(orders[0])
  })
}

// 打开顾客点单 Tab 并选定门店（S001）
async function openCustomer(options: OrderingMockOptions = {}) {
  const fetchMock = orderingFetch(options)
  vi.stubGlobal('fetch', fetchMock)
  const wrapper = mount(App)
  await flush()
  await wrapper.get('[data-testid="tab-customer"]').trigger('click')
  await flush()
  await wrapper.get('[data-testid="customer-store-select"]').setValue('S001')
  await flush()
  return { wrapper, fetchMock }
}

describe('CoffeeFlow V1 顾客点单', () => {
  it('默认停留在门店看板，可切换到顾客点单', async () => {
    const fetchMock = orderingFetch()
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(App)
    await flush()

    expect(wrapper.text()).toContain('门店订单看板')
    expect(wrapper.find('[data-testid="customer-store-select"]').exists()).toBe(false)

    await wrapper.get('[data-testid="tab-customer"]').trigger('click')
    await flush()
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/stores', undefined)
    expect(wrapper.find('[data-testid="customer-store-select"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="customer-store-select"]').findAll('option')).toHaveLength(3)

    await wrapper.get('[data-testid="tab-board"]').trigger('click')
    expect(wrapper.find('[data-testid="customer-store-select"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('门店订单看板')
  })

  it('选定门店后加载商品，停售商品可见但禁用', async () => {
    const { wrapper, fetchMock } = await openCustomer()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/stores/S001/products', undefined)
    expect(wrapper.get('[data-testid="product-card-CF-BEV-001"]').text()).toContain('燕麦拿铁')
    expect(wrapper.get('[data-testid="product-card-CF-BEV-001"]').text()).toContain('32.00')
    expect(wrapper.get('[data-testid="product-card-CF-BEV-001"] img').attributes('src'))
      .toBe('/drinks/CF-BEV-001.png')

    expect(wrapper.get('[data-testid="product-card-CF-BEV-002"]').text()).toContain('不可售')
    expect(wrapper.get('[data-testid="add-to-cart-CF-BEV-002"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="add-to-cart-CF-BEV-001"]').attributes('disabled')).toBeUndefined()
  })

  it('购物车支持加减数量并计算合计金额', async () => {
    const { wrapper } = await openCustomer()

    expect(wrapper.get('[data-testid="cart-total"]').text()).toBe('￥0.00')
    await wrapper.get('[data-testid="add-to-cart-CF-BEV-001"]').trigger('click')
    await wrapper.get('[data-testid="add-to-cart-CF-BEV-001"]').trigger('click')
    await wrapper.get('[data-testid="add-to-cart-CF-BEV-002"]').trigger('click')

    expect(wrapper.get('[data-testid="cart-quantity-CF-BEV-001"]').text()).toBe('2')
    expect(wrapper.get('[data-testid="cart-total"]').text()).toBe('￥64.00')
    // 停售商品无法加入购物车
    expect(wrapper.find('[data-testid="cart-line-CF-BEV-002"]').exists()).toBe(false)

    await wrapper.get('[data-testid="cart-plus-CF-BEV-001"]').trigger('click')
    expect(wrapper.get('[data-testid="cart-quantity-CF-BEV-001"]').text()).toBe('3')
    expect(wrapper.get('[data-testid="cart-total"]').text()).toBe('￥96.00')
    await wrapper.get('[data-testid="cart-minus-CF-BEV-001"]').trigger('click')
    await wrapper.get('[data-testid="cart-minus-CF-BEV-001"]').trigger('click')
    expect(wrapper.get('[data-testid="cart-quantity-CF-BEV-001"]').text()).toBe('1')
    expect(wrapper.get('[data-testid="cart-total"]').text()).toBe('￥32.00')
    // 数量减到 0 时该明细从购物车移除
    await wrapper.get('[data-testid="cart-minus-CF-BEV-001"]').trigger('click')
    expect(wrapper.find('[data-testid="cart-line-CF-BEV-001"]').exists()).toBe(false)
    expect(wrapper.get('[data-testid="cart-total"]').text()).toBe('￥0.00')
  })

  it('提交订单后展示取餐码与金额并刷新门店看板', async () => {
    const { wrapper, fetchMock } = await openCustomer()

    await wrapper.get('[data-testid="add-to-cart-CF-BEV-001"]').trigger('click')
    await wrapper.get('[data-testid="customer-name"]').setValue('顾客13')
    await wrapper.get('[data-testid="customer-phone"]').setValue('8888')
    await wrapper.get('[data-testid="order-note"]').setValue('少冰')
    await wrapper.get('[data-testid="submit-order"]').trigger('click')
    await flush()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/orders', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({
        storeId: 'S001',
        customerName: '顾客13',
        phoneLast4: '8888',
        items: [{ productCode: 'CF-BEV-001', size: '中杯', quantity: 1 }],
        note: '少冰',
      }),
    }))
    expect(wrapper.get('[data-testid="placed-pickup-code"]').text()).toBe('1001')
    expect(wrapper.get('[data-testid="placed-total"]').text()).toBe('￥64.00')
    expect(wrapper.get('[data-testid="cart-total"]').text()).toBe('￥0.00')
    // 挂载时 1 次 + 下单成功后刷新 1 次
    expect(fetchMock.mock.calls.filter(([input, init]) =>
      String(input) === '/api/v1/orders' && !init)).toHaveLength(2)
  })

  it('缺少必填信息时不下单', async () => {
    const { wrapper, fetchMock } = await openCustomer()

    await wrapper.get('[data-testid="add-to-cart-CF-BEV-001"]').trigger('click')
    await wrapper.get('[data-testid="customer-name"]').setValue('顾客13')
    await wrapper.get('[data-testid="customer-phone"]').setValue('88')
    await wrapper.get('[data-testid="submit-order"]').trigger('click')
    await flush()

    expect(wrapper.get('[data-testid="customer-error"]').text()).toContain('手机号后四位')
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false)
  })

  it('按取餐码查询订单并在待制作时取消', async () => {
    const { wrapper, fetchMock } = await openCustomer()

    await wrapper.get('[data-testid="pickup-code-input"]').setValue('1001')
    await wrapper.get('[data-testid="query-pickup"]').trigger('click')
    await flush()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/orders/pickup/1001', undefined)
    expect(wrapper.get('[data-testid="pickup-status"]').text()).toBe('待制作')
    expect(wrapper.get('[data-testid="pickup-result"]').text()).toContain('燕麦拿铁（大杯）× 2')
    expect(wrapper.get('[data-testid="pickup-total"]').text()).toBe('￥64.00')

    await wrapper.get('[data-testid="cancel-phone"]').setValue('8888')
    await wrapper.get('[data-testid="cancel-order"]').trigger('click')
    await flush()

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/orders/CF-1013/cancel', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ pickupCode: '1001', phoneLast4: '8888' }),
    }))
    expect(wrapper.get('[data-testid="pickup-status"]').text()).toBe('已取消')
    expect(wrapper.find('[data-testid="cancel-order"]').exists()).toBe(false)
  })

  it('取餐码未命中时展示服务端提示', async () => {
    const { wrapper } = await openCustomer({ pickupMissing: true })

    await wrapper.get('[data-testid="pickup-code-input"]').setValue('9999')
    await wrapper.get('[data-testid="query-pickup"]').trigger('click')
    await flush()

    expect(wrapper.get('[data-testid="pickup-error"]').text()).toContain('取餐码不存在或不属于当日订单')
    expect(wrapper.find('[data-testid="pickup-result"]').exists()).toBe(false)
  })

  it('制作中的订单不提供取消入口', async () => {
    const { wrapper } = await openCustomer({ pickupStatus: 'IN_PROGRESS' })

    await wrapper.get('[data-testid="pickup-code-input"]').setValue('1001')
    await wrapper.get('[data-testid="query-pickup"]').trigger('click')
    await flush()

    expect(wrapper.get('[data-testid="pickup-status"]').text()).toBe('制作中')
    expect(wrapper.find('[data-testid="cancel-order"]').exists()).toBe(false)
  })

  it('取消失败时展示服务端错误信息', async () => {
    const { wrapper } = await openCustomer({ cancelFails: true })

    await wrapper.get('[data-testid="pickup-code-input"]').setValue('1001')
    await wrapper.get('[data-testid="query-pickup"]').trigger('click')
    await flush()
    await wrapper.get('[data-testid="cancel-phone"]').setValue('0000')
    await wrapper.get('[data-testid="cancel-order"]').trigger('click')
    await flush()

    expect(wrapper.get('[data-testid="cancel-error"]').text()).toContain('订单已开始制作，无法取消')
    expect(wrapper.get('[data-testid="pickup-status"]').text()).toBe('待制作')
  })

  it('门店看板可停售与恢复当前门店的商品', async () => {
    const fetchMock = orderingFetch()
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(App)
    await flush()

    await wrapper.get('[data-testid="stock-panel-toggle"]').trigger('click')
    await flush()
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/stores/S001/products', undefined)
    expect(wrapper.get('[data-testid="stock-product-CF-BEV-001"]').text()).toContain('可售')
    expect(wrapper.get('[data-testid="stock-product-CF-BEV-002"]').text()).toContain('已停售')

    await wrapper.get('[data-testid="product-availability-CF-BEV-001"]').setValue(false)
    await flush()
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/stores/S001/products/CF-BEV-001/availability',
      expect.objectContaining({ method: 'PATCH', body: JSON.stringify({ available: false }) }))
    expect(wrapper.get('[data-testid="stock-product-CF-BEV-001"]').text()).toContain('已停售')

    await wrapper.get('[data-testid="product-availability-CF-BEV-002"]').setValue(true)
    await flush()
    expect(wrapper.get('[data-testid="stock-product-CF-BEV-002"]').text()).toContain('可售')
  })
})

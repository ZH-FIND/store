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

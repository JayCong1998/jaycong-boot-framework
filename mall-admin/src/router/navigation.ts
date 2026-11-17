import {
  House,
  Goods,
  Tickets,
  User,
  Present,
  Document,
  DataAnalysis,
  Setting,
} from '@element-plus/icons-vue'

export const navigation = [
  {
    path: '/dashboard',
    title: '工作台',
    icon: House,
    description: '经营概览与运营待办',
  },
  {
    path: '/products',
    title: '商品管理',
    icon: Goods,
    description: '商品列表、分类与库存管理',
  },
  {
    path: '/orders',
    title: '订单管理',
    icon: Tickets,
    description: '订单查询、发货与售后处理',
  },
  {
    path: '/members',
    title: '会员管理',
    icon: User,
    description: '会员档案与账号状态管理',
    children: [
      { path: '/members', title: '会员列表' },
      { path: '/members/operations', title: '操作记录' },
    ],
  },
  {
    path: '/marketing',
    title: '营销中心',
    icon: Present,
    description: '优惠券与营销活动管理',
  },
  {
    path: '/content',
    title: '内容管理',
    icon: Document,
    description: '首页内容与公告管理',
  },
  {
    path: '/analytics',
    title: '数据统计',
    icon: DataAnalysis,
    description: '销售与转化数据分析',
  },
  {
    path: '/settings',
    title: '系统设置',
    icon: Setting,
    description: '当前账号与系统信息',
  },
]

/** 只允许已注册的后台页面作为登录回跳地址，拒绝外部地址和登录循环。 */
export function safeRedirect(value: unknown): string {
  if (
    typeof value !== 'string' ||
    !value.startsWith('/') ||
    value.startsWith('//') ||
    value.includes('\\')
  )
    return '/dashboard'
  const path = value.split(/[?#]/)[0]
  return navigation.some((item) => item.path === path || item.children?.some((child) => child.path === path)) ? value : '/dashboard'
}

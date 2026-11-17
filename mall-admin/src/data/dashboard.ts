import {
  Money,
  Tickets,
  User,
  TrendCharts,
  Headset,
  Handbag,
  CoffeeCup,
} from '@element-plus/icons-vue'

// 经营接口尚未接入，仅用于还原确认过的工作台原型。
export const metrics = [
  {
    title: '今日销售额',
    value: '¥ 128,560.00',
    change: '12.8%',
    up: true,
    color: 'blue',
    icon: Money,
  },
  {
    title: '今日订单数',
    value: '1,286',
    change: '8.2%',
    up: true,
    color: 'purple',
    icon: Tickets,
  },
  {
    title: '新增会员',
    value: '328',
    change: '16.5%',
    up: true,
    color: 'teal',
    icon: User,
  },
  {
    title: '支付转化率',
    value: '6.82%',
    change: '0.4%',
    up: false,
    color: 'amber',
    icon: TrendCharts,
  },
]
export const todos = [
  {
    title: '待付款订单',
    value: 36,
    color: 'amber',
    path: '/orders',
    status: 'pending-payment',
  },
  {
    title: '待发货订单',
    value: 128,
    color: 'blue',
    path: '/orders',
    status: 'pending-shipment',
  },
  {
    title: '售后待处理',
    value: 12,
    color: 'red',
    path: '/orders',
    status: 'after-sales',
  },
  {
    title: '库存预警商品',
    value: 8,
    color: 'purple',
    path: '/products',
    status: 'low-stock',
  },
]
export const orders = [
  {
    id: '2026111200086',
    product: '无线降噪蓝牙耳机 × 1',
    amount: '299.00',
    status: '待发货',
    type: 'warning',
  },
  {
    id: '2026111200085',
    product: '极简双肩通勤包 × 1',
    amount: '189.00',
    status: '已完成',
    type: 'success',
  },
  {
    id: '2026111200084',
    product: '便携保温随行杯 × 2',
    amount: '158.00',
    status: '待付款',
    type: 'primary',
  },
  {
    id: '2026111200083',
    product: '桌面氛围阅读灯 × 1',
    amount: '129.00',
    status: '待发货',
    type: 'warning',
  },
] as const
export const bestsellers = [
  {
    name: '无线降噪蓝牙耳机',
    category: '数码配件',
    sales: 856,
    icon: Headset,
    color: 'blue',
  },
  {
    name: '极简双肩通勤包',
    category: '箱包服饰',
    sales: 642,
    icon: Handbag,
    color: 'amber',
  },
  {
    name: '便携保温随行杯',
    category: '生活日用',
    sales: 518,
    icon: CoffeeCup,
    color: 'teal',
  },
]

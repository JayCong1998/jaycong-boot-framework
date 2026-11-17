<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { init, use, type EChartsType } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])
const props = defineProps<{ days: number }>()
const container = ref<HTMLDivElement>()
let chart: EChartsType | undefined
let observer: ResizeObserver | undefined
function render() {
  const dates = Array.from({ length: props.days }, (_, i) => {
    const date = new Date()
    date.setDate(date.getDate() - props.days + i + 1)
    return `${date.getMonth() + 1}/${String(date.getDate()).padStart(2, '0')}`
  })
  const base = [68560, 82900, 57300, 95100, 74300, 148920, 128560]
  const values = dates.map((_, i) => base[i % base.length]!)
  chart?.setOption({
    animation: !window.matchMedia('(prefers-reduced-motion: reduce)').matches,
    grid: { left: 52, right: 20, top: 22, bottom: 35 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#182a48',
      borderWidth: 0,
      textStyle: { color: '#fff' },
      valueFormatter: (value: number) =>
        `¥ ${Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#8793a7', margin: 16 },
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 160000,
      interval: 40000,
      axisLabel: {
        color: '#8793a7',
        formatter: (v: number) => (v ? `${v / 10000}万` : '0'),
      },
      splitLine: { lineStyle: { color: '#edf0f6' } },
    },
    series: [
      {
        name: '成交金额（示例）',
        type: 'line',
        data: values,
        symbolSize: 6,
        smooth: 0.15,
        lineStyle: { color: '#3971f7', width: 3 },
        itemStyle: { color: '#3971f7' },
        areaStyle: { color: '#edf3ff', opacity: 1 },
      },
    ],
  })
}
onMounted(() => {
  if (!container.value) return
  chart = init(container.value)
  render()
  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(container.value)
})
watch(() => props.days, render)
onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
})
</script>

<template>
  <div
    ref="container"
    class="sales-chart"
    role="img"
    :aria-label="`最近 ${days} 天成交金额趋势，示例数据`"
  ></div>
</template>

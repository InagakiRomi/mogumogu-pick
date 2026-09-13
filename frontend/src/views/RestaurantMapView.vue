<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue'

import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

import { Icon, PinCirclePanel } from 'leaflet-extra-markers'
import { createElement, icons } from 'lucide'

import client from '@/api/client'
import type { components } from '@/api/schema'
import PrimaryButton from '@/components/common/PrimaryButton.vue'

type NearbyRestaurant = components['schemas']['NearbyRestaurantResponse']

type RestaurantStatus = 'idle' | 'loading' | 'success' | 'error'

const mapContainer = ref<HTMLDivElement | null>(null)

const restaurantStatus = ref<RestaurantStatus>('idle')

let map: L.Map | null = null
let resizeObserver: ResizeObserver | null = null
let restaurantLayer: L.LayerGroup | null = null
let userMarker: L.Marker | null = null

let currentLatitude: number | null = null
let currentLongitude: number | null = null

const DEFAULT_POSITION: L.LatLngExpression = [25.033, 121.5654]
const DEFAULT_ZOOM = 15

/** 建立餐廳 Lucide 圖示 */
const createRestaurantIcon = () => {
  const icon = createElement(icons.Utensils)

  icon.setAttribute('width', '17')
  icon.setAttribute('height', '17')
  icon.setAttribute('stroke-width', '2.5')

  return icon
}

/** 建立目前位置 Lucide 圖示 */
const createUserIcon = () => {
  const icon = createElement(icons.Navigation)

  icon.setAttribute('width', '17')
  icon.setAttribute('height', '17')
  icon.setAttribute('stroke-width', '2.5')

  return icon
}

/** 餐廳 Marker 樣式 */
const restaurantIcon = new Icon({
  svg: PinCirclePanel,
  color: '#ef4444',
  accentColor: '#ffffff',
  contentColor: '#ffffff',
  content: createRestaurantIcon,
  scale: 1.1,
  shadow: 'drop',
})

/** 使用者位置 Marker 樣式 */
const userPositionIcon = new Icon({
  svg: PinCirclePanel,
  color: '#2563eb',
  accentColor: '#ffffff',
  contentColor: '#ffffff',
  content: createUserIcon,
  scale: 1.1,
  shadow: 'drop',
})

/** 建立 Leaflet 地圖 */
const createMap = () => {
  if (!mapContainer.value || map) return

  map = L.map(mapContainer.value, {
    attributionControl: false,
  }).setView(DEFAULT_POSITION, DEFAULT_ZOOM)

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map)

  restaurantLayer = L.layerGroup().addTo(map)

  resizeObserver = new ResizeObserver(() => {
    map?.invalidateSize()
  })

  resizeObserver.observe(mapContainer.value)

  requestAnimationFrame(() => {
    map?.invalidateSize()
  })
}

/** 移動地圖到指定位置 */
const moveMap = (latitude: number, longitude: number, zoom = DEFAULT_ZOOM) => {
  map?.setView([latitude, longitude], zoom)
}

/** 顯示使用者目前位置 */
const showUserPosition = (latitude: number, longitude: number) => {
  if (!map) return

  if (userMarker) {
    userMarker.setLatLng([latitude, longitude])
    return
  }

  userMarker = L.marker([latitude, longitude], {
    icon: userPositionIcon,
  })
    .bindPopup('您的位置')
    .addTo(map)
}

/** 建立餐廳 Popup */
const createRestaurantPopup = (restaurant: NearbyRestaurant) => {
  const container = document.createElement('div')

  const name = document.createElement('strong')
  name.textContent = restaurant.name ?? '未命名餐廳'
  container.appendChild(name)

  if (restaurant.address) {
    const address = document.createElement('div')
    address.textContent = `地址：${restaurant.address}`
    container.appendChild(address)
  }

  if (restaurant.openingHours) {
    const openingHours = document.createElement('div')
    openingHours.textContent = `營業時間：${restaurant.openingHours}`
    container.appendChild(openingHours)
  }

  if (restaurant.phone) {
    const phone = document.createElement('div')
    phone.textContent = `電話：${restaurant.phone}`
    container.appendChild(phone)
  }

  return container
}

/** 將餐廳顯示在地圖上 */
const showRestaurants = (restaurants: NearbyRestaurant[]) => {
  if (!restaurantLayer) return

  const layer = restaurantLayer

  layer.clearLayers()

  restaurants.forEach((restaurant) => {
    if (restaurant.latitude == null || restaurant.longitude == null) {
      return
    }

    const marker = L.marker([restaurant.latitude, restaurant.longitude], {
      icon: restaurantIcon,
    })

    marker.bindPopup(createRestaurantPopup(restaurant))

    marker.addTo(layer)
  })
}

/** 從後端取得附近餐廳 */
const loadRestaurants = async (latitude: number, longitude: number) => {
  restaurantStatus.value = 'loading'

  try {
    const { data, error } = await client.GET('/restaurants/nearby', {
      params: {
        query: {
          latitude,
          longitude,
        },
      },
    })

    if (error) {
      restaurantStatus.value = 'error'
      console.error('取得附近餐廳失敗：', error)
      return
    }

    if (!data) {
      restaurantStatus.value = 'error'
      return
    }

    showRestaurants(data)

    restaurantStatus.value = 'success'

    window.setTimeout(() => {
      if (restaurantStatus.value === 'success') {
        restaurantStatus.value = 'idle'
      }
    }, 2000)
  } catch (error) {
    restaurantStatus.value = 'error'
    console.error('呼叫附近餐廳 API 發生錯誤：', error)
  }
}

/** 重新讀取餐廳 */
const retryLoadRestaurants = async () => {
  if (currentLatitude == null || currentLongitude == null) return

  await loadRestaurants(currentLatitude, currentLongitude)
}

/** 取得使用者目前位置 */
const locateUser = () => {
  if (!navigator.geolocation) {
    console.warn('瀏覽器不支援定位功能')
    return
  }

  navigator.geolocation.getCurrentPosition(
    async ({ coords }) => {
      const latitude = coords.latitude
      const longitude = coords.longitude

      currentLatitude = latitude
      currentLongitude = longitude

      moveMap(latitude, longitude)
      showUserPosition(latitude, longitude)

      await loadRestaurants(latitude, longitude)
    },
    (error) => {
      console.warn('無法取得使用者位置：', error.message)
    },
    {
      enableHighAccuracy: true,
      timeout: 8000,
      maximumAge: 60_000,
    },
  )
}

onMounted(async () => {
  await nextTick()

  createMap()
  locateUser()
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  resizeObserver = null

  restaurantLayer?.clearLayers()
  restaurantLayer = null

  userMarker = null

  map?.remove()
  map = null
})
</script>

<template>
  <div
    class="relative h-[calc(100dvh-4rem-1px)] w-full sm:h-[calc(100dvh-4.5rem-1px)] md:h-[calc(100dvh-5rem-1px)]"
  >
    <div ref="mapContainer" class="h-full w-full overflow-hidden" />

    <div v-if="restaurantStatus !== 'idle'" class="absolute top-4 left-1/2 z-1000 -translate-x-1/2">
      <div
        class="flex items-center gap-3 rounded-lg border bg-background px-4 py-3 text-sm shadow-md"
      >
        <template v-if="restaurantStatus === 'loading'">
          <span
            class="size-4 animate-spin rounded-full border-2 border-muted-foreground border-t-transparent"
          />
          <span>正在讀取附近餐廳...</span>
        </template>

        <template v-else-if="restaurantStatus === 'success'">
          <span>餐廳讀取完成</span>
        </template>

        <template v-else-if="restaurantStatus === 'error'">
          <span>餐廳讀取失敗</span>

          <PrimaryButton variant="outline" class="h-9 px-3" @click="retryLoadRestaurants">
            重新讀取
          </PrimaryButton>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
:deep(.leaflet-container) {
  height: 100%;
  width: 100%;
  z-index: 0;
  font: inherit;
}

:deep(.leaflet-container img.leaflet-tile) {
  max-width: none !important;
  max-height: none !important;
}

:deep(.leaflet-popup-content-wrapper),
:deep(.leaflet-popup-tip) {
  background: color-mix(in oklab, var(--background) 95%, transparent);
  color: var(--secondary-foreground);
  box-shadow: none;
}

:deep(.leaflet-popup-content-wrapper) {
  border: 1px solid var(--border);
  border-radius: var(--radius);
}

:deep(.leaflet-popup-content) {
  margin: 0.75rem 1rem;
  font-size: 1rem;
  line-height: 1.6;
  color: var(--secondary-foreground);
}

:deep(.leaflet-popup-close-button) {
  color: var(--secondary-foreground);
}

:deep(.leaflet-popup-close-button:hover) {
  color: var(--muted-foreground);
}

:deep(.leaflet-control-zoom.leaflet-bar) {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  border: none;
  box-shadow: none;
}

:deep(.leaflet-control-zoom.leaflet-bar a),
:deep(.leaflet-touch .leaflet-control-zoom.leaflet-bar a) {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.75rem;
  height: 2.75rem;
  padding: 0;
  border: 1px solid rgba(146, 80, 58, 0.32);
  border-radius: 0.5rem;
  background: linear-gradient(
    to bottom right,
    rgba(255, 252, 248, 0.98),
    rgba(255, 236, 220, 0.95)
  );
  box-shadow: 0 1px 4px rgba(138, 73, 52, 0.08);
  color: var(--secondary-foreground);
  font: inherit;
  font-weight: 500;
  line-height: 1;
  text-align: center;
  text-decoration: none;
  transition: all 300ms;
}

:deep(.leaflet-control-zoom.leaflet-bar a:first-child),
:deep(.leaflet-control-zoom.leaflet-bar a:last-child) {
  border-radius: 0.5rem;
}

:deep(.leaflet-control-zoom.leaflet-bar a:hover),
:deep(.leaflet-control-zoom.leaflet-bar a:focus) {
  border-color: rgba(146, 80, 58, 0.45);
  background: linear-gradient(to bottom right, rgb(255, 245, 235), rgba(255, 228, 210, 0.98));
  color: #4a2c2a;
  box-shadow: 0 2px 6px rgba(138, 73, 52, 0.1);
  transform: translateY(-0.125rem);
}

:deep(.leaflet-control-zoom.leaflet-bar a:active) {
  transform: scale(0.98);
}

:deep(.leaflet-control-zoom.leaflet-bar a.leaflet-disabled) {
  pointer-events: none;
  opacity: 0.5;
  transform: none;
}
</style>

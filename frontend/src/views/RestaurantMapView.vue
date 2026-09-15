<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue'

import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

import { Icon, PinCirclePanel } from 'leaflet-extra-markers'
import { createElement, icons } from 'lucide'

import client from '@/api/client'
import type { components } from '@/api/schema'
import CreateRestaurantDialog, {
  type CreateRestaurantPrefill,
} from '@/components/form/CreateRestaurantDialog.vue'
import PrimaryButton from '@/components/common/PrimaryButton.vue'
import { useFeedbackDialog } from '@/composables/useFeedbackDialog'
import { getApiErrorMessage } from '@/lib/apiErrorMessage'

type NearbyRestaurant = components['schemas']['NearbyRestaurantResponse']
type NearbyRestaurantSearch = components['schemas']['NearbyRestaurantSearchResponse']

type RestaurantStatus = 'idle' | 'loading' | 'success' | 'error'
type MapInitStatus = 'locating' | 'ready'

const { showFeedback } = useFeedbackDialog()

const mapContainer = ref<HTMLDivElement | null>(null)

const mapInitStatus = ref<MapInitStatus>('locating')
const restaurantStatus = ref<RestaurantStatus>('idle')
const showDefaultLocationNotice = ref(false)
const loadingElapsedSeconds = ref(0)
const restaurantCount = ref(0)
const isCreateDialogOpen = ref(false)
const createPrefill = ref<CreateRestaurantPrefill | null>(null)

let map: L.Map | null = null
let resizeObserver: ResizeObserver | null = null
let restaurantLayer: L.LayerGroup | null = null
let userMarker: L.Marker | null = null
let searchMarker: L.Marker | null = null

const currentLatitude = ref<number | null>(null)
const currentLongitude = ref<number | null>(null)
const committedLatitude = ref<number | null>(null)
const committedLongitude = ref<number | null>(null)

let lastRequestedLatitude: number | null = null
let lastRequestedLongitude: number | null = null

let userLatitude: number | null = null
let userLongitude: number | null = null

let isUnmounted = false

let locateTimer: number | null = null
let defaultLocationNoticeTimer: number | null = null
let loadingElapsedTimer: number | null = null
let successStatusTimer: number | null = null
let restaurantSearchController: AbortController | null = null

/** 定位失敗時使用的預設位置 */
const DEFAULT_LATITUDE = 25.033
const DEFAULT_LONGITUDE = 121.5654

/** 地圖預設縮放層級 */
const DEFAULT_ZOOM = 16

/** 後端允許的搜尋範圍（西南 21.7, 118.0；東北 26.5, 122.2） */
const SEARCH_MIN_LATITUDE = 21.7
const SEARCH_MAX_LATITUDE = 26.5
const SEARCH_MIN_LONGITUDE = 118.0
const SEARCH_MAX_LONGITUDE = 122.2
const OUT_OF_SEARCH_RANGE_MESSAGE = '超出搜尋範圍'

/** 台灣地圖範圍 */
const TAIWAN_BOUNDS = L.latLngBounds(
  [SEARCH_MIN_LATITUDE, SEARCH_MIN_LONGITUDE],
  [SEARCH_MAX_LATITUDE, SEARCH_MAX_LONGITUDE],
)

/** 定位逾時時間 */
const LOCATE_TIMEOUT_MS = 10_000

/** 預設位置提示顯示時間 */
const DEFAULT_LOCATION_NOTICE_MS = 4_000

/** 餐廳讀取成功提示顯示時間 */
const SUCCESS_NOTICE_MS = 2_000

/** 建立餐廳 Marker 圖示 */
const createRestaurantIcon = () => {
  const icon = createElement(icons.Utensils)

  icon.setAttribute('width', '17')
  icon.setAttribute('height', '17')
  icon.setAttribute('stroke-width', '2.5')

  return icon
}

/** 建立使用者位置 Marker 圖示 */
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

/** 搜尋位置 Marker 樣式 */
const searchPositionIcon = new Icon({
  svg: PinCirclePanel,
  color: '#f59e0b',
  accentColor: '#ffffff',
  contentColor: '#ffffff',
  content: createUserIcon,
  scale: 1.1,
  shadow: 'drop',
})

/** 建立 Leaflet 地圖 */
const createMap = (latitude: number, longitude: number) => {
  if (!mapContainer.value || map) return

  map = L.map(mapContainer.value, {
    zoomControl: false,

    // 限制只能移動在台灣範圍
    maxBounds: TAIWAN_BOUNDS,
    maxBoundsViscosity: 1.0,

    // 避免縮太遠看到整個亞洲
    minZoom: 7,
  }).setView([latitude, longitude], DEFAULT_ZOOM)

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    minZoom: 7,
    noWrap: true,
    attribution: '&copy; OpenStreetMap contributors',
  }).addTo(map)

  L.control
    .zoom({
      position: 'bottomright',
    })
    .addTo(map)

  restaurantLayer = L.layerGroup().addTo(map)

  resizeObserver = new ResizeObserver(() => {
    map?.invalidateSize()
  })

  resizeObserver.observe(mapContainer.value)

  requestAnimationFrame(() => {
    map?.invalidateSize()
  })
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
    zIndexOffset: 1000,
    riseOnHover: true,
  })
    .bindPopup(createPositionPopup('您的位置'), {
      minWidth: 120,
      maxWidth: 240,
    })
    .addTo(map)

  enableMarkerHover(userMarker)
}

/** 顯示目前搜尋位置 */
const showSearchPosition = (latitude: number, longitude: number) => {
  if (!map) return

  if (searchMarker) {
    searchMarker.setLatLng([latitude, longitude])
    return
  }

  searchMarker = L.marker([latitude, longitude], {
    icon: searchPositionIcon,
    zIndexOffset: 900,
    riseOnHover: true,
  })
    .bindPopup(createPositionPopup('目前搜尋位置'), {
      minWidth: 140,
      maxWidth: 240,
    })
    .addTo(map)

  enableMarkerHover(searchMarker)
}

/** 滑鼠懸停時放大地圖釘 */
const enableMarkerHover = (marker: L.Marker) => {
  marker.on('mouseover', () => {
    marker.getElement()?.classList.add('map-marker--hovered')
  })

  marker.on('mouseout', () => {
    marker.getElement()?.classList.remove('map-marker--hovered')
  })
}

/** 建立位置 Marker Popup */
const createPositionPopup = (text: string) => {
  const content = document.createElement('div')
  content.className = 'map-position-popup'
  content.textContent = text
  return content
}

/** 開啟新增餐廳彈窗，並帶入地圖上可取得的資料 */
const openCreateRestaurantDialog = (restaurant: NearbyRestaurant) => {
  createPrefill.value = {
    restaurantName: restaurant.name?.trim() || undefined,
    address: restaurant.address?.trim() || undefined,
  }

  map?.closePopup()
  isCreateDialogOpen.value = true
}

/** 關閉新增餐廳彈窗 */
const handleCreateDialogOpenChange = (open: boolean) => {
  isCreateDialogOpen.value = open

  if (!open) {
    createPrefill.value = null
  }
}

/** 建立 Popup 資訊列 */
const createPopupInfo = (label: string, value: string | null | undefined) => {
  if (!value) return null

  const row = document.createElement('div')
  row.className = 'restaurant-popup__row'

  const labelElement = document.createElement('div')
  labelElement.className = 'restaurant-popup__label'
  labelElement.textContent = label

  const valueElement = document.createElement('div')
  valueElement.className = 'restaurant-popup__value'
  valueElement.textContent = value

  row.appendChild(labelElement)
  row.appendChild(valueElement)

  return row
}

/** 建立餐廳 Popup */
const createRestaurantPopup = (restaurant: NearbyRestaurant) => {
  const container = document.createElement('div')
  container.className = 'restaurant-popup'

  const name = document.createElement('div')
  name.className = 'restaurant-popup__name'
  name.textContent = restaurant.name ?? '未命名餐廳'

  container.appendChild(name)

  const address = createPopupInfo('地址', restaurant.address)

  if (address) {
    container.appendChild(address)
  }

  const openingHours = createPopupInfo('營業時間', restaurant.openingHours)

  if (openingHours) {
    container.appendChild(openingHours)
  }

  const phone = createPopupInfo('電話', restaurant.phone)

  if (phone) {
    container.appendChild(phone)
  }

  const addButton = document.createElement('button')
  addButton.type = 'button'
  addButton.className = 'restaurant-popup__add-button'
  addButton.textContent = '新增餐廳'

  L.DomEvent.disableClickPropagation(addButton)
  L.DomEvent.on(addButton, 'click', (event) => {
    L.DomEvent.stop(event)
    openCreateRestaurantDialog(restaurant)
  })

  container.appendChild(addButton)

  return container
}

/** 將餐廳顯示在地圖上 */
const showRestaurants = (restaurants: NearbyRestaurant[]) => {
  const layer = restaurantLayer

  if (!layer) return

  layer.clearLayers()

  for (const restaurant of restaurants) {
    if (restaurant.latitude == null || restaurant.longitude == null) {
      continue
    }

    const marker = L.marker([restaurant.latitude, restaurant.longitude], {
      icon: restaurantIcon,
      riseOnHover: true,
    })

    marker.bindPopup(createRestaurantPopup(restaurant), {
      maxWidth: 320,
      minWidth: 220,
    })

    enableMarkerHover(marker)
    marker.addTo(layer)
  }
}

/** 套用後端回傳的搜尋結果 */
const applySearchResult = (result: NearbyRestaurantSearch) => {
  showRestaurants(result.restaurants ?? [])
  restaurantCount.value = result.total ?? 0

  if (result.latitude == null || result.longitude == null) {
    return
  }

  commitSearchPosition(result.latitude, result.longitude)
  showSearchPosition(result.latitude, result.longitude)
}

/** 清除讀取秒數計時器 */
const clearLoadingElapsedTimer = () => {
  if (loadingElapsedTimer == null) return

  window.clearInterval(loadingElapsedTimer)
  loadingElapsedTimer = null
}

/** 開始計算餐廳讀取秒數 */
const startLoadingElapsedTimer = () => {
  clearLoadingElapsedTimer()

  loadingElapsedSeconds.value = 0

  loadingElapsedTimer = window.setInterval(() => {
    if (isUnmounted) {
      clearLoadingElapsedTimer()
      return
    }

    loadingElapsedSeconds.value += 1
  }, 1_000)
}

/** 清除成功提示計時器 */
const clearSuccessStatusTimer = () => {
  if (successStatusTimer == null) return

  window.clearTimeout(successStatusTimer)
  successStatusTimer = null
}

/** 延遲將成功狀態恢復為 idle */
const scheduleSuccessStatusReset = () => {
  clearSuccessStatusTimer()

  successStatusTimer = window.setTimeout(() => {
    successStatusTimer = null

    if (!isUnmounted && restaurantStatus.value === 'success') {
      restaurantStatus.value = 'idle'
    }
  }, SUCCESS_NOTICE_MS)
}

/** 判斷請求是否被中止 */
const isAbortError = (error: unknown) => {
  return error instanceof DOMException
    ? error.name === 'AbortError'
    : typeof error === 'object' && error != null && 'name' in error && error.name === 'AbortError'
}

/** 中止目前的附近餐廳搜尋 */
const abortRestaurantSearch = () => {
  restaurantSearchController?.abort()
  restaurantSearchController = null
}

/** 還原到上一次已確認的搜尋位置 */
const restoreLastSearchPosition = () => {
  if (committedLatitude.value == null || committedLongitude.value == null) {
    return
  }

  currentLatitude.value = committedLatitude.value
  currentLongitude.value = committedLongitude.value

  showSearchPosition(committedLatitude.value, committedLongitude.value)
}

/** 確認目前搜尋位置 */
const commitSearchPosition = (latitude: number, longitude: number) => {
  committedLatitude.value = latitude
  committedLongitude.value = longitude
  currentLatitude.value = latitude
  currentLongitude.value = longitude
}

/** 取消搜尋附近餐廳 */
const cancelRestaurantSearch = () => {
  if (restaurantStatus.value !== 'loading') return

  abortRestaurantSearch()
  clearLoadingElapsedTimer()
  restaurantStatus.value = 'idle'
  restoreLastSearchPosition()
}

/** 判斷座標是否在後端允許的搜尋範圍內 */
const isWithinSearchRange = (latitude: number, longitude: number) => {
  return (
    Number.isFinite(latitude) &&
    Number.isFinite(longitude) &&
    latitude >= SEARCH_MIN_LATITUDE &&
    latitude <= SEARCH_MAX_LATITUDE &&
    longitude >= SEARCH_MIN_LONGITUDE &&
    longitude <= SEARCH_MAX_LONGITUDE
  )
}

/** 判斷是否為超出搜尋範圍的錯誤 */
const isOutOfSearchRangeError = (error: unknown) => {
  return getApiErrorMessage(error, '') === OUT_OF_SEARCH_RANGE_MESSAGE
}

/** 從後端取得附近餐廳 */
const loadRestaurants = async (latitude: number, longitude: number) => {
  if (isUnmounted) return

  if (!isWithinSearchRange(latitude, longitude)) {
    showFeedback(OUT_OF_SEARCH_RANGE_MESSAGE)
    return
  }

  abortRestaurantSearch()

  lastRequestedLatitude = latitude
  lastRequestedLongitude = longitude

  const controller = new AbortController()
  restaurantSearchController = controller

  restaurantStatus.value = 'loading'

  clearSuccessStatusTimer()
  startLoadingElapsedTimer()

  try {
    const { data, error } = await client.GET('/restaurants/nearby', {
      params: {
        query: {
          latitude,
          longitude,
        },
      },
      signal: controller.signal,
    })

    if (isUnmounted || controller.signal.aborted) return

    clearLoadingElapsedTimer()

    if (error) {
      if (isOutOfSearchRangeError(error)) {
        restaurantStatus.value = 'idle'
        showFeedback(OUT_OF_SEARCH_RANGE_MESSAGE)
        return
      }

      restaurantStatus.value = 'error'
      console.error('取得附近餐廳失敗：', error)
      return
    }

    if (!data) {
      restaurantStatus.value = 'error'
      return
    }

    applySearchResult(data)

    restaurantStatus.value = 'success'

    scheduleSuccessStatusReset()
  } catch (error) {
    if (isUnmounted || controller.signal.aborted || isAbortError(error)) {
      return
    }

    clearLoadingElapsedTimer()

    restaurantStatus.value = 'error'

    console.error('呼叫附近餐廳 API 發生錯誤：', error)
  } finally {
    if (restaurantSearchController === controller) {
      restaurantSearchController = null
    }
  }
}

/** 重新讀取目前搜尋位置附近的餐廳 */
const retryLoadRestaurants = async () => {
  if (lastRequestedLatitude == null || lastRequestedLongitude == null) {
    return
  }

  await loadRestaurants(lastRequestedLatitude, lastRequestedLongitude)
}

/** 搜尋目前地圖中心附近的餐廳 */
const searchCurrentArea = async () => {
  if (!map || restaurantStatus.value === 'loading') {
    return
  }

  const center = map.getCenter()

  await loadRestaurants(center.lat, center.lng)
}

/** 回到目前搜尋位置 */
const goToSearchPosition = () => {
  if (!map || currentLatitude.value == null || currentLongitude.value == null) {
    return
  }

  map.flyTo([currentLatitude.value, currentLongitude.value], DEFAULT_ZOOM, {
    duration: 0.8,
  })
}

/** 回到使用者位置 */
const goToUserPosition = () => {
  if (!map || userLatitude == null || userLongitude == null) {
    return
  }

  map.flyTo([userLatitude, userLongitude], DEFAULT_ZOOM, {
    duration: 0.8,
  })
}

/** 以指定座標初始化地圖 */
const initMapAt = async (latitude: number, longitude: number, isUserPosition: boolean) => {
  if (isUnmounted) return

  createMap(latitude, longitude)

  showSearchPosition(latitude, longitude)

  if (isUserPosition) {
    showUserPosition(latitude, longitude)
  } else {
    revealDefaultLocationNotice()
  }

  mapInitStatus.value = 'ready'

  await nextTick()

  map?.invalidateSize()

  if (isUnmounted) return

  await loadRestaurants(latitude, longitude)
}

/** 清除定位逾時計時器 */
const clearLocateTimer = () => {
  if (locateTimer == null) return

  window.clearTimeout(locateTimer)
  locateTimer = null
}

/** 清除預設位置提示計時器 */
const clearDefaultLocationNoticeTimer = () => {
  if (defaultLocationNoticeTimer == null) return

  window.clearTimeout(defaultLocationNoticeTimer)
  defaultLocationNoticeTimer = null
}

/** 顯示使用預設位置提示 */
const revealDefaultLocationNotice = () => {
  showDefaultLocationNotice.value = true

  clearDefaultLocationNoticeTimer()

  defaultLocationNoticeTimer = window.setTimeout(() => {
    defaultLocationNoticeTimer = null

    if (!isUnmounted) {
      showDefaultLocationNotice.value = false
    }
  }, DEFAULT_LOCATION_NOTICE_MS)
}

/** 取得使用者目前位置，失敗或逾時則回傳 null */
const getUserPosition = (): Promise<{
  latitude: number
  longitude: number
} | null> => {
  if (!navigator.geolocation) {
    console.warn('瀏覽器不支援定位功能')
    return Promise.resolve(null)
  }

  return new Promise((resolve) => {
    let settled = false

    const finish = (
      position: {
        latitude: number
        longitude: number
      } | null,
    ) => {
      if (settled) return

      settled = true

      clearLocateTimer()

      resolve(position)
    }

    locateTimer = window.setTimeout(() => {
      console.warn('取得使用者位置逾時，改用預設座標')

      finish(null)
    }, LOCATE_TIMEOUT_MS)

    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        finish({
          latitude: coords.latitude,
          longitude: coords.longitude,
        })
      },
      (error) => {
        console.warn('無法取得使用者位置：', error.message)

        finish(null)
      },
      {
        enableHighAccuracy: true,
        timeout: LOCATE_TIMEOUT_MS,
        maximumAge: 60_000,
      },
    )
  })
}

/** 初始化頁面 */
onMounted(async () => {
  const position = await getUserPosition()

  if (isUnmounted) return

  if (position) {
    userLatitude = position.latitude
    userLongitude = position.longitude

    await initMapAt(position.latitude, position.longitude, true)

    return
  }

  await initMapAt(DEFAULT_LATITUDE, DEFAULT_LONGITUDE, false)
})

/** 銷毀頁面 */
onUnmounted(() => {
  isUnmounted = true

  abortRestaurantSearch()
  clearLocateTimer()
  clearDefaultLocationNoticeTimer()
  clearLoadingElapsedTimer()
  clearSuccessStatusTimer()

  resizeObserver?.disconnect()
  resizeObserver = null

  restaurantLayer?.clearLayers()
  restaurantLayer = null

  userMarker = null
  searchMarker = null

  map?.remove()
  map = null
})
</script>

<template>
  <div class="relative h-full min-h-full w-full overflow-hidden">
    <div ref="mapContainer" class="h-full w-full" />

    <div
      v-if="mapInitStatus === 'locating'"
      class="absolute inset-0 z-2000 flex items-center justify-center bg-background"
    >
      <div
        class="flex items-center gap-3 rounded-xl border bg-background/95 px-5 py-3 text-sm shadow-lg backdrop-blur-sm"
      >
        <span
          class="size-4 animate-spin rounded-full border-2 border-muted-foreground border-t-transparent"
        />

        <span>正在取得目前位置...</span>
      </div>
    </div>

    <template v-if="mapInitStatus === 'ready'">
      <div class="pointer-events-none absolute top-4 left-4 z-1000">
        <div
          class="min-w-48 rounded-xl border bg-background/95 px-4 py-3 shadow-md backdrop-blur-sm"
        >
          <div class="text-sm font-semibold text-foreground">附近餐廳</div>

          <div
            v-if="currentLatitude != null && currentLongitude != null"
            class="mt-2 space-y-0.5 text-xs text-muted-foreground"
          >
            <div class="font-medium text-foreground">目前搜尋位置</div>

            <div>緯度 {{ currentLatitude.toFixed(5) }}</div>

            <div>經度 {{ currentLongitude.toFixed(5) }}</div>
          </div>

          <div class="mt-2 border-t pt-2 text-xs text-muted-foreground">
            <div
              v-if="restaurantStatus === 'loading'"
              class="flex items-center justify-between gap-2"
            >
              <span>正在搜尋附近餐廳...</span>

              <PrimaryButton
                variant="outline"
                class="pointer-events-auto h-7 px-2.5 text-xs"
                @click="cancelRestaurantSearch"
              >
                取消
              </PrimaryButton>
            </div>

            <template v-else>
              找到
              <span class="font-medium text-foreground">
                {{ restaurantCount }}
              </span>
              間餐廳
            </template>
          </div>
        </div>
      </div>

      <div
        class="pointer-events-none absolute top-4 left-1/2 z-1000 flex -translate-x-1/2 flex-col items-center gap-2"
      >
        <Transition name="map-notice-fade">
          <div
            v-if="showDefaultLocationNotice"
            role="status"
            class="rounded-lg border bg-background/95 px-4 py-2.5 text-sm shadow-md backdrop-blur-sm"
          >
            無法取得目前位置，已使用預設位置
          </div>
        </Transition>

        <Transition name="map-notice-fade">
          <div
            v-if="restaurantStatus === 'loading'"
            role="status"
            class="pointer-events-auto flex items-center gap-3 rounded-lg border bg-background/95 px-4 py-2.5 text-sm shadow-md backdrop-blur-sm"
          >
            <span
              class="size-4 animate-spin rounded-full border-2 border-muted-foreground border-t-transparent"
            />

            <span>
              搜尋附近餐廳中

              <template v-if="loadingElapsedSeconds >= 2">
                · {{ loadingElapsedSeconds }} 秒
              </template>
            </span>

            <PrimaryButton variant="outline" class="h-8 px-3" @click="cancelRestaurantSearch">
              取消
            </PrimaryButton>
          </div>
        </Transition>

        <Transition name="map-notice-fade">
          <div
            v-if="restaurantStatus === 'error'"
            role="alert"
            class="pointer-events-auto flex items-center gap-3 rounded-lg border bg-background/95 px-4 py-2.5 text-sm shadow-md backdrop-blur-sm"
          >
            <span>餐廳讀取失敗</span>

            <PrimaryButton variant="outline" class="h-8 px-3" @click="retryLoadRestaurants">
              重新讀取
            </PrimaryButton>
          </div>
        </Transition>
      </div>

      <div
        class="absolute bottom-6 left-1/2 z-1000 flex -translate-x-1/2 flex-wrap items-center justify-center gap-2"
      >
        <PrimaryButton
          class="h-10 whitespace-nowrap rounded-full px-5 shadow-lg"
          :disabled="restaurantStatus === 'loading'"
          @click="searchCurrentArea"
        >
          搜尋此區域
        </PrimaryButton>

        <PrimaryButton
          v-if="currentLatitude != null && currentLongitude != null"
          variant="outline"
          class="h-10 whitespace-nowrap rounded-full bg-background/95 px-4 shadow-lg backdrop-blur-sm"
          @click="goToSearchPosition"
        >
          回到搜尋位置
        </PrimaryButton>

        <PrimaryButton
          v-if="userLatitude != null && userLongitude != null"
          variant="outline"
          class="h-10 whitespace-nowrap rounded-full bg-background/95 px-4 shadow-lg backdrop-blur-sm"
          @click="goToUserPosition"
        >
          回到我的位置
        </PrimaryButton>
      </div>
    </template>

    <CreateRestaurantDialog
      :open="isCreateDialogOpen"
      :prefill="createPrefill"
      @update:open="handleCreateDialogOpenChange"
    />
  </div>
</template>

<style scoped>
.map-notice-fade-enter-active {
  transition:
    opacity 200ms ease,
    transform 200ms ease;
}

.map-notice-fade-leave-active {
  transition:
    opacity 300ms ease,
    transform 300ms ease;
}

.map-notice-fade-enter-from,
.map-notice-fade-leave-to {
  opacity: 0;
  transform: translateY(-0.375rem);
}

:deep(.leaflet-container) {
  height: 100%;
  width: 100%;
  z-index: 0;
  font: inherit;
  background: var(--muted);
}

:deep(.leaflet-container img.leaflet-tile) {
  max-width: none !important;
  max-height: none !important;
}

:deep(.extra-marker) {
  overflow: visible;
  cursor: pointer;
}

:deep(.extra-marker .extra-marker-icon) {
  transform-origin: bottom center;
  transition: transform 180ms ease;
}

:deep(.extra-marker .extra-marker-content) {
  transform-origin: 50% calc(100% * 41 / 30);
  transition: transform 180ms ease;
}

:deep(.extra-marker:hover),
:deep(.extra-marker.map-marker--hovered) {
  z-index: 10000 !important;
}

:deep(.extra-marker:hover .extra-marker-icon),
:deep(.extra-marker:hover .extra-marker-content),
:deep(.extra-marker.map-marker--hovered .extra-marker-icon),
:deep(.extra-marker.map-marker--hovered .extra-marker-content) {
  transform: scale(1.28);
}

:deep(.leaflet-popup-content-wrapper),
:deep(.leaflet-popup-tip) {
  background: color-mix(in oklab, var(--background) 97%, transparent);
  color: var(--foreground);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

:deep(.leaflet-popup-content-wrapper) {
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: calc(var(--radius) + 0.25rem);
}

:deep(.leaflet-popup-content) {
  margin: 0;
  width: auto !important;
  font: inherit;
  color: var(--foreground);
}

:deep(.map-position-popup) {
  padding: 0.75rem 2.25rem 0.75rem 1rem;
  font-size: 0.875rem;
  font-weight: 500;
  line-height: 1.4;
  white-space: nowrap;
}

:deep(.leaflet-popup-tip-container) {
  margin-top: -1px;
}

:deep(.leaflet-popup-close-button) {
  top: 0.375rem !important;
  right: 0.375rem !important;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.75rem !important;
  height: 1.75rem !important;
  border-radius: 9999px;
  color: var(--muted-foreground) !important;
  font-size: 1.25rem !important;
  line-height: 1 !important;
  transition:
    background-color 150ms ease,
    color 150ms ease;
}

:deep(.leaflet-popup-close-button:hover) {
  background: var(--muted);
  color: var(--foreground) !important;
}

:deep(.restaurant-popup) {
  min-width: 220px;
  max-width: 280px;
  padding: 1rem 1.125rem;
}

:deep(.restaurant-popup__name) {
  margin-bottom: 0.875rem;
  padding-right: 1.5rem;
  font-size: 1rem;
  font-weight: 600;
  line-height: 1.4;
  color: var(--foreground);
}

:deep(.restaurant-popup__row) {
  margin-top: 0.625rem;
}

:deep(.restaurant-popup__label) {
  margin-bottom: 0.125rem;
  font-size: 0.75rem;
  font-weight: 500;
  line-height: 1.4;
  color: var(--muted-foreground);
}

:deep(.restaurant-popup__value) {
  font-size: 0.875rem;
  line-height: 1.5;
  color: var(--foreground);
  overflow-wrap: anywhere;
}

:deep(.restaurant-popup__add-button) {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  margin-top: 0.875rem;
  height: 2.5rem;
  padding: 0 0.75rem;
  border: none;
  border-radius: 0.5rem;
  background: linear-gradient(to bottom right, #d78867, #c96d57);
  box-shadow: 0 3px 8px rgba(138, 73, 52, 0.18);
  color: var(--primary-foreground);
  font: inherit;
  font-size: 0.875rem;
  font-weight: 600;
  line-height: 1;
  cursor: pointer;
  transition:
    transform 150ms ease,
    box-shadow 150ms ease,
    filter 150ms ease;
}

:deep(.restaurant-popup__add-button:hover) {
  box-shadow: 0 4px 10px rgba(138, 73, 52, 0.22);
  filter: brightness(1.04);
  transform: translateY(-0.125rem);
}

:deep(.restaurant-popup__add-button:active) {
  box-shadow: 0 1px 4px rgba(138, 73, 52, 0.1);
  transform: scale(0.98);
}

:deep(.leaflet-control-zoom.leaflet-bar) {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-right: 1rem;
  margin-bottom: 1rem;
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
  border-radius: 0.625rem;
  background: color-mix(in oklab, var(--background) 95%, transparent);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  color: var(--foreground);
  font: inherit;
  font-size: 1.25rem;
  font-weight: 500;
  line-height: 1;
  text-align: center;
  text-decoration: none;
  backdrop-filter: blur(6px);
  transition:
    border-color 150ms ease,
    background-color 150ms ease,
    box-shadow 150ms ease,
    transform 150ms ease;
}

:deep(.leaflet-control-zoom.leaflet-bar a:first-child),
:deep(.leaflet-control-zoom.leaflet-bar a:last-child) {
  border-radius: 0.625rem;
}

:deep(.leaflet-control-zoom.leaflet-bar a:hover),
:deep(.leaflet-control-zoom.leaflet-bar a:focus) {
  border-color: rgba(146, 80, 58, 0.45);
  background: var(--background);
  color: var(--foreground);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-0.125rem);
}

:deep(.leaflet-control-zoom.leaflet-bar a:active) {
  transform: scale(0.97);
}

:deep(.leaflet-control-zoom.leaflet-bar a.leaflet-disabled) {
  pointer-events: none;
  opacity: 0.4;
  transform: none;
}

:deep(.leaflet-control-attribution) {
  border-radius: 0.375rem 0 0 0;
  background: color-mix(in oklab, var(--background) 85%, transparent) !important;
  color: var(--muted-foreground);
  font-size: 0.625rem;
  backdrop-filter: blur(4px);
}

:deep(.leaflet-control-attribution a) {
  color: var(--muted-foreground);
}

@media (max-width: 640px) {
  :deep(.leaflet-control-zoom.leaflet-bar) {
    margin-right: 0.75rem;
    margin-bottom: 0.75rem;
  }

  :deep(.leaflet-control-zoom.leaflet-bar a),
  :deep(.leaflet-touch .leaflet-control-zoom.leaflet-bar a) {
    width: 2.5rem;
    height: 2.5rem;
  }

  :deep(.restaurant-popup) {
    min-width: 190px;
    max-width: 240px;
  }
}
</style>

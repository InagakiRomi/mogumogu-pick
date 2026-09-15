import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import type { components } from '@/api/schema'

type NearbyRestaurant = components['schemas']['NearbyRestaurantResponse']
type NearbyRestaurantSearch = components['schemas']['NearbyRestaurantSearchResponse']

export const useNearbyRestaurantStore = defineStore(
  'nearbyRestaurant',
  () => {
    const latitude = ref<number | null>(null)
    const longitude = ref<number | null>(null)
    const restaurants = ref<NearbyRestaurant[]>([])
    const total = ref(0)

    const hasCachedSearch = computed(() => latitude.value != null && longitude.value != null)

    const saveSearch = (
      result: NearbyRestaurantSearch,
      fallback?: {
        latitude: number
        longitude: number
      },
    ) => {
      restaurants.value = result.restaurants ?? []
      total.value = result.total ?? 0
      latitude.value = result.latitude ?? fallback?.latitude ?? latitude.value
      longitude.value = result.longitude ?? fallback?.longitude ?? longitude.value
    }

    return {
      latitude,
      longitude,
      restaurants,
      total,
      hasCachedSearch,
      saveSearch,
    }
  },
  {
    persist: {
      key: 'nearbyRestaurant',
      storage: sessionStorage,
      pick: ['latitude', 'longitude', 'restaurants', 'total'],
    },
  },
)

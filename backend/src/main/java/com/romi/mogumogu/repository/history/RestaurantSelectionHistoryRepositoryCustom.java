package com.romi.mogumogu.repository.history;

/** 餐廳抽選歷史的自訂資料庫操作 */
public interface RestaurantSelectionHistoryRepositoryCustom {

    /** 將歷史紀錄 ID 自動編號對齊至目前最大值 + 1；表空時下次從 1 開始 */
    void resetHistoryIdSequence();
}

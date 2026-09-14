package com.romi.mogumogu.util;

public class RestaurantDataFormatter {

    /** 最長優先，避免 0836 被拆成 08 */
    private static final String[] LANDLINE_AREA_CODES = {
            "0836", "0826", "037", "049", "082", "089",
            "02", "03", "04", "05", "06", "07", "08"
    };

    private RestaurantDataFormatter() {
    }

    public static String formatPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        // 去掉空白、-、括號
        String number = phone.replaceAll("[\\s\\-()]", "");

        // +886 / 886 -> 國內號碼；OSM 有時已帶 0（+886 0966...）就不再重複加
        if (number.startsWith("+886") || number.startsWith("886")) {
            number = number.startsWith("+886")
                    ? number.substring(4)
                    : number.substring(3);

            if (!number.startsWith("0")) {
                number = "0" + number;
            }
        }

        if (!number.matches("^0\\d+$")) {
            return phone;
        }

        // 台灣市話：02xxxxxxxx
        if (number.matches("^02\\d{8}$")) {
            return number.substring(0, 2)
                    + "-"
                    + number.substring(2, 6)
                    + "-"
                    + number.substring(6);
        }

        // 手機：09xxxxxxxx
        if (number.matches("^09\\d{8}$")) {
            return number.substring(0, 4)
                    + "-"
                    + number.substring(4, 7)
                    + "-"
                    + number.substring(7);
        }

        String areaCode = matchLandlineAreaCode(number);
        if (areaCode != null) {
            return areaCode + " " + number.substring(areaCode.length());
        }

        return phone;
    }

    /** 依最長區碼切分市話 */
    private static String matchLandlineAreaCode(String number) {
        for (String code : LANDLINE_AREA_CODES) {
            if (number.startsWith(code) && number.length() > code.length()) {
                return code;
            }
        }
        return null;
    }

    public static String formatOpeningHours(String openingHours) {
        if (openingHours == null || openingHours.isBlank()) {
            return null;
        }

        if ("24/7".equalsIgnoreCase(openingHours.trim())) {
            return "24 小時營業";
        }

        String result = openingHours;

        // 先處理範圍，避免 Mo 被提前取代
        result = result
                .replace("Mo-Su", "每日")
                .replace("Mo-Fr", "週一至週五")
                .replace("Mo-Sa", "週一至週六")
                .replace("Sa-Su", "週六至週日")
                .replace("Su-Fr", "週日至週五")
                .replace("We-Mo", "週三至週一");

        // 再處理單一天
        result = result
                .replace("Mo", "週一")
                .replace("Tu", "週二")
                .replace("We", "週三")
                .replace("Th", "週四")
                .replace("Fr", "週五")
                .replace("Sa", "週六")
                .replace("Su", "週日");

        // OSM 常用 ; 分隔不同時段/日期
        result = result
                .replace(";", "；")
                .replace(",", "、");

        return result;
    }
}

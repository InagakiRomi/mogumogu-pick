package com.romi.mogumogu.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class RestaurantDataFormatterTest {

    @Nested
    @DisplayName("formatPhone 電話格式化測試")
    class FormatPhoneTest {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
                " ",
                "   ",
                "\t",
                "\n"
        })
        @DisplayName("電話為 null 或空白時，應回傳 null")
        void shouldReturnNullWhenPhoneIsNullOrBlank(String phone) {

            String result = RestaurantDataFormatter.formatPhone(phone);

            assertNull(result);
        }

        @ParameterizedTest
        @CsvSource({
                "0212345678, 02-1234-5678",
                "'02 1234 5678', 02-1234-5678",
                "02-1234-5678, 02-1234-5678",
                "'(02)12345678', 02-1234-5678",
                "'(02) 1234-5678', 02-1234-5678"
        })
        @DisplayName("02 市話應格式化成 02-xxxx-xxxx")
        void shouldFormatTaipeiLandline(
                String input,
                String expected) {

            String result = RestaurantDataFormatter.formatPhone(input);

            assertEquals(expected, result);
        }

        @ParameterizedTest
        @CsvSource({
                "0912345678, 0912-345-678",
                "'0912 345 678', 0912-345-678",
                "0912-345-678, 0912-345-678",
                "'(0912)345678', 0912-345-678"
        })
        @DisplayName("手機應格式化成 09xx-xxx-xxx")
        void shouldFormatMobilePhone(
                String input,
                String expected) {

            String result = RestaurantDataFormatter.formatPhone(input);

            assertEquals(expected, result);
        }

        @ParameterizedTest
        @CsvSource({
                "+886912345678, 0912-345-678",
                "886912345678, 0912-345-678",
                "'+886 912 345 678', 0912-345-678",
                "'+886-912-345-678', 0912-345-678",

                "+8860912345678, 0912-345-678",
                "8860912345678, 0912-345-678"
        })
        @DisplayName("886 手機國碼應轉回台灣國內格式")
        void shouldConvertTaiwanCountryCodeMobile(
                String input,
                String expected) {

            String result = RestaurantDataFormatter.formatPhone(input);

            assertEquals(expected, result);
        }

        @ParameterizedTest
        @CsvSource({
                "+886212345678, 02-1234-5678",
                "886212345678, 02-1234-5678",
                "'+886 2 1234 5678', 02-1234-5678",
                "'886-2-1234-5678', 02-1234-5678",

                "+8860212345678, 02-1234-5678",
                "8860212345678, 02-1234-5678"
        })
        @DisplayName("886 市話國碼應轉回台灣國內格式")
        void shouldConvertTaiwanCountryCodeLandline(
                String input,
                String expected) {

            String result = RestaurantDataFormatter.formatPhone(input);

            assertEquals(expected, result);
        }

        @ParameterizedTest
        @CsvSource({
                "031234567, '03 1234567'",
                "0412345678, '04 12345678'",
                "051234567, '05 1234567'",
                "061234567, '06 1234567'",
                "071234567, '07 1234567'",
                "081234567, '08 1234567'",

                "0371234567, '037 1234567'",
                "0491234567, '049 1234567'",
                "0821234567, '082 1234567'",
                "0891234567, '089 1234567'",

                "0826123456, '0826 123456'",
                "0836123456, '0836 123456'"
        })
        @DisplayName("其他市話應依區碼切分")
        void shouldFormatOtherLandlineAreaCodes(
                String input,
                String expected) {

            String result = RestaurantDataFormatter.formatPhone(input);

            assertEquals(expected, result);
        }

        @Test
        @DisplayName("0836 必須優先於 08")
        void shouldMatch0836Before08() {

            String result = RestaurantDataFormatter.formatPhone("0836123456");

            assertEquals("0836 123456", result);
        }

        @Test
        @DisplayName("0826 必須優先於 082 與 08")
        void shouldMatch0826Before082And08() {

            String result = RestaurantDataFormatter.formatPhone("0826123456");

            assertEquals("0826 123456", result);
        }

        @Test
        @DisplayName("082 必須優先於 08")
        void shouldMatch082Before08() {

            String result = RestaurantDataFormatter.formatPhone("0821234567");

            assertEquals("082 1234567", result);
        }

        @Test
        @DisplayName("089 必須優先於 08")
        void shouldMatch089Before08() {

            String result = RestaurantDataFormatter.formatPhone("0891234567");

            assertEquals("089 1234567", result);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "12345678",
                "912345678",
                "abcdef",
                "hello",
                "02ABCDEF",
                "0912ABC678",
                "+123456789",
                "886",
                "+886",
                "0"
        })
        @DisplayName("無法辨識的電話格式應維持原字串")
        void shouldReturnOriginalPhoneWhenInvalid(String input) {

            String result = RestaurantDataFormatter.formatPhone(input);

            assertEquals(input, result);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "021234567",
                "02123456789",
                "091234567",
                "09123456789"
        })
        @DisplayName("02 或手機長度錯誤時，不應套用標準格式")
        void shouldNotApplyStandardFormatWhenLengthInvalid(
                String input) {

            String result = RestaurantDataFormatter.formatPhone(input);

            if (input.startsWith("02")) {
                assertEquals(
                        "02 " + input.substring(2),
                        result);
            } else {
                assertEquals(
                        input,
                        result);
            }
        }

        @ParameterizedTest
        @CsvSource({
                "'02 (1234) 5678', 02-1234-5678",
                "'02-1234 5678', 02-1234-5678",
                "'09 12-345 (678)', 0912-345-678"
        })
        @DisplayName("空白、括號、減號應被移除後重新格式化")
        void shouldRemoveAllowedSeparators(
                String input,
                String expected) {

            String result = RestaurantDataFormatter.formatPhone(input);

            assertEquals(expected, result);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "02-1234-5678#123",
                "02/12345678",
                "0912.345.678",
                "0912-345-678 ext 123"
        })
        @DisplayName("未支援的特殊符號應回傳原始字串")
        void shouldReturnOriginalWhenContainsUnsupportedCharacters(
                String input) {

            String result = RestaurantDataFormatter.formatPhone(input);

            assertEquals(input, result);
        }

        @Test
        @DisplayName("0800 目前會被當成 08 區碼處理")
        void shouldCurrentlyTreat0800As08AreaCode() {

            String result = RestaurantDataFormatter.formatPhone("0800123456");

            assertEquals("08 00123456", result);
        }

        @Test
        @DisplayName("03 開頭即使號碼長度奇怪，目前仍會被視為市話")
        void shouldCurrentlyAcceptUnusualLandlineLength() {

            String result = RestaurantDataFormatter.formatPhone("03123");

            assertEquals("03 123", result);
        }
    }

    @Nested
    @DisplayName("formatOpeningHours 營業時間格式化測試")
    class FormatOpeningHoursTest {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
                " ",
                "   ",
                "\t",
                "\n"
        })
        @DisplayName("營業時間為 null 或空白時應回傳 null")
        void shouldReturnNullWhenOpeningHoursIsNullOrBlank(
                String openingHours) {

            String result = RestaurantDataFormatter
                    .formatOpeningHours(openingHours);

            assertNull(result);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "24/7",
                "24/7 ",
                " 24/7",
                " 24/7 ",
                "24/7"
        })
        @DisplayName("24/7 應顯示 24 小時營業")
        void shouldFormatTwentyFourSeven(String input) {

            String result = RestaurantDataFormatter
                    .formatOpeningHours(input);

            assertEquals("24 小時營業", result);
        }

        @ParameterizedTest
        @CsvSource({
                "'Mo-Su 09:00-18:00', '每日 09:00-18:00'",
                "'Mo-Fr 09:00-18:00', '週一至週五 09:00-18:00'",
                "'Mo-Sa 09:00-18:00', '週一至週六 09:00-18:00'",
                "'Sa-Su 09:00-18:00', '週六至週日 09:00-18:00'",
                "'Su-Fr 09:00-18:00', '週日至週五 09:00-18:00'",
                "'We-Mo 09:00-18:00', '週三至週一 09:00-18:00'"
        })
        @DisplayName("指定的星期範圍應轉成中文")
        void shouldFormatDayRanges(
                String input,
                String expected) {

            String result = RestaurantDataFormatter
                    .formatOpeningHours(input);

            assertEquals(expected, result);
        }

        @ParameterizedTest
        @CsvSource({
                "'Mo 09:00-18:00', '週一 09:00-18:00'",
                "'Tu 09:00-18:00', '週二 09:00-18:00'",
                "'We 09:00-18:00', '週三 09:00-18:00'",
                "'Th 09:00-18:00', '週四 09:00-18:00'",
                "'Fr 09:00-18:00', '週五 09:00-18:00'",
                "'Sa 09:00-18:00', '週六 09:00-18:00'",
                "'Su 09:00-18:00', '週日 09:00-18:00'"
        })
        @DisplayName("單一天星期應轉成中文")
        void shouldFormatSingleDay(
                String input,
                String expected) {

            String result = RestaurantDataFormatter
                    .formatOpeningHours(input);

            assertEquals(expected, result);
        }

        @ParameterizedTest
        @CsvSource({
                "'Mo,Tu,We', '週一、週二、週三'",
                "'Mo,We,Fr', '週一、週三、週五'",
                "'Sa,Su', '週六、週日'"
        })
        @DisplayName("逗號分隔的星期應轉成頓號")
        void shouldReplaceCommaBetweenDays(
                String input,
                String expected) {

            String result = RestaurantDataFormatter
                    .formatOpeningHours(input);

            assertEquals(expected, result);
        }

        @Test
        @DisplayName("多組營業時間的分號應改成全形分號")
        void shouldReplaceSemicolon() {

            String input = "Mo-Fr 09:00-18:00; Sa 10:00-14:00";

            String result = RestaurantDataFormatter
                    .formatOpeningHours(input);

            assertEquals(
                    "週一至週五 09:00-18:00； 週六 10:00-14:00",
                    result);
        }

        @Test
        @DisplayName("同一天有兩個時段時逗號目前也會變成頓號")
        void shouldReplaceTimeRangeCommaWithChineseComma() {

            String input = "Mo 11:00-14:00,17:00-20:00";

            String result = RestaurantDataFormatter
                    .formatOpeningHours(input);

            assertEquals(
                    "週一 11:00-14:00、17:00-20:00",
                    result);
        }

        @Test
        @DisplayName("複雜 OSM 營業時間格式應正確轉換")
        void shouldFormatComplexOpeningHours() {

            String input = "Mo-Fr 11:00-14:00,17:00-21:00; Sa-Su 11:00-22:00";

            String result = RestaurantDataFormatter
                    .formatOpeningHours(input);

            assertEquals(
                    "週一至週五 11:00-14:00、17:00-21:00； 週六至週日 11:00-22:00",
                    result);
        }

        @ParameterizedTest
        @CsvSource({
                "'Mo-Tu 09:00-18:00', '週一-週二 09:00-18:00'",
                "'Tu-Th 09:00-18:00', '週二-週四 09:00-18:00'",
                "'Fr-Su 09:00-18:00', '週五-週日 09:00-18:00'"
        })
        @DisplayName("沒有特別定義的範圍，目前只會翻譯星期而保留 -")
        void shouldTranslateUnknownDayRangesPartially(
                String input,
                String expected) {

            String result = RestaurantDataFormatter
                    .formatOpeningHours(input);

            assertEquals(expected, result);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "unknown",
                "closed",
                "PH off",
                "by appointment",
                "09:00-18:00"
        })
        @DisplayName("沒有星期代碼的內容應保持原樣")
        void shouldKeepUnknownOpeningHours(String input) {

            String result = RestaurantDataFormatter
                    .formatOpeningHours(input);

            assertEquals(input, result);
        }

        @Test
        @DisplayName("星期代碼目前區分大小寫")
        void shouldNotTranslateLowercaseDayCode() {

            String input = "mo-fr 09:00-18:00";

            String result = RestaurantDataFormatter
                    .formatOpeningHours(input);

            assertEquals(
                    "mo-fr 09:00-18:00",
                    result);
        }

        @Test
        @DisplayName("一般營業時間目前會保留前後空白")
        void shouldKeepWhitespaceForNormalOpeningHours() {

            String input = " Mo-Fr 09:00-18:00 ";

            String result = RestaurantDataFormatter
                    .formatOpeningHours(input);

            assertEquals(
                    " 週一至週五 09:00-18:00 ",
                    result);
        }
    }
}

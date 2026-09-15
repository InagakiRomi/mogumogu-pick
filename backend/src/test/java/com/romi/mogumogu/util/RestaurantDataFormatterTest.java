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
    @DisplayName("formatPhone")
    class FormatPhoneTest {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
                " ",
                "   ",
                "\t",
                "\n"
        })
        @DisplayName("Returns null when phone is null or blank")
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
        @DisplayName("Formats Taipei landlines as 02-xxxx-xxxx")
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
        @DisplayName("Formats mobile numbers as 09xx-xxx-xxx")
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
        @DisplayName("Converts 886 mobile country codes back to domestic format")
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
        @DisplayName("Converts 886 landline country codes back to domestic format")
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
        @DisplayName("Splits other landlines by area code")
        void shouldFormatOtherLandlineAreaCodes(
                String input,
                String expected) {

            String result = RestaurantDataFormatter.formatPhone(input);

            assertEquals(expected, result);
        }

        @Test
        @DisplayName("0836 takes priority over 08")
        void shouldMatch0836Before08() {

            String result = RestaurantDataFormatter.formatPhone("0836123456");

            assertEquals("0836 123456", result);
        }

        @Test
        @DisplayName("0826 takes priority over 082 and 08")
        void shouldMatch0826Before082And08() {

            String result = RestaurantDataFormatter.formatPhone("0826123456");

            assertEquals("0826 123456", result);
        }

        @Test
        @DisplayName("082 takes priority over 08")
        void shouldMatch082Before08() {

            String result = RestaurantDataFormatter.formatPhone("0821234567");

            assertEquals("082 1234567", result);
        }

        @Test
        @DisplayName("089 takes priority over 08")
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
        @DisplayName("Unrecognized phone formats keep the original string")
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
        @DisplayName("Does not apply the standard format when 02 or mobile length is wrong")
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
        @DisplayName("Removes spaces, parentheses, and hyphens before reformatting")
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
        @DisplayName("Unsupported special characters return the original string")
        void shouldReturnOriginalWhenContainsUnsupportedCharacters(
                String input) {

            String result = RestaurantDataFormatter.formatPhone(input);

            assertEquals(input, result);
        }

        @Test
        @DisplayName("0800 is currently treated as the 08 area code")
        void shouldCurrentlyTreat0800As08AreaCode() {

            String result = RestaurantDataFormatter.formatPhone("0800123456");

            assertEquals("08 00123456", result);
        }

        @Test
        @DisplayName("Numbers starting with 03 are still treated as landlines even with odd length")
        void shouldCurrentlyAcceptUnusualLandlineLength() {

            String result = RestaurantDataFormatter.formatPhone("03123");

            assertEquals("03 123", result);
        }
    }

    @Nested
    @DisplayName("formatOpeningHours")
    class FormatOpeningHoursTest {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
                " ",
                "   ",
                "\t",
                "\n"
        })
        @DisplayName("Returns null when opening hours are null or blank")
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
        @DisplayName("24/7 is formatted as the 24-hour Chinese label")
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
        @DisplayName("Known weekday ranges are translated into Chinese labels")
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
        @DisplayName("Single weekday codes are translated into Chinese labels")
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
        @DisplayName("Comma-separated weekdays become ideographic commas")
        void shouldReplaceCommaBetweenDays(
                String input,
                String expected) {

            String result = RestaurantDataFormatter
                    .formatOpeningHours(input);

            assertEquals(expected, result);
        }

        @Test
        @DisplayName("Semicolons between opening-hour groups become full-width semicolons")
        void shouldReplaceSemicolon() {

            String input = "Mo-Fr 09:00-18:00; Sa 10:00-14:00";

            String result = RestaurantDataFormatter
                    .formatOpeningHours(input);

            assertEquals(
                    "週一至週五 09:00-18:00； 週六 10:00-14:00",
                    result);
        }

        @Test
        @DisplayName("Commas between two time ranges on the same day currently become ideographic commas")
        void shouldReplaceTimeRangeCommaWithChineseComma() {

            String input = "Mo 11:00-14:00,17:00-20:00";

            String result = RestaurantDataFormatter
                    .formatOpeningHours(input);

            assertEquals(
                    "週一 11:00-14:00、17:00-20:00",
                    result);
        }

        @Test
        @DisplayName("Complex OSM opening-hours strings are converted correctly")
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
        @DisplayName("Undefined ranges currently translate weekdays and keep the hyphen")
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
        @DisplayName("Content without weekday codes is left unchanged")
        void shouldKeepUnknownOpeningHours(String input) {

            String result = RestaurantDataFormatter
                    .formatOpeningHours(input);

            assertEquals(input, result);
        }

        @Test
        @DisplayName("Weekday codes are currently case-sensitive")
        void shouldNotTranslateLowercaseDayCode() {

            String input = "mo-fr 09:00-18:00";

            String result = RestaurantDataFormatter
                    .formatOpeningHours(input);

            assertEquals(
                    "mo-fr 09:00-18:00",
                    result);
        }

        @Test
        @DisplayName("Normal opening hours currently keep leading and trailing whitespace")
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

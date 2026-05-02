package com.romi.mogumogu.logging.formatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

/** 自訂 log 輸出格式 */
public class MyCustomFormatter extends Formatter {

  // 時間格式設定
  private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
  private final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

  // ANSI 顏色碼
  private static final String RESET = "\u001B[0m";
  private static final String RED = "\u001B[31m";
  private static final String ORANGE = "\u001B[38;5;208m";
  private static final String GREEN = "\u001B[32m";
  private static final String YELLOW = "\u001B[33m";
  private static final String BLUE = "\u001B[34m";
  private static final String CYAN = "\u001B[36m";
  private static final String WHITE = "\u001B[37m";

  /** 定義 log 的最終輸出格式 */
  @Override
  public String format(LogRecord record) {
    StringBuilder sb = new StringBuilder();
    String levelColor = getColorForLevel(record.getLevel());

    // 加上時間
    sb.append(WHITE)
        .append(dateFormat.format(new Date(record.getMillis())))
        .append(RESET);

    // 加上 log 級別（INFO、WARN 等），並上色
    sb.append(" ");
    sb.append(levelColor)
        .append(record.getLevel().getName())
        .append(RESET);

    // 加上 logger 名稱（哪個類別輸出的）
    sb.append(" ");
    sb.append(YELLOW)
        .append("[")
        .append(record.getLoggerName())
        .append("]")
        .append(RESET);

    // 加上實際訊息內容
    sb.append(" : ");
    sb.append(WHITE)
        .append(formatMessage(record))
        .append(RESET)
        .append("\n");

    return sb.toString();
  }

  /** 根據 log 級別回傳對應的顏色 */
  private String getColorForLevel(Level level) {
    if (level == Level.SEVERE)
      return RED;
    if (level == Level.WARNING)
      return ORANGE;
    if (level == Level.INFO)
      return GREEN;
    if (level == Level.CONFIG)
      return CYAN;
    if (level == Level.FINE || level == Level.FINER || level == Level.FINEST)
      return BLUE;
    return WHITE;
  }
}

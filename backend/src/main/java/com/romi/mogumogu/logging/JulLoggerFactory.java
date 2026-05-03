package com.romi.mogumogu.logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

import com.romi.mogumogu.logging.formatter.AnsiColoredJulFormatter;

@Component
/** 建立並初始化指定用途的 JUL logger */
public class JulLoggerFactory {

    // 指定用途的 logger 名稱
    public static final String MAIN = "Main";
    public static final String TOOL = "Tool";

    /** 印出主用途的 logger */
    public Logger printMainLog() {
        return getConfiguredLogger(MAIN);
    }

    /** 印出工具用途的 logger */
    public Logger printToolLog() {
        return getConfiguredLogger(TOOL);
    }

    /** 回傳指定名稱的 JUL logger */
    public Logger getConfiguredLogger(String name) {
        Logger logger = Logger.getLogger(name);
        ensureConsoleHandler(logger);
        return logger;
    }

    /** 初始化 logger 的主控台輸出與格式 */
    private void ensureConsoleHandler(Logger logger) {
        synchronized (logger) {
            if (logger.getHandlers().length > 0) {
                return;
            }
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.ALL);

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.ALL);
            consoleHandler.setFormatter(new AnsiColoredJulFormatter());
            logger.addHandler(consoleHandler);
        }
    }
}

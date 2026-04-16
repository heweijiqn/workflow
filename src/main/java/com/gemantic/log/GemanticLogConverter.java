package com.gemantic.log;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.apache.commons.lang3.RegExUtils;

import java.text.Normalizer;

/**
 * 解决日志伪造漏洞
 */
public class GemanticLogConverter extends ClassicConverter {

    private static final String NORMALIZER_REGEX = "%0a|%0A|%0d|%0D|\r|\n";

    private static final String NORMALIZER_PACKAGE = "com.gemantic";

    @Override
    public String convert(ILoggingEvent event) {
        if (event.getLoggerName().startsWith(NORMALIZER_PACKAGE)) {
            return convertLog(event.getFormattedMessage());
        } else {
            return event.getFormattedMessage();
        }
    }

    public static String convertLog(String logs) {
        return RegExUtils.removeAll(Normalizer.normalize(logs, Normalizer.Form.NFKC), NORMALIZER_REGEX);
    }


}

package com.jjk.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

public class AuditLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-audit");

    public static void logEvent(String type, UUID actor, String detail) {
        LOGGER.info("[{}] actor={} {}", type, actor, detail);
    }
}

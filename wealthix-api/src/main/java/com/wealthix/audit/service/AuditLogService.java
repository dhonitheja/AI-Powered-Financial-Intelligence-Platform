package com.wealthix.audit.service;

import com.wealthix.audit.model.AuditAction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@Slf4j
public class AuditLogService {

    public void log(UUID userId, AuditAction action, String resourceType, UUID resourceId, HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        log.info("[AUDIT] User: {}, Action: {}, Resource: {}:{}, IP: {}", 
                userId, action, resourceType, resourceId, ipAddress);
        // Implementation for saving to DB would go here
    }
}

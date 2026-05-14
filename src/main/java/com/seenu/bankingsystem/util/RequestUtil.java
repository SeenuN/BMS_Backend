package com.seenu.bankingsystem.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class RequestUtil {

    public static String getClientIp() {

        ServletRequestAttributes attr =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attr == null) return "UNKNOWN";

        HttpServletRequest request = attr.getRequest();

        Object ip = request.getAttribute("clientIp");

        return ip != null ? ip.toString() : request.getRemoteAddr();
    }
}
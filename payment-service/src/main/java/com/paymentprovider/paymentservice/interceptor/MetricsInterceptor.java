package com.paymentprovider.paymentservice.interceptor;

import com.paymentprovider.paymentservice.services.PaymentMetricsService;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to collect API metrics automatically
 */
@Component
public class MetricsInterceptor implements HandlerInterceptor {

    private static final String TIMER_ATTRIBUTE = "metrics.timer";

    private final PaymentMetricsService metricsService;

    public MetricsInterceptor(PaymentMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Timer.Sample timer = metricsService.startApiTimer();
        request.setAttribute(TIMER_ATTRIBUTE, timer);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Timer.Sample timer = (Timer.Sample) request.getAttribute(TIMER_ATTRIBUTE);
        if (timer != null) {
            String endpoint = getEndpointName(request);
            String method = request.getMethod();
            int statusCode = response.getStatus();

            metricsService.stopApiTimer(timer, endpoint, method, statusCode);
        }
    }

    private String getEndpointName(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }

        // Normalize path parameters
        return uri.replaceAll("/\\d+", "/{id}")
                .replaceAll("/[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}", "/{uuid}");
    }
}
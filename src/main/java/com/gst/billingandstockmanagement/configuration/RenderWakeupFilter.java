package com.gst.billingandstockmanagement.configuration;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Order(1)
public class RenderWakeupFilter implements Filter {

    @Autowired
    private RenderWarmupService renderWarmupService;

    private final AtomicBoolean firstRequest = new AtomicBoolean(true);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (firstRequest.compareAndSet(true, false)) {
            // First request after startup — trigger warmup asynchronously
            renderWarmupService.triggerWarmup();
        }

        // Never blocks the actual request
        chain.doFilter(request, response);
    }
}
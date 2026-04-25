package com.gst.billingandstockmanagement.configuration;

import jakarta.servlet.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Order(1)
public class KoyebWakeupFilter implements Filter {

    @Autowired
    private KoyebWarmupService renderWarmupService;

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
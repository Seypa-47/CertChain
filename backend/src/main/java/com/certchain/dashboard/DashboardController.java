package com.certchain.dashboard;

import com.certchain.auth.AuthenticatedPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboard;
    public DashboardController(DashboardService dashboard) { this.dashboard = dashboard; }
    @GetMapping
    public DashboardResponse get(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return dashboard.get(principal.organizationId());
    }
}

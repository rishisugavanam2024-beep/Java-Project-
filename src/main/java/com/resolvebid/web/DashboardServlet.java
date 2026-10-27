package com.resolvebid.web;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.resolvebid.model.Proposal;
import com.resolvebid.model.ServiceRequest;
import com.resolvebid.model.UserAccount;
import com.resolvebid.service.MarketplaceService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class DashboardServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final MarketplaceService service;
    public DashboardServlet(MarketplaceService service) { this.service = service; }
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        UserAccount user = WebSupport.user(request);
        List<ServiceRequest> requests = service.requestsFor(user);
        Map<Long, List<Proposal>> proposals = new LinkedHashMap<>();
        for (ServiceRequest item : requests) proposals.put(item.id(), service.rankedProposals(item.id()));
        request.setAttribute("user", user); request.setAttribute("requests", requests);
        request.setAttribute("proposals", proposals); request.setAttribute("stats", service.stats());
        request.setAttribute("categories", MarketplaceService.CATEGORIES);
        request.setAttribute("activity", service.recentActivity());
        WebSupport.consumeFlash(request); WebSupport.page(request, response, "dashboard");
    }
}

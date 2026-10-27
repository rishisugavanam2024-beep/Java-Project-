package com.resolvebid.web;

import java.io.IOException;
import java.util.stream.Collectors;
import com.resolvebid.model.ServiceRequest;
import com.resolvebid.model.UserAccount;
import com.resolvebid.service.MarketplaceService;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class RequestApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final MarketplaceService service;

    public RequestApiServlet(MarketplaceService service) { this.service = service; }

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        UserAccount user = WebSupport.user(request);
        String json = service.requestsFor(user).stream().map(this::json).collect(Collectors.joining(",", "[", "]"));
        response.getWriter().write(json);
    }

    private String json(ServiceRequest r) {
        return "{\"id\":" + r.id() + ",\"category\":\"" + escape(r.category()) + "\",\"description\":\"" + escape(r.description()) + "\",\"status\":\"" + r.status() + "\"}";
    }

    private String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"); }
}

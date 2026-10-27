package com.resolvebid.web;

import java.io.IOException;
import java.time.LocalDate;
import com.resolvebid.exception.AuthorizationException;
import com.resolvebid.exception.ValidationException;
import com.resolvebid.model.Proposal;
import com.resolvebid.model.ServiceRequest;
import com.resolvebid.model.UserAccount;
import com.resolvebid.service.MarketplaceService;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class MarketplaceActionServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final MarketplaceService service;
    public MarketplaceActionServlet(MarketplaceService service) { this.service = service; }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!WebSupport.validCsrf(request)) { response.sendError(403, "Security token expired."); return; }
        UserAccount user = WebSupport.user(request);
        try {
            switch (request.getPathInfo() == null ? "" : request.getPathInfo()) {
                case "/request" -> {
                    long id = service.postRequest(user, request.getParameter("category"), request.getParameter("description"),
                            request.getParameter("urgency"), request.getParameter("budget"));
                    WebSupport.flash(request, "success", "Request #" + id + " is live. Providers can now respond.");
                }
                case "/proposal" -> {
                    ServiceRequest item = service.requests().stream().filter(r -> r.id() == WebSupport.longParam(request, "requestId")).findFirst()
                            .orElseThrow(() -> new ValidationException("That request is no longer available."));
                    service.submitProposal(user, item, request.getParameter("diagnosis"), request.getParameter("price"),
                            request.getParameter("etaDays"), request.getParameter("warrantyDays"));
                    WebSupport.flash(request, "success", "Your proposal was submitted.");
                }
                case "/accept" -> {
                    long requestId = WebSupport.longParam(request, "requestId");
                    long proposalId = WebSupport.longParam(request, "proposalId");
                    ServiceRequest item = service.requests().stream().filter(r -> r.id() == requestId).findFirst()
                            .orElseThrow(() -> new ValidationException("Request not found."));
                    Proposal proposal = service.proposals(requestId).stream().filter(p -> p.id() == proposalId).findFirst()
                            .orElseThrow(() -> new ValidationException("Proposal not found."));
                    service.acceptProposal(user, item, proposal, LocalDate.parse(request.getParameter("date")));
                    WebSupport.flash(request, "success", "Proposal accepted and service booked.");
                }
                default -> throw new ValidationException("Unknown action.");
            }
        } catch (ValidationException | AuthorizationException | IllegalArgumentException ex) {
            WebSupport.flash(request, "error", ex.getMessage() == null ? "Please check the form." : ex.getMessage());
        } catch (RuntimeException ex) {
            WebSupport.flash(request, "error", "The request could not be completed. Please try again.");
        }
        WebSupport.redirect(request, response, "/app/dashboard");
    }
}

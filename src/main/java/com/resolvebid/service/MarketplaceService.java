package com.resolvebid.service;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.resolvebid.exception.AuthorizationException;
import com.resolvebid.dao.DatabaseManager;
import com.resolvebid.exception.ValidationException;
import com.resolvebid.model.Booking;
import com.resolvebid.model.DashboardStats;
import com.resolvebid.model.Proposal;
import com.resolvebid.model.Permission;
import com.resolvebid.model.Role;
import com.resolvebid.model.ServiceRequest;
import com.resolvebid.model.UserAccount;
import com.resolvebid.util.FileService;
import com.resolvebid.util.Validation;

public final class MarketplaceService {
    public static final String[] CATEGORIES = { "Home Appliances", "Computer Repair", "Electrical",
            "Plumbing", "Vehicle Service", "Other" };
    private final DatabaseManager database;
    private final FileService files;
    private final ProposalRankingStrategy rankingStrategy;
    private final Set<String> supportedCategories = new LinkedHashSet<>(List.of(CATEGORIES));
    private final LinkedList<String> recentActivity = new LinkedList<>();

    public MarketplaceService(DatabaseManager database, FileService files, ProposalRankingStrategy rankingStrategy) {
        this.database = database;
        this.files = files;
        this.rankingStrategy = rankingStrategy;
    }

    public long postRequest(String customer, String category, String description, String urgency, String budget) throws ValidationException {
        String validCustomer = Validation.required(customer, "Customer name");
        String validCategory = Validation.required(category, "Category");
        String validDescription = Validation.required(description, "Problem description");
        if (validDescription.length() < 12) throw new ValidationException("Describe the problem in at least 12 characters.");
        BigDecimal validBudget = Validation.positiveMoney(budget, "Budget");
        long id = database.createRequest(validCustomer, validCategory, validDescription, Validation.required(urgency, "Urgency"), validBudget);
        files.appendAudit("Request #" + id + " posted by " + validCustomer);
        return id;
    }

    public long postRequest(UserAccount user, String category, String description, String urgency, String budget)
            throws ValidationException {
        require(user, Permission.POST_REQUEST);
        String validCategory = Validation.required(category, "Category");
        if (!supportedCategories.contains(validCategory)) throw new ValidationException("Choose a supported category.");
        String validDescription = Validation.required(description, "Problem description");
        if (validDescription.length() < 12) throw new ValidationException("Describe the problem in at least 12 characters.");
        long id = database.createRequest(user.id(), user.displayName(), validCategory, validDescription,
                Validation.required(urgency, "Urgency"), Validation.positiveMoney(budget, "Budget"));
        record("Request #" + id + " posted by " + user.displayName());
        files.appendAudit("Request #" + id + " posted by " + user.username());
        return id;
    }

    public void submitProposal(ServiceRequest request, String provider, String diagnosis, String price,
            String etaDays, String warrantyDays) throws ValidationException {
        if (request == null) throw new ValidationException("Select a request first.");
        String validDiagnosis = Validation.required(diagnosis, "Diagnosis");
        if (validDiagnosis.length() < 12) throw new ValidationException("Provide a more specific diagnosis.");
        database.createProposal(request.id(), Validation.required(provider, "Provider name"), validDiagnosis,
                Validation.positiveMoney(price, "Price"), Validation.positiveInt(etaDays, "ETA"),
                Validation.positiveInt(warrantyDays, "Warranty"));
        files.appendAudit("Proposal submitted for request #" + request.id());
    }

    public void submitProposal(UserAccount user, ServiceRequest request, String diagnosis, String price,
            String etaDays, String warrantyDays) throws ValidationException {
        require(user, Permission.SUBMIT_PROPOSAL);
        if (request == null) throw new ValidationException("Select a request first.");
        String validDiagnosis = Validation.required(diagnosis, "Diagnosis");
        if (validDiagnosis.length() < 12) throw new ValidationException("Provide a more specific diagnosis.");
        database.createProposal(request.id(), user.id(), user.displayName(), validDiagnosis,
                Validation.positiveMoney(price, "Price"), Validation.positiveInt(etaDays, "ETA"),
                Validation.positiveInt(warrantyDays, "Warranty"));
        record(user.displayName() + " proposed on request #" + request.id());
        files.appendAudit("Proposal submitted by " + user.username() + " for request #" + request.id());
    }

    public void acceptProposal(ServiceRequest request, Proposal proposal, LocalDate date) throws ValidationException {
        if (request == null || proposal == null) throw new ValidationException("Select a request and proposal.");
        if (date == null || date.isBefore(LocalDate.now())) throw new ValidationException("Choose today or a future service date.");
        database.book(request.id(), proposal.id(), date);
        files.appendAudit("Proposal #" + proposal.id() + " accepted for request #" + request.id());
    }

    public void acceptProposal(UserAccount user, ServiceRequest request, Proposal proposal, LocalDate date)
            throws ValidationException {
        require(user, Permission.ACCEPT_PROPOSAL);
        if (request == null || proposal == null || proposal.requestId() != request.id())
            throw new ValidationException("Select a valid request and proposal.");
        if (user.role() != Role.ADMIN && !database.requestOwnedBy(request.id(), user.id()))
            throw new AuthorizationException("You can only accept proposals for your own requests.");
        if (date == null || date.isBefore(LocalDate.now())) throw new ValidationException("Choose today or a future service date.");
        database.book(request.id(), proposal.id(), date);
        record("Proposal #" + proposal.id() + " accepted for request #" + request.id());
        files.appendAudit("Proposal #" + proposal.id() + " accepted by " + user.username());
    }

    public List<ServiceRequest> requests() { return database.findRequests(); }
    public List<ServiceRequest> requestsFor(UserAccount user) {
        if (user == null) throw new AuthorizationException("Sign in to continue.");
        return user.role() == Role.CUSTOMER ? database.findRequestsForCustomer(user.id()) : database.findRequests();
    }
    public List<Proposal> proposals(long requestId) { return database.findProposals(requestId); }
    public List<Booking> bookings() { return database.findBookings(); }
    public DashboardStats stats() { return database.stats(); }

    public Map<Long, Double> scores(List<Proposal> proposals) {
        Map<Long, Double> scores = new LinkedHashMap<>();
        proposals.forEach(p -> scores.put(p.id(), rankingStrategy.score(p)));
        return scores;
    }

    public List<Proposal> rankedProposals(long requestId) {
        List<Proposal> ranked = new ArrayList<>(proposals(requestId));
        ranked.sort(Comparator.comparingDouble(rankingStrategy::score).reversed());
        return ranked;
    }

    public String rankingExplanation() { return rankingStrategy.explanation(); }
    public Set<String> supportedCategories() { return Set.copyOf(supportedCategories); }
    public synchronized List<String> recentActivity() { return List.copyOf(recentActivity); }
    public Path exportBookings() throws Exception { return files.exportBookings(bookings()); }

    private void require(UserAccount user, Permission permission) {
        if (user == null || !user.can(permission)) throw new AuthorizationException("Your role cannot perform this action.");
    }

    private synchronized void record(String message) {
        recentActivity.addFirst(message);
        while (recentActivity.size() > 50) recentActivity.removeLast();
    }
}

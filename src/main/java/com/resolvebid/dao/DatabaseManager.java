package com.resolvebid.dao;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.resolvebid.exception.DataAccessException;
import com.resolvebid.model.Booking;
import com.resolvebid.model.DashboardStats;
import com.resolvebid.model.Proposal;
import com.resolvebid.model.ProposalStatus;
import com.resolvebid.model.RequestStatus;
import com.resolvebid.model.Role;
import com.resolvebid.model.ServiceRequest;
import com.resolvebid.model.UserAccount;
import com.resolvebid.model.UserCredentials;

public final class DatabaseManager {
    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager(Path databasePath) {
        this("jdbc:h2:file:" + databasePath.toAbsolutePath().toString().replace('\\', '/')
                + ";MODE=MySQL", "sa", "");
    }

    public DatabaseManager(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    private Connection connection() throws SQLException { return DriverManager.getConnection(url, user, password); }

    public void initialize() {
        String[] schema = {
            "CREATE TABLE IF NOT EXISTS users (id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(64) UNIQUE NOT NULL, display_name VARCHAR(100) NOT NULL, password_hash VARBINARY(64) NOT NULL, password_salt VARBINARY(32) NOT NULL, password_iterations INT NOT NULL, role VARCHAR(20) NOT NULL, enabled BOOLEAN NOT NULL, created_at TIMESTAMP NOT NULL)",
            "CREATE TABLE IF NOT EXISTS service_requests (id BIGINT AUTO_INCREMENT PRIMARY KEY, customer_user_id BIGINT NULL, customer_name VARCHAR(100) NOT NULL, category VARCHAR(60) NOT NULL, description VARCHAR(1000) NOT NULL, urgency VARCHAR(30) NOT NULL, budget DECIMAL(12,2) NOT NULL, status VARCHAR(20) NOT NULL, created_at TIMESTAMP NOT NULL, FOREIGN KEY(customer_user_id) REFERENCES users(id))",
            "CREATE TABLE IF NOT EXISTS proposals (id BIGINT AUTO_INCREMENT PRIMARY KEY, request_id BIGINT NOT NULL, provider_user_id BIGINT NULL, provider_name VARCHAR(100) NOT NULL, diagnosis VARCHAR(1000) NOT NULL, price DECIMAL(12,2) NOT NULL, eta_days INT NOT NULL, warranty_days INT NOT NULL, status VARCHAR(20) NOT NULL, created_at TIMESTAMP NOT NULL, FOREIGN KEY(request_id) REFERENCES service_requests(id), FOREIGN KEY(provider_user_id) REFERENCES users(id))",
            "CREATE TABLE IF NOT EXISTS bookings (id BIGINT AUTO_INCREMENT PRIMARY KEY, request_id BIGINT NOT NULL, proposal_id BIGINT NOT NULL, scheduled_for DATE NOT NULL, status VARCHAR(30) NOT NULL, FOREIGN KEY(request_id) REFERENCES service_requests(id), FOREIGN KEY(proposal_id) REFERENCES proposals(id))",
            "CREATE TABLE IF NOT EXISTS reviews (id BIGINT AUTO_INCREMENT PRIMARY KEY, booking_id BIGINT NOT NULL, rating INT NOT NULL, comment VARCHAR(1000), FOREIGN KEY(booking_id) REFERENCES bookings(id))"
        };
        try (Connection c = connection(); Statement s = c.createStatement()) {
            for (String sql : schema) s.execute(sql);
            s.execute("ALTER TABLE service_requests ADD COLUMN IF NOT EXISTS customer_user_id BIGINT NULL");
            s.execute("ALTER TABLE proposals ADD COLUMN IF NOT EXISTS provider_user_id BIGINT NULL");
            try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM service_requests")) {
                rs.next();
                if (rs.getLong(1) == 0) seed(c);
            }
        } catch (SQLException ex) { throw new DataAccessException("Could not initialize the database.", ex); }
    }

    private void seed(Connection c) throws SQLException {
        long ac = insertRequest(c, null, "Aarav Mehta", "Home Appliances", "AC runs but the room is not cooling.", "Within 24 hours", new BigDecimal("4500"));
        long laptop = insertRequest(c, null, "Diya Rao", "Computer Repair", "Laptop overheats and shuts down during classes.", "This week", new BigDecimal("7000"));
        insertProposal(c, ac, null, "CoolCare Services", "Likely refrigerant leak; pressure test and refill included.", new BigDecimal("2800"), 1, 90);
        insertProposal(c, ac, null, "ArcticFix", "Indoor coil inspection, deep clean and gas-level correction.", new BigDecimal("2400"), 2, 60);
        insertProposal(c, laptop, null, "ByteWorks", "Thermal service with fan test and new thermal compound.", new BigDecimal("2200"), 2, 30);
    }

    private long insertRequest(Connection c, Long customerId, String customer, String category, String description,
            String urgency, BigDecimal budget) throws SQLException {
        String sql = "INSERT INTO service_requests(customer_user_id,customer_name,category,description,urgency,budget,status,created_at) VALUES(?,?,?,?,?,?,'OPEN',?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            nullableLong(ps, 1, customerId); ps.setString(2, customer); ps.setString(3, category);
            ps.setString(4, description); ps.setString(5, urgency); ps.setBigDecimal(6, budget);
            ps.setObject(7, LocalDateTime.now()); ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); return keys.getLong(1); }
        }
    }

    private void insertProposal(Connection c, long requestId, Long providerId, String provider, String diagnosis,
            BigDecimal price, int eta, int warranty) throws SQLException {
        String sql = "INSERT INTO proposals(request_id,provider_user_id,provider_name,diagnosis,price,eta_days,warranty_days,status,created_at) VALUES(?,?,?,?,?,?,?,'PENDING',?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, requestId); nullableLong(ps, 2, providerId); ps.setString(3, provider);
            ps.setString(4, diagnosis); ps.setBigDecimal(5, price); ps.setInt(6, eta);
            ps.setInt(7, warranty); ps.setObject(8, LocalDateTime.now()); ps.executeUpdate();
        }
    }

    private void nullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) ps.setNull(index, java.sql.Types.BIGINT); else ps.setLong(index, value);
    }

    public long createRequest(String customer, String category, String description, String urgency, BigDecimal budget) {
        return createRequest(null, customer, category, description, urgency, budget);
    }

    public long createRequest(Long customerId, String customer, String category, String description, String urgency, BigDecimal budget) {
        try (Connection c = connection()) { return insertRequest(c, customerId, customer, category, description, urgency, budget); }
        catch (SQLException ex) { throw new DataAccessException("Could not save the request.", ex); }
    }

    public void createProposal(long requestId, String provider, String diagnosis, BigDecimal price, int eta, int warranty) {
        createProposal(requestId, null, provider, diagnosis, price, eta, warranty);
    }

    public void createProposal(long requestId, Long providerId, String provider, String diagnosis, BigDecimal price, int eta, int warranty) {
        try (Connection c = connection()) { insertProposal(c, requestId, providerId, provider, diagnosis, price, eta, warranty); }
        catch (SQLException ex) { throw new DataAccessException("Could not save the proposal.", ex); }
    }

    public List<ServiceRequest> findRequests() { return queryRequests("SELECT * FROM service_requests ORDER BY created_at DESC", null); }

    public List<ServiceRequest> findRequestsForCustomer(long customerId) {
        return queryRequests("SELECT * FROM service_requests WHERE customer_user_id=? ORDER BY created_at DESC", customerId);
    }

    private List<ServiceRequest> queryRequests(String sql, Long customerId) {
        List<ServiceRequest> result = new ArrayList<>();
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (customerId != null) ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(mapRequest(rs)); }
            return result;
        } catch (SQLException ex) { throw new DataAccessException("Could not load requests.", ex); }
    }

    public Optional<ServiceRequest> findRequest(long id) {
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM service_requests WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(mapRequest(rs)) : Optional.empty(); }
        } catch (SQLException ex) { throw new DataAccessException("Could not load the request.", ex); }
    }

    public boolean requestOwnedBy(long requestId, long customerId) {
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM service_requests WHERE id=? AND customer_user_id=?")) {
            ps.setLong(1, requestId); ps.setLong(2, customerId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1) == 1; }
        } catch (SQLException ex) { throw new DataAccessException("Could not verify request ownership.", ex); }
    }

    public List<Proposal> findProposals(long requestId) {
        List<Proposal> result = new ArrayList<>();
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM proposals WHERE request_id=? ORDER BY created_at DESC")) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(mapProposal(rs)); }
            return result;
        } catch (SQLException ex) { throw new DataAccessException("Could not load proposals.", ex); }
    }

    public Optional<Proposal> findProposal(long proposalId) {
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM proposals WHERE id=?")) {
            ps.setLong(1, proposalId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(mapProposal(rs)) : Optional.empty(); }
        } catch (SQLException ex) { throw new DataAccessException("Could not load the proposal.", ex); }
    }

    public void book(long requestId, long proposalId, LocalDate date) {
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            try (PreparedStatement insert = c.prepareStatement("INSERT INTO bookings(request_id,proposal_id,scheduled_for,status) VALUES(?,?,?,'CONFIRMED')");
                 PreparedStatement request = c.prepareStatement("UPDATE service_requests SET status='BOOKED' WHERE id=? AND status='OPEN'");
                 PreparedStatement proposal = c.prepareStatement("UPDATE proposals SET status=CASE WHEN id=? THEN 'ACCEPTED' ELSE 'REJECTED' END WHERE request_id=?")) {
                insert.setLong(1, requestId); insert.setLong(2, proposalId); insert.setObject(3, date); insert.executeUpdate();
                request.setLong(1, requestId);
                if (request.executeUpdate() != 1) throw new SQLException("Request is no longer available.");
                proposal.setLong(1, proposalId); proposal.setLong(2, requestId); proposal.executeUpdate();
                c.commit();
            } catch (SQLException ex) { c.rollback(); throw ex; }
        } catch (SQLException ex) { throw new DataAccessException("Could not confirm the booking.", ex); }
    }

    public List<Booking> findBookings() {
        String sql = "SELECT b.id,b.request_id,b.proposal_id,p.provider_name,r.category,b.scheduled_for,b.status FROM bookings b JOIN proposals p ON p.id=b.proposal_id JOIN service_requests r ON r.id=b.request_id ORDER BY b.id DESC";
        List<Booking> result = new ArrayList<>();
        try (Connection c = connection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) result.add(new Booking(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getString(4),rs.getString(5),rs.getObject(6,LocalDate.class),rs.getString(7)));
            return result;
        } catch (SQLException ex) { throw new DataAccessException("Could not load bookings.", ex); }
    }

    public DashboardStats stats() {
        try (Connection c = connection(); Statement s = c.createStatement()) {
            long open = scalar(s, "SELECT COUNT(*) FROM service_requests WHERE status='OPEN'");
            long proposals = scalar(s, "SELECT COUNT(*) FROM proposals");
            long bookings = scalar(s, "SELECT COUNT(*) FROM bookings");
            long completed = scalar(s, "SELECT COUNT(*) FROM service_requests WHERE status='COMPLETED'");
            return new DashboardStats(open, proposals, bookings, completed);
        } catch (SQLException ex) { throw new DataAccessException("Could not load dashboard metrics.", ex); }
    }

    public long userCount() {
        try (Connection c = connection(); Statement s = c.createStatement()) { return scalar(s, "SELECT COUNT(*) FROM users"); }
        catch (SQLException ex) { throw new DataAccessException("Could not count users.", ex); }
    }

    public long createUser(String username, String displayName, byte[] hash, byte[] salt, int iterations, Role role) {
        String sql = "INSERT INTO users(username,display_name,password_hash,password_salt,password_iterations,role,enabled,created_at) VALUES(?,?,?,?,?,?,TRUE,?)";
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username); ps.setString(2, displayName); ps.setBytes(3, hash); ps.setBytes(4, salt);
            ps.setInt(5, iterations); ps.setString(6, role.name()); ps.setObject(7, LocalDateTime.now()); ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); return keys.getLong(1); }
        } catch (SQLException ex) { throw new DataAccessException("Could not create the account.", ex); }
    }

    public Optional<UserCredentials> findCredentials(String username) {
        String sql = "SELECT * FROM users WHERE LOWER(username)=LOWER(?)";
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                UserAccount account = new UserAccount(rs.getLong("id"), rs.getString("username"),
                        rs.getString("display_name"), Role.valueOf(rs.getString("role")));
                return Optional.of(new UserCredentials(account, rs.getBytes("password_hash"),
                        rs.getBytes("password_salt"), rs.getInt("password_iterations"), rs.getBoolean("enabled")));
            }
        } catch (SQLException ex) { throw new DataAccessException("Could not load the account.", ex); }
    }

    private long scalar(Statement s, String sql) throws SQLException { try (ResultSet rs = s.executeQuery(sql)) { rs.next(); return rs.getLong(1); } }
    private ServiceRequest mapRequest(ResultSet rs) throws SQLException { return new ServiceRequest(rs.getLong("id"),rs.getString("customer_name"),rs.getString("category"),rs.getString("description"),rs.getString("urgency"),rs.getBigDecimal("budget"),RequestStatus.valueOf(rs.getString("status")),rs.getObject("created_at",LocalDateTime.class)); }
    private Proposal mapProposal(ResultSet rs) throws SQLException { return new Proposal(rs.getLong("id"),rs.getLong("request_id"),rs.getString("provider_name"),rs.getString("diagnosis"),rs.getBigDecimal("price"),rs.getInt("eta_days"),rs.getInt("warranty_days"),ProposalStatus.valueOf(rs.getString("status")),rs.getObject("created_at",LocalDateTime.class)); }
}

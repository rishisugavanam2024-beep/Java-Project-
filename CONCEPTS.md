# Java concept map — exact implementation

| Concept | Exact files | What it does |
|---|---|---|
| OOP | `model/*`, `MarketplaceService.java`, `DatabaseManager.java`, `AppShell.java` | Separates domain objects, business rules, database access, and UI responsibilities. |
| Inheritance | `Customer.java`, `ServiceProvider.java` | Both extend the common `MarketplaceUser` base class. Every servlet also inherits from Jakarta `HttpServlet`; `ResolveBidApp` inherits from JavaFX `Application`. |
| Polymorphism | `ProposalRankingStrategy.java`, `ValueRankingStrategy.java`; `MarketplaceUser.java` subclasses | `MarketplaceService` calls the strategy interface without depending on one scoring implementation. User subclasses override `roleLabel()`. |
| Packages | `com.resolvebid.app`, `.config`, `.dao`, `.exception`, `.model`, `.security`, `.service`, `.ui`, `.util`, `.web` | Organizes the full application by responsibility. |
| Abstract class | `MarketplaceUser.java` | Defines shared identity fields and the abstract `roleLabel()` method implemented by customer/provider subclasses. |
| User-defined exceptions | `ValidationException.java`, `AuthorizationException.java`, `DataAccessException.java` | Represents invalid input, forbidden role actions, and JDBC failures with application-specific messages. |
| File handling | `FileService.java` | Appends audit events to a text log and exports booking records to CSV. |
| Arrays | `MarketplaceService.CATEGORIES` | A fixed `String[]` of supported service categories used by both UIs. |
| ArrayList / List | `DatabaseManager.java`, `MarketplaceService.java` | Loads JDBC rows into typed lists and sorts proposal rankings. |
| LinkedList | `MarketplaceService.recentActivity` | Stores the newest 50 marketplace events as a bounded activity feed. |
| Set | `MarketplaceService.supportedCategories` | A `LinkedHashSet` validates unique categories while preserving display order. |
| Map | `MarketplaceService.scores`, `DashboardServlet.java` | Associates proposal IDs with scores and request IDs with their proposal lists. |
| JavaFX | `ResolveBidApp.java`, `LoginView.java`, `AppShell.java` | Builds the complete desktop login and role-aware marketplace UI in Java. |
| JDBC + RDBMS | `DatabaseManager.java`, `AppConfig.java` | Uses JDBC prepared statements, generated keys, joins, schema creation, and a transaction. H2 is the local RDBMS; MySQL 8.4 is the deployment RDBMS. |
| Servlets | `LoginServlet.java`, `RegisterServlet.java`, `DashboardServlet.java`, `MarketplaceActionServlet.java`, `RequestApiServlet.java`, `HealthServlet.java` | Handles authentication, HTML workflow actions, JSON data, and health checks in embedded Tomcat. |
| JSP | `WEB-INF/views/login.jsp`, `register.jsp`, `dashboard.jsp` | Server-renders the secure responsive website; JSP files cannot be requested directly because they are under `WEB-INF`. |
| Multithreading | `AsyncTaskService.java`, `ResolveBidApp.java`, `EmbeddedWebServer.java` | A 3-thread worker pool runs database/export work away from the JavaFX UI thread; `Platform.runLater` returns results safely to the UI; a separate daemon thread starts embedded Tomcat so the desktop window never freezes. Tomcat also handles web requests concurrently. |

Security-related implementations are in `security/*`, `AuthFilter.java`, and the role checks inside `MarketplaceService.java`.

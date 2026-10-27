# ResolveBid

ResolveBid is a complete Java 21 service marketplace: customers post real-world problems, providers submit proposals, and customers compare and book the right solution. It includes a JavaFX desktop client and a responsive Servlet/JSP website backed by JDBC.

## Run in Eclipse

1. In Package Explorer, expand `resolvebid`.
2. Right-click `pom.xml` → **Maven** → **Update Project** → **OK**.
3. To run JavaFX: right-click `ResolveBid.launch` → **Run As** → **ResolveBid**. Sign in as `admin` / `ChangeMe123!` for local development.
4. To run only the website: open **Run Configurations** → **Maven Build** → set Goals to `exec:java` only if you configure the main class, or run `ResolveBidServer.java` as a Java Application.
5. Open `http://localhost:8090/login`.

The local run uses the embedded H2 relational database. Deployment uses MySQL 8.4 through the same JDBC data-access layer.

## Command line

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
java -jar target\resolvebid-1.0.0.jar
```

The local Maven 3.9.11 installation and dependencies are already present in `.tools`. See [DEPLOYMENT.md](DEPLOYMENT.md) for Docker + MySQL deployment.

## Security

- PBKDF2-HMAC-SHA256 salted password hashes (210,000 iterations)
- customer, provider, and administrator roles with server-side authorization
- CSRF tokens on every state-changing website form
- session ID rotation after login and 30-minute idle timeout
- login throttling after repeated failures
- HttpOnly, SameSite=Strict cookies; optional Secure cookies
- CSP, clickjacking, MIME sniffing, referrer, and permissions headers
- prepared JDBC statements and transactional booking
- environment variables for database and administrator secrets

## Java concepts demonstrated

See [CONCEPTS.md](CONCEPTS.md) for exact files and processes covering OOP, inheritance, polymorphism, packages, abstract classes, user-defined exceptions, file handling, arrays, lists, linked lists, sets, maps, JavaFX, JDBC, Servlets, JSP, and multithreading.

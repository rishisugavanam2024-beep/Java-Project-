# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

Java 21, JavaFX desktop client, Jakarta Servlets and JSP on embedded Apache Tomcat, JDBC, H2 for local development, MySQL for deployment, Maven, and Docker.

## Users

- Customers describe a service problem, compare provider proposals, and confirm a booking.
- Service providers review open requests and submit diagnosis, price, ETA, and warranty proposals.
- Administrators operate and verify the deployed service.

## Product Purpose

ResolveBid is a reverse marketplace for problem-based services. Customers begin with a problem rather than searching a catalogue of predefined services. Providers compete with comparable proposals, and the customer selects the best fit.

## Positioning

The product structures a problem into a competitive service request and compares provider value across price, speed, and warranty.

## Operating Context

The product has a deployable browser interface and a JavaFX desktop client. Both clients use the same Java service layer and JDBC database.

## Capabilities and Constraints

- Java is the application language.
- JavaFX remains the desktop user interface.
- The deployable website uses Servlets and JSP.
- Customer and provider accounts have separate permissions.
- Authentication secrets and production database credentials come from environment variables.
- Deployment targets a Dockerized Java server with MySQL.
- The required academic concepts remain visible in the implementation: OOP, packages, inheritance, polymorphism, abstract classes, custom exceptions, file handling, collections, JavaFX, JDBC, Servlets, JSP, and multithreading.

## Brand Commitments

The working product name is ResolveBid. The voice is direct, practical, and trustworthy.

## Evidence on Hand

The original five-slide FixMatch project brief defines the reverse-marketplace workflow and required Java concepts. No external testimonials, commercial claims, or production usage evidence exists.

## Product Principles

- A customer can describe symptoms without knowing the technical solution.
- Proposals remain comparable and transparent.
- Security is part of every workflow, not a separate administrative feature.
- Deployment configuration never hardcodes production credentials.
- Database and file work must not freeze the JavaFX interface.

## Accessibility & Inclusion

The web interface must support keyboard navigation, visible focus states, responsive layouts, readable contrast, and clear error recovery.

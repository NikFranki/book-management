# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

Next.js App Router with TypeScript for the separate frontend; Spring Boot remains the REST API backend.

## Users

- Administrators manage the book catalog and inventory, and can review all borrowing records.
- Readers search the catalog, borrow available books, and return their own loans.

## Product Purpose

Provide a usable library-management interface while serving as a hands-on project for learning Spring Boot and a modern web frontend. Success means users can sign in, find a book, and complete catalog or borrowing tasks through the existing API.

## Positioning

The project connects book availability and personal borrowing history in one small library workflow: borrowing reduces the available count, and returning restores it while keeping the record.

## Operating Context

The frontend runs separately from the Spring Boot API during development. Administrators primarily manage the catalog on desktop; readers also need to search and manage loans on a phone.

## Capabilities and Constraints

- Authentication uses a JWT returned by `POST /api/auth/login`..
- Registration creates a reader account; public registration cannot assign administrator role.
- The book API supports paginated search, CRUD, and available-copy counts.
- Borrowing records are tied to the authenticated username; readers can see and return their own records.
- The backend is available at `http://localhost:8080` during local development.
- The frontend must respect the role rules enforced by the backend and handle API errors and expired tokens.

## Evidence on Hand

- Existing backend endpoints and request/response models under `src/main/java/com/example/bookmanagement/`.
- No supplied brand assets, customer claims, or production library data. Use only real API responses or clearly labeled empty states.

## Product Principles

- Make book availability and loan state easy to scan.
- Keep administrator catalog tasks and reader borrowing tasks clear by role.
- Preserve borrowing history while showing the current status.
- Explain errors in plain language and give a clear recovery action.

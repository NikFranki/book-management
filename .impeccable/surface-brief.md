# 书间前端 · 馆藏与借阅

## Direction

Approved direction: **B · 纸本文库**. The user selected this direction from `.impeccable/mocks/decision/index.html`.

- World: a calm reading room and printed library catalogue.
- Composition: warm-paper navigation at left; quiet catalogue work area; book titles and availability are the primary scan targets.
- Type and material: restrained serif wordmark and page title, readable sans-serif controls, paper and cloth-cover tones, fine separators.
- Signature interaction: book catalogue actions stay adjacent to each title; create/edit tasks use a focused side drawer.
- Cross-surface reach: desktop remains the administrative workspace; mobile moves navigation to a compact two-tab bar and allows catalogue overflow to scroll horizontally.
- Honest risk: the warm palette and book-cover cues must not lower contrast or make inventory status ambiguous.

## First surface

The home screen serves the signed-in user's role. Administrators search, page, create, edit, and delete catalogue entries. Readers search the same live catalogue, borrow available copies, and view or return their own loans. Empty, loading, error, and success states use backend-backed content only.

## Constraints

- The Spring Boot API remains at `http://localhost:8080` during local development.
- Next.js rewrites `/api/*` to that backend, keeping browser requests same-origin.
- Login and registration use the existing API contracts. JWT is kept in browser local storage for this learning project.
- The UI must not offer administrator registration; public registration creates reader accounts only.

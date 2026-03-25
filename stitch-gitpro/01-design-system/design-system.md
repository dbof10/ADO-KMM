# Design System Specification: The Engineering Editorial

## 1. Overview & Creative North Star

### The Creative North Star: "The Architectural Ledger"
Standard DevOps tools often feel like cluttered spreadsheets. This design system reimagines the technical workspace as a high-end architectural ledger—a space where precision meets prestige. We move beyond the "utilitarian grid" by embracing an editorial layout that prioritizes content clarity through tonal depth and intentional asymmetry.

By leveraging **The Architectural Ledger** philosophy, we replace rigid 1px lines with structural shifts in surface tone. The goal is a digital environment that feels solid, authoritative, and breathable, allowing developers to navigate complex code diffs and work items without visual fatigue.

---

## 2. Colors & Surface Logic

This system utilizes a sophisticated palette that anchors the familiar Azure DevOps Blue within a refined, multi-layered neutral environment.

### Color Tokens
- **Primary:** `#005faa` (Core Brand) | **Primary Container:** `#0078d4` (Interactive Accent)
- **Surface Palette:** Ranges from `surface-container-lowest` (#ffffff) to `surface-dim` (#dadad9).
- **Functional Tones:** `tertiary` (#974700) for warnings and `error` (#ba1a1a) for critical failures.

### The "No-Line" Rule
**Strict Mandate:** Designers are prohibited from using 1px solid borders to section off major UI areas (Sidebar, Header, Main Content). 
- Boundaries must be defined solely through background color shifts. 
- *Example:* A `surface-container-low` (#f4f3f2) navigation sidebar sitting adjacent to a `surface` (#faf9f8) main content area.

### Surface Hierarchy & Nesting
Treat the UI as a physical stack of fine paper. 
- **Level 0 (Base):** `surface` (#faf9f8).
- **Level 1 (Main Content Area):** `surface-container-low` (#f4f3f2).
- **Level 2 (Active Cards/Work Items):** `surface-container-lowest` (#ffffff).
- **Level 3 (Floating Menus):** `surface-bright` with Glassmorphism.

### The "Glass & Gradient" Rule
To elevate the "Premium" feel, use a subtle linear gradient on primary CTAs: `linear-gradient(135deg, #005faa 0%, #0078d4 100%)`. For floating utility panels, apply a   `backdrop-blur` of 12px over a 90% opaque `surface-container-highest` to create a "frosted" architectural effect.

---

## 3. Typography

The system utilizes **Inter** for its modern, geometric clarity, replacing the standard Segoe UI to provide a more bespoke, editorial feel.

| Role | Token | Size | Weight | Intent |
| :--- | :--- | :--- | :--- | :--- |
| **Display** | `display-md` | 2.75rem | 700 | Large data visualizations / Dashboard KPIs |
| **Headline** | `headline-sm` | 1.5rem | 600 | Page titles (e.g., "Pull Request #1204") |
| **Title** | `title-sm` | 1rem | 600 | Section headers and card titles |
| **Body** | `body-md` | 0.875rem | 400 | Standard UI text and descriptions |
| **Label** | `label-sm` | 0.6875rem | 700 | Status badges and uppercase metadata |

**Editorial Note:** Use `title-lg` for primary navigation items with generous tracking (letter-spacing: 0.02em) to create an authoritative hierarchy.

---

## 4. Elevation & Depth

### The Layering Principle
Depth is achieved through "Tonal Stacking." To elevate a component (like a code comment thread), do not use a shadow. Instead, place the comment container (`surface-container-lowest`) inside the thread background (`surface-container-high`). The contrast in lightness provides the "lift."

### Ambient Shadows
When an element must float (e.g., a Profile Dropdown), use an **Ambient Shadow**:
- `box-shadow: 0 12px 32px -4px rgba(26, 28, 28, 0.08);`
- The shadow color is derived from `on-surface` (#1a1c1c), ensuring it feels like a natural obstruction of light.

### The "Ghost Border" Fallback
If a border is required for accessibility (e.g., in a high-density diff view), use the **Ghost Border**:
- `outline-variant` (#c0c7d4) at **15% opacity**.
- Never use 100% opaque lines; they "shatter" the editorial flow.

---

## 5. Components

### Buttons
- **Primary:** Gradient fill (`primary` to `primary-container`), white text, `md` (0.375rem) roundedness.
- **Secondary:** Transparent background, `primary` text, `outline-variant` (20% opacity) Ghost Border.
- **Tertiary (Ghost):** No background or border. Text only. Used for low-priority actions in toolbars.

### Code Diffs & Syntax
- **Additions:** Background `surface-container-lowest`, text `on-primary-fixed-variant` with a subtle green left-accent (2px).
- **Deletions:** Background `error-container` (at 30% opacity), text `on-error-container`.
- **Note:** Forbid 1px borders between lines. Use the `0.5` (0.1rem) spacing scale to separate logical blocks.

### Status Badges
Status indicators must use the `label-sm` type. 
- **Active:** `primary-fixed` background with `on-primary-fixed` text.
- **Critical:** `error-container` background with `on-error-container` text.
- **Shape:** Use `full` (9999px) roundedness for a pill shape to contrast against the `md` roundedness of the UI containers.

### Input Fields
- **Default State:** `surface-container-highest` background, no border.
- **Focus State:** 2px solid `primary-container` bottom-accent only. This mimics the "architectural" feel of a ledger line.

---

## 6. Do's and Don'ts

### Do
- **Do** use white space as a structural element. If two sections feel cluttered, increase spacing to scale `8` (1.75rem) before considering a divider.
- **Do** use "Nested Rounding." If a card has `lg` (0.5rem) corners, an inner button should have `md` (0.375rem) corners to maintain visual harmony.
- **Do** utilize `surface-tint` for subtle hover states on interactive rows.

### Don'ts
- **Don't** use pure black (#000000) for text. Always use `on-surface` (#1a1c1c) to maintain the premium, ink-on-paper look.
- **Don't** use standard 1px gray dividers (`#ccc`). Use a 2px gap that reveals the `surface-container` color underneath.
- **Don't** use harsh transitions. Every state change (hover, active) should have a 200ms ease-out transition.

### Accessibility Note
While we prioritize "No-Line" design, always ensure that `on-surface` text maintains at least a 4.5:1 contrast ratio against whatever `surface-container` level it occupies. Use the `outline` token (#717783) if a user-interface element becomes indistinguishable for low-vision users.

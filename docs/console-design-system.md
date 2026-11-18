# Console design system

## Intent

StackEmu is a local developer tool. The interface puts sandbox state, the next useful action, and the S3/SQS/SNS workspaces ahead of decorative branding. The shell uses a compact project header and one persistent navigation model; resource pages keep their task-specific controls and confirmations.

## Visual foundation

- **Display:** Space Grotesk Variable for page and section headings.
- **Interface:** IBM Plex Sans for navigation, labels, forms, tables, and body copy.
- **Code:** the platform monospace stack for endpoints, identifiers, and commands.
- **Dark surfaces:** graphite canvas with layered warm surfaces and a restrained amber highlight.
- **Light surfaces:** warm paper canvas with dark ink and deeper amber for readable contrast.
- **Depth:** a low contrast amber canvas glow, subtle surface gradients, and a restrained panel shadow. Decoration is disabled for high contrast, forced colors, and reduced transparency.
- **Status:** separate green, amber, red, and blue semantic colors; labels and icons accompany color.

The local font files are the source of truth. The console does not fetch fonts, icons, or runtime assets from a third party.

## Layout rules

- Desktop uses a 14.5rem navigation rail, a single 4.5rem project header, and a centered workspace capped at 92.5rem.
- At 1200px, navigation and header spacing contract. At 1080px, the overview stacks its status and action panels. At 900px, navigation moves above the workspace. At 620px, navigation becomes five equal touch targets and content uses a single column.
- Resource tables stay scrollable when the data requires columns; pages with a mobile resource-card view use that view at narrow widths.
- Do not truncate a status message or an actionable error. Long identifiers may wrap or scroll inside their own control.
- Interactive targets have a 44px minimum where space allows. Focus remains visible and motion follows the reduced-motion preference.

## Components

- **Project header:** brand, active project, public local endpoint, connection details, language, and theme controls.
- **Navigation:** overview, S3, SQS, SNS, and operation history. Current location uses both text and an active treatment.
- **Status row:** service icon, service name, plain-language detail, and a semantic status pill.
- **Next step:** one recommended action, its reason, a copyable endpoint, and a copyable host command. It must not imply that the browser can run a host command.
- **Resource inventory:** one link per supported service with its declared resource count.
- **Resource pages:** shared page headings, action groups, panels, table surfaces, filters, forms, empty states, and explicit destructive actions.
- **Feedback:** operation notices use live regions; copy and request feedback stays adjacent to the control that produced it.

## Interaction contract

- Links change location; buttons submit or change the current view. Compact S3 bucket/folder creation and SQS create/send/receive/configure actions may open in the shared dialog. Uploads, batches, transfers, sharing, destructive reviews, and operation tracking remain canonical pages.
- Opening a compact action adds a same-document history entry. Back or closing the dialog returns to the originating list and keeps its filter and scroll position; Forward reopens the action. Reload and direct links render the full action page.
- A changed form asks before the dialog is discarded. Validation responses preserve server-returned values and move focus to the first invalid field. A request with an uncertain outcome is not retried automatically.
- Destructive controls identify the bucket, object, queue, topic, or operation and show the consequences before confirmation. Confirmations remain proportional to the impact.
- Buttons share primary, secondary, danger, and icon-only treatments. Disabled controls cannot appear active; pending controls expose `aria-busy` and live status text. Success, failure, and uncertain outcomes use the nearby status or alert region.
- Icons are local inline SVG partials on a 24 × 24 grid. Strokes use `currentColor`, a 1.75-unit width, and rounded ends and joins. Service marks distinguish S3 storage, SQS queues, and SNS broadcast. Decorative SVGs use `aria-hidden="true"`; icon-only controls must provide an accessible name in the control itself.
- Motion is limited to short state feedback and dialog transitions and honors `prefers-reduced-motion`.

## Implementation

The Qute templates remain server-rendered, and HTMX remains progressive enhancement. Avoid client-side frameworks, continuous polling, remote requests, and duplicate workflows. Shared visual tokens and layout rules live in `stackemu-console/src/main/frontend/base.css`, and the build produces the served `app.css`; behavior-bearing markup stays in `stackemu-console/src/main/resources/templates/` and existing JavaScript.

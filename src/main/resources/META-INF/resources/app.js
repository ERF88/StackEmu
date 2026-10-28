// Progressive enhancement: native navigation/forms and server validation remain authoritative.
// This filter intentionally searches only the rendered page, never the entire S3 inventory.
const resourceFilters = new WeakSet();
function initializeResourceFilters() {
  for (const filter of document.querySelectorAll("[data-resource-filter]")) {
    if (resourceFilters.has(filter)) continue;
    const browser = filter.closest("[data-resource-browser]");
    const query = filter.querySelector("[data-resource-query]");
    const clear = filter.querySelector("[data-filter-clear]");
    const count = filter.querySelector("[data-filter-count]");
    const empty = browser?.querySelector("[data-filter-empty]");
    if (!browser || !query || !clear || !count || !empty) continue;
    const normalize = (value) =>
      value.normalize("NFC").toLocaleLowerCase(document.documentElement.lang);
    const rows = Array.from(browser.querySelectorAll(".resource-table tbody tr"), (row) => ({
      element: row,
      name: normalize(row.querySelector("th code")?.textContent || ""),
    }));
    function applyFilter() {
      const term = normalize(query.value);
      let matches = 0;
      for (const row of rows) {
        row.element.hidden = !row.name.includes(term);
        if (!row.element.hidden) matches += 1;
      }
      count.textContent = `${matches} / ${rows.length}`;
      empty.hidden = matches !== 0;
      clear.hidden = query.value.length === 0;
    }
    query.addEventListener("input", applyFilter);
    clear.addEventListener("click", () => {
      query.value = "";
      applyFilter();
      query.focus();
    });
    resourceFilters.add(filter);
    filter.hidden = false;
    applyFilter();
  }
}
initializeResourceFilters();
document.addEventListener("htmx:afterSwap", initializeResourceFilters);

// Icon labels remain available by keyboard; Escape dismisses without moving focus.
document.addEventListener("keydown", (event) => {
  if (event.key !== "Escape") return;
  for (const action of document.querySelectorAll(".icon-action:is(:hover, :focus-visible)"))
    action.setAttribute("data-tooltip-dismissed", "");
});
for (const type of ["focusin", "pointerover"]) {
  document.addEventListener(type, (event) => {
    const action = event.target.closest(".icon-action");
    if (action && !action.contains(event.relatedTarget))
      action.removeAttribute("data-tooltip-dismissed");
  });
}

function refreshFeedback(event, busy) {
  const trigger = event.detail?.elt;
  if (!trigger?.matches("[data-refresh-panel]")) return;
  const panel = document.getElementById("bucket-panel");
  const status = document.getElementById("loading");
  panel?.setAttribute("aria-busy", String(busy));
  if (status) {
    status.textContent = busy
      ? status.dataset.loadingLabel
      : event.detail.successful === true
        ? ""
        : status.dataset.failedLabel;
  }
}

document.addEventListener("htmx:beforeRequest", (event) => refreshFeedback(event, true));
document.addEventListener("htmx:afterRequest", (event) => refreshFeedback(event, false));

function submitLabel(button) {
  return button.querySelector("[data-submit-label]") || button;
}

function isPrimaryClick(event) {
  return event.button === 0 && !event.ctrlKey && !event.metaKey && !event.shiftKey && !event.altKey;
}

const INTERACTION_FEEDBACK_DELAY_MS = 200;
// Keep Tab at modal boundaries instead of allowing browser chrome to take focus.
for (const dialog of document.querySelectorAll("dialog"))
  dialog.addEventListener("keydown", (event) => {
    if (
      !dialog.open ||
      event.defaultPrevented ||
      event.key !== "Tab" ||
      event.ctrlKey ||
      event.metaKey ||
      event.altKey
    )
      return;
    const controls = [
      ...dialog.querySelectorAll(
        'a[href], button:not([disabled]), input:not([disabled]):not([type="hidden"]), textarea:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])',
      ),
    ].filter(
      (element) =>
        element.closest("dialog") === dialog &&
        element.getClientRects().length &&
        window.getComputedStyle(element).visibility !== "hidden" &&
        !element.closest("[inert]"),
    );
    const first = controls[0],
      last = controls.at(-1);
    if (!first) {
      event.preventDefault();
      dialog.tabIndex = -1;
      dialog.focus();
    } else if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  });
const interactionFeedbackOwners = new WeakMap();
function beginInteractionFeedback(
  trigger,
  label,
  status = document.querySelector("[data-action-launch-status]"),
  onSlow = null,
) {
  if (!trigger) return { finish() {} };
  interactionFeedbackOwners.get(trigger)?.finish();
  const originalBusy = trigger.getAttribute("aria-busy");
  let timer;
  const state = {
    slow: false,
    finish() {
      window.clearTimeout(timer);
      if (interactionFeedbackOwners.get(trigger) === state) {
        interactionFeedbackOwners.delete(trigger);
        trigger.removeAttribute("data-action-opening");
        trigger.removeAttribute("data-opening-visible");
        if (originalBusy === null) trigger.removeAttribute("aria-busy");
        else trigger.setAttribute("aria-busy", originalBusy);
      }
      if (status && interactionFeedbackOwners.get(status) === state) {
        interactionFeedbackOwners.delete(status);
        status.textContent = "";
      }
    },
  };
  interactionFeedbackOwners.set(trigger, state);
  if (status) interactionFeedbackOwners.set(status, state);
  trigger.setAttribute("aria-busy", "true");
  trigger.setAttribute("data-action-opening", "");
  timer = window.setTimeout(() => {
    if (interactionFeedbackOwners.get(trigger) !== state) return;
    state.slow = true;
    trigger.setAttribute("data-opening-visible", "");
    if (status && interactionFeedbackOwners.get(status) === state) status.textContent = label;
    onSlow?.();
  }, INTERACTION_FEEDBACK_DELAY_MS);
  return state;
}

// Refresh only the current read listing. A failed read never retries a mutation.
let serviceRefresh = null;
function refreshServiceContext(href = window.location.href) {
  if (!window.htmx?.ajax) return false;
  const current = new window.URL(window.location.href);
  const destination = new window.URL(href, current);
  if (destination.origin !== current.origin || destination.pathname !== current.pathname)
    return false;
  let id, marker, endpoint;
  if (current.pathname === "/s3") {
    id = "bucket-panel";
    marker = "bucket-list";
    endpoint = new window.URL("/buckets" + current.search, current);
  } else if (
    current.pathname === "/s3/bucket" &&
    destination.searchParams.get("bucket") === current.searchParams.get("bucket") &&
    (destination.searchParams.get("prefix") || "") === (current.searchParams.get("prefix") || "")
  ) {
    id = "object-list-panel";
    marker = "object-list";
    endpoint = new window.URL(current.pathname + current.search, current);
  } else return false;
  const target = document.getElementById(id);
  if (!target) return false;
  if (serviceRefresh) {
    serviceRefresh.again = href;
    return true;
  }
  const field = target.querySelector("[data-resource-query]");
  const table = target.querySelector(".table-scroll");
  const focused = target.contains(document.activeElement) ? document.activeElement : null;
  const state = {
    target,
    id,
    marker,
    endpoint,
    xhr: null,
    swapped: false,
    filter: field?.value || "",
    scroll: table?.scrollTop || 0,
    horizontal: table?.scrollLeft || 0,
    focusId: focused?.id,
    focusHref: focused?.getAttribute("href"),
  };
  serviceRefresh = state;
  const status = document.getElementById(id === "bucket-panel" ? "loading" : "object-loading");
  if (status) status.textContent = "";
  state.feedback = beginInteractionFeedback(
    target.querySelector("[data-refresh-objects]") ||
      document.querySelector("[data-refresh-panel]") ||
      target,
    status?.dataset.loadingLabel || target.dataset.loadingLabel || "",
    status,
  );
  target.setAttribute("aria-busy", "true");
  window.htmx
    .ajax("GET", endpoint.pathname + endpoint.search, {
      source: target,
      target,
      select: `#${id}`,
      swap: "outerHTML ignoreTitle:true swap:0s settle:0s",
    })
    .catch(() => {
      // Preserve the listing and a manual retry on transport failure.
    })
    .finally(() => {
      if (serviceRefresh !== state) return;
      state.feedback.finish();
      serviceRefresh = null;
      const live = document.getElementById(id);
      live?.setAttribute("aria-busy", "false");
      const feedback = document.getElementById(
        id === "bucket-panel" ? "loading" : "object-loading",
      );
      if (feedback)
        feedback.textContent = state.swapped
          ? ""
          : feedback.dataset.failedLabel || live?.dataset.failedLabel || "";
      if (state.again) refreshServiceContext(state.again);
    });
  return true;
}
document.addEventListener("htmx:beforeRequest", (event) => {
  if (serviceRefresh && event.detail.elt === serviceRefresh.target)
    serviceRefresh.xhr = event.detail.xhr;
});
document.addEventListener("htmx:beforeSwap", (event) => {
  const state = serviceRefresh;
  if (!state || event.detail.xhr !== state.xhr) return;
  const xhr = event.detail.xhr;
  const response = new window.URL(xhr.responseURL, window.location.href);
  if (
    !state.target.isConnected ||
    event.detail.target !== state.target ||
    xhr.status !== 200 ||
    response.origin !== state.endpoint.origin ||
    response.pathname !== state.endpoint.pathname ||
    response.search !== state.endpoint.search ||
    xhr.getResponseHeader("X-StackEmu-Fragment") !== state.marker ||
    !xhr.getResponseHeader("Content-Type")?.startsWith("text/html") ||
    !xhr.responseText.includes(`id="${state.id}"`)
  ) {
    event.preventDefault();
    return;
  }
  event.detail.ignoreTitle = true;
  event.detail.selectOverride = `#${state.id}`;
});
document.addEventListener("htmx:afterSwap", (event) => {
  const state = serviceRefresh;
  if (!state || event.detail.xhr !== state.xhr) return;
  state.swapped = true;
  const target = document.getElementById(state.id);
  if (target) target.dataset.contextUpdated = "";
  const field = target?.querySelector("[data-resource-query]");
  if (field) {
    field.value = state.filter;
    field.dispatchEvent(new window.Event("input", { bubbles: true }));
  }
  const table = target?.querySelector(".table-scroll");
  if (table) {
    table.scrollTop = state.scroll;
    table.scrollLeft = state.horizontal;
  }
  const focused = state.focusId
    ? document.getElementById(state.focusId)
    : [...(target?.querySelectorAll("a[href]") || [])].find(
        (link) => state.focusHref && link.getAttribute("href") === state.focusHref,
      );
  if (focused && target?.contains(focused)) focused.focus({ preventScroll: true });
  else if (document.activeElement === document.body && !document.querySelector("dialog[open]")) {
    const heading = document.querySelector("main h1");
    if (heading) {
      heading.tabIndex = -1;
      heading.focus({ preventScroll: true });
    }
  }
});
document.addEventListener(
  "click",
  (event) => {
    const link = event.target.closest("[data-refresh-panel], [data-refresh-objects]");
    if (!link || !isPrimaryClick(event)) return;
    if (serviceRefresh || refreshServiceContext(link.href)) {
      event.preventDefault();
      event.stopImmediatePropagation();
    }
  },
  true,
);

// ADR-0025: htmx alone renders checked same-origin action workspaces; never emulator HTML.
function initializeActionDialog() {
  const dialog = document.getElementById("action-dialog");
  if (!dialog?.showModal || !window.htmx?.ajax) return;
  const content = document.getElementById("action-dialog-content");
  const status = dialog.querySelector("[data-action-status]");
  const close = dialog.querySelector("[data-action-close]");
  const title = document.getElementById("action-dialog-title");
  const managed = new WeakSet();
  let active = null;
  let uploadXhr = null;
  let trigger = null;
  let mutationPending = false;
  let completed = false;
  let returnHref = null;
  let completionNotice = null;
  let opening = false;
  let openingFeedback = null;
  let openingReadyXhr = null;
  let openingFrame = null;
  let openingFocus = null;
  const launchStatus = document.querySelector("[data-action-launch-status]");

  function finishOpening() {
    window.cancelAnimationFrame(openingFrame);
    openingFrame = null;
    openingReadyXhr = null;
    openingFocus = null;
    openingFeedback?.finish();
    openingFeedback = null;
    opening = false;
  }
  function revealPreparation() {
    if (!opening || !content.querySelector('[data-operation-state="PREPARING"]')) return;
    finishOpening();
    dialog.showModal();
    close.focus();
  }
  function cancelOpening() {
    if (!opening) return;
    const xhr = active?.xhr;
    active = null;
    finishOpening();
    xhr?.abort();
    content.replaceChildren();
    status.textContent = "";
    pending(false);
  }

  function pending(value) {
    mutationPending = value;
    close.disabled = value;
    content.setAttribute("aria-busy", String(value));
  }
  function fail(mutating) {
    active?.feedback?.finish();
    pending(false);
    status.textContent = mutating ? dialog.dataset.uncertainLabel : dialog.dataset.failedLabel;
    close.querySelector("span").textContent = dialog.dataset.closeLabel;
    if (opening) {
      finishOpening();
      dialog.showModal();
      close.focus();
    }
    // An unmarked/network response is not a safe invitation to repeat a mutation.
    for (const button of content.querySelectorAll('button[type="submit"]')) {
      button.disabled = true;
      if (button.dataset.idleLabel) submitLabel(button).textContent = button.dataset.idleLabel;
    }
    for (const form of content.querySelectorAll("form")) {
      form.dataset.actionBlocked = "true";
      form.setAttribute("aria-busy", "false");
    }
  }
  function request(method, url, source) {
    status.textContent = method === "GET" ? "" : source.dataset.submittingLabel;
    content.setAttribute("aria-busy", "true");
    active = {
      xhr: null,
      mutating: method === "POST",
      path: new window.URL(url).pathname,
      feedback:
        method === "GET" && !opening
          ? beginInteractionFeedback(source, dialog.dataset.loadingLabel, status)
          : null,
    };
    window.htmx
      .ajax(method, url, {
        source,
        target: content,
        select: "#action-workspace",
        swap: "innerHTML ignoreTitle:true swap:0s settle:0s",
      })
      .catch(() => {
        // Matching afterRequest handles failure; never automatically repeat a POST.
      });
  }
  document.addEventListener("click", (event) => {
    const link = event.target.closest("a[href]");
    if (!link || event.defaultPrevented || !isPrimaryClick(event)) return;
    // Keep canonical form reloads native; nesting another copy would duplicate form IDs.
    if (
      (link.closest("#action-workspace") && !link.hasAttribute("data-action-review")) ||
      link.closest(".preferences-panel, .primary-navigation, .back-navigation") ||
      link.hasAttribute("download") ||
      (link.target && link.target !== "_self")
    )
      return;
    const url = new window.URL(link.href);
    if (
      url.origin !== window.location.origin ||
      !(
        [
          "/s3/write",
          "/s3/delete-object",
          "/s3/delete-bucket",
          "/s3/empty-bucket",
          "/s3/transfer",
          "/s3/share",
        ].includes(url.pathname) || /^\/operations\/[a-f0-9-]{36}\/review$/.test(url.pathname)
      )
    )
      return;
    if (dialog.open) {
      if (!link.hasAttribute("data-action-review")) return;
      event.preventDefault();
      if (active || mutationPending) return;
      completed = false;
      returnHref = null;
      close.querySelector("span").textContent = dialog.dataset.cancelLabel;
      close.focus();
      request("GET", link.href, dialog);
      return;
    }
    event.preventDefault();
    if (opening && trigger === link) return;
    cancelOpening();
    trigger = link;
    completed = false;
    returnHref = null;
    close.querySelector("span").textContent = dialog.dataset.cancelLabel;
    title.textContent = link.getAttribute("aria-label") || link.textContent.trim();
    content.replaceChildren();
    opening = true;
    openingFeedback = beginInteractionFeedback(
      trigger,
      dialog.dataset.loadingLabel,
      launchStatus,
      revealPreparation,
    );
    request("GET", link.href, dialog);
  });
  document.addEventListener("submit", (event) => {
    const form = event.target;
    if (
      !dialog.open ||
      !content.contains(form) ||
      !form.matches(".mutation-form") ||
      form.matches("[data-upload-form]")
    )
      return;
    event.preventDefault();
    if (
      mutationPending ||
      active ||
      form.dataset.actionBlocked === "true" ||
      !form.reportValidity()
    )
      return;
    request("POST", form.action, form);
  });
  document.addEventListener("htmx:configRequest", (event) => {
    if (event.detail.elt === dialog || content.contains(event.detail.elt))
      event.detail.timeout =
        active?.mutating || event.detail.elt.matches("[data-upload-form]") ? 120000 : 10000;
  });
  document.addEventListener("htmx:beforeRequest", (event) => {
    const source = event.detail.elt;
    if (source !== dialog && !content.contains(source)) return;
    if (source.matches("[data-upload-form]")) {
      uploadXhr = event.detail.xhr;
      pending(true);
      return;
    }
    if (!active) return;
    active.xhr = event.detail.xhr;
    managed.add(active.xhr);
    if (active.mutating) {
      pending(true);
      source.setAttribute("aria-busy", "true");
      const button = source.querySelector('button[type="submit"]');
      button.dataset.idleLabel = submitLabel(button).textContent;
      button.disabled = true;
      submitLabel(button).textContent = source.dataset.submittingLabel;
    }
  });
  document.addEventListener("htmx:beforeSwap", (event) => {
    const xhr = event.detail.xhr;
    if (!managed.has(xhr)) return;
    if ((!dialog.open && !opening) || active?.xhr !== xhr || event.detail.target !== content) {
      event.preventDefault();
      return;
    }
    const response = new window.URL(xhr.responseURL, window.location.href);
    if (
      response.origin !== window.location.origin ||
      response.pathname !== active.path ||
      ![200, 403, 404, 409, 422, 429, 500, 502, 503].includes(xhr.status) ||
      xhr.getResponseHeader("X-StackEmu-Action") !== "workspace" ||
      !xhr.getResponseHeader("Content-Type")?.startsWith("text/html") ||
      !xhr.responseText.includes('id="action-workspace"')
    ) {
      event.preventDefault();
      fail(active.mutating);
      return;
    }
    event.detail.shouldSwap = true;
    event.detail.isError = false;
    event.detail.ignoreTitle = true;
    event.detail.selectOverride = "#action-workspace";
  });
  document.addEventListener("htmx:afterSwap", (event) => {
    const uploadSwap = event.detail.xhr === uploadXhr;
    // Upload uses outerHTML: its original target/form are detached after a successful swap.
    // Correlate the live request instead; the upload controller owns the response gates.
    if ((!dialog.open && !opening) || (!uploadSwap && !content.contains(event.detail.target)))
      return;
    if (uploadSwap && !content.querySelector("#mutation-workspace")) return;
    if (!uploadSwap && event.detail.target === content && event.detail.xhr !== active?.xhr) return;
    if (
      opening &&
      content.querySelector('[data-operation-state="PREPARING"]') &&
      !openingFeedback?.slow
    ) {
      active?.feedback?.finish();
      active = null;
      pending(false);
      status.textContent = "";
      close.querySelector("span").textContent = dialog.dataset.closeLabel;
      return;
    }
    if (opening) {
      openingReadyXhr = event.detail.xhr;
    }
    uploadXhr = null;
    const wasGet = active && !active.mutating;
    active?.feedback?.finish();
    active = null;
    pending(false);
    status.textContent = "";
    const result = content.querySelector(
      "[data-operation-success], [data-action-completed], [data-action-accepted]",
    );
    completed = Boolean(result);
    close.querySelector("span").textContent = content.querySelector("form")
      ? dialog.dataset.cancelLabel
      : dialog.dataset.closeLabel;
    if (content.querySelector("[data-action-completed], [data-action-accepted]"))
      returnHref = content.querySelector(".back-navigation a")?.href;
    if (completed) {
      const destination = content.querySelector("[data-result-return]");
      if (destination) returnHref = destination.href;
      completionNotice = {
        message: result.dataset.notice || result.textContent.trim(),
        operation: result.dataset.operationId || "",
        pending: result.matches("[data-action-accepted]"),
        time: Date.now(),
      };
      dialog.close();
      return;
    }
    const heading = content.querySelector("h1");
    if (heading) title.textContent = heading.textContent.trim();
    const focus =
      content.querySelector('[aria-invalid="true"]') ||
      (wasGet &&
      !content.querySelector(
        'form[action^="/s3/delete-"], form[action="/s3/empty-bucket"], form[action^="/operations/"]',
      )
        ? content.querySelector('input:not([type="hidden"]):not([readonly])')
        : null);
    if (opening) openingFocus = focus || close;
    else (focus || close).focus();
  });
  document.addEventListener("htmx:afterSettle", (event) => {
    if (!opening || event.detail.target !== content || event.detail.xhr !== openingReadyXhr) return;
    const expected = openingReadyXhr;
    window.cancelAnimationFrame(openingFrame);
    openingFrame = window.requestAnimationFrame(() => {
      if (!opening || openingReadyXhr !== expected || !content.querySelector("#action-workspace"))
        return;
      const focus = openingFocus?.isConnected ? openingFocus : close;
      content.setAttribute("aria-busy", "false");
      status.textContent = "";
      finishOpening();
      dialog.showModal();
      focus.focus();
    });
  });
  document.addEventListener("htmx:afterRequest", (event) => {
    if (uploadXhr && event.detail.xhr === uploadXhr) {
      uploadXhr = null;
      pending(false);
      close.querySelector("span").textContent = dialog.dataset.closeLabel;
    }
    if (active?.xhr !== event.detail.xhr) return;
    fail(active.mutating);
    active = null;
  });
  close.addEventListener("click", () => dialog.close());
  document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape" || !opening) return;
    event.preventDefault();
    cancelOpening();
    trigger?.focus({ preventScroll: true });
  });
  document.addEventListener("stackemu:action-refresh", (event) => {
    if ((!dialog.open && !opening) || active || mutationPending) return;
    const url = new window.URL(event.detail, window.location.href);
    if (
      url.origin === window.location.origin &&
      /^\/operations\/[a-f0-9-]{36}\/review$/.test(url.pathname)
    )
      request("GET", url.href, dialog);
  });
  dialog.addEventListener("cancel", (event) => {
    if (mutationPending) event.preventDefault();
  });
  dialog.addEventListener("close", () => {
    if (dialog.open) return;
    const xhr = active?.xhr;
    active?.feedback?.finish();
    active = null;
    uploadXhr = null;
    xhr?.abort();
    pending(false);
    content.replaceChildren();
    status.textContent = "";
    if (completed) {
      const source = trigger?.href ? new window.URL(trigger.href, window.location.href) : null;
      const current = new window.URL(window.location.href);
      const localWrite =
        source?.pathname === "/s3/write" &&
        current.pathname === "/s3/bucket" &&
        source.searchParams.get("bucket") === current.searchParams.get("bucket");
      if (refreshServiceContext(localWrite ? current.href : returnHref || current.href)) {
        document.dispatchEvent(
          new window.CustomEvent("stackemu:notice", { detail: completionNotice }),
        );
        const focus = trigger?.isConnected ? trigger : document.querySelector("main h1");
        if (focus) {
          if (focus.tagName === "H1") focus.tabIndex = -1;
          focus.focus({ preventScroll: true });
        }
      } else {
        try {
          window.sessionStorage.setItem("stackemu:notice", JSON.stringify(completionNotice));
        } catch {
          /* Native results remain available when storage is disabled. */
        }
        window.location.assign(returnHref || window.location.href);
      }
    } else if (trigger?.isConnected) trigger.focus();
  });
  window.addEventListener("pagehide", () => {
    // Leaving never promises cancellation or rollback of a server-side mutation.
    completed = false;
    cancelOpening();
    if (dialog.open) dialog.close();
  });
}

initializeActionDialog();

document.addEventListener("submit", (event) => {
  const form = event.target;
  if (!form.matches(".mutation-form") || event.defaultPrevented) return;
  if (form.getAttribute("aria-busy") === "true") {
    event.preventDefault();
    return;
  }
  form.setAttribute("aria-busy", "true");
  const button = form.querySelector('button[type="submit"]');
  button.dataset.idleLabel = submitLabel(button).textContent;
  submitLabel(button).textContent = form.dataset.submittingLabel;
  button.disabled = true;
  const status = document.getElementById("submission-status");
  if (status) status.textContent = form.dataset.submittingLabel;
});

// Browsers can restore the old form from the back/forward cache after navigation.
window.addEventListener("pageshow", () => {
  for (const form of document.querySelectorAll(".mutation-form")) {
    form.setAttribute("aria-busy", "false");
    const button = form.querySelector('button[type="submit"]');
    button.disabled = false;
    if (button.dataset.idleLabel) submitLabel(button).textContent = button.dataset.idleLabel;
  }
  const status = document.getElementById("submission-status");
  if (status) status.textContent = "";
});

function initializeObjectClipboard(container) {
  for (const panel of container.querySelectorAll(".clipboard-panel[data-copy-source]")) {
    const parameters = new window.URL(panel.dataset.copySource, window.location.href).searchParams;
    let encodedFallback = false;
    for (const field of panel.querySelectorAll("textarea[data-copy-param]")) {
      const value = parameters.get(field.dataset.copyParam);
      if (value === null) continue;
      field.dataset.copyValue = value;
      const hasControls = [...value].some((character) => {
        const code = character.codePointAt(0);
        return code < 32 || code === 127;
      });
      field.value = hasControls ? window.encodeURIComponent(value) : value;
      encodedFallback ||= hasControls;
    }
    panel.querySelector("[data-encoded-note]").hidden = !encodedFallback;
    for (const button of panel.querySelectorAll("[data-copy-field]")) button.hidden = false;
  }
}

// Navigation state contains only bounded local listing coordinates, never file bodies or signed URLs.
function listingState() {
  return {
    filter: document.querySelector("[data-resource-query]")?.value.slice(0, 256) || "",
    scroll: Math.round(document.querySelector(".table-scroll")?.scrollTop || 0),
    horizontal: Math.round(document.querySelector(".table-scroll")?.scrollLeft || 0),
    pageScroll: Math.round(window.scrollY),
  };
}

function withListOrigin(href) {
  const source = new window.URL(window.location.href);
  const target = new window.URL(href, source);
  if (
    source.pathname !== "/s3/bucket" ||
    target.origin !== source.origin ||
    target.searchParams.get("bucket") !== source.searchParams.get("bucket")
  )
    return target.href;
  target.hash = new window.URLSearchParams({
    list: source.pathname + source.search,
    ...listingState(),
  });
  return target.href;
}

function checkedList(value, bucket) {
  try {
    if (!value || value.length > 8192 || !value.startsWith("/s3/bucket?")) return null;
    const url = new window.URL(value, window.location.href);
    const allowed = new Set(["bucket", "prefix", "token", "lang", "theme", "density"]);
    if (
      url.origin !== window.location.origin ||
      url.pathname !== "/s3/bucket" ||
      url.username ||
      url.password ||
      url.hash ||
      url.searchParams.get("bucket") !== bucket ||
      [...url.searchParams.keys()].some(
        (key) => !allowed.has(key) || url.searchParams.getAll(key).length !== 1,
      )
    )
      return null;
    return url;
  } catch {
    return null;
  }
}

function initializeCanonicalNavigation() {
  const parameters = new window.URLSearchParams(window.location.hash.slice(1));
  const bucket = new window.URL(window.location.href).searchParams.get("bucket");
  const origin = checkedList(parameters.get("list"), bucket);
  const safeState = (values) => ({
    filter: String(values.filter || "").slice(0, 256),
    scroll: Math.min(10000000, Math.max(0, Number(values.scroll) || 0)),
    horizontal: Math.min(10000000, Math.max(0, Number(values.horizontal) || 0)),
    pageScroll: Math.min(10000000, Math.max(0, Number(values.pageScroll) || 0)),
  });
  const saved = safeState(Object.fromEntries(parameters));
  if (origin) {
    for (const name of ["lang", "theme", "density"]) {
      const value = new window.URL(window.location.href).searchParams.get(name);
      if (value) origin.searchParams.set(name, value);
    }
    const candidate = document.querySelector(".back-navigation a");
    const back =
      candidate && new window.URL(candidate.href, window.location.href).pathname === "/s3/bucket"
        ? candidate
        : null;
    if (back) {
      origin.hash = new window.URLSearchParams({ restore: "1", ...saved });
      back.href = origin.href;
    }
    for (const link of document.querySelectorAll(
      ".object-panel a, #object-inspector-view a, .back-navigation a, .preferences-panel a",
    )) {
      const target = new window.URL(link.href, window.location.href);
      if (
        target.origin === window.location.origin &&
        target.searchParams.get("bucket") === bucket
      ) {
        target.hash = parameters.toString();
        // The listing return has its own restore state rather than an object-origin hash.
        if (link !== back) link.href = target.href;
      }
    }
  }
  function restore() {
    if (window.location.pathname !== "/s3/bucket") return;
    const currentParameters = new window.URLSearchParams(window.location.hash.slice(1));
    const state =
      currentParameters.get("restore") === "1"
        ? safeState(Object.fromEntries(currentParameters))
        : window.history.state?.stackemuList;
    if (!state) return;
    const bounded = safeState(state);
    const query = document.querySelector("[data-resource-query]");
    if (query) {
      query.value = bounded.filter;
      query.dispatchEvent(new window.Event("input", { bubbles: true }));
    }
    window.requestAnimationFrame(() => {
      const table = document.querySelector(".table-scroll");
      if (table) {
        table.scrollTop = bounded.scroll;
        table.scrollLeft = bounded.horizontal;
      }
      window.scrollTo(0, bounded.pageScroll);
    });
    window.history.replaceState(
      { ...window.history.state, stackemuList: bounded },
      "",
      window.location.pathname +
        window.location.search +
        (currentParameters.has("inspect") ? window.location.hash : ""),
    );
  }
  restore();
  window.addEventListener("pageshow", restore);
  document.addEventListener("pointerdown", (event) => {
    const link = event.target.closest("a[data-object-detail]");
    if (link) link.href = withListOrigin(link.href);
  });
  document.addEventListener("focusin", (event) => {
    const link = event.target.closest("a[data-object-detail]");
    if (link) link.href = withListOrigin(link.href);
  });
  document.addEventListener(
    "click",
    (event) => {
      const link = event.target.closest("a[href]");
      if (!link) return;
      if (window.location.pathname === "/s3/bucket") {
        const state = listingState();
        window.history.replaceState({ ...window.history.state, stackemuList: state }, "");
        if (
          link.matches("[data-object-detail]") ||
          new window.URL(link.href, window.location.href).pathname === "/s3/versions"
        )
          link.href = withListOrigin(link.href);
        if (link.closest(".preferences-panel")) {
          const target = new window.URL(link.href, window.location.href);
          if (
            target.origin === window.location.origin &&
            target.pathname === "/s3/bucket" &&
            target.searchParams.get("bucket") === bucket
          ) {
            target.hash = new window.URLSearchParams({ restore: "1", ...state });
            link.href = target.href;
          }
        }
      }
    },
    true,
  );
}

function initializeNavigationGuard() {
  const dialog = document.getElementById("navigation-guard");
  if (!dialog?.showModal) return;
  const dirty = new WeakSet();
  let lastField = null;
  let destination = null;
  let trigger = null;
  for (const type of ["input", "change"])
    document.addEventListener(type, (event) => {
      const form = event.target.closest(".mutation-form");
      if (form && !form.closest("dialog")) {
        dirty.add(form);
        lastField = event.target;
      }
    });
  document.addEventListener(
    "click",
    (event) => {
      const link = event.target.closest("a[href]");
      if (
        !link ||
        event.defaultPrevented ||
        !isPrimaryClick(event) ||
        link.target === "_blank" ||
        link.hasAttribute("download") ||
        ![...document.querySelectorAll("main .mutation-form")].some((form) => dirty.has(form))
      )
        return;
      const url = new window.URL(link.href, window.location.href);
      if (url.origin !== window.location.origin || url.username || url.password) return;
      event.preventDefault();
      event.stopImmediatePropagation();
      destination = url.href;
      trigger = link;
      if (!dialog.open) dialog.showModal();
      dialog.querySelector("[data-keep-editing]").focus();
    },
    true,
  );
  dialog.querySelector("[data-keep-editing]").addEventListener("click", () => dialog.close());
  dialog.querySelector("[data-discard-changes]").addEventListener("click", () => {
    const url = destination;
    destination = null;
    dialog.close();
    if (url) window.location.assign(url);
  });
  dialog.addEventListener("close", () => {
    if (destination && lastField?.isConnected) lastField.focus();
    else if (destination && trigger?.isConnected) trigger.focus();
    destination = null;
  });
}

initializeCanonicalNavigation();
initializeNavigationGuard();

function initializeObjectDrawer() {
  const drawer = document.getElementById("object-drawer");
  if (!drawer || typeof drawer.showModal !== "function" || !window.htmx?.ajax) return;
  const content = document.getElementById("object-drawer-content");
  const status = document.getElementById("drawer-status");
  const close = drawer.querySelector("[data-drawer-close]");
  const retry = drawer.querySelector("[data-drawer-retry]");
  const title = document.getElementById("drawer-title");
  const initialTitle = title.textContent;
  let drawerOpening = false;
  let drawerFeedback = null;
  let readTrigger = null;
  const paths = new Set([
    "/s3/object",
    "/s3/object-metadata",
    "/s3/versions",
    "/s3/preview",
    "/s3/preview/image",
  ]);
  const fields = new Set([
    "bucket",
    "key",
    "lang",
    "theme",
    "density",
    "keyMarker",
    "versionMarker",
  ]);
  const bucket = new window.URL(window.location.href).searchParams.get("bucket");
  let sourceLink = null;
  let activeRequest = null;
  let activeEndpoint = null;
  let currentView = null;
  let closing = false;
  let leaving = false;

  function checkedView(value) {
    try {
      const url = new window.URL(value, window.location.href);
      if (
        url.origin !== window.location.origin ||
        !paths.has(url.pathname) ||
        url.username ||
        url.password ||
        url.search.length > 8192 ||
        url.searchParams.get("bucket") !== bucket ||
        [...url.searchParams.keys()].some(
          (key) => !fields.has(key) || url.searchParams.getAll(key).length !== 1,
        )
      )
        return null;
      url.hash = "";
      return url;
    } catch {
      return null;
    }
  }

  function discardRequest(preserveContent = false) {
    const previous = activeRequest;
    activeRequest = null;
    previous?.abort();
    drawerFeedback?.finish();
    drawerFeedback = null;
    drawerOpening = false;
    if (!preserveContent) content.replaceChildren();
    content.setAttribute("aria-busy", "false");
    status.textContent = "";
    retry.hidden = true;
  }

  function hide() {
    const wasOpen = drawer.open;
    if (drawer.open) drawer.close();
    discardRequest();
    if (!leaving && wasOpen) {
      const target = sourceLink?.isConnected ? sourceLink : document.querySelector("main h1");
      if (target) {
        if (target.tagName === "H1") target.tabIndex = -1;
        target.focus();
      }
    }
  }

  function showFailure() {
    drawerFeedback?.finish();
    drawerFeedback = null;
    drawerOpening = false;
    content.replaceChildren();
    content.setAttribute("aria-busy", "false");
    status.textContent = drawer.dataset.failedLabel;
    retry.hidden = false;
    if (!drawer.open) drawer.showModal();
    close.focus();
  }

  function requestDetails(url) {
    discardRequest(drawer.open);
    currentView = url;
    const endpoint = new window.URL(url.href);
    if (url.pathname === "/s3/object") endpoint.pathname = "/s3/object-panel";
    activeEndpoint = endpoint;
    if (!drawer.open) title.textContent = initialTitle;
    drawerOpening = !drawer.open;
    drawerFeedback = beginInteractionFeedback(
      readTrigger?.isConnected ? readTrigger : sourceLink || close,
      drawer.dataset.loadingLabel,
      drawer.open ? status : document.querySelector("[data-action-launch-status]"),
    );
    content.setAttribute("aria-busy", "true");
    window.htmx
      .ajax("GET", endpoint.pathname + endpoint.search, {
        source: drawer,
        target: content,
        swap: "innerHTML swap:0s settle:0s",
      })
      .catch(() => {
        /* Matching afterRequest presents transport failures. */
      });
  }

  function synchronize() {
    if (closing) {
      closing = false;
      window.history.replaceState(
        { ...window.history.state, stackemuInspectorDepth: 0 },
        "",
        window.location.pathname + window.location.search,
      );
      hide();
      return;
    }
    const value = new window.URLSearchParams(window.location.hash.slice(1)).get("inspect");
    const url = value ? checkedView(value) : null;
    if (url) {
      if ((drawer.open || activeRequest) && currentView?.href === url.href) return;
      if (!sourceLink?.isConnected) {
        sourceLink = [...document.querySelectorAll(".object-table a[data-object-detail]")].find(
          (link) =>
            new window.URL(link.href).searchParams.get("key") === url.searchParams.get("key"),
        );
      }
      requestDetails(url);
    } else hide();
  }

  function openView(url) {
    if (activeRequest && currentView?.href === url.href) return;
    const hash = new window.URLSearchParams({ inspect: url.pathname + url.search });
    const current = Number(window.history.state?.stackemuInspectorDepth);
    const depth = Number.isInteger(current) && current >= 0 && current < 32 ? current + 1 : 1;
    window.history.pushState(
      { ...window.history.state, stackemuInspectorDepth: depth },
      "",
      "#" + hash,
    );
    requestDetails(url);
  }

  function closeInspector() {
    const depth = Number(window.history.state?.stackemuInspectorDepth);
    if (Number.isInteger(depth) && depth > 0 && depth <= 32) {
      closing = true;
      window.history.go(-depth);
    } else {
      window.history.replaceState(
        { ...window.history.state, stackemuInspectorDepth: 0 },
        "",
        window.location.pathname + window.location.search,
      );
      hide();
    }
  }

  document.addEventListener("click", (event) => {
    const link = event.target.closest("a");
    if (
      !link ||
      event.defaultPrevented ||
      !isPrimaryClick(event) ||
      link.hasAttribute("download") ||
      link.target === "_blank"
    )
      return;
    if (!link.matches("[data-object-detail]") && !content.contains(link)) return;
    const url = checkedView(link.href);
    if (!url) return;
    event.preventDefault();
    if (link.matches("[data-object-detail]")) sourceLink = link;
    readTrigger = link;
    openView(url);
  });
  document.addEventListener("htmx:configRequest", (event) => {
    if (event.detail.elt === drawer) event.detail.timeout = 10000;
  });
  document.addEventListener("htmx:beforeRequest", (event) => {
    if (event.detail.elt === drawer) activeRequest = event.detail.xhr;
  });
  document.addEventListener("htmx:beforeSwap", (event) => {
    if (event.detail.target !== content) return;
    const request = event.detail.xhr;
    if ((!drawer.open && !drawerOpening) || request !== activeRequest) {
      event.preventDefault();
      return;
    }
    const response = new window.URL(request.responseURL || window.location.href);
    const fragment =
      activeEndpoint?.pathname === "/s3/object-panel" ? "object-details" : "object-inspector";
    if (
      request.status !== 200 ||
      response.origin !== window.location.origin ||
      response.pathname !== activeEndpoint?.pathname ||
      response.search !== activeEndpoint.search ||
      request.getResponseHeader("X-StackEmu-Fragment") !== fragment ||
      !request.getResponseHeader("Content-Type")?.startsWith("text/html") ||
      (fragment === "object-inspector" &&
        !request.responseText.includes('id="object-inspector-view"'))
    ) {
      event.preventDefault();
      showFailure();
      return;
    }
    if (fragment === "object-inspector") event.detail.selectOverride = "#object-inspector-view";
    event.detail.ignoreTitle = true;
  });
  document.addEventListener("htmx:afterSwap", (event) => {
    if (
      event.detail.target !== content ||
      event.detail.xhr !== activeRequest ||
      (!drawer.open && !drawerOpening)
    )
      return;
    drawerFeedback?.finish();
    drawerFeedback = null;
    drawerOpening = false;
    content.setAttribute("aria-busy", "false");
    status.textContent = "";
    title.textContent =
      content.querySelector("[data-inspector-title]")?.dataset.inspectorTitle || initialTitle;
    initializeObjectClipboard(content);
    initializeImagePreview();
    if (!drawer.open) drawer.showModal();
    close.focus();
  });
  document.addEventListener("htmx:afterRequest", (event) => {
    if (
      event.detail.elt === drawer &&
      event.detail.xhr === activeRequest &&
      (drawer.open || drawerOpening) &&
      event.detail.successful !== true
    )
      showFailure();
  });
  close.addEventListener("click", closeInspector);
  document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape" || event.defaultPrevented || !drawerOpening) return;
    event.preventDefault();
    discardRequest();
    closeInspector();
  });
  drawer.addEventListener("cancel", (event) => {
    event.preventDefault();
    closeInspector();
  });
  retry.addEventListener("click", () => {
    if (currentView) requestDetails(currentView);
  });
  window.addEventListener("pagehide", () => {
    leaving = true;
    hide();
  });
  window.addEventListener("pageshow", () => {
    leaving = false;
    synchronize();
  });
  window.addEventListener("popstate", synchronize);
  synchronize();
}

document.addEventListener("click", async (event) => {
  const button = event.target.closest("button[data-copy-field]");
  if (!button || button.disabled) return;
  const panel = button.closest(".clipboard-panel");
  const field = button.closest(".copy-field").querySelector("textarea");
  const status = panel.querySelector(".copy-status");
  status.textContent = "";
  panel.dataset.copyState = "pending";
  const feedback = beginInteractionFeedback(
    button,
    document.querySelector("[data-action-launch-status]")?.dataset.loadingLabel || "",
  );
  button.disabled = true;
  try {
    if (!window.isSecureContext || !window.navigator.clipboard?.writeText) {
      throw new Error("Clipboard unavailable");
    }
    await window.navigator.clipboard.writeText(field.dataset.copyValue ?? field.value);
    if (panel.isConnected) {
      status.textContent = panel.dataset.copiedLabel;
      panel.dataset.copyState = "success";
    }
  } catch {
    if (panel.isConnected) {
      status.textContent = panel.dataset.copyFailedLabel;
      panel.dataset.copyState = "failure";
      field.focus();
      field.select();
    }
  } finally {
    feedback.finish();
    button.disabled = false;
  }
});

initializeObjectDrawer();
initializeObjectClipboard(document);

// Native read navigation keeps browser behavior; the shared indicator never delays it.
let nativeReadFeedback = null;
const nativeReadPaths = new Set([
  "/",
  "/s3",
  "/s3/bucket",
  "/connection",
  "/components",
  "/s3/object",
  "/s3/object-metadata",
  "/s3/versions",
  "/s3/preview",
  "/s3/preview/image",
  "/operations",
]);
document.addEventListener("click", (event) => {
  const link = event.target.closest("a[href]");
  if (
    !link ||
    event.defaultPrevented ||
    !isPrimaryClick(event) ||
    link.hasAttribute("download") ||
    (link.target && link.target !== "_self")
  )
    return;
  const url = new window.URL(link.href, window.location.href);
  if (
    url.origin !== window.location.origin ||
    url.username ||
    url.password ||
    (!nativeReadPaths.has(url.pathname) && !/^\/operations\/[a-f0-9-]{36}$/.test(url.pathname)) ||
    url.pathname + url.search === window.location.pathname + window.location.search
  )
    return;
  nativeReadFeedback?.finish();
  nativeReadFeedback = beginInteractionFeedback(
    link,
    document.querySelector("[data-action-launch-status]")?.dataset.loadingLabel || "",
  );
});
for (const event of ["pagehide", "pageshow"])
  window.addEventListener(event, () => {
    nativeReadFeedback?.finish();
    nativeReadFeedback = null;
  });

function initializeUploadEnhancement() {
  if (!window.htmx?.ajax || !window.FormData || !window.XMLHttpRequest) return;
  let active = null;
  const managedRequests = new WeakSet();

  function writeStatus(state, text) {
    if (state.status.textContent !== text) state.status.textContent = text;
  }

  function processing(state) {
    if (active !== state) return;
    state.processing = true;
    state.progress.value = 100;
    writeStatus(state, state.form.dataset.uploadProcessing);
  }

  function restoreUnconfirmed(state, rejected = false) {
    if (active !== state) return;
    active = null;
    state.form.setAttribute("aria-busy", "false");
    for (const control of state.controls) control.disabled = false;
    submitLabel(state.submit).textContent = state.idleLabel;
    state.progress.removeAttribute("value");
    writeStatus(
      state,
      rejected ? state.form.dataset.uploadRejected : state.form.dataset.uploadUncertain,
    );
  }

  function prepareForms() {
    for (const form of document.querySelectorAll("[data-upload-form]")) {
      if (form.dataset.uploadReady === "true") continue;
      form.dataset.uploadReady = "true";
      const input = form.querySelector('input[type="file"]');
      const zone = form.querySelector("[data-upload-drop]");
      const dropHint = form.querySelector("[data-upload-drop-hint]");
      const status = form.querySelector("[data-drop-status]");
      function selectionChanged() {
        status.textContent =
          input.files.length === 1 ? `${form.dataset.dropSelected} ${input.files[0].name}` : "";
      }
      input.addEventListener("change", selectionChanged);
      if (typeof window.DataTransfer === "function") {
        dropHint.hidden = false;
        zone.addEventListener("dragover", (event) => {
          event.preventDefault();
          zone.dataset.dragging = String(form.getAttribute("aria-busy") !== "true");
          if (event.dataTransfer) event.dataTransfer.dropEffect = active ? "none" : "copy";
        });
        zone.addEventListener("dragleave", (event) => {
          if (!zone.contains(event.relatedTarget)) zone.dataset.dragging = "false";
        });
        zone.addEventListener("drop", (event) => {
          event.preventDefault();
          zone.dataset.dragging = "false";
          if (form.getAttribute("aria-busy") === "true") return;
          const transfer = event.dataTransfer;
          const items = [...(transfer?.items ?? [])];
          const file = transfer?.files[0];
          let regularFile;
          try {
            regularFile =
              transfer?.files.length === 1 &&
              items.length === 1 &&
              items[0].kind === "file" &&
              !items[0].webkitGetAsEntry?.()?.isDirectory &&
              !file.webkitRelativePath;
            if (regularFile) input.files = transfer.files;
          } catch {
            regularFile = false; // Unsupported browser assignment: retain picker fallback.
          }
          if (regularFile) selectionChanged();
          else status.textContent = form.dataset.dropRejected;
        });
      }
      form.addEventListener("submit", (event) => {
        if (event.defaultPrevented) return;
        event.preventDefault();
        if (active || !form.reportValidity()) return;
        const submit = form.querySelector('button[type="submit"]');
        const controls = [...form.querySelectorAll('input[type="file"], [name="confirm"], button')];
        active = {
          form,
          submit,
          controls,
          idleLabel: submitLabel(submit).textContent,
          status: form.querySelector("[data-upload-status]"),
          progress: form.querySelector("progress"),
          xhr: null,
          pendingProgress: null,
          percent: 0,
          announcedStep: -1,
          processing: false,
        };
        window.htmx
          .ajax("POST", form.action, {
            source: form,
            target: "#mutation-workspace",
            select: "#mutation-workspace",
            swap: "outerHTML ignoreTitle:true swap:0s settle:0s",
          })
          .catch(() => {
            // Matching transport errors are handled by afterRequest, never auto-retried.
          });
      });
    }
  }

  document.addEventListener("htmx:configRequest", (event) => {
    if (active && event.detail.elt === active.form) event.detail.timeout = 120000;
  });
  document.addEventListener("htmx:beforeRequest", (event) => {
    if (!active || event.detail.elt !== active.form) return;
    const state = active;
    state.xhr = event.detail.xhr;
    managedRequests.add(state.xhr);
    state.form.setAttribute("aria-busy", "true");
    state.form.querySelector("[data-upload-progress]").hidden = false;
    state.form.querySelector("[data-upload-drop]").dataset.dragging = "false";
    state.progress.removeAttribute("value");
    writeStatus(state, state.form.dataset.submittingLabel);
    submitLabel(state.submit).textContent = state.form.dataset.submittingLabel;
    // htmx already captured FormData; disabling these controls cannot strip request fields.
    for (const control of state.controls) control.disabled = true;
    state.xhr.upload.addEventListener("progress", (native) => {
      if (active === state)
        state.pendingProgress = {
          loaded: native.loaded,
          total: native.total,
          computable: native.lengthComputable,
        };
    });
    state.xhr.upload.addEventListener("load", () => processing(state));
  });
  document.addEventListener("htmx:xhr:progress", (event) => {
    if (!active || event.target !== active.form || !active.pendingProgress) return;
    const state = active;
    const sample = state.pendingProgress;
    state.pendingProgress = null;
    if (
      state.processing ||
      sample.loaded !== event.detail.loaded ||
      sample.total !== event.detail.total ||
      sample.computable !== event.detail.lengthComputable
    )
      return;
    if (
      !sample.computable ||
      !Number.isFinite(sample.total) ||
      sample.total <= 0 ||
      !Number.isFinite(sample.loaded) ||
      sample.loaded < 0
    ) {
      state.progress.removeAttribute("value");
      writeStatus(state, state.form.dataset.submittingLabel);
      return;
    }
    state.percent = Math.min(
      100,
      Math.max(state.percent, Math.floor((sample.loaded / sample.total) * 100)),
    );
    state.progress.value = state.percent;
    if (state.percent === 100) processing(state);
    else if (Math.floor(state.percent / 10) !== state.announcedStep) {
      state.announcedStep = Math.floor(state.percent / 10);
      writeStatus(
        state,
        state.form.dataset.uploadSending.replace("{percent}", String(state.percent)),
      );
    }
  });
  document.addEventListener("htmx:beforeSwap", (event) => {
    if (!managedRequests.has(event.detail.xhr)) return;
    if (!active || event.detail.xhr !== active.xhr) {
      event.preventDefault();
      return;
    }
    const request = event.detail.xhr;
    if (!active.form.isConnected || event.detail.target.id !== "mutation-workspace") {
      event.preventDefault();
      return;
    }
    if (
      ![200, 403, 404, 409, 422, 500, 502, 503].includes(request.status) ||
      request.getResponseHeader("X-StackEmu-Upload") !== "workspace" ||
      !request.getResponseHeader("Content-Type")?.startsWith("text/html")
    ) {
      event.preventDefault();
      restoreUnconfirmed(active, request.status === 413);
      return;
    }
    event.detail.shouldSwap = true;
    event.detail.isError = false;
    event.detail.ignoreTitle = true;
    event.detail.selectOverride = "#mutation-workspace";
  });
  document.addEventListener("htmx:afterSwap", (event) => {
    if (!active || event.detail.xhr !== active.xhr) return;
    active = null;
    prepareForms();
    const workspace = document.getElementById("mutation-workspace");
    // The action-window controller owns modal focus/title, not the underlying document.
    if (workspace.closest("#action-dialog")) return;
    document.title = workspace.dataset.pageTitle;
    const focus = workspace.querySelector('[aria-invalid="true"]') ?? workspace.querySelector("h1");
    if (focus) {
      if (focus.tagName === "H1") focus.tabIndex = -1;
      focus.focus();
    }
  });
  document.addEventListener("htmx:afterRequest", (event) => {
    if (active && event.detail.xhr === active.xhr) restoreUnconfirmed(active);
  });
  window.addEventListener("pagehide", () => {
    if (!active) return;
    const request = active.xhr;
    restoreUnconfirmed(active);
    request?.abort(); // Navigation is not a rollback or server cancellation guarantee.
  });
  document.addEventListener("htmx:afterSwap", prepareForms);
  prepareForms();
}

initializeUploadEnhancement();

function initializeOperationNotice() {
  const notice = document.querySelector("[data-operation-notice]");
  if (!notice) return;
  const message = notice.querySelector("[data-notice-message]");
  let resultHeading = null;
  let noticeRevision = 0;
  const successLifetime = 5000;
  let dismissTimer = null;
  let remaining = successLifetime;
  let started = 0;
  function pauseDismissal() {
    if (dismissTimer !== null)
      remaining = Math.max(0, remaining - (window.performance.now() - started));
    window.clearTimeout(dismissTimer);
    dismissTimer = null;
  }
  function resumeDismissal() {
    if (
      dismissTimer !== null ||
      notice.hidden ||
      notice.dataset.noticeTone !== "success" ||
      notice.hasAttribute("data-operation-poll") ||
      document.hidden ||
      notice.matches(":hover, :focus-within")
    )
      return;
    started = window.performance.now();
    dismissTimer = window.setTimeout(() => {
      dismissTimer = null;
      remaining = 0;
      if (document.hidden || notice.matches(":hover, :focus-within")) return;
      dismiss();
    }, remaining);
  }
  function restartDismissal() {
    pauseDismissal();
    remaining = successLifetime;
    resumeDismissal();
  }
  function dismiss() {
    noticeRevision++;
    pauseDismissal();
    const returnFocus = notice.contains(document.activeElement);
    notice.hidden = true;
    notice.removeAttribute("data-operation-poll");
    message.textContent = "";
    if (returnFocus && resultHeading?.isConnected) {
      resultHeading.tabIndex = -1;
      resultHeading.focus();
    }
  }
  notice.addEventListener("pointerenter", pauseDismissal);
  notice.addEventListener("pointerleave", resumeDismissal);
  notice.addEventListener("focusin", pauseDismissal);
  notice.addEventListener("focusout", () => window.queueMicrotask(resumeDismissal));
  notice.addEventListener("stackemu:notice-updated", () => {
    noticeRevision++;
    restartDismissal();
  });
  document.addEventListener("visibilitychange", () =>
    document.hidden ? pauseDismissal() : resumeDismissal(),
  );
  window.addEventListener("pagehide", pauseDismissal);
  window.addEventListener("pageshow", resumeDismissal);
  function announceMessage(text, tone, announce = true) {
    const revision = ++noticeRevision;
    notice.hidden = false;
    notice.dataset.noticeTone = tone;
    message.setAttribute("aria-live", announce ? "polite" : "off");
    if (announce) message.setAttribute("role", "status");
    else message.removeAttribute("role");
    const updateMessage = () => {
      if (!notice.hidden && revision === noticeRevision) message.textContent = text;
    };
    // Establish the live region first; newer outcomes invalidate deferred announcements.
    if (announce) window.requestAnimationFrame(updateMessage);
    else updateMessage();
  }
  function showResult(announce) {
    const heading = document.querySelector("h1[data-operation-success]");
    if (!heading || heading === resultHeading || heading.closest("#action-dialog")) return;
    resultHeading = heading;
    announceMessage(heading.textContent.trim(), "success", announce);
    restartDismissal();
  }
  notice.querySelector("[data-notice-dismiss]").addEventListener("click", dismiss);
  document.addEventListener("htmx:afterSwap", (event) => {
    // UX-3 focuses this checked result heading; do not announce it twice.
    if (event.detail.target.id === "mutation-workspace") showResult(false);
  });
  showResult(true);
  function present(result, focusHeading) {
    if (!result || !(Date.now() - result.time < 60000) || typeof result.message !== "string")
      return;
    announceMessage(result.message, result.pending ? "info" : "success");
    const link = notice.querySelector("[data-notice-link]");
    link.hidden = true;
    link.removeAttribute("href");
    notice.removeAttribute("data-operation-poll");
    if (/^[a-f0-9-]{36}$/.test(result.operation)) {
      link.href = `/operations/${result.operation}?${new window.URLSearchParams({ lang: document.documentElement.lang, theme: document.documentElement.dataset.theme })}`;
      link.hidden = false;
      if (result.pending) {
        notice.dataset.operationPoll = result.operation;
        notice.dataset.operationState = "RUNNING";
        notice.dataset.operationRefresh = window.location.href;
      }
    }
    resultHeading = document.querySelector("main h1");
    if (focusHeading && resultHeading) {
      resultHeading.tabIndex = -1;
      resultHeading.focus();
    }
    restartDismissal();
  }
  document.addEventListener("stackemu:notice", (event) => {
    present(event.detail, false);
    scheduleOperationPoll(0);
  });
  try {
    const stored = window.sessionStorage.getItem("stackemu:notice");
    window.sessionStorage.removeItem("stackemu:notice");
    present(stored ? JSON.parse(stored) : null, true);
  } catch {
    /* Disabled session storage leaves canonical feedback available. */
  }
}

initializeOperationNotice();

// Only local durable records are polled. No S3 reads, hidden-page work or write retries.
let operationPollTimer = null;
let operationPollRunning = false;
let operationPollDelay = 150;
function scheduleOperationPoll(delay = 150) {
  window.clearTimeout(operationPollTimer);
  operationPollTimer = null;
  if (document.hidden || operationPollRunning || !document.querySelector("[data-operation-poll]"))
    return;
  operationPollTimer = window.setTimeout(pollVisibleOperations, delay);
}
document.addEventListener("htmx:afterSwap", () => {
  operationPollDelay = 150;
  scheduleOperationPoll();
});
document.addEventListener("visibilitychange", () => {
  operationPollDelay = 150;
  scheduleOperationPoll(0);
});
async function pollVisibleOperations() {
  if (operationPollRunning) return;
  operationPollRunning = true;
  if (!document.hidden) {
    for (const element of document.querySelectorAll("[data-operation-poll]")) {
      const id = element.dataset.operationPoll;
      if (!/^[a-f0-9-]{36}$/.test(id)) continue;
      try {
        const response = await window.fetch(`/operations/${id}/state`, {
          cache: "no-store",
          signal: window.AbortSignal.timeout(5000),
        });
        if (!response.ok || !response.headers.get("Content-Type")?.startsWith("application/json"))
          continue;
        const state = await response.json();
        if (
          !element.isConnected ||
          document.hidden ||
          element.hidden ||
          element.dataset.operationPoll !== id
        )
          continue;
        const progress = element.querySelector("[data-operation-progress]");
        if (progress && Number.isSafeInteger(state.done) && Number.isSafeInteger(state.total))
          progress.textContent = `${state.done} / ${state.total}`;
        if (state.state !== element.dataset.operationState) {
          if (element.matches("[data-operation-notice]")) {
            if (state.active) {
              element.dataset.operationState = state.state;
              continue;
            }
            const labels = {
              SUCCEEDED: "completedLabel",
              PARTIAL: "partialLabel",
              FAILED: "failedLabel",
              UNCERTAIN: "uncertainLabel",
              INTERRUPTED: "interruptedLabel",
            };
            const message = element.dataset[labels[state.state]];
            if (!message) continue;
            element.removeAttribute("data-operation-poll");
            element.dataset.noticeTone = state.state === "SUCCEEDED" ? "success" : "warning";
            element.querySelector("[data-notice-message]").textContent = message;
            element.dispatchEvent(new window.Event("stackemu:notice-updated"));
            // Update the visible service once after a confirmed terminal state, never health polling.
            refreshServiceContext();
            continue;
          }
          const destination = new window.URL(
            element.dataset.operationRefresh,
            window.location.href,
          );
          if (
            destination.origin !== window.location.origin ||
            !(
              destination.pathname === "/operations" ||
              destination.pathname.startsWith(`/operations/${id}`)
            )
          )
            continue;
          if (element.closest("#action-dialog"))
            document.dispatchEvent(
              new window.CustomEvent("stackemu:action-refresh", { detail: destination.href }),
            );
          else window.location.replace(destination.href);
        }
      } catch {
        /* Keep last observed state; no inferred completion or mutation retry. */
      }
    }
  }
  operationPollRunning = false;
  operationPollDelay = Math.min(2000, operationPollDelay * 2);
  scheduleOperationPoll(operationPollDelay);
}
scheduleOperationPoll();

function initializeImagePreview() {
  const image = document.querySelector("[data-image-preview]");
  const status = document.querySelector("[data-image-status]");
  if (!image || !status) return;
  const finish = () => {
    const available = image.naturalWidth > 0;
    image.hidden = !available;
    status.textContent = available ? status.dataset.success : status.dataset.failure;
  };
  image.addEventListener("load", finish);
  image.addEventListener("error", finish);
  if (image.complete) finish();
  else status.textContent = status.dataset.loading;
}

initializeImagePreview();

// Progressive enhancement: native navigation/forms and server validation remain authoritative.
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
  let trigger = null;
  let mutationPending = false;
  let completed = false;
  let returnHref = null;

  function pending(value) {
    mutationPending = value;
    close.disabled = value;
    content.setAttribute("aria-busy", String(value));
  }
  function fail(mutating) {
    pending(false);
    status.textContent = mutating ? dialog.dataset.uncertainLabel : dialog.dataset.failedLabel;
    close.querySelector("span").textContent = dialog.dataset.closeLabel;
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
    status.textContent =
      method === "GET" ? dialog.dataset.loadingLabel : source.dataset.submittingLabel;
    content.setAttribute("aria-busy", "true");
    active = { xhr: null, mutating: method === "POST", path: new window.URL(url).pathname };
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
    if (
      !link ||
      event.defaultPrevented ||
      event.button !== 0 ||
      event.ctrlKey ||
      event.metaKey ||
      event.shiftKey ||
      event.altKey
    )
      return;
    // Keep canonical form reloads native; nesting another copy would duplicate form IDs.
    if (
      link.closest("#action-workspace") ||
      link.hasAttribute("download") ||
      (link.target && link.target !== "_self")
    )
      return;
    const url = new window.URL(link.href);
    if (
      url.origin !== window.location.origin ||
      !["/s3/write", "/s3/delete-object"].includes(url.pathname)
    )
      return;
    if (dialog.open) return;
    event.preventDefault();
    trigger = link;
    completed = false;
    returnHref = null;
    close.querySelector("span").textContent = dialog.dataset.cancelLabel;
    title.textContent = link.textContent.trim();
    content.replaceChildren();
    dialog.showModal();
    close.focus();
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
    if (!dialog.open || active?.xhr !== xhr || event.detail.target !== content) {
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
    if (!dialog.open || !content.contains(event.detail.target)) return;
    if (event.detail.target === content && event.detail.xhr !== active?.xhr) return;
    const wasGet = active && !active.mutating;
    active = null;
    pending(false);
    status.textContent = "";
    completed = Boolean(content.querySelector("[data-operation-success], [data-action-completed]"));
    if (completed || !content.querySelector("form"))
      close.querySelector("span").textContent = dialog.dataset.closeLabel;
    if (content.querySelector("[data-action-completed]"))
      returnHref = content.querySelector(".back-navigation a")?.href;
    const heading = content.querySelector("h1");
    if (heading) title.textContent = heading.textContent.trim();
    const focus =
      content.querySelector('[aria-invalid="true"]') ||
      (wasGet && !content.querySelector('form[action="/s3/delete-object"]')
        ? content.querySelector('input:not([type="hidden"]):not([readonly])')
        : null);
    (focus || close).focus();
  });
  document.addEventListener("htmx:afterRequest", (event) => {
    if (event.detail.elt?.matches("[data-upload-form]") && content.contains(event.detail.elt)) {
      pending(false);
      close.querySelector("span").textContent = dialog.dataset.closeLabel;
    }
    if (active?.xhr !== event.detail.xhr) return;
    fail(active.mutating);
    active = null;
  });
  close.addEventListener("click", () => dialog.close());
  dialog.addEventListener("cancel", (event) => {
    if (mutationPending) event.preventDefault();
  });
  dialog.addEventListener("close", () => {
    if (dialog.open) return;
    const xhr = active?.xhr;
    active = null;
    xhr?.abort();
    pending(false);
    content.replaceChildren();
    status.textContent = "";
    if (completed) window.location.assign(returnHref || window.location.href);
    else if (trigger?.isConnected) trigger.focus();
  });
  window.addEventListener("pagehide", () => {
    // Leaving never promises cancellation or rollback of a server-side mutation.
    completed = false;
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
  document.getElementById("submission-status").textContent = form.dataset.submittingLabel;
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

function initializeObjectDrawer() {
  const drawer = document.getElementById("object-drawer");
  if (!drawer || typeof drawer.showModal !== "function" || !window.htmx?.ajax) return;
  const content = document.getElementById("object-drawer-content");
  const status = document.getElementById("drawer-status");
  const close = drawer.querySelector("[data-drawer-close]");
  const retry = drawer.querySelector("[data-drawer-retry]");
  const fullPage = drawer.querySelector("[data-drawer-page]");
  let sourceLink = null;
  let activeRequest = null;

  function discardRequest() {
    const previous = activeRequest;
    activeRequest = null;
    previous?.abort();
    content.replaceChildren();
    content.setAttribute("aria-busy", "false");
    status.textContent = "";
    retry.hidden = true;
  }

  function showFailure() {
    content.replaceChildren();
    content.setAttribute("aria-busy", "false");
    status.textContent = drawer.dataset.failedLabel;
    retry.hidden = false;
  }

  function requestDetails(link) {
    discardRequest();
    sourceLink = link;
    fullPage.href = link.href;
    const endpoint = new window.URL(link.href);
    endpoint.pathname = "/s3/object-panel";
    status.textContent = drawer.dataset.loadingLabel;
    content.setAttribute("aria-busy", "true");
    if (!drawer.open) drawer.showModal();
    close.focus();
    // Only htmx swaps the checked, escaped same-origin fragment (ADR-0015).
    window.htmx
      .ajax("GET", endpoint.pathname + endpoint.search, {
        source: drawer,
        target: content,
        swap: "innerHTML swap:0s settle:0s",
      })
      .catch(() => {
        // Transport errors are presented by the matching afterRequest event below.
      });
  }

  document.addEventListener("click", (event) => {
    const link = event.target.closest("a[data-object-detail]");
    if (
      !link ||
      event.defaultPrevented ||
      event.button !== 0 ||
      event.ctrlKey ||
      event.metaKey ||
      event.shiftKey ||
      event.altKey
    )
      return;
    event.preventDefault();
    requestDetails(link);
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
    if (!drawer.open || request !== activeRequest) {
      event.preventDefault();
      return;
    }
    if (
      request.status !== 200 ||
      request.getResponseHeader("X-StackEmu-Fragment") !== "object-details"
    ) {
      event.preventDefault();
      showFailure();
    }
  });
  document.addEventListener("htmx:afterSwap", (event) => {
    if (event.detail.target !== content || event.detail.xhr !== activeRequest || !drawer.open)
      return;
    content.setAttribute("aria-busy", "false");
    status.textContent = "";
    const parameters = new window.URL(fullPage.href).searchParams;
    let encodedFallback = false;
    for (const field of content.querySelectorAll("textarea[data-copy-param]")) {
      const value = parameters.get(field.dataset.copyParam);
      field.dataset.copyValue = value;
      const hasControls = [...value].some((character) => {
        const code = character.codePointAt(0);
        return code < 32 || code === 127;
      });
      field.value = hasControls ? window.encodeURIComponent(value) : value;
      encodedFallback ||= hasControls;
    }
    content.querySelector("[data-encoded-note]").hidden = !encodedFallback;
    for (const button of content.querySelectorAll("[data-copy-field]")) button.hidden = false;
  });
  document.addEventListener("htmx:afterRequest", (event) => {
    if (
      event.detail.elt === drawer &&
      event.detail.xhr === activeRequest &&
      drawer.open &&
      event.detail.successful !== true
    )
      showFailure();
  });
  close.addEventListener("click", () => drawer.close());
  retry.addEventListener("click", () => {
    if (sourceLink?.isConnected) requestDetails(sourceLink);
  });
  drawer.addEventListener("close", () => {
    if (drawer.open) return;
    discardRequest();
    if (sourceLink?.isConnected) sourceLink.focus();
  });
  function resetDrawer() {
    if (drawer.open) drawer.close();
    discardRequest();
  }
  window.addEventListener("pagehide", resetDrawer);
  window.addEventListener("pageshow", resetDrawer);
  window.addEventListener("popstate", resetDrawer);
}

document.addEventListener("click", async (event) => {
  const button = event.target.closest("button[data-copy-field]");
  if (!button || button.disabled) return;
  const panel = button.closest(".clipboard-panel");
  const field = button.closest(".copy-field").querySelector("textarea");
  const status = panel.querySelector(".copy-status");
  status.textContent = "";
  panel.dataset.copyState = "pending";
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
    button.disabled = false;
  }
});

initializeObjectDrawer();

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
      const picker = form.querySelector("[data-upload-picker]");
      const status = form.querySelector("[data-drop-status]");
      function selectionChanged() {
        status.textContent =
          input.files.length === 1 ? `${form.dataset.dropSelected} ${input.files[0].name}` : "";
      }
      input.addEventListener("change", selectionChanged);
      if (typeof window.DataTransfer === "function") {
        zone.hidden = false;
        picker.addEventListener("click", () => input.click());
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
  function showResult(announce) {
    const heading = document.querySelector("h1[data-operation-success]");
    if (!heading || heading === resultHeading || heading.closest("#action-dialog")) return;
    resultHeading = heading;
    message.setAttribute("aria-live", announce ? "polite" : "off");
    if (announce) message.setAttribute("role", "status");
    else message.removeAttribute("role");
    notice.hidden = false;
    const updateMessage = () => {
      if (!notice.hidden && resultHeading === heading)
        message.textContent = heading.textContent.trim();
    };
    // Establish the visible live region before inserting native-navigation success text.
    if (announce) window.requestAnimationFrame(updateMessage);
    else updateMessage();
  }
  notice.querySelector("[data-notice-dismiss]").addEventListener("click", () => {
    const returnFocus = notice.contains(document.activeElement);
    notice.hidden = true;
    message.textContent = "";
    if (returnFocus && resultHeading?.isConnected) {
      resultHeading.tabIndex = -1;
      resultHeading.focus();
    }
  });
  document.addEventListener("htmx:afterSwap", (event) => {
    // UX-3 focuses this checked result heading; do not announce it twice.
    if (event.detail.target.id === "mutation-workspace") showResult(false);
  });
  showResult(true);
}

initializeOperationNotice();

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

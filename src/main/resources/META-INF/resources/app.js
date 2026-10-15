// Progressive feedback only: native forms and server validation remain authoritative.
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

document.addEventListener("submit", (event) => {
  const form = event.target;
  if (!form.matches(".mutation-form") || event.defaultPrevented) return;
  if (form.getAttribute("aria-busy") === "true") {
    event.preventDefault();
    return;
  }
  form.setAttribute("aria-busy", "true");
  const button = form.querySelector('button[type="submit"]');
  button.dataset.idleLabel = button.textContent;
  button.textContent = form.dataset.submittingLabel;
  button.disabled = true;
  document.getElementById("submission-status").textContent = form.dataset.submittingLabel;
});

// Browsers can restore the old form from the back/forward cache after navigation.
window.addEventListener("pageshow", () => {
  for (const form of document.querySelectorAll(".mutation-form")) {
    form.setAttribute("aria-busy", "false");
    const button = form.querySelector('button[type="submit"]');
    button.disabled = false;
    if (button.dataset.idleLabel) button.textContent = button.dataset.idleLabel;
  }
  const status = document.getElementById("submission-status");
  if (status) status.textContent = "";
});

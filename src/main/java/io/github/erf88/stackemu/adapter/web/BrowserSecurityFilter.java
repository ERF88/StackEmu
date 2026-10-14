package io.github.erf88.stackemu.adapter.web;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/** Defense-in-depth for REST pages; this does not authenticate local users. */
@Provider
public class BrowserSecurityFilter implements ContainerResponseFilter {
  @Override
  public void filter(ContainerRequestContext request, ContainerResponseContext response) {
    response.getHeaders().putSingle("X-Content-Type-Options", "nosniff");
    response.getHeaders().putSingle("Referrer-Policy", "no-referrer");
    response.getHeaders().putSingle("X-Frame-Options", "DENY");
    response
        .getHeaders()
        .putSingle(
            "Content-Security-Policy",
            "default-src 'none'; script-src 'self'; style-src 'self'; img-src 'self'; "
                + "connect-src 'self'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'");
  }
}

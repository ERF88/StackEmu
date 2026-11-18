package io.github.erf88.stackemu.adapter.web.sqs;

import io.github.erf88.stackemu.application.sqs.SqsException;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** HTTP presentation details shared only by SQS resources. */
final class SqsWebSupport {
  private SqsWebSupport() {}

  static Response workspace(TemplateInstance template) {
    return workspace(Response.Status.OK.getStatusCode(), template);
  }

  static Response workspace(int status, TemplateInstance template) {
    return Response.status(status)
        .type(MediaType.TEXT_HTML_TYPE)
        .header("Cache-Control", "no-store")
        .header("X-StackEmu-Action", "workspace")
        .entity(template.render())
        .build();
  }

  static int status(SqsException failure) {
    return switch (failure.kind()) {
      case INVALID -> 400;
      case FORBIDDEN -> 403;
      case NOT_FOUND -> 404;
      case CONFLICT -> 409;
      case UNAVAILABLE -> 503;
      case FAILED -> 502;
    };
  }

  static String messageKey(SqsException failure) {
    return switch (failure.kind()) {
      case INVALID -> "sqsInvalidRequest";
      case FORBIDDEN -> "accessDenied";
      case NOT_FOUND -> "sqsQueueNotFound";
      case CONFLICT -> "sqsQueueConflict";
      case UNAVAILABLE -> "sqsUnavailable";
      case FAILED -> "operationFailed";
    };
  }

  static String queueHref(String queue, String language, String theme, String density) {
    return "/sqs/queue?name="
        + encode(queue)
        + "&lang="
        + encode(language)
        + "&theme="
        + encode(theme)
        + "&density="
        + encode(density);
  }

  static String serviceHref(String language, String theme, String density) {
    return "/sqs?lang="
        + encode(language)
        + "&theme="
        + encode(theme)
        + "&density="
        + encode(density);
  }

  static Response queueRedirect(String queue, String language, String theme, String density) {
    return Response.seeOther(URI.create(queueHref(queue, language, theme, density))).build();
  }

  static Response serviceRedirect(String language, String theme, String density) {
    return Response.seeOther(URI.create(serviceHref(language, theme, density))).build();
  }

  private static String encode(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }
}

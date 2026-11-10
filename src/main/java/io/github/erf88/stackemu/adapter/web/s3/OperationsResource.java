package io.github.erf88.stackemu.adapter.web.s3;

import io.github.erf88.stackemu.adapter.web.UiContext;
import io.github.erf88.stackemu.application.s3.OperationException;
import io.github.erf88.stackemu.application.s3.S3OperationPort;
import io.github.erf88.stackemu.application.s3.S3OperationPort.Kind;
import io.github.erf88.stackemu.application.s3.S3OperationPort.Operation;
import io.github.erf88.stackemu.application.s3.S3OperationPort.Request;
import io.github.erf88.stackemu.application.s3.S3OperationPort.State;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

@Path("/operations")
@Blocking
@Produces(MediaType.TEXT_HTML)
public class OperationsResource {
  private final S3OperationPort operations;

  public OperationsResource(S3OperationPort operations) {
    this.operations = operations;
  }

  public record OperationPageView(
      Operation operation, String backHref, String reviewHref, String message, boolean accepted) {
    public String titleKey() {
      return "operationKind" + operation.request().kind().name();
    }

    public String consequenceKey() {
      return "operationConsequence" + operation.request().kind().name();
    }

    public String stateKey() {
      return "operationState" + operation.state().name();
    }

    public boolean failed() {
      return operation.state() == State.FAILED
          || operation.state() == State.PARTIAL
          || operation.state() == State.UNCERTAIN;
    }

    public boolean cleanupAvailable() {
      return operation.request().kind() == Kind.DELETE_BUCKET
          && "CONFLICT".equals(operation.message());
    }

    public String cleanupHref() {
      return "/s3/empty-bucket?bucket=" + S3ViewMapper.encode(operation.request().bucket());
    }

    public boolean transferConflict() {
      return (operation.request().kind() == Kind.COPY || operation.request().kind() == Kind.MOVE)
          && operation.state() == State.FAILED
          && operation.message().equals("CONFLICT");
    }

    public String transferHref() {
      var request = operation.request();
      return "/s3/transfer?bucket="
          + S3ViewMapper.encode(request.bucket())
          + "&key="
          + S3ViewMapper.encode(request.key())
          + "&move="
          + (request.kind() == Kind.MOVE)
          + "&destinationBucket="
          + S3ViewMapper.encode(request.destinationBucket())
          + "&destinationKey="
          + S3ViewMapper.encode(request.destinationKey());
    }
  }

  @CheckedTemplate
  public static class Templates {
    public static native TemplateInstance review(OperationPageView view, UiContext ui);

    public static native TemplateInstance error(String message, UiContext ui);
  }

  @GET
  @Path("{id}/review")
  public Response review(
      @PathParam("id") String id,
      @QueryParam("lang") String lang,
      @QueryParam("theme") String theme,
      @QueryParam("density") String density) {
    var ui = UiContext.resolve(lang, null, theme, density, "/operations/" + id + "/review");
    try {
      return review(operations.get(id), ui, "", false, 200);
    } catch (OperationException failure) {
      return error(failure, ui);
    }
  }

  public Response prepare(Request request, UiContext ui) {
    try {
      String id = operations.prepare(request);
      return review(operations.get(id), ui, "", false, 200);
    } catch (OperationException failure) {
      return error(failure, ui);
    }
  }

  @POST
  @Path("{id}/start")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response start(
      @PathParam("id") String id,
      @RestForm String bucketName,
      @RestForm String lang,
      @RestForm String theme,
      @RestForm String density) {
    var ui = UiContext.resolve(lang, null, theme, density, "/operations/" + id + "/review");
    try {
      var result = operations.start(id, bucketName);
      return review(
          result,
          ui,
          "",
          result.state() == State.RUNNING || result.state() == State.SUCCEEDED,
          200);
    } catch (OperationException failure) {
      try {
        return review(
            operations.get(id),
            ui,
            "operationReason" + failure.reason().name(),
            false,
            status(failure));
      } catch (OperationException missing) {
        return error(missing, ui);
      }
    }
  }

  @GET
  @Path("{id}/state")
  public Response state(@PathParam("id") String id) {
    try {
      var operation = operations.get(id);
      return Response.ok(
              "{\"state\":\""
                  + operation.state().name()
                  + "\",\"done\":"
                  + operation.done()
                  + ",\"total\":"
                  + operation.total()
                  + ",\"active\":"
                  + operation.active()
                  + "}")
          .type(MediaType.APPLICATION_JSON)
          .header("Cache-Control", "no-store")
          .build();
    } catch (OperationException failure) {
      return Response.status(status(failure)).build();
    }
  }

  public Response invalid(UiContext ui) {
    return response(422, Templates.error("invalidRequest", ui));
  }

  private Response review(
      Operation operation, UiContext ui, String message, boolean accepted, int status) {
    return response(status, Templates.review(view(operation, ui, message, accepted), ui));
  }

  private static OperationPageView view(
      Operation operation, UiContext ui, String message, boolean accepted) {
    var request = operation.request();
    String back = "/s3?" + ui.query();
    if (!operation.bucketConfirmation()) {
      int slash =
          request
              .key()
              .lastIndexOf('/', request.key().length() - (request.key().endsWith("/") ? 2 : 1));
      String prefix = slash < 0 ? "" : request.key().substring(0, slash + 1);
      back = S3ViewMapper.bucketPath(request.bucket(), prefix, null) + "&" + ui.query();
    }
    return new OperationPageView(
        operation,
        back,
        "/operations/" + operation.id() + "/review?" + ui.query(),
        message,
        accepted);
  }

  private static Response response(int status, TemplateInstance template) {
    return Response.status(status)
        .type(MediaType.TEXT_HTML)
        .header("Cache-Control", "no-store")
        .header("X-StackEmu-Action", "workspace")
        .entity(template.render())
        .build();
  }

  private static int status(OperationException failure) {
    return switch (failure.reason()) {
      case MISSING -> 404;
      case NAME -> 422;
      case FORBIDDEN -> 403;
      case UNAVAILABLE -> 503;
      case CAPACITY -> 429;
      default -> 409;
    };
  }

  private static Response error(OperationException failure, UiContext ui) {
    return response(
        status(failure), Templates.error("operationReason" + failure.reason().name(), ui));
  }
}

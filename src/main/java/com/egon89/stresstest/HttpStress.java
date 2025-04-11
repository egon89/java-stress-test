package com.egon89.stresstest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

public class HttpStress {
  private static final List<String> VALID_METHODS = List.of("GET", "HEAD", "PATCH", "POST", "PUT", "DELETE");

  public void execute(HttpStressInput input) throws InterruptedException, ExecutionException {
    System.out.println("Starting stress test...");
    System.out.printf("URL: %s, Method: %s, Request(s): %d, Concurrency: %d%n",
        input.url, input.method, input.totalRequest, input.concurrentRequest);

    var startTime = Instant.now();
    try (var client = HttpClient.newHttpClient()) {
      var resultQueue = new LinkedBlockingQueue<Result>();
      var semaphore = new ArrayBlockingQueue<Object>(input.concurrentRequest);

      try(var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        scope.fork(() -> {
          processResult(resultQueue, input.totalRequest);
          return null;
        });

        for (int i = 0; i < input.totalRequest; i++) {
          final var requestNumber = i + 1;
          semaphore.put(new Object());

          scope.fork(() -> {
            var requestStart = Instant.now();
            try {
              var bodyRequest = input.body.isEmpty()
                  ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(input.body);
              var requestBuilder = HttpRequest.newBuilder(URI.create(input.url))
                  .method(input.method, bodyRequest);
              input.headers.forEach(requestBuilder::header);
              var request = requestBuilder.build();

              var response = client.send(request, HttpResponse.BodyHandlers.ofString());

              var requestDuration = Duration.between(requestStart, Instant.now());
              var result = new Result(response.statusCode(), requestDuration);
              resultQueue.put(result);

              System.out.printf("Request %d: Status Code: %d (%dms)%n",
                  requestNumber, result.statusCode(), result.duration().toMillis());
            } catch (Exception e) {
              System.err.println(e);
              resultQueue.put(new Result(-1, Duration.ZERO));
            }

            if (input.intervalSec > 0) {
              Thread.sleep(input.intervalSec * 1000L);
            }
            semaphore.take();

            return null;
          });
        }
        scope.join();
        scope.throwIfFailed();
      }
    }

    var totalDuration = Duration.between(startTime, Instant.now());
    System.out.printf("Total Duration: %s(ms)%n", totalDuration.toMillis());
  }

  public static void validate(HttpStressInput input) {

  }

  private void processResult(BlockingQueue<Result> resultQueue, int totalRequest) throws InterruptedException {
    var processed = 0;
    var totalDuration = Duration.ZERO;
    var statusCodeMap = new HashMap<Integer, Integer>();

    while (processed < totalRequest) {
      var result = resultQueue.take();
      statusCodeMap.merge(result.statusCode(), 1, Integer::sum);
      totalDuration = totalDuration.plus(result.duration());
      processed++;
    }

    var averageResponseTime = totalRequest > 0 ? totalDuration.toMillis() / totalRequest : 0L;

    System.out.println("\n--- Summary Report ---");
    System.out.printf("Total requests: %d%n", totalRequest);
    statusCodeMap.forEach((statusCode, counter) ->
        System.out.printf("Status: %d, Counter:%d%n", statusCode, counter));
    System.out.printf("Average response time: %d(ms)%n", averageResponseTime);

  }

  public record HttpStressInput(
      String url,
      String method,
      int totalRequest,
      int concurrentRequest,
      int intervalSec,
      String body,
      Map<String, String> headers
  ) {
    public HttpStressInput {
      if (Objects.isNull(url) || url.isBlank()) {
        throw new IllegalArgumentException("url must not be null or blank");
      }

      if (Objects.isNull(method) || method.isBlank()) {
        throw new IllegalArgumentException("method must not be null or blank");
      }

      if (totalRequest <= 0) {
        throw new IllegalArgumentException("totalRequest must be positive");
      }

      if (concurrentRequest <= 0) {
        throw new IllegalArgumentException("concurrentRequest must be positive");
      }

      if (intervalSec < 0) {
        throw new IllegalArgumentException("intervalSec must be zero or positive");
      }

      if(!VALID_METHODS.contains(method.toUpperCase())) {
        throw new IllegalArgumentException(
            "invalid HTTP method: %s. Allowed methods are: GET, HEAD, PATCH, POST, PUT, DELETE".formatted(method));
      }
    }
  }

  private record Result(int statusCode, Duration duration) {}
}

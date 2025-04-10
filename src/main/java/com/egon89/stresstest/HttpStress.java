package com.egon89.stresstest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class HttpStress {
  public void execute(String url, String method, int totalRequest, int concurrentRequest, int intervalSec, String body, Map<String, String> headers) throws InterruptedException, ExecutionException {
    System.out.println("Starting stress test...");
    System.out.printf("URL: %s, Method: %s, Request(s): %d, Concurrency: %d%n",
        url, method, totalRequest, concurrentRequest);

    var startTime = Instant.now();
    try (var client = HttpClient.newHttpClient()) {
      var resultQueue = new LinkedBlockingQueue<Result>();
      var semaphore = new ArrayBlockingQueue<Object>(concurrentRequest);

      try(var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        scope.fork(() -> {
          processResult(resultQueue, totalRequest);
          return null;
        });

        for (int i = 0; i < totalRequest; i++) {
          final var requestNumber = i + 1;
          semaphore.put(new Object());

          scope.fork(() -> {
            var requestStart = Instant.now();
            try {
              var bodyRequest = body.isEmpty()
                  ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body);
              var requestBuilder = HttpRequest.newBuilder(URI.create(url))
                  .method(method, bodyRequest);
              headers.forEach(requestBuilder::header);
              var request = requestBuilder.build();

              var response = client.send(request, HttpResponse.BodyHandlers.ofString());

              var requestDuration = Duration.between(requestStart, Instant.now());
              var result = new Result(response.statusCode(), requestDuration);
              resultQueue.put(result);

              System.out.printf("Request %d: Status Code: %d (%dms)%n",
                  requestNumber, result.statusCode(), result.duration().toMillis());
            } catch (Exception e) {
              resultQueue.put(new Result(-1, Duration.ZERO));
            }

            if (intervalSec > 0) {
              Thread.sleep(intervalSec * 1000L);
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

  private record Result(int statusCode, Duration duration) {}
}

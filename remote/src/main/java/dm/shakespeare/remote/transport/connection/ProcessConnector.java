/*
 * Copyright 2019 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dm.shakespeare.remote.transport.connection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import dm.shakespeare.log.LogPrinters;
import dm.shakespeare.log.Logger;
import dm.shakespeare.remote.config.BuildConfig;
import dm.shakespeare.remote.io.JavaSerializer;
import dm.shakespeare.remote.io.RawData;
import dm.shakespeare.remote.io.Serializer;
import dm.shakespeare.remote.transport.message.RemoteRequest;
import dm.shakespeare.remote.transport.message.RemoteResponse;
import dm.shakespeare.remote.util.PLZW;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 06/07/2019.
 */
public class ProcessConnector implements Connector {

  private final AtomicInteger connectionCount = new AtomicInteger();
  private final String endBoundary;
  private final InputStream inputStream;
  private final Logger logger;
  private final PrintStream printStream;
  private final HashMap<String, RemoteResponse> responses = new HashMap<String, RemoteResponse>();
  private final Serializer serializer;
  private final String startBoundary;

  private BufferedReader reader;
  private Sender sender;

  public ProcessConnector(@NotNull final PrintStream printStream,
      @NotNull final InputStream inputStream, @NotNull final String startBoundary,
      @NotNull final String endBoundary) {
    this(defaultSerializer(), printStream, inputStream, startBoundary, endBoundary,
        defaultLogger());
  }

  public ProcessConnector(@NotNull final PrintStream printStream,
      @NotNull final InputStream inputStream, @NotNull final String startBoundary,
      @NotNull final String endBoundary, @NotNull final Logger logger) {
    this(defaultSerializer(), printStream, inputStream, startBoundary, endBoundary, logger);
  }

  public ProcessConnector(@NotNull final Serializer serializer,
      @NotNull final PrintStream printStream, @NotNull final InputStream inputStream,
      @NotNull final String startBoundary, @NotNull final String endBoundary) {
    this(serializer, printStream, inputStream, startBoundary, endBoundary, defaultLogger());
  }

  public ProcessConnector(@NotNull final Serializer serializer,
      @NotNull final PrintStream printStream, @NotNull final InputStream inputStream,
      @NotNull final String startBoundary, @NotNull final String endBoundary,
      @NotNull final Logger logger) {
    this.serializer = ConstantConditions.notNull("serializer", serializer);
    this.printStream = ConstantConditions.notNull("printStream", printStream);
    this.inputStream = ConstantConditions.notNull("inputStream", inputStream);
    this.startBoundary = ConstantConditions.notNull("startBoundary", startBoundary);
    this.endBoundary = ConstantConditions.notNull("endBoundary", endBoundary);
    this.logger = ConstantConditions.notNull("logger", logger);
  }

  @NotNull
  private static byte[] decode(@NotNull final String string) {
    return PLZW.getDecoder().decode(string);
  }

  @NotNull
  private static Logger defaultLogger() {
    return new Logger(LogPrinters.javaLoggingPrinter(ProcessConnector.class.getName()));
  }

  @NotNull
  private static Serializer defaultSerializer() {
    final JavaSerializer serializer = new JavaSerializer();
    serializer.whitelist(Collections.singleton("**"));
    return serializer;
  }

  @NotNull
  private static String encodeBytes(@NotNull final byte[] bytes) {
    return PLZW.getEncoder().encode(bytes);
  }

  @NotNull
  public Sender connect(@NotNull final Receiver receiver) {
    if (connectionCount.getAndIncrement() == 0) {
      try {
        reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

      } catch (final UnsupportedEncodingException e) {
        throw new IllegalStateException(e);
      }

      final ExecutorService executorService = Executors.newSingleThreadExecutor();
      new Thread(new Runnable() {

        public void run() {
          final StringBuilder builder = new StringBuilder();
          try {
            final String startBoundary = ProcessConnector.this.startBoundary;
            final String endBoundary = ProcessConnector.this.endBoundary;
            String line;
            while ((line = reader.readLine()) != null) {
              if (line.startsWith(startBoundary) && line.endsWith(endBoundary)) {
                builder.append(line, startBoundary.length(), line.length() - endBoundary.length());
                try {
                  logger.dbg("processing message");
                  Object object = deserialize(builder.toString());
                  if (object instanceof RequestWrapper) {
                    final RequestWrapper requestWrapper = (RequestWrapper) object;
                    logger.dbg("receiving request: " + requestWrapper.getRequestId());
                    executorService.execute(new Runnable() {

                      public void run() {
                        final RemoteRequest request = requestWrapper.getRequest();
                        try {
                          logger.dbg("processing request: " + requestWrapper.getRequestId());
                          final RemoteResponse response = receiver.receive(request);
                          final ResponseWrapper responseWrapper =
                              new ResponseWrapper(response, requestWrapper.getRequestId());
                          printStream.println(serialize(responseWrapper));

                        } catch (final Exception e) {
                          logger.err(e,
                              "failed to process request: " + requestWrapper.getRequestId());
                          if (request != null) {
                            final ResponseWrapper responseWrapper =
                                new ResponseWrapper(request.buildResponse().withError(e),
                                    requestWrapper.getRequestId());
                            try {
                              printStream.println(serialize(responseWrapper));

                            } catch (final Exception ex) {
                              logger.err(ex, "failed to send error response: "
                                  + requestWrapper.getRequestId());
                            }
                          }
                        }
                      }
                    });

                  } else if (object instanceof ResponseWrapper) {
                    final ResponseWrapper responseWrapper = (ResponseWrapper) object;
                    logger.dbg("receiving response: " + responseWrapper.getRequestId());
                    synchronized (responses) {
                      final HashMap<String, RemoteResponse> responses =
                          ProcessConnector.this.responses;
                      responses.put(responseWrapper.getRequestId(), responseWrapper.getResponse());
                      responses.notifyAll();
                    }
                  }
                  builder.setLength(0);

                } catch (final Exception e) {
                  logger.err(e, "failed to process message: " + line);
                }
              }
            }

          } catch (final IOException e) {
            logger.wrn(e, "error while reading input stream");

          } finally {
            executorService.shutdown();
          }
        }
      }).start();
      sender = new Sender() {

        public void disconnect() {
          if (connectionCount.decrementAndGet() == 0) {
            try {
              reader.close();

            } catch (final IOException ignored) {
            }
            printStream.close();
          }
        }

        @NotNull
        public RemoteResponse send(@NotNull final RemoteRequest request,
            @Nullable final String receiverId) throws Exception {
          final String requestId = UUID.randomUUID().toString();
          final RequestWrapper requestWrapper = new RequestWrapper(request, requestId);
          logger.dbg("sending request: " + requestWrapper.getRequestId());
          final PrintStream printStream = ProcessConnector.this.printStream;
          printStream.println(serialize(requestWrapper));
          printStream.flush();
          RemoteResponse response;
          synchronized (responses) {
            final HashMap<String, RemoteResponse> responses = ProcessConnector.this.responses;
            while ((response = responses.remove(requestId)) == null) {
              responses.wait();
            }
          }
          logger.dbg("request response: " + requestWrapper.getRequestId());
          return response;
        }
      };
    }
    return new ProcessSender(sender);
  }

  @NotNull
  private Object deserialize(@NotNull final String string) throws Exception {
    return serializer.deserialize(RawData.wrap(decode(string)),
        ProcessConnector.class.getClassLoader());
  }

  @NotNull
  private String serialize(@NotNull final Object object) throws Exception {
    return startBoundary + encodeBytes(serializer.serialize(object)) + endBoundary;
  }

  private static class ProcessSender implements Sender {

    private final AtomicBoolean connected = new AtomicBoolean(true);
    private final Sender sender;

    private ProcessSender(@NotNull final Sender sender) {
      this.sender = sender;
    }

    public void disconnect() {
      if (connected.getAndSet(false)) {
        sender.disconnect();
      }
    }

    @NotNull
    public RemoteResponse send(@NotNull final RemoteRequest request,
        @Nullable final String receiverId) throws Exception {
      return sender.send(request, receiverId);
    }
  }

  private static class RequestWrapper implements Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final RemoteRequest request;
    private final String requestId;

    private RequestWrapper() {
      request = null;
      requestId = null;
    }

    private RequestWrapper(@NotNull final RemoteRequest request, @NotNull final String requestId) {
      this.request = request;
      this.requestId = requestId;
    }

    public RemoteRequest getRequest() {
      return request;
    }

    public String getRequestId() {
      return requestId;
    }
  }

  private static class ResponseWrapper implements Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final String requestId;
    private final RemoteResponse response;

    private ResponseWrapper() {
      response = null;
      requestId = null;
    }

    private ResponseWrapper(@NotNull final RemoteResponse response,
        @NotNull final String requestId) {
      this.response = response;
      this.requestId = requestId;
    }

    public String getRequestId() {
      return requestId;
    }

    public RemoteResponse getResponse() {
      return response;
    }
  }
}

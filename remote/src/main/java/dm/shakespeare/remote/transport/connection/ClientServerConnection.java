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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.log.LogPrinters;
import dm.shakespeare.log.Logger;
import dm.shakespeare.remote.config.BuildConfig;
import dm.shakespeare.remote.transport.connection.Connector.Receiver;
import dm.shakespeare.remote.transport.connection.Connector.Sender;
import dm.shakespeare.remote.transport.message.RemoteRequest;
import dm.shakespeare.remote.transport.message.RemoteResponse;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 07/22/2019.
 */
public class ClientServerConnection {

  @NotNull
  public static ClientConnectorBuilder newClientConnector(@NotNull final ConnectorClient client) {
    return new ClientConnectorBuilder(client);
  }

  @NotNull
  public static ServerConnectorBuilder newServerConnector() {
    return new ServerConnectorBuilder();
  }

  public interface ConnectorClient {

    @NotNull
    Object request(@NotNull Object payload) throws Exception;
  }

  public static class ClientConnector implements Connector {

    private final ConnectorClient client;
    private final Logger logger;
    private final long maxPollingMillis;
    private final long minPollingMillis;

    private ClientConnector(@NotNull final ConnectorClient client, final long minPollingMillis,
        final long maxPollingMillis, @NotNull final Logger logger) {
      this.client = client;
      this.minPollingMillis = minPollingMillis;
      this.maxPollingMillis = maxPollingMillis;
      this.logger = logger;
    }

    @NotNull
    public Sender connect(@NotNull final Receiver receiver) {
      return new ClientSender(receiver, client, minPollingMillis, maxPollingMillis, logger);
    }
  }

  public static class ClientConnectorBuilder {

    private final ConnectorClient client;

    private Logger logger;
    private long maxPollingMillis = TimeUnit.MINUTES.toMillis(15);
    private long minPollingMillis = -1;

    private ClientConnectorBuilder(@NotNull final ConnectorClient client) {
      this.client = ConstantConditions.notNull("client", client);
    }

    @NotNull
    public Connector build() {
      final Logger logger = this.logger;
      return new ClientConnector(client, minPollingMillis, maxPollingMillis,
          (logger != null) ? logger
              : new Logger(LogPrinters.javaLoggingPrinter(ClientConnector.class.getName())));
    }

    @NotNull
    public ClientConnectorBuilder withLogger(@NotNull final Logger logger) {
      this.logger = ConstantConditions.notNull("logger", logger);
      return this;
    }

    @NotNull
    public ClientConnectorBuilder withMaxPollingMillis(final long maxPollingMillis) {
      this.maxPollingMillis = maxPollingMillis;
      return this;
    }

    @NotNull
    public ClientConnectorBuilder withMinPollingMillis(final long minPollingMillis) {
      this.minPollingMillis = minPollingMillis;
      return this;
    }
  }

  public static class ServerConnector implements Connector {

    private final Logger logger;
    private final Object mutex = new Object();
    private final int piggyBackRequests;
    private final HashMap<String, RemoteResponse> responses = new HashMap<String, RemoteResponse>();

    private Receiver receiver;

    private ServerConnector(final int piggyBackRequests, @NotNull final Logger logger) {
      this.piggyBackRequests = piggyBackRequests;
      this.logger = logger;
    }

    @NotNull
    public Sender connect(@NotNull final Receiver receiver) throws Exception {
      synchronized (mutex) {
        this.receiver = ConstantConditions.notNull("receiver", receiver);
      }
      return new Sender() {

        private volatile boolean isConnected = true;

        public void disconnect() {
          isConnected = false;
        }

        @NotNull
        public RemoteResponse send(@NotNull final RemoteRequest request,
            @Nullable final String receiverId) throws Exception {
          if (!isConnected) {
            throw new IllegalStateException("sender is disconnected");
          }
          final String requestId = UUID.randomUUID().toString();
          final RequestWrapper requestWrapper = new RequestWrapper(request, requestId);
          // TODO: 2019-07-22 enqueue request
          RemoteResponse response;
          synchronized (responses) {
            final HashMap<String, RemoteResponse> responses = ServerConnector.this.responses;
            while ((response = responses.remove(requestId)) == null) {
              responses.wait();
            }
          }
          return response;
        }
      };
    }

    @Nullable
    public Object receive(@NotNull final Object payload) throws Exception {
      final Receiver receiver;
      synchronized (mutex) {
        receiver = this.receiver;
        if (receiver == null) {
          throw new IllegalStateException("not connected");
        }
      }

      if (payload instanceof RemoteRequest) {
        final RemoteRequest remoteRequest = (RemoteRequest) payload;
        try {
          final RemoteResponse remoteResponse = receiver.receive(remoteRequest);
          // TODO: 2019-07-22 piggyBack
          if (piggyBackRequests > 0) {

          }
          return new ResponseWrapper(remoteResponse, null, Collections.<RequestWrapper>emptyList());

        } catch (final Exception e) {
          return new ResponseWrapper(remoteRequest.buildResponse().withError(e), null,
              Collections.<RequestWrapper>emptyList());
        }

      } else if (payload instanceof RemoteRequestsGet) {
        // TODO: 2019-07-22 consume queue
        return new RemoteRequestsResult();

      } else if (payload instanceof ResponseWrapper) {
        final ResponseWrapper responseWrapper = (ResponseWrapper) payload;
        synchronized (responses) {
          final HashMap<String, RemoteResponse> responses = ServerConnector.this.responses;
          responses.put(responseWrapper.getRequestId(), responseWrapper.getResponse());
          responses.notifyAll();
        }
        return null;
      }
      throw new IllegalArgumentException("unsupported payload type");
    }
  }

  public static class ServerConnectorBuilder {

    private Logger logger;
    private int piggyBackRequests;
    // TODO: 2019-07-22 queue size + timeout

    private ServerConnectorBuilder() {
    }

    @NotNull
    public ServerConnector build() {
      final Logger logger = this.logger;
      return new ServerConnector(piggyBackRequests, (logger != null) ? logger
          : new Logger(LogPrinters.javaLoggingPrinter(ClientConnector.class.getName())));
    }

    @NotNull
    public ServerConnectorBuilder withLogger(@NotNull final Logger logger) {
      this.logger = ConstantConditions.notNull("logger", logger);
      return this;
    }

    @NotNull
    public ServerConnectorBuilder withPiggyBackRequests(final int piggyBackRequests) {
      this.piggyBackRequests = piggyBackRequests;
      return this;
    }
  }

  private static class ClientSender implements Sender {

    private static final ScheduledExecutorService executorService =
        ExecutorServices.asScheduled(Executors.newCachedThreadPool());

    private final ConnectorClient client;
    private final Logger logger;
    private final long maxPollingMillis;
    private final long minPollingMillis;
    private final Object mutex = new Object();
    private final Runnable pollingTask;
    private final Receiver receiver;
    private final String senderId = UUID.randomUUID().toString();
    private long currentDelayMillis;
    private volatile ScheduledFuture<?> future;
    private volatile boolean isConnected = true;

    private ClientSender(@NotNull final Receiver receiver, @NotNull final ConnectorClient client,
        final long minPollingMillis, final long maxPollingMillis, @NotNull final Logger logger) {
      this.receiver = ConstantConditions.notNull("receiver", receiver);
      this.client = client;
      this.minPollingMillis = minPollingMillis;
      this.maxPollingMillis = maxPollingMillis;
      this.logger = logger;
      pollingTask = new Runnable() {

        public void run() {
          try {
            final Object response = client.request(new RemoteRequestsGet(senderId));
            if (response instanceof RemoteRequestsResult) {
              handleRequests(((RemoteRequestsResult) response).getRequests());

            } else {
              logger.wrn("[%s] invalid response from server: %s", ClientSender.this, response);
            }

          } catch (final Exception e) {
            logger.err(e, "[%s] failed to send request", ClientSender.this);
          }
          final long delay;
          synchronized (mutex) {
            delay = (currentDelayMillis = Math.min(currentDelayMillis << 1, maxPollingMillis));
          }
          future = executorService.schedule(this, delay, TimeUnit.MILLISECONDS);
        }
      };
      reschedulePolling();
    }

    public void disconnect() {
      isConnected = false;
    }

    @NotNull
    public RemoteResponse send(@NotNull final RemoteRequest request,
        @Nullable final String receiverId) throws Exception {
      if (!isConnected) {
        throw new IllegalStateException("sender is disconnected");
      }
      logger.dbg("[%s] sending request to [%s]: %s", this, receiverId, request);
      final Object response = client.request(request.withSenderId(senderId));
      if (response instanceof ResponseWrapper) {
        final ResponseWrapper responseWrapper = (ResponseWrapper) response;
        handleRequests(responseWrapper.getRequests());
        return responseWrapper.getResponse();

      } else {
        throw new IllegalArgumentException("invalid response from server: " + response);
      }
    }

    private void handleRequests(@Nullable final List<RequestWrapper> requests) {
      if ((requests == null) || requests.isEmpty()) {
        return;
      }
      reschedulePolling();
      executorService.execute(new Runnable() {

        public void run() {
          for (final RequestWrapper requestWrapper : requests) {
            final RemoteRequest remoteRequest = requestWrapper.getRequest();
            logger.dbg("[%s] receiving request: %s", this, remoteRequest);
            try {
              final RemoteResponse remoteResponse = receiver.receive(remoteRequest);
              client.request(new ResponseWrapper(remoteResponse, requestWrapper.getRequestId(),
                  Collections.<RequestWrapper>emptyList()));

            } catch (final Exception e) {
              logger.err(e, "[%s] failed to handle request", ClientSender.this);
              try {
                client.request(new ResponseWrapper(remoteRequest.buildResponse().withError(e),
                    requestWrapper.getRequestId(), Collections.<RequestWrapper>emptyList()));

              } catch (final Exception ex) {
                logger.err(ex, "[%s] failed to send error", ClientSender.this);
              }
            }
          }
        }
      });
    }

    private void reschedulePolling() {
      long delay = 0;
      Runnable task = null;
      synchronized (mutex) {
        if (maxPollingMillis >= 0) {
          delay = (currentDelayMillis = Math.max(minPollingMillis, 0));
          task = pollingTask;
        }
      }

      if (task != null) {
        if (future != null) {
          future.cancel(false);
        }
        future = executorService.schedule(task, delay, TimeUnit.MILLISECONDS);
      }
    }
  }

  private static class RemoteRequestsGet implements Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final String senderId;

    private RemoteRequestsGet() {
      senderId = null;
    }

    private RemoteRequestsGet(final String senderId) {
      this.senderId = senderId;
    }

    public String getSenderId() {
      return senderId;
    }
  }

  private static class RemoteRequestsResult implements Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final List<RequestWrapper> requests;

    private RemoteRequestsResult() {
      requests = null;
    }

    private RemoteRequestsResult(@Nullable final List<RequestWrapper> requests) {
      this.requests = requests;
    }

    public List<RequestWrapper> getRequests() {
      return requests;
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
    private final List<RequestWrapper> requests;
    private final RemoteResponse response;

    private ResponseWrapper() {
      response = null;
      requestId = null;
      requests = null;
    }

    private ResponseWrapper(@NotNull final RemoteResponse response,
        @Nullable final String requestId, @Nullable final List<RequestWrapper> requests) {
      this.response = response;
      this.requestId = requestId;
      this.requests = requests;
    }

    public String getRequestId() {
      return requestId;
    }

    public List<RequestWrapper> getRequests() {
      return requests;
    }

    public RemoteResponse getResponse() {
      return response;
    }
  }
}

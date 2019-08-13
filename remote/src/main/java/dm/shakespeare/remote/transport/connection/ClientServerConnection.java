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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.log.LogPrinters;
import dm.shakespeare.log.Logger;
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
    ClientResponse request(@NotNull ClientRequest request) throws Exception;
  }

  public static class ClientConnector implements Connector {

    private final ConnectorClient client;
    private final Logger logger;
    private final int maxPiggyBackRequests;
    private final long maxPollingMillis;
    private final int maxPollingRequests;
    private final long minPollingMillis;
    private final String senderId;

    private ClientConnector(@NotNull final ConnectorClient client, final String senderId,
        final int maxPollingRequests, final long minPollingMillis, final long maxPollingMillis,
        final int maxPiggyBackRequests, @NotNull final Logger logger) {
      this.client = client;
      this.senderId = senderId;
      this.maxPollingRequests = maxPollingRequests;
      this.minPollingMillis = minPollingMillis;
      this.maxPollingMillis = maxPollingMillis;
      this.maxPiggyBackRequests = maxPiggyBackRequests;
      this.logger = logger;
    }

    @NotNull
    public Sender connect(@NotNull final Receiver receiver) {
      return new ClientSender(receiver, client, senderId, maxPollingRequests, minPollingMillis,
          maxPollingMillis, maxPiggyBackRequests, logger);
    }
  }

  public static class ClientConnectorBuilder {

    private final ConnectorClient client;

    private Logger logger;
    private int maxPiggyBackRequests;
    private long maxPollingMillis = TimeUnit.MINUTES.toMillis(15);
    private int maxPollingRequests;
    private long minPollingMillis = -1;
    private String senderId;

    private ClientConnectorBuilder(@NotNull final ConnectorClient client) {
      this.client = ConstantConditions.notNull("client", client);
    }

    @NotNull
    public Connector build() {
      final Logger logger = this.logger;
      return new ClientConnector(client, senderId, maxPollingRequests, minPollingMillis,
          maxPollingMillis, maxPiggyBackRequests, (logger != null) ? logger
          : new Logger(LogPrinters.javaLoggingPrinter(ClientConnector.class.getName())));
    }

    @NotNull
    public ClientConnectorBuilder withLogger(final Logger logger) {
      this.logger = logger;
      return this;
    }

    @NotNull
    public ClientConnectorBuilder withMaxPiggyBackRequests(final int maxPiggyBackRequests) {
      this.maxPiggyBackRequests = maxPiggyBackRequests;
      return this;
    }

    @NotNull
    public ClientConnectorBuilder withMaxPollingMillis(final long maxPollingMillis) {
      this.maxPollingMillis = maxPollingMillis;
      return this;
    }

    @NotNull
    public ClientConnectorBuilder withMaxPollingRequests(final int maxPollingRequests) {
      this.maxPollingRequests = maxPollingRequests;
      return this;
    }

    @NotNull
    public ClientConnectorBuilder withMinPollingMillis(final long minPollingMillis) {
      this.minPollingMillis = minPollingMillis;
      return this;
    }

    @NotNull
    public ClientConnectorBuilder withSenderId(final String senderId) {
      this.senderId = senderId;
      return this;
    }
  }

  public static class ServerConnector implements Connector {

    private final Logger logger;
    private final Object mutex = new Object();
    private final RequestQueue queue;
    private final HashMap<String, RemoteResponse> responses = new HashMap<String, RemoteResponse>();

    private Receiver receiver;

    private ServerConnector(final int maxServerRequests, final long requestExpirationMillis,
        @NotNull final Logger logger) {
      this.queue =
          new RequestQueue(maxServerRequests, requestExpirationMillis, TimeUnit.MILLISECONDS);
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
          final ServerRequest serverRequest =
              new ServerRequest().withRequest(request).withRequestId(requestId);
          queue.enqueue(receiverId, serverRequest);
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

    @NotNull
    public ClientResponse receive(@NotNull final ClientRequest request) {
      final Receiver receiver;
      synchronized (mutex) {
        receiver = this.receiver;
        if (receiver == null) {
          throw new IllegalStateException("not connected");
        }
      }

      if (request instanceof ServerResponse) {
        final ServerResponse serverResponse = (ServerResponse) request;
        synchronized (responses) {
          final HashMap<String, RemoteResponse> responses = ServerConnector.this.responses;
          responses.put(serverResponse.getRequestId(), serverResponse.getResponse());
          responses.notifyAll();
        }
        return new ClientResponse();
      }
      final ClientResponse clientResponse = new ClientResponse();
      final RemoteRequest remoteRequest = request.getRequest();
      if (remoteRequest != null) {
        if (!(remoteRequest instanceof GetRequest)) {
          try {
            final RemoteResponse remoteResponse = receiver.receive(remoteRequest);
            clientResponse.setResponse(remoteResponse);

          } catch (final Exception e) {
            clientResponse.setResponse(remoteRequest.buildResponse().withError(e));
          }
        }

        if (request.getMaxIncludedRequests() > 0) {
          clientResponse.setRequests(
              queue.dequeue(remoteRequest.getSenderId(), request.getMaxIncludedRequests()));
        }

      } else {
        logger.wrn("[%s] remote request is missing: %s", this, request);
      }
      return clientResponse;
    }
  }

  public static class ServerConnectorBuilder {

    private Logger logger;
    private int maxServerRequests;
    private long requestExpirationMillis;

    private ServerConnectorBuilder() {
    }

    @NotNull
    public ServerConnector build() {
      final Logger logger = this.logger;
      return new ServerConnector(maxServerRequests, requestExpirationMillis,
          (logger != null) ? logger
              : new Logger(LogPrinters.javaLoggingPrinter(ServerConnector.class.getName())));
    }

    @NotNull
    public ServerConnectorBuilder withLogger(final Logger logger) {
      this.logger = logger;
      return this;
    }

    @NotNull
    public ServerConnectorBuilder withMaxServerRequests(final int maxServerRequests) {
      this.maxServerRequests = maxServerRequests;
      return this;
    }

    @NotNull
    public ServerConnectorBuilder withRequestExpirationMillis(final long requestExpirationMillis) {
      this.requestExpirationMillis = requestExpirationMillis;
      return this;
    }
  }

  private static class ClientSender implements Sender {

    private static final ScheduledExecutorService executorService =
        Executors.newSingleThreadScheduledExecutor();
    // TODO: 2019-07-23 optimize

    private final ConnectorClient client;
    private final Logger logger;
    private final int maxPiggyBackRequests;
    private final long maxPollingMillis;
    private final int maxPollingRequests;
    private final long minPollingMillis;
    private final Object mutex = new Object();
    private final Runnable pollingTask;
    private final Receiver receiver;
    private final String senderId;

    private long currentDelayMillis;
    private volatile boolean isConnected = true;
    private volatile ScheduledFuture<?> scheduledFuture;

    private ClientSender(@NotNull final Receiver receiver, @NotNull final ConnectorClient client,
        final String senderId, final int maxPollingRequests, final long minPollingMillis,
        final long maxPollingMillis, final int maxPiggyBackRequests, @NotNull final Logger logger) {
      this.receiver = ConstantConditions.notNull("receiver", receiver);
      this.client = client;
      this.senderId = senderId;
      this.maxPollingRequests = maxPollingRequests;
      this.minPollingMillis = minPollingMillis;
      this.maxPollingMillis = maxPollingMillis;
      this.maxPiggyBackRequests = maxPiggyBackRequests;
      this.logger = logger;
      pollingTask = new Runnable() {

        public void run() {
          try {
            final ClientResponse response = client.request(
                new ClientRequest().withRequest(new GetRequest().withSenderId(senderId))
                    .withMaxIncludedRequests(maxPollingRequests));
            handleRequests(response.getRequests());

          } catch (final Exception e) {
            logger.err(e, "[%s] failed to send request", ClientSender.this);
          }
          final long delay;
          synchronized (mutex) {
            delay = (currentDelayMillis = Math.min(currentDelayMillis << 1, maxPollingMillis));
          }
          scheduledFuture = executorService.schedule(this, delay, TimeUnit.MILLISECONDS);
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
      final ClientResponse response = client.request(
          new ClientRequest().withRequest(request.withSenderId(senderId))
              .withMaxIncludedRequests(maxPiggyBackRequests));
      handleRequests(response.getRequests());
      return response.getResponse();
    }

    private void handleRequests(@Nullable final List<ServerRequest> requests) {
      if ((requests == null) || requests.isEmpty()) {
        return;
      }
      reschedulePolling();
      executorService.execute(new Runnable() {

        public void run() {
          final ArrayList<ServerResponse> responses = new ArrayList<ServerResponse>();
          for (final ServerRequest serverRequest : requests) {
            final RemoteRequest remoteRequest = serverRequest.getRequest();
            logger.dbg("[%s] receiving request: %s", this, remoteRequest);
            try {
              final RemoteResponse remoteResponse = receiver.receive(remoteRequest);
              responses.add(new ServerResponse().withResponse(remoteResponse)
                  .withRequestId(serverRequest.getRequestId())
                  .withMaxIncludedRequests(maxPiggyBackRequests));

            } catch (final Exception e) {
              logger.err(e, "[%s] failed to handle request", ClientSender.this);
              responses.add(
                  new ServerResponse().withResponse(remoteRequest.buildResponse().withError(e))
                      .withRequestId(serverRequest.getRequestId())
                      .withMaxIncludedRequests(maxPiggyBackRequests));
            }

            for (final ServerResponse response : responses) {
              try {
                client.request(response);

              } catch (final Exception e) {
                logger.err(e, "[%s] failed to send response: %s", ClientSender.this, response);
              }
            }
          }
        }
      });
    }

    private void reschedulePolling() {
      if ((maxPollingMillis >= 0) && (maxPollingRequests > 0)) {
        final long delay;
        final Runnable task;
        synchronized (mutex) {
          delay = (currentDelayMillis = Math.max(minPollingMillis, 0));
          task = pollingTask;
        }

        if (scheduledFuture != null) {
          scheduledFuture.cancel(false);
        }
        scheduledFuture = executorService.schedule(task, delay, TimeUnit.MILLISECONDS);
      }
    }
  }
}

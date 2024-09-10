package cn.vertx.proxy;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;

public class Proxy extends AbstractVerticle {
  @Override
  public void start(Promise<Void> startPromise) {
    NetClient client = vertx.createNetClient();
    HttpServer server = vertx.createHttpServer();
    server.requestHandler(req -> {
          if (req.method() == HttpMethod.CONNECT) {
            String proxyAddress = req.uri();
            int idx = proxyAddress.indexOf(':');
            String host = proxyAddress.substring(0, idx);
            int port = Integer.parseInt(proxyAddress.substring(idx + 1));
            System.out.println(host + ":" + port);
            client.connect(port, host).onComplete(ar -> {
              if (ar.succeeded()) {
                NetSocket serverSocket = ar.result();
                serverSocket.pause();
                req.toNetSocket()
                    .onComplete(ar2 -> {
                      if (ar2.succeeded()) {
                        NetSocket clientSocket = ar2.result();
                        serverSocket.exceptionHandler(Throwable::printStackTrace);
                        serverSocket.handler(clientSocket::write);
                        serverSocket.closeHandler(v -> clientSocket.close());
                        clientSocket.exceptionHandler(Throwable::printStackTrace);
                        clientSocket.handler(serverSocket::write);
                        clientSocket.closeHandler(v -> serverSocket.close());
                        serverSocket.resume();
                      } else {
                        serverSocket.close();
                      }
                    });
              } else {
                ar.cause().printStackTrace();
                req.response().setStatusCode(403).end();
              }
            });
          } else {
            req.response().setStatusCode(405).end();
          }
        })
        .listen(80)
        .onComplete(ar -> {
          if (ar.succeeded()) {
            startPromise.complete();
          } else {
            startPromise.fail(ar.cause());
          }
        });
  }
}

package org.stormpx.dl.kit;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class Http {
    public static HttpClient client;
    private static String USER_AGENT="Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:99.0) Gecko/20100101 Firefox/99.0";

    public static void build(URI proxy, String ua, Executor executor){
        var builder=HttpClient.newBuilder()
//                .executor(executor)
                .followRedirects(HttpClient.Redirect.NORMAL);
        if (proxy!=null){
            builder.proxy(new DownloadProxySelector(proxy));
        }
        if (ua!=null){
            USER_AGENT=ua;
        }


        client=builder.build();
    }

    public static ReqResult request(URI uri, ByteRange byteRange) throws IOException {
        try {
            return requestAsync(uri,byteRange).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getMessage(),e);
        }

    }

    public static CompletableFuture<ReqResult> requestAsync(URI uri, ByteRange byteRange) {


            var builder=HttpRequest.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(5))
                    .uri(uri).GET()
                    .setHeader("user-agent",USER_AGENT);
            if (byteRange!=null){
                builder.setHeader("range",byteRange.start()+"-"+ byteRange.end());
            }
            return Http.client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofInputStream())
                    .thenApply((response)->{
                        try {
                            if (response.statusCode()!=200&&response.statusCode()!=206){
                                response.body().close();
                                return new ReqResult(false);
                            }
                            ReqResult reqResult = new ReqResult(true);
                            reqResult.setTargetUri(response.uri());
                            reqResult.setM3u8File(
                                    uri.getPath().endsWith(".m3u8")
                                            ||uri.getPath().endsWith(".m3u")
                                            ||response.headers().allValues("content-type").stream()
                                            .anyMatch(str->
                                                    Objects.equals(str,"application/vnd.apple.mpegurl")||Objects.equals(str,"audio/mpegurl")||Objects.equals(str,"application/x-mpegurl"))
                            );

                            reqResult.setContentLength(response.headers().firstValue("content-length").map(Integer::valueOf).orElse(-1));
                            reqResult.setInputStream(response.body());

                            return reqResult;
                        } catch (IOException e) {
                            throw new RuntimeException(e.getMessage(),e);
                        }
                    });

    }


    private static class DownloadProxySelector extends ProxySelector{
        private List<Proxy> proxies;

        public DownloadProxySelector(URI uri) {
            Objects.requireNonNull(uri);
            Objects.requireNonNull(uri.getScheme());
            Objects.requireNonNull(uri.getHost());
            if (uri.getPort()==-1){
                throw new IllegalArgumentException("port of the proxy address is undefined");
            }
            String scheme = uri.getScheme();
            Proxy.Type type;
            if (scheme.startsWith("http")){
                type= Proxy.Type.HTTP;
            }else if (scheme.startsWith("socks")){
                type= Proxy.Type.SOCKS;
            }else{
                throw new IllegalArgumentException("proxy address only supported starts with ['http','socks']");
            }
            ;
            proxies=List.of(new Proxy(type,new InetSocketAddress(uri.getHost(),uri.getPort())));
        }

        @Override
        public List<Proxy> select(URI uri) {
            return proxies;
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            System.err.println(ioe.getLocalizedMessage());
        }
    }
}

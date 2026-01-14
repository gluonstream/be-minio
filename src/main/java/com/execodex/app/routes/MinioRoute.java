package com.execodex.app.routes;

import com.execodex.app.handler.MinioHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class MinioRoute {

    private final MinioHandler minioHandler;

    public MinioRoute(MinioHandler minioHandler) {
        this.minioHandler = minioHandler;
    }

    @Bean
    public RouterFunction<ServerResponse> minioRouters() {

        return RouterFunctions.route()
                .GET("/minio", request -> ServerResponse.ok().bodyValue("Minio Service"))
                .POST("/minio/{bucket}", minioHandler::createBucket )
                .build();
    }
}

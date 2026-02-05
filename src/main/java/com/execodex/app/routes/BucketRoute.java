package com.execodex.app.routes;

import com.execodex.app.handler.BucketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class BucketRoute {

    private final BucketHandler bucketHandler;

    public BucketRoute(BucketHandler bucketHandler) {
        this.bucketHandler = bucketHandler;
    }

    @Bean
    public RouterFunction<ServerResponse> minioRouters() {

        return RouterFunctions.route()
                .nest(RequestPredicates.path("/api"), builder -> builder
                        .GET("/minio", request -> ServerResponse.ok().bodyValue("Minio Service"))
                        .POST("/minio/{bucket}", bucketHandler::createBucket)
                        .GET("/minio/{bucket}", bucketHandler::getAllFiles)
                        .POST("/minio/{bucket}/upload", bucketHandler::uploadFile)
                        .GET("/minio/{bucket}/download/{filename}", bucketHandler::downloadFile)
                        .GET("/minio/{bucket}/link2/{filename}", bucketHandler::getPresignedUrl)
                        .GET("/minio/{bucket}/link/{duration}/{filename}", bucketHandler::getPresignedUrl)
                        .DELETE("/minio/{bucket}/{filename}", bucketHandler::deleteFile)
                )
                .build();
    }
}

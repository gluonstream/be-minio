package com.execodex.app.handler;

import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.buffer.DataBuffer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class BucketHandler {

    private final S3AsyncClient s3AsyncClient;
    private final DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();

    public BucketHandler(S3AsyncClient s3AsyncClient) {
        this.s3AsyncClient = s3AsyncClient;
    }

    public Mono<ServerResponse> createBucket(ServerRequest serverRequest) {
        String bucket = serverRequest.pathVariable("bucket");

        CreateBucketRequest bucketRequest = CreateBucketRequest.builder().bucket(bucket).build();

        return Mono.fromFuture(s3AsyncClient.createBucket(bucketRequest))
                .thenReturn("Bucket created successfully")
                .onErrorResume(e -> e instanceof BucketAlreadyExistsException || e instanceof BucketAlreadyOwnedByYouException,
                        e -> Mono.just("Bucket already exists"))
                .flatMap(message -> ServerResponse.ok().bodyValue(message))
                .onErrorResume(e -> ServerResponse.status(500).bodyValue("Error: " + e.getMessage()))
                ;
    }

    public Mono<ServerResponse> uploadFile(ServerRequest serverRequest) {
        String bucket = serverRequest.pathVariable("bucket");
        List<String> tag = serverRequest.headers().header("X-tag");
        //we will put in the DB later

        return serverRequest.multipartData()
                .flatMapMany(parts -> Flux.fromIterable(parts.get("file")))
                .cast(FilePart.class)
                .flatMap(filePart -> {
                    return DataBufferUtils.join(filePart.content())
                            .flatMap(dataBuffer -> {
                                long contentLength = dataBuffer.readableByteCount();
                                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                        .bucket(bucket)
                                        .key(filePart.filename())
                                        .contentType(filePart.headers().getContentType().toString())
                                        .contentLength(contentLength)
                                        .build();

                                return Mono.fromFuture(s3AsyncClient.putObject(putObjectRequest,
                                                AsyncRequestBody.fromByteBuffer(dataBuffer.asByteBuffer())))
                                        .doFinally(signalType -> DataBufferUtils.release(dataBuffer))
                                        .thenReturn(filePart.filename());
                            });
                })
                .collectList()
                .flatMap(filenames -> ServerResponse.ok().bodyValue("Uploaded files: " + String.join(", ", filenames)))
                .onErrorResume(e -> ServerResponse.status(500).bodyValue("Error: " + e.getMessage()));
    }

    public Mono<ServerResponse> downloadFile(ServerRequest serverRequest) {
        String bucket = serverRequest.pathVariable("bucket");
        String filename = serverRequest.pathVariable("filename");

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(filename)
                .build();

        return Mono.fromFuture(s3AsyncClient.getObject(getObjectRequest, AsyncResponseTransformer.toPublisher()))
                .flatMap(responsePublisher -> {
                    GetObjectResponse getObjectResponse = responsePublisher.response();
                    Flux<DataBuffer> dataBufferFlux = Flux.from(responsePublisher)
                            .map(bufferFactory::wrap);

                    return ServerResponse.ok()
                            .contentType(MediaType.parseMediaType(getObjectResponse.contentType()))
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                            .contentLength(getObjectResponse.contentLength())
                            .body(dataBufferFlux, DataBuffer.class);
                })
                .onErrorResume(e -> ServerResponse.status(404).bodyValue("File not found: " + e.getMessage()));
    }

    // In the future, implement renameBucket method

}

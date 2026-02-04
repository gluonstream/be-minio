package com.execodex.app.handler;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class BucketHandler {

    private final S3AsyncClient s3AsyncClient;
    private final S3Presigner s3Presigner;
    private final DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();

    public BucketHandler(S3AsyncClient s3AsyncClient, S3Presigner s3Presigner) {
        this.s3AsyncClient = s3AsyncClient;
        this.s3Presigner = s3Presigner;
    }

    public Mono<ServerResponse> createBucket(ServerRequest serverRequest) {
        String bucket = serverRequest.pathVariable("bucket");
        var location = serverRequest.uri();

        CreateBucketRequest bucketRequest = CreateBucketRequest.builder().bucket(bucket).build();

        return Mono.fromFuture(s3AsyncClient.createBucket(bucketRequest))
                .thenReturn("Bucket created successfully")
                .onErrorResume(e -> e instanceof BucketAlreadyExistsException || e instanceof BucketAlreadyOwnedByYouException,
                        e -> Mono.just("Bucket already exists"))
                .flatMap(message -> {
                    if ("Bucket already exists".equals(message)) {
                        return ServerResponse.ok()
                                .header(HttpHeaders.LOCATION, location.toString())
                                .bodyValue(message);
                    }
                    return ServerResponse.created(location).bodyValue(location);
                })
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
                .flatMap(filePart -> DataBufferUtils.join(filePart.content())
                        .flatMap(dataBuffer -> {
                            String filename = filePart.filename();
                            long contentLength = dataBuffer.readableByteCount();

                            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                    .bucket(bucket)
                                    .key(filename)
                                    .contentLength(contentLength)
                                    .contentType(Optional.ofNullable(filePart.headers().getContentType())
                                            .map(MediaType::toString)
                                            .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                                    .build();

                            AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromByteBuffer(dataBuffer.asByteBuffer());

                            return Mono.fromFuture(s3AsyncClient.putObject(putObjectRequest, asyncRequestBody))
                                    .doFinally(signalType -> DataBufferUtils.release(dataBuffer))
                                    .thenReturn(filename);
                        }))
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

    public Mono<ServerResponse> deleteFile(ServerRequest serverRequest) {
        String bucket = serverRequest.pathVariable("bucket");
        String filename = serverRequest.pathVariable("filename");
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(filename)
                .build();
        return Mono.fromFuture(s3AsyncClient.deleteObject(deleteObjectRequest))
                .flatMap(responsePublisher -> ServerResponse.accepted().build())
                .onErrorResume(e -> ServerResponse.status(404).bodyValue("File not found: " + e.getMessage()));


    }

    public Mono<ServerResponse> getAllFiles(ServerRequest serverRequest) {
        String bucket = serverRequest.pathVariable("bucket");

        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .build();

        return Mono.fromFuture(s3AsyncClient.listObjectsV2(listRequest))
                .map(response -> response.contents().stream()
                        .map(S3Object::key)
                        .toList())
                .flatMap(keys -> ServerResponse.ok().bodyValue(keys))
                .onErrorResume(e -> ServerResponse.status(500).bodyValue("Error: " + e.getMessage()));
    }

    public Mono<ServerResponse> getPresignedUrl(ServerRequest serverRequest) {
        String bucket = serverRequest.pathVariable("bucket");
        String filename = serverRequest.pathVariable("filename");
        String duration;
        try {
            duration = serverRequest.pathVariable("duration");
        } catch (IllegalArgumentException e) {
            duration = "PT10M";
        }
        Duration durationValue = Duration.parse(duration);


        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(filename)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(durationValue)
                .getObjectRequest(getObjectRequest)
                .build();

        return Mono.fromCallable(() -> s3Presigner
                        .presignGetObject(presignRequest))
                .filter(PresignedRequest::isBrowserExecutable)
                .map(PresignedGetObjectRequest::url)
                .flatMap(url -> ServerResponse.ok().bodyValue(url.toString()))
                .onErrorResume(e -> ServerResponse.status(500).bodyValue("Error: " + e.getMessage()));
    }

    // In the future, implement renameBucket method

}

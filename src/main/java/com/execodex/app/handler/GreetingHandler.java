package com.execodex.app.handler;

import com.execodex.app.domain.Appointment;
import com.execodex.app.service.AppointmentService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Component
public class GreetingHandler {
    private final AppointmentService appointmentService;

    public GreetingHandler(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    public Mono<ServerResponse> handleGreetings(ServerRequest request) {
        return ServerResponse.ok().bodyValue("Hello from APP");
    }

    public Mono<ServerResponse> handleAppointment(ServerRequest serverRequest) {
        //
        Flux<Appointment> allAppointments = appointmentService
                .getAllAppointments()
                .doOnNext(System.out::println)
                .delayElements(Duration.ofMillis(400));
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(allAppointments, Appointment.class);
    }

    public Mono<ServerResponse> handleHello(ServerRequest serverRequest) {
        return serverRequest.principal()
                .cast(JwtAuthenticationToken.class)
                .flatMap(jwtToken -> {
                    return ServerResponse.ok().bodyValue(jwtToken);
                });
    }

    public Mono<ServerResponse> me(ServerRequest serverRequest) {
        return serverRequest.principal()
                .cast(JwtAuthenticationToken.class)
                .log()
                .flatMap(jwtToken -> {
                    String username = jwtToken.getToken().getClaimAsString("preferred_username");
                    return ServerResponse.ok().bodyValue(Map.of(
                            "username", username
                    ));
                });
    }
}

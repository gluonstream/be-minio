package com.execodex.app.routes;

import com.execodex.app.handler.GreetingHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class AppRouter {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/hello",
                    method = RequestMethod.GET,
                    beanClass = GreetingHandler.class,
                    beanMethod = "handleHello",
                    operation = @Operation(
                            operationId = "getHello",
                            summary = "Get greeting message",
                            description = "Returns a simple greeting message in plain text",
                            tags = {"Greeting"},
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Successful operation",
                                            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))
                                    )
                            }
                    )
            ),
            @RouterOperation(
                    path = "/appointment",
                    method = RequestMethod.GET,
                    beanClass = GreetingHandler.class,
                    beanMethod = "handleAppointment",
                    operation = @Operation(
                            operationId = "getAppointment",
                            summary = "Get appointment information",
                            description = "Returns appointment details",
                            tags = {"Appointment"},
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Successful operation",
                                            content = @Content(mediaType = "application/json")
                                    )
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> routerFunction(GreetingHandler greetingHandler) {
        return RouterFunctions.route()
                .add(route(GET("/hello").and(accept(MediaType.TEXT_PLAIN)), greetingHandler::handleHello))
                .add(route(GET("/appointment"), greetingHandler::handleAppointment))
                .build()
                ;

    }
}

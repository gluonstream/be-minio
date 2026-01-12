package com.execodex.app.routes;

import com.execodex.app.handler.GreetingHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class AppRouter {

    @Bean

    public RouterFunction<ServerResponse> routerFunction(GreetingHandler greetingHandler) {
        return RouterFunctions.route()
                .add(route(GET("/hello").and(accept(MediaType.TEXT_PLAIN)), greetingHandler::handleHello))
                .add(route(GET("/appointment"), greetingHandler::handleAppointment))
                .build()
                ;

    }
}

package ru.volunteerproject.apigateway.config;

import static ru.volunteerproject.apigateway.config.GatewayRouteSpec.AccessType.PROTECTED;
import static ru.volunteerproject.apigateway.config.GatewayRouteSpec.AccessType.PUBLIC;
import static ru.volunteerproject.apigateway.config.GatewayRouteSpec.ClientKeyType.IP;
import static ru.volunteerproject.apigateway.config.GatewayRouteSpec.ClientKeyType.USER;
import static ru.volunteerproject.apigateway.config.GatewayRouteSpec.TargetService.AUTH;
import static ru.volunteerproject.apigateway.config.GatewayRouteSpec.TargetService.HELP;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
public final class GatewayRouteCatalog {

    private final List<GatewayRouteSpec> routes;

    public GatewayRouteCatalog() {
        this(List.of(
                route("registerUser", "/api/v1/auth/register", HttpMethod.POST, AUTH, PUBLIC, IP),
                route("login", "/api/v1/auth/login", HttpMethod.POST, AUTH, PUBLIC, IP),
                route("refreshTokens", "/api/v1/auth/refresh", HttpMethod.POST, AUTH, PUBLIC, IP),
                route("logout", "/api/v1/auth/logout", HttpMethod.POST, AUTH, PUBLIC, IP),
                route("createTask", "/api/v1/help/tasks", HttpMethod.POST, HELP, PROTECTED, USER),
                route("listTasks", "/api/v1/help/tasks", HttpMethod.GET, HELP, PUBLIC, IP),
                route("getTask", "/api/v1/help/tasks/{taskId}", HttpMethod.GET, HELP, PUBLIC, IP),
                route("updateTask", "/api/v1/help/tasks/{taskId}", HttpMethod.PATCH, HELP, PROTECTED, USER),
                route("deleteTask", "/api/v1/help/tasks/{taskId}", HttpMethod.DELETE, HELP, PROTECTED, USER),
                route("createTaskResponse", "/api/v1/help/tasks/{taskId}/responses", HttpMethod.POST, HELP, PROTECTED, USER),
                route("listTaskResponses", "/api/v1/help/tasks/{taskId}/responses", HttpMethod.GET, HELP, PROTECTED, USER),
                route("getTaskResponse", "/api/v1/help/tasks/{taskId}/responses/{responseId}", HttpMethod.GET, HELP, PROTECTED, USER),
                route("decideTaskResponse", "/api/v1/help/tasks/{taskId}/responses/{responseId}", HttpMethod.PATCH, HELP, PROTECTED, USER),
                route("withdrawTaskResponse", "/api/v1/help/tasks/{taskId}/responses/{responseId}", HttpMethod.DELETE, HELP, PROTECTED, USER),
                route("listMyResponses", "/api/v1/help/me/responses", HttpMethod.GET, HELP, PROTECTED, USER),
                route("changeTaskStatus", "/api/v1/help/tasks/{taskId}/status", HttpMethod.PATCH, HELP, PROTECTED, USER)
        ));
    }

    GatewayRouteCatalog(List<GatewayRouteSpec> routes) {
        this.routes = List.copyOf(routes);
        validateUniqueIds(this.routes);
    }

    public List<GatewayRouteSpec> routes() {
        return routes;
    }

    public Set<String> routeIds() {
        return routes.stream()
                .map(GatewayRouteSpec::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static GatewayRouteSpec route(
            String id,
            String path,
            HttpMethod method,
            GatewayRouteSpec.TargetService targetService,
            GatewayRouteSpec.AccessType accessType,
            GatewayRouteSpec.ClientKeyType clientKeyType
    ) {
        return route(id, path, List.of(method), targetService, accessType, clientKeyType);
    }

    private static GatewayRouteSpec route(
            String id,
            String path,
            List<HttpMethod> methods,
            GatewayRouteSpec.TargetService targetService,
            GatewayRouteSpec.AccessType accessType,
            GatewayRouteSpec.ClientKeyType clientKeyType
    ) {
        return new GatewayRouteSpec(
                id, List.of(path), methods, targetService, accessType, clientKeyType);
    }

    private static void validateUniqueIds(List<GatewayRouteSpec> routes) {
        Set<String> routeIds = new HashSet<>();
        for (GatewayRouteSpec route : routes) {
            if (!routeIds.add(route.id())) {
                throw new IllegalArgumentException("Duplicate gateway route id: " + route.id());
            }
        }
    }
}

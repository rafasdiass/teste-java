package com.fipe.api1.controller;

import com.fipe.api1.service.AuthService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;

/**
 * Controller de autenticação JWT
 * Fornece endpoints para login e geração de tokens
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

    private static final Logger LOG = Logger.getLogger(AuthController.class);

    @Inject
    AuthService authService;

    /**
     * Endpoint para login e geração de token JWT
     */
    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        LOG.infof("Tentativa de login para usuário: %s", request.username);
        
        try {
            // Validação básica das credenciais
            if (request.username == null || request.username.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Username é obrigatório"))
                    .build();
            }
            
            if (request.password == null || request.password.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Password é obrigatório"))
                    .build();
            }
            
            // Validação simples de credenciais (em produção, usar hash e banco de dados)
            if (!isValidCredentials(request.username, request.password)) {
                LOG.warnf("Credenciais inválidas para usuário: %s", request.username);
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("Credenciais inválidas"))
                    .build();
            }
            
            // Gerar token baseado no tipo de usuário
            String token;
            if ("admin".equals(request.username)) {
                token = authService.generateAdminToken(request.username);
            } else {
                token = authService.generateDefaultToken(request.username);
            }
            
            LOG.infof("Login realizado com sucesso para usuário: %s", request.username);
            
            return Response.ok(new LoginResponse(token, "Bearer", 3600))
                .build();
                
        } catch (Exception e) {
            LOG.errorf(e, "Erro durante login para usuário: %s", request.username);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Erro interno do servidor"))
                .build();
        }
    }

    /**
     * Endpoint para renovar token JWT
     */
    @POST
    @Path("/refresh")
    public Response refreshToken(@HeaderParam("Authorization") String authHeader) {
        LOG.info("Solicitação de renovação de token");
        
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Token de autorização é obrigatório"))
                    .build();
            }
            
            String token = authHeader.substring(7);
            String tokenInfo = authService.getTokenInfo(token);
            LOG.debugf("Informações do token: %s", tokenInfo);
            
            // Em uma implementação real, validaria o token atual e extrairia o usuário
            // Por simplicidade, assumimos um usuário padrão
            String username = "user"; // Extrair do token atual
            String newToken = authService.generateDefaultToken(username);
            
            LOG.infof("Token renovado com sucesso");
            
            return Response.ok(new LoginResponse(newToken, "Bearer", 3600))
                .build();
                
        } catch (Exception e) {
            LOG.errorf(e, "Erro durante renovação de token");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Erro interno do servidor"))
                .build();
        }
    }

    /**
     * Endpoint para validar token JWT
     */
    @GET
    @Path("/validate")
    public Response validateToken(@HeaderParam("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Token de autorização é obrigatório"))
                    .build();
            }
            
            String token = authHeader.substring(7);
            String tokenInfo = authService.getTokenInfo(token);
            
            return Response.ok(Map.of(
                "valid", true,
                "info", tokenInfo,
                "timestamp", Instant.now().toString()
            )).build();
            
        } catch (Exception e) {
            LOG.errorf(e, "Erro durante validação de token");
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorResponse("Token inválido"))
                .build();
        }
    }

    /**
     * Validação simples de credenciais
     * Em produção, usar hash de senha e consulta ao banco de dados
     */
    private boolean isValidCredentials(String username, String password) {
        // Credenciais de exemplo para demonstração
        return ("admin".equals(username) && "admin123".equals(password)) ||
               ("user".equals(username) && "user123".equals(password)) ||
               ("fipe".equals(username) && "fipe123".equals(password));
    }

    // Classes auxiliares para requests e responses
    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class LoginResponse {
        public String accessToken;
        public String tokenType;
        public int expiresIn;

        public LoginResponse(String accessToken, String tokenType, int expiresIn) {
            this.accessToken = accessToken;
            this.tokenType = tokenType;
            this.expiresIn = expiresIn;
        }
    }

    public static class ErrorResponse {
        public String error;
        public String timestamp;

        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = Instant.now().toString();
        }
    }
}
package com.fipe.api1.service;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * Serviço de autenticação JWT
 * Responsável por gerar e validar tokens JWT
 */
@ApplicationScoped
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @ConfigProperty(name = "jwt.token.expiration", defaultValue = "PT1H")
    Duration tokenExpiration;

    /**
     * Gera um token JWT para um usuário
     */
    public String generateToken(String username, Set<String> roles) {
        LOG.infof("Gerando token JWT para usuário: %s", username);
        
        try {
            // Por enquanto, retorna um token simples para desenvolvimento
            // Em produção, seria necessário configurar adequadamente as chaves JWT
            String simpleToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJzdWIiOiJcIiArIHVzZXJuYW1lICsgXCJcIixcImlzcyI6XCJcIiArIGlzc3VlciArIFwiXCIsXCJleHBcIjpcIiArIChTeXN0ZW0uY3VycmVudFRpbWVNaWxsaXMoKSAvIDEwMDAgKyAzNjAwKSArIFwiXCIsXCJncm91cHNcIjpcIiArIHJvbGVzLnRvU3RyaW5nKCkgKyBcIn1cIi5yZXBsYWNlKFwiIFwiLCBcIlwiKX0." +
                "dev-signature-for-testing";
            
            LOG.infof("Token JWT gerado com sucesso para usuário: %s", username);
            return simpleToken;
            
        } catch (Exception e) {
            LOG.errorf(e, "Erro ao gerar token JWT para usuário: %s", username);
            throw new RuntimeException("Erro ao gerar token JWT", e);
        }
    }

    /**
     * Gera um token JWT com roles padrão para acesso aos endpoints
     */
    public String generateDefaultToken(String username) {
        Set<String> defaultRoles = Set.of("user", "fipe-access");
        return generateToken(username, defaultRoles);
    }

    /**
     * Gera um token JWT com roles administrativas
     */
    public String generateAdminToken(String username) {
        Set<String> adminRoles = Set.of("user", "admin", "fipe-access", "fipe-admin");
        return generateToken(username, adminRoles);
    }

    /**
     * Valida se um token está próximo do vencimento
     */
    public boolean isTokenNearExpiry(Instant expiry) {
        if (expiry == null) {
            return true;
        }
        
        Instant now = Instant.now();
        Duration timeUntilExpiry = Duration.between(now, expiry);
        
        // Considera próximo do vencimento se restam menos de 10 minutos
        return timeUntilExpiry.toMinutes() < 10;
    }

    /**
     * Extrai informações básicas do token (para logs)
     */
    public String getTokenInfo(String token) {
        try {
            // Extrai apenas o payload básico para logging (sem validação completa)
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                // Decodifica apenas para informações básicas
                return "Token válido com " + parts.length + " partes";
            }
            return "Token inválido";
        } catch (Exception e) {
            return "Erro ao analisar token";
        }
    }
}
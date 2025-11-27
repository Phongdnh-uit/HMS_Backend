package com.hms.auth_service.configs;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.util.Base64;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

@Configuration
public class JwtConfig {
    public static final SignatureAlgorithm JWT_ALGORITHM = SignatureAlgorithm.RS256;

    @Value("${jwt.private-key}")
    private String privateKeyPem;

    @Value("${jwt.public-key}")
    private String publicKeyPem;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    @PostConstruct
    public void initKeys() throws Exception {
        this.privateKey = loadPrivateKey(privateKeyPem);
        this.publicKey = loadPublicKey(publicKeyPem);
    }

    @Bean
    JwtEncoder jwtEncoder() {
        RSAKey rsaKey = new RSAKey.Builder(this.publicKey).privateKey(this.privateKey).build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(jwkSet));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(this.publicKey).signatureAlgorithm(JWT_ALGORITHM).build();
    }

    private RSAPrivateKey loadPrivateKey(String pem) throws Exception {
        String privateKeyString =
                pem.replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s", "");

        byte[] keyBytes = Base64.from(privateKeyString).decode();

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(spec);
    }

    private RSAPublicKey loadPublicKey(String pem) throws Exception {
        String publicKeyString =
                pem.replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s", "");

        byte[] keyBytes = Base64.from(publicKeyString).decode();

        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }
}

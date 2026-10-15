package com.zoonza.playground.verification.internal.infrastructure.persistence;

import com.zoonza.playground.shared.vo.Email;
import com.zoonza.playground.verification.internal.application.required.VerificationStore;
import com.zoonza.playground.verification.internal.domain.Verification;
import com.zoonza.playground.verification.internal.domain.VerificationPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisVerificationStore implements VerificationStore {
    private static final String KEY_PREFIX = "verification:";

    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(Verification verification) {
        String key = getKey(verification.getPurpose(), verification.getEmail());
        String value = objectMapper.writeValueAsString(verification);
        Duration timeToLive = Duration.between(clock.instant(), verification.getExpiresAt());

        redisTemplate.opsForValue().set(key, value, timeToLive);
    }

    @Override
    public Optional<Verification> findBy(Email email, VerificationPurpose purpose) {
        String key = getKey(purpose, email);

        return Optional.ofNullable(redisTemplate.opsForValue().get(key))
                .map(value -> objectMapper.readValue(value, Verification.class));
    }

    private String getKey(VerificationPurpose purpose, Email email) {
        return "%s:%s:%s".formatted(
                KEY_PREFIX,
                purpose.name().toLowerCase(),
                email.value()
        );
    }
}

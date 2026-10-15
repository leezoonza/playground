package com.zoonza.playground.verification.internal.infrastructure.persistence;

import com.zoonza.playground.RedisContainerConfiguration;
import com.zoonza.playground.shared.fixture.EmailFixture;
import com.zoonza.playground.shared.vo.Email;
import com.zoonza.playground.support.MutableClock;
import com.zoonza.playground.verification.fixture.VerificationCodeFixture;
import com.zoonza.playground.verification.fixture.VerificationFixture;
import com.zoonza.playground.verification.internal.domain.Verification;
import com.zoonza.playground.verification.internal.domain.VerificationPurpose;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.zoonza.playground.verification.fixture.VerificationFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import({
        RedisContainerConfiguration.class,
        RedisVerificationStore.class,
        RedisVerificationStoreTest.ClockConfiguration.class
})
class RedisVerificationStoreTest {

    @Autowired
    private RedisVerificationStore verificationStore;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void tearDown() {
        redisTemplate.execute((RedisConnection connection) -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    @Test
    @DisplayName("저장한 인증 정보를 그대로 조회한다")
    void saveAndFind() {
        Verification verification = VerificationFixture.requested();

        verificationStore.save(verification);

        assertThat(findSignup(EmailFixture.email()))
                .get()
                .usingRecursiveComparison()
                .isEqualTo(verification);
    }

    @Test
    @DisplayName("인증 완료 상태와 실패 횟수도 그대로 조회한다")
    void saveAndFindChangedState() {
        Verification verified = VerificationFixture.verified();
        Verification failed = VerificationFixture.failed(3);

        verificationStore.save(verified);
        Optional<Verification> foundVerified = findSignup(EmailFixture.email());
        verificationStore.save(failed);
        Optional<Verification> foundFailed = findSignup(EmailFixture.email());

        assertThat(foundVerified).get().usingRecursiveComparison().isEqualTo(verified);
        assertThat(foundFailed).get().usingRecursiveComparison().isEqualTo(failed);
    }

    @Test
    @DisplayName("같은 이메일과 인증 유형으로 다시 저장하면 덮어쓴다")
    void overwrite() {
        verificationStore.save(VerificationFixture.requested());
        Verification failed = VerificationFixture.failed(1);

        verificationStore.save(failed);

        assertThat(findSignup(EmailFixture.email()))
                .get()
                .extracting(Verification::getFailedAttempts)
                .isEqualTo(1);
        assertThat(allKeys()).hasSize(1);
    }

    @Test
    @DisplayName("이메일이 다르면 따로 저장된다")
    void separateByEmail() {
        Email otherEmail = new Email("other@example.com");
        Verification other = Verification.create(
                "other-id",
                otherEmail,
                VerificationPurpose.SIGNUP,
                VerificationCodeFixture.wrongCode(),
                REQUESTED_AT
        );

        verificationStore.save(VerificationFixture.requested());
        verificationStore.save(other);

        assertThat(findSignup(EmailFixture.email())).get()
                .extracting(Verification::getCode)
                .isEqualTo(VerificationCodeFixture.code());
        assertThat(findSignup(otherEmail)).get()
                .extracting(Verification::getCode)
                .isEqualTo(VerificationCodeFixture.wrongCode());
    }

    @Test
    @DisplayName("저장된 인증 정보가 없으면 빈 값을 반환한다")
    void notFound() {
        assertThat(findSignup(EmailFixture.email())).isEmpty();
    }

    @Test
    @DisplayName("만료 시각까지 남은 시간을 TTL로 설정한다")
    void timeToLive() {
        verificationStore.save(VerificationFixture.requested());

        assertThat(ttlSecondsOfOnlyKey()).isBetween(CODE_VALIDITY.toSeconds() - 5, CODE_VALIDITY.toSeconds());
    }

    @Test
    @DisplayName("인증이 완료되면 늘어난 만료 시각으로 TTL을 설정한다")
    void timeToLiveAfterVerified() {
        verificationStore.save(VerificationFixture.verified());

        assertThat(ttlSecondsOfOnlyKey()).isBetween(VERIFIED_VALIDITY.toSeconds() - 5, VERIFIED_VALIDITY.toSeconds());
    }

    private Optional<Verification> findSignup(Email email) {
        return verificationStore.findBy(email, VerificationPurpose.SIGNUP);
    }

    private Set<String> allKeys() {
        return redisTemplate.keys("*");
    }

    private Long ttlSecondsOfOnlyKey() {
        String key = allKeys().iterator().next();

        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfiguration {

        @Bean
        MutableClock clock() {
            return new MutableClock(REQUESTED_AT);
        }
    }
}

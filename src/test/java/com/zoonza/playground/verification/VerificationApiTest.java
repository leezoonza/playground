package com.zoonza.playground.verification;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.zoonza.playground.shared.fixture.EmailFixture;
import com.zoonza.playground.support.ApiTest;
import com.zoonza.playground.support.TestMailServer;
import com.zoonza.playground.verification.internal.domain.VerificationErrorCode;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.zoonza.playground.verification.fixture.VerificationFixture.MAX_ATTEMPTS;
import static com.zoonza.playground.verification.fixture.VerificationRequestFixture.sendRequest;
import static com.zoonza.playground.verification.fixture.VerificationRequestFixture.verifyRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@ApiTest
class VerificationApiTest {
    private static final String SEND_URI = "/api/verifications/signup";
    private static final String VERIFY_URI = "/api/verifications/signup/verify";
    private static final Pattern CODE_PATTERN = Pattern.compile("\\[(\\d{6})]");
    private static final long MAIL_TIMEOUT_MILLIS = 5_000;

    @RegisterExtension
    static GreenMailExtension mailServer = TestMailServer.create();

    @Autowired
    private RestTestClient client;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        redisTemplate.execute((RedisConnection connection) -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    @Test
    @DisplayName("인증번호를 메일로 받아 인증하면 성공하고, 같은 번호로 다시 인증할 수 없다")
    void sendAndVerify() throws Exception {
        sendCode();

        MimeMessage mail = awaitMail(1);
        assertThat(mail.getAllRecipients()).extracting(Object::toString).containsExactly(EmailFixture.EMAIL_VALUE);
        assertThat(mail.getFrom()).extracting(Object::toString).containsExactly(TestMailServer.USERNAME);
        assertThat(mail.getSubject()).isEqualTo("playground 이메일 인증코드입니다.");
        String code = extractCode(mail);

        verify(code)
                .expectStatus().isOk();
        verify(code)
                .expectStatus().isEqualTo(VerificationErrorCode.ALREADY_VERIFIED.getStatus())
                .expectBody().jsonPath("$.code").isEqualTo(VerificationErrorCode.ALREADY_VERIFIED.getCode());
    }

    @Test
    @DisplayName("다시 발송하면 마지막으로 받은 인증번호만 유효하다")
    void resend() throws Exception {
        sendCode();
        String oldCode = extractCode(awaitMail(1));
        sendCode();
        String newCode = extractCode(awaitMail(2));
        assumeFalse(oldCode.equals(newCode), "두 인증번호가 우연히 같으면 검증할 수 없다");

        verify(oldCode)
                .expectStatus().isEqualTo(VerificationErrorCode.CODE_MISMATCH.getStatus())
                .expectBody().jsonPath("$.code").isEqualTo(VerificationErrorCode.CODE_MISMATCH.getCode());
        verify(newCode)
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("인증번호를 최대 횟수만큼 틀리면 올바른 번호로도 인증할 수 없다")
    void tooManyAttempts() throws Exception {
        sendCode();
        String code = extractCode(awaitMail(1));
        String wrongCode = code.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            verify(wrongCode)
                    .expectBody().jsonPath("$.code").isEqualTo(VerificationErrorCode.CODE_MISMATCH.getCode());
        }

        verify(code)
                .expectStatus().isEqualTo(VerificationErrorCode.TOO_MANY_ATTEMPTS.getStatus())
                .expectBody().jsonPath("$.code").isEqualTo(VerificationErrorCode.TOO_MANY_ATTEMPTS.getCode());
    }

    @Test
    @DisplayName("인증번호를 발송하지 않았으면 인증할 수 없다")
    void notRequested() {
        verify("123456")
                .expectStatus().isEqualTo(VerificationErrorCode.VERIFICATION_NOT_FOUND.getStatus())
                .expectBody().jsonPath("$.code").isEqualTo(VerificationErrorCode.VERIFICATION_NOT_FOUND.getCode());
    }

    private void sendCode() {
        post(SEND_URI, sendRequest())
                .expectStatus().isOk();
    }

    private RestTestClient.ResponseSpec verify(String code) {
        return post(VERIFY_URI, verifyRequest(EmailFixture.EMAIL_VALUE, code));
    }

    private RestTestClient.ResponseSpec post(String uri, String body) {
        return client.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange();
    }

    /**
     * 메일은 비동기로 발송되므로 count번째 메일이 도착할 때까지 기다린 뒤 가장 최근 메일을 반환한다.
     */
    private MimeMessage awaitMail(int count) {
        assertThat(mailServer.waitForIncomingEmail(MAIL_TIMEOUT_MILLIS, count))
                .as("%d번째 메일이 %dms 안에 도착해야 한다", count, MAIL_TIMEOUT_MILLIS)
                .isTrue();

        MimeMessage[] messages = mailServer.getReceivedMessages();

        return messages[count - 1];
    }

    private String extractCode(MimeMessage mail) throws Exception {
        Matcher matcher = CODE_PATTERN.matcher(mail.getContent().toString());
        assertThat(matcher.find()).as("메일 본문에 인증번호가 있어야 한다").isTrue();

        return matcher.group(1);
    }
}

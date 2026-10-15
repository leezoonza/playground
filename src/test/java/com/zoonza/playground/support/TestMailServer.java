package com.zoonza.playground.support;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;

/**
 * application-test.yml의 spring.mail 설정과 같은 포트·계정으로 뜨는 테스트용 SMTP 서버.
 */
public final class TestMailServer {
    public static final ServerSetup SMTP = ServerSetupTest.SMTP;
    public static final String USERNAME = "test@example.com";
    public static final String PASSWORD = "test-password";

    private TestMailServer() {
    }

    public static GreenMailExtension create() {
        return new GreenMailExtension(SMTP)
                .withConfiguration(GreenMailConfiguration.aConfig().withUser(USERNAME, PASSWORD));
    }
}

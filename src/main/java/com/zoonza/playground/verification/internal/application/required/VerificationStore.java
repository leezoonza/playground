package com.zoonza.playground.verification.internal.application.required;

import com.zoonza.playground.shared.vo.Email;
import com.zoonza.playground.verification.internal.domain.Verification;
import com.zoonza.playground.verification.internal.domain.VerificationPurpose;

import java.util.Optional;

public interface VerificationStore {
    void save(Verification verification);

    Optional<Verification> findBy(Email email, VerificationPurpose purpose);
}

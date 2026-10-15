package com.zoonza.playground.verification.fake;

import com.zoonza.playground.shared.vo.Email;
import com.zoonza.playground.verification.internal.application.required.VerificationStore;
import com.zoonza.playground.verification.internal.domain.Verification;
import com.zoonza.playground.verification.internal.domain.VerificationPurpose;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakeVerificationStore implements VerificationStore {
    private final Map<Key, Verification> store = new HashMap<>();
    private int saveCount;

    @Override
    public void save(Verification verification) {
        store.put(new Key(verification.getEmail(), verification.getPurpose()), verification);
        saveCount++;
    }

    @Override
    public Optional<Verification> findBy(Email email, VerificationPurpose purpose) {
        return Optional.ofNullable(store.get(new Key(email, purpose)));
    }

    public int saveCount() {
        return saveCount;
    }

    private record Key(Email email, VerificationPurpose purpose) {
    }
}

package org.bgjug.jprime.core.repository;

import java.util.List;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bgjug.jprime.core.model.ResetPasswordToken;
import org.bgjug.jprime.core.model.User;

@ApplicationScoped
public class ResetPasswordTokenRepository implements PanacheRepository<ResetPasswordToken> {

    public Optional<ResetPasswordToken> findByTokenId(String tokenIdHash) {
        return find("tokenId", tokenIdHash).firstResultOptional();
    }

    public List<ResetPasswordToken> findAllByOwner(User owner) {
        return list("owner", owner);
    }
}

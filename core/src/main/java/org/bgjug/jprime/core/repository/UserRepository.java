package org.bgjug.jprime.core.repository;

import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bgjug.jprime.core.model.User;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    /** Any user (plain user, speaker or sponsor) with this e-mail. */
    public Optional<User> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    /** Turns a plain user into a speaker - same trick as the legacy app (single table inheritance). */
    public void convertToSpeaker(long userId) {
        getEntityManager().createNativeQuery("update User set DTYPE = 'Speaker' where id = :id")
            .setParameter("id", userId)
            .executeUpdate();
    }
}

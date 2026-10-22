package org.bgjug.jprime.core.repository;

import java.util.List;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.Speaker;

@ApplicationScoped
public class SpeakerRepository implements PanacheRepository<Speaker> {

    public Optional<Speaker> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    public List<Speaker> findFeatured(Branch branch) {
        return list("select distinct s from Speaker s left join s.submissions sub left join s.coSpeakerSubmissions co "
            + "where (sub.featured = true and sub.branch = ?1) or (co.featured = true and co.branch = ?1)", branch);
    }

    public List<Speaker> findConfirmed(Branch branch) {
        return list("select distinct s from Speaker s left join s.submissions sub left join s.coSpeakerSubmissions co "
            + "where ((sub.status = org.bgjug.jprime.core.model.SubmissionStatus.CONFIRMED or sub.featured = true) "
            + "and sub.branch = ?1) "
            + "or ((co.status = org.bgjug.jprime.core.model.SubmissionStatus.CONFIRMED or co.featured = true) "
            + "and co.branch = ?1) order by s.firstName, s.lastName", branch);
    }

    public boolean isConfirmed(Long speakerId, Branch branch) {
        Long count = getEntityManager().createQuery("select count(s) from Speaker s where s.id = :id and ("
                + "exists (select 1 from Submission sub where sub.speaker = s and sub.branch = :branch "
                + "and sub.status = org.bgjug.jprime.core.model.SubmissionStatus.CONFIRMED) or "
                + "exists (select 1 from Submission co where co.coSpeaker = s and co.branch = :branch "
                + "and co.status = org.bgjug.jprime.core.model.SubmissionStatus.CONFIRMED))", Long.class)
            .setParameter("id", speakerId)
            .setParameter("branch", branch)
            .getSingleResult();
        return count != null && count > 0;
    }

    public PanacheQuery<Speaker> byBranch(Branch branch) {
        return find("select distinct s from Speaker s left join s.submissions sub left join s.coSpeakerSubmissions co "
            + "where sub.branch = ?1 or co.branch = ?1 order by s.id desc", branch);
    }

    public PanacheQuery<Speaker> all() {
        return find("from Speaker s order by s.id desc");
    }
}

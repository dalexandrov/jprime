package org.bgjug.jprime.core.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.Submission;
import org.bgjug.jprime.core.model.SubmissionStatus;

@ApplicationScoped
public class SubmissionRepository implements PanacheRepository<Submission> {

    public List<Submission> findByBranchAndStatus(Branch branch, SubmissionStatus status) {
        return list("branch = ?1 and status = ?2", branch, status);
    }

    public PanacheQuery<Submission> byBranchAndStatus(Branch branch, SubmissionStatus status) {
        return find("branch = ?1 and status = ?2", Sort.descending("id"), branch, status);
    }

    public PanacheQuery<Submission> byBranch(Branch branch) {
        return find("branch", Sort.descending("id"), branch);
    }

    public PanacheQuery<Submission> everything() {
        return findAll(Sort.descending("id"));
    }

    public Map<SubmissionStatus, Long> countByStatus(Branch branch) {
        List<Object[]> rows = getEntityManager()
            .createQuery("select s.status, count(s) from Submission s where s.branch = :branch "
                + "group by s.status order by s.status", Object[].class)
            .setParameter("branch", branch)
            .getResultList();
        Map<SubmissionStatus, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put((SubmissionStatus) row[0], (Long) row[1]);
        }
        return result;
    }
}

package org.bgjug.jprime.core.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.StringUtils;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.Visitor;

@ApplicationScoped
public class VisitorRepository implements PanacheRepository<Visitor> {

    public List<Visitor> newestFirst(Branch branch) {
        return list("select v from Visitor v join fetch v.registrant r where r.branch = ?1 "
            + "order by v.createdDate desc, v.id desc", branch);
    }

    public List<Visitor> withTicket(Branch branch) {
        return list("select v from Visitor v join v.registrant r where r.branch = ?1 and v.ticket is not null",
            branch);
    }

    public Optional<Visitor> findByTicket(String ticket) {
        return find("ticket", ticket).firstResultOptional();
    }

    public List<Visitor> findByName(Branch branch, String name) {
        return list("select v from Visitor v join v.registrant r where r.branch = ?1 and lower(v.name) = ?2", branch,
            name.toLowerCase());
    }

    public List<Visitor> findByNameAndCompany(Branch branch, String name, String company) {
        return list("select v from Visitor v join v.registrant r where r.branch = ?1 and lower(v.name) = ?2 "
            + "and lower(v.company) = ?3", branch, name.toLowerCase(), company.toLowerCase());
    }

    /** Free search used by the registration (check-in) mobile application. */
    public List<Visitor> search(Branch branch, String email, String firstName, String lastName, String company) {
        StringBuilder query = new StringBuilder(
            "select v from Visitor v join v.registrant r where v.ticket is not null and r.branch = :branch");
        Map<String, Object> params = new HashMap<>();
        params.put("branch", branch);
        if (StringUtils.isNotBlank(email)) {
            query.append(" and lower(v.email) like :email");
            params.put("email", '%' + email.toLowerCase() + '%');
        }
        String name = "";
        if (StringUtils.isNotBlank(firstName)) {
            name += firstName + '%';
        }
        if (StringUtils.isNotBlank(lastName)) {
            name += name.isEmpty() ? "% " + lastName + '%' : ' ' + lastName + '%';
        }
        if (!name.isEmpty()) {
            query.append(" and lower(v.name) like :name");
            params.put("name", name.toLowerCase());
        }
        if (StringUtils.isNotBlank(company)) {
            query.append(" and (lower(v.company) like :company or lower(r.name) like :company)");
            params.put("company", '%' + company.toLowerCase() + '%');
        }
        return find(query.toString(), params).list();
    }

    /** Visitors of the branch that have paid or are sponsored and still have no ticket. */
    public List<Visitor> pendingTickets(Branch branch) {
        List<Visitor> result = new ArrayList<>();
        for (Visitor v : list("select v from Visitor v join fetch v.registrant r where r.branch = ?1 and "
            + "(v.ticket is null or v.ticket = '') and (v.status is null or v.status <> org.bgjug.jprime.core.model.VisitorStatus.REQUESTING)",
            branch)) {
            if (StringUtils.isNotBlank(v.anyEmail())) {
                result.add(v);
            }
        }
        return result;
    }
}

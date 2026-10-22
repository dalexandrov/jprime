package org.bgjug.jprime.core.service;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.Visitor;
import org.bgjug.jprime.core.model.VisitorJPro;
import org.bgjug.jprime.core.model.VisitorStatus;
import org.bgjug.jprime.core.repository.VisitorJProRepository;
import org.bgjug.jprime.core.repository.VisitorRepository;

/**
 * "I am here!" registrations for the raffle, and the raffle candidate lists.
 */
@ApplicationScoped
public class PresenceService {

    @Inject
    VisitorRepository visitorRepository;

    @Inject
    VisitorJProRepository visitorJProRepository;

    @Inject
    BranchService branchService;

    public record RaffleEntry(String name, String company) {
    }

    /** By badge id, or by name (+ company). Returns true when at least one visitor was found. */
    @Transactional
    public boolean markPresent(Long id, String name, String company) {
        Branch branch = branchService.getCurrentBranch();
        List<Visitor> matched;
        if (id != null) {
            matched = visitorRepository.findByIdOptional(id).stream().toList();
        } else if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(company)) {
            matched = visitorRepository.findByNameAndCompany(branch, name.trim(), company.trim());
        } else if (StringUtils.isNotBlank(name)) {
            matched = visitorRepository.findByName(branch, name.trim());
        } else {
            matched = List.of();
        }
        matched.forEach(v -> v.setPresent(true));
        return !matched.isEmpty();
    }

    @Transactional
    public boolean markJProPresent(String name, String email) {
        if (StringUtils.isBlank(name)) {
            return false;
        }
        List<VisitorJPro> matched = StringUtils.isNotBlank(email) ?
                                    visitorJProRepository.findByNameAndEmail(name.trim(), email.trim()) :
                                    visitorJProRepository.findByName(name.trim());
        matched.forEach(v -> v.setPresent(true));
        return !matched.isEmpty();
    }

    /** Present visitors that paid or are sponsored. */
    public List<RaffleEntry> raffleCandidates() {
        return visitorRepository.newestFirst(branchService.getCurrentBranch())
            .stream()
            .filter(v -> v.isPresent() && (v.getStatus() == VisitorStatus.PAYED || v.getStatus() == VisitorStatus.SPONSORED))
            .map(v -> new RaffleEntry(v.getName(), v.getCompany()))
            .toList();
    }

    public List<RaffleEntry> jProRaffleCandidates() {
        return visitorJProRepository.newestFirst()
            .stream()
            .filter(VisitorJPro::isPresent)
            .map(v -> new RaffleEntry(v.getName(), maskEmail(v.getEmail())))
            .toList();
    }

    /** john@example.com -> john@*******.com */
    public static String maskEmail(String email) {
        if (email == null) {
            return "";
        }
        int at = email.indexOf('@');
        int lastDot = email.lastIndexOf('.');
        if (at < 0 || lastDot <= at) {
            return email;
        }
        return email.substring(0, at + 1) + "*".repeat(lastDot - at - 1) + email.substring(lastDot);
    }
}

package org.bgjug.jprime.core.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.JPrimeException;
import org.bgjug.jprime.core.model.TicketPrice;
import org.bgjug.jprime.core.model.TicketType;
import org.bgjug.jprime.core.repository.BranchRepository;
import org.bgjug.jprime.core.repository.TicketPriceRepository;

/**
 * Conference editions ("branches") and their ticket prices.
 */
@ApplicationScoped
public class BranchService {

    public static final String CURRENT_BRANCH_CACHE = "current-branch";
    public static final String TICKET_PRICES_CACHE = "ticket-prices";
    private static final Duration MIN_DURATION = Duration.ofHours(4);

    @Inject
    BranchRepository branchRepository;

    @Inject
    TicketPriceRepository ticketPriceRepository;

    /**
     * The current edition. Cached (see {@code quarkus.cache.caffeine.current-branch.*}) - the returned entity is
     * detached, use {@link #currentBranchReference()} when you need to link it to a new entity.
     */
    @CacheResult(cacheName = CURRENT_BRANCH_CACHE)
    public Branch getCurrentBranch() {
        return branchRepository.findCurrent()
            .orElseThrow(() -> new JPrimeException("No current branch configured. Mark one branch as current."));
    }

    /** Managed reference to the current branch (no select). */
    public Branch currentBranchReference() {
        return branchRepository.getEntityManager().getReference(Branch.class, getCurrentBranch().getLabel());
    }

    public Branch reference(Branch branch) {
        return branch == null ? null : branchRepository.getEntityManager().getReference(Branch.class, branch.getLabel());
    }

    public Optional<Branch> getNextBranch() {
        return branchRepository.findByYear(getCurrentBranch().getYear() + 1);
    }

    public List<Branch> allBranches() {
        return branchRepository.allByYear();
    }

    public Optional<Branch> findByLabel(String label) {
        return label == null ? Optional.empty() : branchRepository.findByIdOptional(label);
    }

    public Branch findByYear(int year) {
        return branchRepository.findByYear(year)
            .orElseThrow(() -> new IllegalArgumentException("Invalid branch year: " + year));
    }

    @Transactional
    @CacheInvalidateAll(cacheName = CURRENT_BRANCH_CACHE)
    public void setAsCurrent(int year) {
        Branch newCurrent = findByYear(year);
        branchRepository.findCurrent().ifPresent(current -> current.setCurrentBranch(false));
        branchRepository.flush();
        newCurrent.setCurrentBranch(true);
    }

    /**
     * Creates or updates a branch together with its ticket prices.
     */
    @Transactional
    @CacheInvalidateAll(cacheName = CURRENT_BRANCH_CACHE)
    @CacheInvalidateAll(cacheName = TICKET_PRICES_CACHE)
    public Branch saveBranch(Branch data, Map<TicketType, java.math.BigDecimal> prices) {
        validate(data);
        Branch branch = branchRepository.findByYear(data.getYear()).orElseGet(() -> {
            Branch b = new Branch(data.getYear());
            branchRepository.persist(b);
            return b;
        });
        branch.setStartDate(data.getStartDate());
        branch.setDuration(data.getDuration());
        branch.setCfpOpenDate(data.getCfpOpenDate());
        branch.setCfpCloseDate(data.getCfpCloseDate());
        branch.setSoldOut(data.isSoldOut());
        branch.setAgendaPublished(data.isAgendaPublished());
        branch.setSoldOutPackages(data.getSoldOutPackages());

        for (Map.Entry<TicketType, java.math.BigDecimal> entry : prices.entrySet()) {
            TicketPrice price = ticketPriceRepository.findByBranchAndType(branch, entry.getKey()).orElseGet(() -> {
                TicketPrice p = new TicketPrice(entry.getValue(), entry.getKey(), branch);
                ticketPriceRepository.persist(p);
                return p;
            });
            price.setPrice(entry.getValue());
            if (entry.getKey() == TicketType.EARLY_BIRD) {
                price.setValidFrom(branch.getCfpOpenDate());
                price.setValidUntil(branch.getCfpCloseDate());
            }
        }
        return branch;
    }

    private static void validate(Branch branch) {
        if (branch.getStartDate() == null || branch.getYear() != branch.getStartDate().getYear()) {
            throw new IllegalArgumentException("The start date must be in the branch year");
        }
        if (branch.getDuration() == null || branch.getDuration().compareTo(MIN_DURATION) < 0) {
            throw new IllegalArgumentException("The duration must be at least 4 hours");
        }
        if (branch.getCfpOpenDate() != null && branch.getStartDate().isBefore(branch.getCfpOpenDate())) {
            throw new IllegalArgumentException("CFP must open before the conference starts");
        }
        if (branch.getCfpCloseDate() != null && branch.getStartDate().isBefore(branch.getCfpCloseDate())) {
            throw new IllegalArgumentException("CFP must close before the conference starts");
        }
    }

    @Transactional
    @CacheInvalidateAll(cacheName = CURRENT_BRANCH_CACHE)
    @CacheInvalidateAll(cacheName = TICKET_PRICES_CACHE)
    public void deleteBranch(int year) {
        Branch branch = findByYear(year);
        ticketPriceRepository.deleteByBranch(branch);
        branchRepository.delete(branch);
    }

    /** Ticket prices of the branch, keyed by type. */
    @CacheResult(cacheName = TICKET_PRICES_CACHE)
    public Map<TicketType, TicketPrice> getTicketPrices(String branchLabel) {
        Optional<Branch> branch = findByLabel(branchLabel);
        if (branch.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<TicketType, TicketPrice> result = new EnumMap<>(TicketType.class);
        ticketPriceRepository.findByBranch(branch.get()).forEach(p -> result.put(p.getTicketType(), p));
        return result;
    }

    public Map<TicketType, TicketPrice> getTicketPrices(Branch branch) {
        return getTicketPrices(branch.getLabel());
    }

    /** Early bird price until the CFP closes, regular afterwards. */
    public TicketPrice getTicketPrice(Branch branch, LocalDateTime at) {
        Map<TicketType, TicketPrice> prices = getTicketPrices(branch);
        LocalDateTime when = at != null ? at : LocalDateTime.now();
        if (branch.getCfpCloseDate() != null && branch.getCfpCloseDate().isAfter(when)) {
            return prices.getOrDefault(TicketType.EARLY_BIRD, prices.get(TicketType.REGULAR));
        }
        return prices.get(TicketType.REGULAR);
    }
}

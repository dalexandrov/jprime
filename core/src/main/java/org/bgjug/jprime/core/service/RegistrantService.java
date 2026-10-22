package org.bgjug.jprime.core.service;

import java.util.List;
import java.util.Optional;

import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.PaymentType;
import org.bgjug.jprime.core.model.Registrant;
import org.bgjug.jprime.core.model.Visitor;
import org.bgjug.jprime.core.model.VisitorStatus;
import org.bgjug.jprime.core.repository.RegistrantRepository;
import org.bgjug.jprime.core.repository.VisitorRepository;

@ApplicationScoped
public class RegistrantService {

    @Inject
    RegistrantRepository registrantRepository;

    @Inject
    VisitorRepository visitorRepository;

    @Inject
    NumberGeneratorService numberGenerator;

    public Optional<Registrant> findById(long id) {
        return registrantRepository.findByIdOptional(id);
    }

    public Optional<Registrant> findByEpayInvoiceNumber(long number) {
        return registrantRepository.findByEpayInvoiceNumber(number);
    }

    public PageResult<Registrant> findByBranch(Branch branch, int page, int size) {
        return PageResult.of(registrantRepository.byBranch(branch), Page.of(page, size));
    }

    public List<Registrant> listByBranch(Branch branch) {
        return registrantRepository.listByBranch(branch);
    }

    /**
     * Persists a <b>new</b> registrant and assigns the invoice numbers (same rules as the legacy app):
     * bank transfer gets a proforma number, ePay gets an ePay number (and a real one once paid).
     */
    @Transactional
    public Registrant register(Registrant registrant) {
        if (registrant.getPaymentType() == PaymentType.BANK_TRANSFER) {
            registrant.setProformaInvoiceNumber(numberGenerator.nextProformaInvoiceNumber());
        } else {
            if (registrant.getEpayInvoiceNumber() == 0) {
                registrant.setEpayInvoiceNumber(numberGenerator.nextEpayInvoiceNumber());
            }
            if (registrant.getEpayResponse() != null && registrant.getRealInvoiceNumber() == 0) {
                registrant.setRealInvoiceNumber(numberGenerator.nextRealInvoiceNumber());
            }
        }
        registrant.getVisitors().forEach(v -> v.setRegistrant(registrant));
        registrantRepository.persist(registrant);
        return registrant;
    }

    /** Plain save (admin edit) - no invoice number magic. */
    @Transactional
    public Registrant save(Registrant registrant) {
        if (registrant.getId() == null) {
            registrantRepository.persist(registrant);
            return registrant;
        }
        return registrantRepository.getEntityManager().merge(registrant);
    }

    @Transactional
    public long generateRealInvoiceNumber(Registrant registrant) {
        long number = numberGenerator.nextRealInvoiceNumber();
        registrant.setRealInvoiceNumber(number);
        return number;
    }

    @Transactional
    public void markPaid(Registrant registrant) {
        registrant.getVisitors().forEach(visitor -> visitor.setStatus(VisitorStatus.PAYED));
    }

    @Transactional
    public void delete(long id) {
        registrantRepository.findByIdOptional(id).ifPresent(registrantRepository::delete);
    }

    @Transactional
    public Visitor addVisitor(long registrantId, Visitor visitor) {
        Registrant registrant = registrantRepository.findByIdOptional(registrantId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown registrant " + registrantId));
        registrant.addVisitor(visitor);
        visitorRepository.persist(visitor);
        return visitor;
    }

    /** Deletes a visitor, and its registrant when it was an "individual" registrant left without visitors. */
    @Transactional
    public void deleteVisitor(long visitorId) {
        Visitor visitor = visitorRepository.findByIdOptional(visitorId).orElse(null);
        if (visitor == null) {
            return;
        }
        Registrant registrant = visitor.getRegistrant();
        registrant.getVisitors().remove(visitor);
        visitorRepository.delete(visitor);
        if (registrant.getVisitors().isEmpty() && java.util.Objects.equals(registrant.getEmail(), visitor.getEmail())
            && java.util.Objects.equals(registrant.getName(), visitor.getName())) {
            registrantRepository.delete(registrant);
        }
    }
}

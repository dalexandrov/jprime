package org.bgjug.jprime.core.service;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.bgjug.jprime.core.model.InvoiceNumberCounter;
import org.bgjug.jprime.core.model.JPrimeException;

/**
 * Hands out invoice numbers. The counters live in single-row tables; the row is locked with
 * {@code SELECT ... FOR UPDATE} so concurrent requests - even from the two different applications - never get the
 * same number (the legacy app only used a JVM {@code synchronized} block).
 */
@ApplicationScoped
public class NumberGeneratorService {

    @Inject
    EntityManager em;

    public long nextEpayInvoiceNumber() {
        return next(InvoiceNumberCounter.EPAY);
    }

    public long nextRealInvoiceNumber() {
        return next(InvoiceNumberCounter.REAL);
    }

    public long nextProformaInvoiceNumber() {
        return next(InvoiceNumberCounter.PROFORMA);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    @SuppressWarnings("unchecked")
    public long next(InvoiceNumberCounter counter) {
        String table = counter.table();
        List<Object[]> rows = em.createNativeQuery("SELECT id, counter FROM " + table + " ORDER BY id FOR UPDATE")
            .getResultList();
        if (rows.size() > 1) {
            throw new JPrimeException(table + " table has more than one row. Fix that!");
        }
        if (rows.isEmpty()) {
            long value = counter.initialValue();
            em.createNativeQuery("INSERT INTO " + table + " (id, counter) VALUES (1, :next)")
                .setParameter("next", value + 1)
                .executeUpdate();
            return value;
        }
        Object[] row = rows.get(0);
        long value = ((Number) row[1]).longValue();
        em.createNativeQuery("UPDATE " + table + " SET counter = :next WHERE id = :id")
            .setParameter("next", value + 1)
            .setParameter("id", ((Number) row[0]).intValue())
            .executeUpdate();
        return value;
    }
}

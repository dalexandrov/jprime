package org.bgjug.jprime.core.model;

import java.time.LocalDateTime;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * Fills the audit columns (created_by, created_date, last_modified_by, last_modified_date) exactly like the
 * legacy {@code EntityBaseListener} did.
 */
public class AuditListener {

    @PrePersist
    public void beforeSaveNew(Object entity) {
        if (entity instanceof Auditable auditable) {
            if (auditable.getCreatedDate() == null) {
                auditable.setCreatedDate(LocalDateTime.now());
            }
            auditable.setCreatedBy(currentUserName());
        }
    }

    @PreUpdate
    public void beforeUpdate(Object entity) {
        if (entity instanceof Auditable auditable) {
            auditable.setLastModifiedDate(LocalDateTime.now());
            auditable.setLastModifiedBy(currentUserName());
        }
    }

    static String currentUserName() {
        try {
            if (Arc.container() == null || !Arc.container().requestContext().isActive()) {
                return "SYSTEM";
            }
            try (InstanceHandle<SecurityIdentity> handle = Arc.container().instance(SecurityIdentity.class)) {
                if (handle.isAvailable()) {
                    SecurityIdentity identity = handle.get();
                    if (identity != null && !identity.isAnonymous() && identity.getPrincipal() != null) {
                        return identity.getPrincipal().getName();
                    }
                }
            }
        } catch (RuntimeException e) {
            // fall through - auditing must never break persistence
        }
        return "SYSTEM";
    }
}

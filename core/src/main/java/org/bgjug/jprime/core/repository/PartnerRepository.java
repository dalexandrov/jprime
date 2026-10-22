package org.bgjug.jprime.core.repository;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bgjug.jprime.core.model.Partner;
import org.bgjug.jprime.core.model.PartnerPackage;

@ApplicationScoped
public class PartnerRepository implements PanacheRepository<Partner> {

    public List<Partner> findActive(PartnerPackage partnerPackage) {
        if (partnerPackage == PartnerPackage.MEDIA) {
            // partners without a package are treated as media partners
            return list("active = true and (partnerPackage = ?1 or partnerPackage is null)", partnerPackage);
        }
        return list("active = true and partnerPackage = ?1", partnerPackage);
    }
}

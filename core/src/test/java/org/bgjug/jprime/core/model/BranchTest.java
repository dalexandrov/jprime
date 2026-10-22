package org.bgjug.jprime.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

class BranchTest {

    @Test
    void labelIsDerivedFromTheYear() {
        assertEquals("YEAR_2026", new Branch(2026).getLabel());
    }

    @Test
    void soldOutPackagesAreStoredAsJson() {
        Branch branch = new Branch(2026);
        branch.soldOutPackageSet(EnumSet.of(SponsorPackage.GOLD, SponsorPackage.SILVER));
        assertTrue(branch.getSoldOutPackages().contains("\"GOLD\""));
        assertTrue(branch.isPackageSoldOut(SponsorPackage.SILVER));
        assertFalse(branch.isPackageSoldOut(SponsorPackage.PLATINUM));

        // the format written by the legacy application
        branch.setSoldOutPackages("[\"PLATINUM\"]");
        assertEquals(EnumSet.of(SponsorPackage.PLATINUM), branch.soldOutPackageSet());
        branch.setSoldOutPackages("");
        assertTrue(branch.soldOutPackageSet().isEmpty());
    }

    @Test
    void registrantVatNumberGetsThePrefix() {
        Registrant registrant = new Registrant();
        registrant.setVatNumber("123456789");
        assertEquals("BG123456789", registrant.getVatNumber());
        registrant.setVatNumber("BG987");
        assertEquals("BG987", registrant.getVatNumber());
    }
}

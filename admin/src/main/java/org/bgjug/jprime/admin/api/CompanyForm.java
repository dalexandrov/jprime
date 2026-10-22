package org.bgjug.jprime.admin.api;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

/** Multipart form for sponsors and partners. */
public class CompanyForm {

    @RestForm
    public String companyName;
    @RestForm
    public String companyWebsite;
    @RestForm
    public String email;
    @RestForm
    public String description;
    @RestForm
    public String sponsorPackage;
    @RestForm
    public String partnerPackage;
    @RestForm
    public boolean active;
    /** Resize the logo to 180x64 (as the legacy "resize image" checkbox). */
    @RestForm
    public boolean resizeImage;
    @RestForm
    public FileUpload logo;
}

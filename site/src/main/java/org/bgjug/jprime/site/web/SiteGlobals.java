package org.bgjug.jprime.site.web;

import java.util.List;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.Tag;
import org.bgjug.jprime.core.repository.TagRepository;
import org.bgjug.jprime.core.service.BranchService;

/**
 * Data used by the common layout (menu, footer). Available in all templates as {@code inject:site}.
 */
@Named("site")
@RequestScoped
public class SiteGlobals {

    @Inject
    TagRepository tagRepository;

    @Inject
    BranchService branchService;

    @Inject
    SecurityIdentity identity;

    private List<Tag> tags;

    /** Tags shown in the main menu (year tags are archives and are skipped, like before). */
    public List<Tag> getMenuTags() {
        if (tags == null) {
            tags = tagRepository.allTags().stream().filter(t -> !t.isYearTag()).toList();
        }
        return tags;
    }

    public Branch getBranch() {
        return branchService.getCurrentBranch();
    }

    public int getYear() {
        return getBranch().getYear();
    }

    public boolean isLoggedIn() {
        return !identity.isAnonymous();
    }

    public String getUserName() {
        return identity.isAnonymous() ? "" : identity.getPrincipal().getName();
    }

    public int getCopyrightYear() {
        return java.time.Year.now().getValue();
    }
}

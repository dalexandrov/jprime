package org.bgjug.jprime.core.service;

import java.util.List;
import java.util.Optional;

import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.Speaker;
import org.bgjug.jprime.core.repository.SpeakerRepository;

/** Speaker queries with the per-branch flags already computed. */
@ApplicationScoped
public class SpeakerService {

    @Inject
    SpeakerRepository speakerRepository;

    @Inject
    BranchService branchService;

    public List<Speaker> featured() {
        Branch branch = branchService.getCurrentBranch();
        return speakerRepository.findFeatured(branch).stream().map(s -> s.updateFlags(branch)).toList();
    }

    public List<Speaker> confirmed() {
        Branch branch = branchService.getCurrentBranch();
        return speakerRepository.findConfirmed(branch).stream().map(s -> s.updateFlags(branch)).toList();
    }

    /** The speaker, only if confirmed or featured for the current branch (public profile page). */
    public Optional<Speaker> publicProfile(long id) {
        Branch branch = branchService.getCurrentBranch();
        return speakerRepository.findByIdOptional(id)
            .map(s -> s.updateFlags(branch))
            .filter(s -> s.isFeatured() || speakerRepository.isConfirmed(s.getId(), branch));
    }

    public PageResult<Speaker> page(Branch branch, int page, int size) {
        Branch flagsFor = branch != null ? branch : branchService.getCurrentBranch();
        PageResult<Speaker> result = PageResult.of(
            branch != null ? speakerRepository.byBranch(branch) : speakerRepository.all(), Page.of(page, size));
        result.content().forEach(s -> s.updateFlags(flagsFor));
        return result;
    }

    public List<Speaker> search(String firstName, String lastName, String email) {
        Branch branch = branchService.getCurrentBranch();
        StringBuilder query = new StringBuilder("from Speaker s where 1 = 1");
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        if (org.apache.commons.lang3.StringUtils.isNotBlank(email)) {
            query.append(" and lower(s.email) like :email");
            params.put("email", '%' + email.toLowerCase() + '%');
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(firstName)) {
            query.append(" and lower(s.firstName) like :firstName");
            params.put("firstName", '%' + firstName.toLowerCase() + '%');
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(lastName)) {
            query.append(" and lower(s.lastName) like :lastName");
            params.put("lastName", '%' + lastName.toLowerCase() + '%');
        }
        return speakerRepository.find(query.toString(), params).list().stream().map(s -> s.updateFlags(branch)).toList();
    }
}

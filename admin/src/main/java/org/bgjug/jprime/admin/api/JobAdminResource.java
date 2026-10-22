package org.bgjug.jprime.admin.api;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.bgjug.jprime.admin.dto.Dtos.JobDto;
import org.bgjug.jprime.core.service.BackgroundJobService;

/** Pending background jobs and jobs completed in the last 8 hours. */
@Path("/api/admin/jobs")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminSupport.ADMIN)
public class JobAdminResource {

    @Inject
    BackgroundJobService jobService;

    @GET
    public List<JobDto> list() {
        return jobService.recentJobs().stream().map(JobDto::of).toList();
    }
}

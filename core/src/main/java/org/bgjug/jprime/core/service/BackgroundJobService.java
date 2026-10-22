package org.bgjug.jprime.core.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bgjug.jprime.core.model.BackgroundJob;
import org.bgjug.jprime.core.repository.BackgroundJobRepository;
import org.jboss.logging.Logger;

/**
 * Runs long admin operations (e.g. mailing hundreds of tickets) in the background and records the progress in the
 * {@code BackgroundJob} table so that the admin UI can show it.
 */
@ApplicationScoped
public class BackgroundJobService {

    private static final Logger LOG = Logger.getLogger(BackgroundJobService.class);

    @Inject
    BackgroundJobRepository jobRepository;

    public String createJob(String description) {
        String id = UUID.randomUUID().toString();
        QuarkusTransaction.requiringNew().run(() -> {
            BackgroundJob job = new BackgroundJob(id, description.length() > 128 ? description.substring(0, 128) :
                                                      description);
            job.setStatus("Initiated");
            jobRepository.persist(job);
        });
        return id;
    }

    /**
     * Processes the items one by one on a worker thread. Each item runs in its own transaction.
     *
     * @param itemIds  identifiers of the items (use ids, not entities - the job runs outside the request)
     * @param executor returns true on success
     * @param describe used in the log for failed items
     */
    public <T> CompletableFuture<Void> runJob(String jobId, List<T> itemIds, Predicate<T> executor,
        Function<T, String> describe) {
        return CompletableFuture.runAsync(() -> {
            int complete = 0;
            int failed = 0;
            for (T item : itemIds) {
                boolean success;
                try {
                    success = QuarkusTransaction.requiringNew().call(() -> executor.test(item));
                } catch (RuntimeException e) {
                    LOG.errorf(e, "Background job %s failed for %s", jobId, describe.apply(item));
                    success = false;
                }
                complete++;
                if (!success) {
                    failed++;
                }
                String status = String.format("%d from %d has been completed.", complete, itemIds.size())
                    + (failed > 0 ? String.format(" There are also %d failed jobs!!!", failed) : "");
                String logLine = success ? null : String.format("Job execution failed for %s!!!", describe.apply(item));
                QuarkusTransaction.requiringNew().run(() -> {
                    BackgroundJob job = jobRepository.findById(jobId);
                    job.setStatus(status);
                    if (logLine != null) {
                        job.appendToJobLog(logLine);
                    }
                });
            }
            QuarkusTransaction.requiringNew().run(() -> jobRepository.findById(jobId).setCompleted(LocalDateTime.now()));
        }, Infrastructure.getDefaultWorkerPool());
    }

    /** Pending jobs and jobs completed in the last 8 hours. */
    public List<BackgroundJob> recentJobs() {
        return jobRepository.findRecent(LocalDateTime.now().minusHours(8));
    }
}

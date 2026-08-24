package com.example.passportinspector.worker;

import lombok.RequiredArgsConstructor;
import com.example.passportinspector.model.type.JobStatus;
import com.example.passportinspector.model.type.PassportCheckStatus;
import com.example.passportinspector.repository.JobRepository;
import com.example.passportinspector.repository.PassportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobStatusService {

    private final JobRepository jobRepository;
    private final PassportRepository passportRepository;

    @Transactional
    public void markInProgress(UUID jobId) {
        jobRepository.findByJobId(jobId)
                .filter(job -> job.getJobStatus() == JobStatus.PENDING)
                .ifPresent(job -> {
                    job.setJobStatus(JobStatus.IN_PROGRESS);
                    job.setAttempts(job.getAttempts() + 1);
                    job.setInProgressSince(Instant.now());
                    jobRepository.save(job);
                });
    }

    @Transactional
    public void completeIfAllPassportsProcessed(UUID jobId) {
        long total = passportRepository.countByJobId(jobId);

        if (total == 0) {
            return;
        }

        long notComplete = passportRepository.countByJobIdAndCheckStatusNot(
                jobId,
                PassportCheckStatus.COMPLETED
        );

        if (notComplete == 0) {
            jobRepository.findByJobId(jobId)
                    .ifPresent(job -> {
                        job.setJobStatus(JobStatus.COMPLETED);
                        jobRepository.save(job);
                    });
        }
    }

    @Transactional
    public void failJob(UUID jobId) {
        jobRepository.findByJobId(jobId)
                .ifPresent(job -> {
                    job.setJobStatus(JobStatus.FAILED);
                    jobRepository.save(job);
                });
    }
}
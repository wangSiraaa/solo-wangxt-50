package com.moldtrial.repo;

import com.moldtrial.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public final class Repositories {

    public interface MoldRevisionRepository extends JpaRepository<MoldRevision, Long> {
        List<MoldRevision> findByMoldCodeOrderByRevisionAsc(String moldCode);
        Optional<MoldRevision> findByMoldCodeAndRevision(String moldCode, String revision);
    }

    public interface CavityRepository extends JpaRepository<Cavity, Long> {
        List<Cavity> findByMoldRevisionIdOrderByCavityNoAsc(Long moldRevisionId);
    }

    public interface ParameterGroupRepository extends JpaRepository<ParameterGroup, Long> {
        Optional<ParameterGroup> findByCode(String code);
    }

    public interface BatchRepository extends JpaRepository<TrialBatch, Long> {
        Optional<TrialBatch> findByCode(String code);
        List<TrialBatch> findAllByOrderByTrialAtAsc();
    }

    public interface IssueRepository extends JpaRepository<Issue, Long> {
        Optional<Issue> findByCode(String code);
        List<Issue> findByDiscoveredBatchIdOrderByCavityNoAscCodeAsc(Long batchId);
        List<Issue> findAllByOrderByCodeAsc();
        long countBySeverityAndStatusNot(Severity severity, IssueStatus status);
    }

    public interface RetestRepository extends JpaRepository<Retest, Long> {
        List<Retest> findByIssueIdOrderByCreatedAtAsc(Long issueId);
        long countByCodeStartingWith(String prefix);
    }

    public interface RectificationRepository extends JpaRepository<RectificationRecord, Long> {
        List<RectificationRecord> findByIssueIdOrderByCreatedAtAsc(Long issueId);
    }

    public interface EvidenceRepository extends JpaRepository<SampleEvidence, Long> {
        long countByRetestIsNull();
    }

    private Repositories() {}
}

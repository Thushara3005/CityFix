package com.cityfix.service;

import com.cityfix.dto.CreateReportRequest;
import com.cityfix.exception.DuplicateUpvoteException;
import com.cityfix.exception.ReportNotFoundException;
import com.cityfix.model.Category;
import com.cityfix.model.Report;
import com.cityfix.model.ReportStatus;
import com.cityfix.model.Upvote;
import com.cityfix.repository.ReportRepository;
import com.cityfix.repository.UpvoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ReportRepository reportRepository;
    private final UpvoteRepository upvoteRepository;

    public ReportService(ReportRepository reportRepository, UpvoteRepository upvoteRepository) {
        this.reportRepository = reportRepository;
        this.upvoteRepository = upvoteRepository;
    }

    public Report createReport(CreateReportRequest request) {
        LocalDateTime now = LocalDateTime.now();

        Report report = new Report();
        report.setTitle(request.title() == null ? null : request.title().trim());
report.setDescription(request.description() == null ? null : request.description().trim());
report.setCategory(request.category());
report.setArea(request.area() == null ? null : request.area().trim());
report.setAddress(request.address() == null ? null : request.address().trim());
report.setReporterName(request.reporterName() == null ? null : request.reporterName().trim());
report.setReporterEmail(
        request.reporterEmail() == null
                ? null
                : request.reporterEmail().trim().toLowerCase(Locale.ROOT)
);
        report.setStatus(ReportStatus.REPORTED);
        report.setUpvoteCount(0);
        report.setCreatedAt(now);
        report.setUpdatedAt(now);

        Report savedReport = reportRepository.save(report);
        log.info("Report {} created in area {}", savedReport.getId(), savedReport.getArea());
        return savedReport;
    }

    public Report findReportById(Long id) {
        return reportRepository.findById(id).orElseThrow(() -> new ReportNotFoundException(id));
    }

    public List<Report> findReports(String area, ReportStatus status, Category category, String sort) {
        return reportRepository.findAll(createSort(sort)).stream()
                .filter(report -> area == null || report.getArea().equalsIgnoreCase(area))
                .filter(report -> status == null || report.getStatus() == status)
                .filter(report -> category == null || report.getCategory() == category)
                .toList();
    }

    public Report updateStatus(Long id, ReportStatus newStatus) {
        Report report = findReportById(id);
        LocalDateTime now = LocalDateTime.now();

        report.setStatus(newStatus);
        report.setUpdatedAt(now);
        if (newStatus == ReportStatus.FIXED) {
            report.setFixedAt(now);
        }

        Report savedReport = reportRepository.save(report);
        log.info("Report {} status changed to {}", id, newStatus);
        return savedReport;
    }
    
    @Transactional
public Report upvoteReport(Long id, String voterEmail) {
    Report report = findReportById(id);

    String normalizedEmail = voterEmail == null
            ? null
            : voterEmail.trim().toLowerCase(Locale.ROOT);

    if (normalizedEmail == null || normalizedEmail.isBlank()) {
        throw new IllegalArgumentException("Voter email is required");
    }

    if (upvoteRepository.existsByReportIdAndVoterEmail(id, normalizedEmail)) {
        throw new DuplicateUpvoteException(normalizedEmail);
    }

    Upvote upvote = new Upvote();
    upvote.setReport(report);
    upvote.setVoterEmail(normalizedEmail);
    upvote.setCreatedAt(LocalDateTime.now());
    upvoteRepository.save(upvote);

    report.setUpvoteCount(report.getUpvoteCount() + 1);
    return reportRepository.save(report);
}
public Map<String, Map<String, Long>> getStats() {
    Map<String, Long> byStatus = new LinkedHashMap<>();

    for (ReportStatus status : ReportStatus.values()) {
        byStatus.put(status.name(), reportRepository.countByStatus(status));
    }

    Map<String, Long> byCategory = new LinkedHashMap<>();

    for (Category category : Category.values()) {
        byCategory.put(category.name(), reportRepository.countByCategory(category));
    }

    return Map.of("byStatus", byStatus, "byCategory", byCategory);
}

private Sort createSort(String sort) {
    Sort newestFirst = Sort.by(Sort.Direction.DESC, "createdAt");

    if ("votes".equals(sort)) {
        return Sort.by(Sort.Direction.DESC, "upvoteCount").and(newestFirst);
    }

    return newestFirst;
}
}

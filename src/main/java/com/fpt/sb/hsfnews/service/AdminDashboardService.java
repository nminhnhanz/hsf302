package com.fpt.sb.hsfnews.service;

import com.fpt.sb.hsfnews.entity.Article;
import com.fpt.sb.hsfnews.entity.ArticleStatus;
import com.fpt.sb.hsfnews.repository.ArticleRepository;
import com.fpt.sb.hsfnews.repository.CommentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {

    private static final DateTimeFormatter YEAR_MONTH_LABEL = DateTimeFormatter.ofPattern("MM/yyyy");

    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;

    public AdminDashboardService(ArticleRepository articleRepository, CommentRepository commentRepository) {
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
    }

    public DashboardData buildDashboardData(String modeRaw,
                                            Integer yearRaw,
                                            Integer compareYearARaw,
                                            Integer compareYearBRaw) {
        int currentYear = LocalDateTime.now().getYear();
        Mode mode = Mode.from(modeRaw);
        List<Integer> availableYears = resolveAvailableYears(currentYear);

        int selectedYear = normalizeYear(yearRaw, availableYears, currentYear);
        int compareYearA = normalizeYear(compareYearARaw, availableYears, selectedYear);
        int compareYearB = normalizeYear(compareYearBRaw, availableYears, fallbackCompareYear(compareYearA, availableYears));

        if (compareYearA == compareYearB) {
            compareYearB = fallbackCompareYear(compareYearA, availableYears);
        }

        long totalArticles = articleRepository.count();
        long publishedArticles = articleRepository.countByStatus(ArticleStatus.PUBLISHED);
        long draftArticles = articleRepository.countByStatus(ArticleStatus.DRAFT);
        long totalComments = commentRepository.count();

        ChartPayload chart = switch (mode) {
            case YEAR -> buildSelectedYearChart(selectedYear);
            case RECENT_12_MONTHS -> buildRecent12MonthsChart();
            case COMPARE_YEARS -> buildCompareYearsChart(compareYearA, compareYearB);
        };

        List<TopArticle> topCommented = articleRepository.findTopArticlesByCommentCount(PageRequest.of(0, 5))
                .stream()
                .map(row -> {
                    Article article = (Article) row[0];
                    long comments = ((Number) row[1]).longValue();
                    return new TopArticle(article.getId(), article.getTitle(), comments);
                })
                .collect(Collectors.toList());

        return new DashboardData(totalArticles,
                publishedArticles,
                draftArticles,
                totalComments,
                mode.name(),
                selectedYear,
                compareYearA,
                compareYearB,
                availableYears,
                chart.labels,
                chart.primaryValues,
                chart.primaryLabel,
                chart.secondaryValues,
                chart.secondaryLabel,
                topCommented);
    }

    private List<Integer> resolveAvailableYears(int currentYear) {
        TreeSet<Integer> years = new TreeSet<>(Comparator.reverseOrder());
        articleRepository.findAll().stream()
                .map(Article::getCreatedAt)
                .filter(v -> v != null)
                .map(LocalDateTime::getYear)
                .forEach(years::add);
        years.add(currentYear);
        return new ArrayList<>(years);
    }

    private int normalizeYear(Integer yearRaw, List<Integer> availableYears, int fallback) {
        if (yearRaw != null && availableYears.contains(yearRaw)) {
            return yearRaw;
        }
        return fallback;
    }

    private int fallbackCompareYear(int compareYearA, List<Integer> availableYears) {
        for (Integer year : availableYears) {
            if (year != compareYearA) {
                return year;
            }
        }
        return compareYearA;
    }

    private ChartPayload buildSelectedYearChart(int year) {
        List<Article> articles = articleRepository.findByCreatedAtGreaterThanEqual(LocalDateTime.of(year, 1, 1, 0, 0));
        return new ChartPayload(monthLabels(),
                toMonthValuesInYear(articles, year),
                String.valueOf(year),
                List.of(),
                "");
    }

    private ChartPayload buildRecent12MonthsChart() {
        YearMonth end = YearMonth.now();
        YearMonth start = end.minusMonths(11);
        List<Article> articles = articleRepository.findByCreatedAtGreaterThanEqual(start.atDay(1).atStartOfDay());

        Map<YearMonth, Long> grouped = articles.stream()
                .collect(Collectors.groupingBy(a -> YearMonth.from(a.getCreatedAt()), Collectors.counting()));

        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (YearMonth cursor = start; !cursor.isAfter(end); cursor = cursor.plusMonths(1)) {
            labels.add(cursor.format(YEAR_MONTH_LABEL));
            values.add(grouped.getOrDefault(cursor, 0L));
        }

        return new ChartPayload(labels, values, "Last 12 months", List.of(), "");
    }

    private ChartPayload buildCompareYearsChart(int yearA, int yearB) {
        int minYear = Math.min(yearA, yearB);
        List<Article> articles = articleRepository.findByCreatedAtGreaterThanEqual(LocalDateTime.of(minYear, 1, 1, 0, 0));

        return new ChartPayload(monthLabels(),
                toMonthValuesInYear(articles, yearA),
                String.valueOf(yearA),
                toMonthValuesInYear(articles, yearB),
                String.valueOf(yearB));
    }

    private List<String> monthLabels() {
        List<String> labels = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            labels.add("M" + month);
        }
        return labels;
    }

    private List<Long> toMonthValuesInYear(List<Article> articles, int year) {
        Map<Integer, Long> grouped = new HashMap<>();
        for (Article article : articles) {
            if (article.getCreatedAt() == null || article.getCreatedAt().getYear() != year) {
                continue;
            }
            int month = article.getCreatedAt().getMonthValue();
            grouped.put(month, grouped.getOrDefault(month, 0L) + 1L);
        }

        List<Long> values = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            values.add(grouped.getOrDefault(month, 0L));
        }
        return values;
    }

    public record DashboardData(long totalArticles,
                                long publishedArticles,
                                long draftArticles,
                                long totalComments,
                                String selectedMode,
                                int selectedYear,
                                int selectedCompareYearA,
                                int selectedCompareYearB,
                                List<Integer> availableYears,
                                List<String> labels,
                                List<Long> primarySeries,
                                String primarySeriesLabel,
                                List<Long> secondarySeries,
                                String secondarySeriesLabel,
                                List<TopArticle> topCommentedArticles) {
    }

    public record TopArticle(Long id, String title, long commentCount) {
    }

    private record ChartPayload(List<String> labels,
                                List<Long> primaryValues,
                                String primaryLabel,
                                List<Long> secondaryValues,
                                String secondaryLabel) {
    }

    private enum Mode {
        YEAR,
        RECENT_12_MONTHS,
        COMPARE_YEARS;

        static Mode from(String raw) {
            if (raw == null || raw.isBlank()) {
                return YEAR;
            }
            try {
                return Mode.valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return YEAR;
            }
        }
    }

}






package com.fpt.sb.hsfnews.config;

import com.fpt.sb.hsfnews.entity.*;
import com.fpt.sb.hsfnews.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ArticleRepository articleRepository;
    private final PasswordEncoder passwordEncoder;

    // Đọc giá trị từ application.properties
    @Value("${app.seed-demo-data:false}")
    private boolean seedDemoData;

    public DataInitializer(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           TagRepository tagRepository,
                           ArticleRepository articleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.articleRepository = articleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. Dữ liệu BẮT BUỘC (System Data): Luôn luôn khởi tạo (VD: Admin)
        User admin = seedAdmin();
        seedMembers();

        if (!seedDemoData) {
            System.out.println("⚠️ The Seeding Demo Data feature is currently OFF. Skip generating Sample Data.");
            return;
        }

        LocalDateTime baseDate = LocalDateTime.now().withHour(9).withMinute(0).withSecond(0).withNano(0);
        System.out.println("✅ The Seeding Demo Data feature is ON. Loading sample data...");

        // --- 1. SEED CATEGORIES ---
        Category dsa = seedCategory("DSA", "Data Structures & Algorithms, improving logical thinking and problem-solving skills.");
        Category database = seedCategory("Database", "RDBMS knowledge, query optimization, and database schema design.");
        Category springBoot = seedCategory("Spring Boot", "Spring framework concepts and application development."); // Đã bổ sung vì bên dưới gọi tới

        // --- 2. SEED TAGS ---
        Tag tSpring = seedTag("Spring");
        Tag tJpa = seedTag("JPA");
        Tag tThymeleaf = seedTag("Thymeleaf");
        Tag tRest = seedTag("REST API");
        Tag tSecurity = seedTag("Security");

        Tag tOop = seedTag("OOP");
        Tag tBigO = seedTag("Big-O");
        Tag tAlgorithm = seedTag("Algorithm");

        Tag tSql = seedTag("SQL");
        Tag tIndex = seedTag("Indexing");
        Tag tTransaction = seedTag("Transaction");

        // --- 3. SEED ARTICLES ---
        seedArticleIfMissing(
                "Spring Boot MVC + Thymeleaf: Standard Project Structure for Beginners",
                "A guide on organizing controller, service, repository folders, and rendering views with Thymeleaf for easy maintenance.",
                """
                <p>When starting with Spring Boot, organizing your source code logically is crucial for easy scaling later on.</p>
                <img src="https://picsum.photos/seed/spring_content/800/400" alt="Project Structure" class="img-fluid rounded my-4 shadow-sm">
                <h2>1. Package Structure</h2>
                <ul>
                    <li><code>controller/</code>: Contains classes handling HTTP Requests.</li>
                    <li><code>service/</code>: Contains the Business Logic.</li>
                    <li><code>repository/</code>: Contains interfaces interacting with the Database.</li>
                    <li><code>entity/</code>: Classes mapped to database tables (JPA).</li>
                </ul>
                <h2>2. Controller Example</h2>
                <pre><code>@Controller
public class HomeController {
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("message", "Hello Thymeleaf!");
        return "index";
    }
}</code></pre>
                """,
                "https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=1200&q=80",
                ArticleStatus.PUBLISHED, admin, springBoot, Set.of(tSpring, tThymeleaf),
                baseDate.minusDays(4)
        );

        seedArticleIfMissing(
                "The N+1 Query Problem in JPA and How to Fix It with @EntityGraph",
                "A deep dive into common JPA performance issues and efficient data fetching strategies.",
                """
                <img src="https://picsum.photos/seed/jpa_content/800/400" alt="Database Queries" class="img-fluid rounded my-4 shadow-sm">
                <h2>The Cause</h2>
                <p>It occurs when you fetch a list of parent entities (1 query), and then for each parent, JPA fires another query to fetch the child entities (N queries).</p>
                <h2>The Solution: Using @EntityGraph</h2>
                <pre><code>@EntityGraph(attributePaths = {"author", "category", "tags"})
@Query("SELECT a FROM Article a")
Page&lt;Article&gt; findAllWithDetails(Pageable pageable);</code></pre>
                <p>With just one annotation, Spring Data JPA automatically converts it into a <code>LEFT JOIN FETCH</code> at the database level.</p>
                """,
                "https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=1200&q=80",
                ArticleStatus.PUBLISHED, admin, springBoot, Set.of(tSpring, tJpa),
                baseDate.minusWeeks(2)
        );

        seedArticleIfMissing(
                "Designing Standard RESTful APIs in Spring Boot",
                "Principles of naming endpoints, using correct HTTP Methods, and JSON response formats.",
                """
                <p>A good API needs to be intuitive and strictly follow REST conventions.</p>
                <ul>
                    <li>Use the correct HTTP Method: GET (Retrieve), POST (Create), PUT (Update entirely), PATCH (Update partially), DELETE (Remove).</li>
                    <li>Always return the appropriate HTTP Status Code (200 OK, 201 Created, 404 Not Found, 400 Bad Request...).</li>
                </ul>
                """,
                "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=1200&q=80",
                ArticleStatus.PUBLISHED, admin, springBoot, Set.of(tSpring, tRest),
                baseDate.minusMonths(1).minusDays(6)
        );

        seedArticleIfMissing(
                "Big-O Notation: Understand It to Write Optimized Code",
                "Comprehensive guide on time/space complexity (Big-O) for common data structures and algorithms.",
                """
                <p>Big-O notation is how we communicate the efficiency of an algorithm as the input data (N) grows.</p>
                <img src="https://picsum.photos/seed/math_content/800/400" alt="Big-O Chart" class="img-fluid rounded my-4 shadow-sm">
                <h2>Common Complexities</h2>
                <ul>
                    <li><code>O(1)</code>: Looking up an element in a HashMap.</li>
                    <li><code>O(N log N)</code>: Efficient sorting algorithms like Merge Sort, Quick Sort.</li>
                    <li><code>O(N^2)</code>: Nested loops (Bubble Sort).</li>
                </ul>
                """,
                "https://images.unsplash.com/photo-1509228627152-72ae9ae6848d?auto=format&fit=crop&w=1200&q=80",
                ArticleStatus.PUBLISHED, admin, dsa, Set.of(tBigO, tAlgorithm),
                baseDate.minusMonths(2).minusDays(12)
        );

        seedArticleIfMissing(
                "Graph Traversal: BFS and DFS Algorithms",
                "Distinguishing Breadth-First Search (BFS) and Depth-First Search (DFS) through practical examples.",
                """
                <p>Graphs are used everywhere: Social Networks, Google Maps, Network Routing...</p>
                <img src="https://picsum.photos/seed/graph_content/800/400" alt="Graph Nodes" class="img-fluid rounded my-4 shadow-sm">
                <h2>1. Breadth-First Search (BFS)</h2>
                <p>Explores the graph layer by layer, utilizing a <b>Queue</b>. Excellent for finding the shortest path on an unweighted graph.</p>
                <h2>2. Depth-First Search (DFS)</h2>
                """,
                "https://images.unsplash.com/photo-1516259762381-22954d7d3ad2?auto=format&fit=crop&w=1200&q=80",
                ArticleStatus.PUBLISHED, admin, dsa, Set.of(tAlgorithm),
                baseDate.minusMonths(3).minusDays(3)
        );

        seedArticleIfMissing(
                "Database Indexing: When to Use Indexes and When NOT To?",
                "An overview of B-Tree indexes, Composite indexes, and the trade-offs of using indexes.",
                """
                <p>Many developers mistakenly believe that throwing an Index at a slow query will always fix it. In reality, Indexes are a double-edged sword.</p>
                <img src="https://picsum.photos/seed/index_content/800/400" alt="Database Indexing" class="img-fluid rounded my-4 shadow-sm">
                <h2>Pros</h2>
                <p>Significantly speeds up <code>SELECT</code> queries, especially those with <code>WHERE</code> and <code>ORDER BY</code> clauses.</p>
                <h2>Cons</h2>
                """,
                "https://images.unsplash.com/photo-1544197150-b99a580bb7a8?auto=format&fit=crop&w=1200&q=80", // Reused an image URL here
                ArticleStatus.PUBLISHED, admin, database, Set.of(tSql, tIndex),
                baseDate.minusMonths(5)
        );

        seedArticleIfMissing(
                "What are ACID Properties in Database Transactions?",
                "An overview of ACID principles ensuring database transaction reliability.",
                """
                <p>To ensure data integrity (especially in banking and payment systems), Databases rely on the ACID concept.</p>
                <img src="https://picsum.photos/seed/acid_content/800/400" alt="Server Room" class="img-fluid rounded my-4 shadow-sm">
                <ul>
                    <li><b>Atomicity:</b> The transaction must complete 100% or fail entirely (Rollback). There is no "halfway" execution.</li>
                    <li><b>Consistency:</b> The database must transition from one valid state to another valid state after the transaction.</li>
                    <li><b>Isolation:</b> Concurrent transactions running in parallel must not interfere with each other.</li>
                    <li><b>Durability:</b> Once committed, the data must be permanently saved to the disk, surviving even power failures.</li>
                </ul>
                """,
                "https://images.unsplash.com/photo-1544197150-b99a580bb7a8?auto=format&fit=crop&w=1200&q=80",
                ArticleStatus.PUBLISHED, admin, database, Set.of(tSql, tTransaction),
                baseDate.minusMonths(7).minusDays(8)
        );

        seedArticleIfMissing(
                "Spring Security Basics (Draft)",
                "A draft post to explore Spring Security configurations.",
                "<p>Coming soon...</p>",
                "https://images.unsplash.com/photo-1586281380349-632531db7ed4?auto=format&fit=crop&w=1200&q=80",
                ArticleStatus.DRAFT, admin, springBoot, Set.of(tSpring, tSecurity),
                baseDate.minusDays(1)
        );

        String[] dbTopics = {"Normalization", "NoSQL vs SQL", "Connection Pooling", "Stored Procedures", "Triggers"};
        for (int i = 0; i < dbTopics.length; i++) {
            seedArticleIfMissing(
                    dbTopics[i] + " Explained",
                    "A short article explaining the core concepts of " + dbTopics[i] + " to improve your database design mindset.",
                    "<p>This is a detailed post about <strong>" + dbTopics[i] + "</strong>. Happy coding!</p><img src=\"https://picsum.photos/seed/dynamic_" + i + "/800/400\" alt=\"Database topic\" class=\"img-fluid rounded my-4 shadow-sm\">",
                    "https://picsum.photos/seed/db" + i + "/1200/600",
                    ArticleStatus.PUBLISHED, admin, database, Set.of(tSql),
                    baseDate.minusMonths(8 + i).minusDays((long) i * 4)
            );
        }
    }

    // --- HELPER METHODS ---

    private User seedAdmin() {
        return userRepository.findByUsernameIgnoreCase("admin")
                .orElseGet(() -> {
                    User u = new User();
                    u.setUsername("admin");
                    // Encode password with BCrypt for code-first approach
                    u.setPassword(passwordEncoder.encode("admin123"));
                    u.setFullName("Administrator");
                    u.setEmail("admin@hsfnews.com");
                    u.setRole(Role.ADMIN);
                    return userRepository.save(u);
                });
    }

    private void seedMembers() {
        userRepository.findByUsernameIgnoreCase("member01").orElseGet(() -> {
            User u = new User();
            u.setUsername("member01");
            u.setPassword(passwordEncoder.encode("member123"));
            u.setFullName("Member 01");
            u.setEmail("member01@hsfnews.com");
            u.setRole(Role.MEMBER);
            return userRepository.save(u);
        });

        userRepository.findByUsernameIgnoreCase("member02").orElseGet(() -> {
            User u = new User();
            u.setUsername("member02");
            u.setPassword(passwordEncoder.encode("member123"));
            u.setFullName("Member 02");
            u.setEmail("member02@hsfnews.com");
            u.setRole(Role.MEMBER);
            return userRepository.save(u);
        });
    }

    private Category seedCategory(String name, String description) {
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Category c = new Category();
                    c.setName(name);
                    c.setDescription(description);
                    return categoryRepository.save(c);
                });
    }

    private Tag seedTag(String name) {
        return tagRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Tag t = new Tag();
                    t.setName(name);
                    return tagRepository.save(t);
                });
    }

    private void seedArticleIfMissing(String title,
                                      String summary,
                                      String content,
                                      String thumbnail,
                                      ArticleStatus status,
                                      User author,
                                      Category category,
                                      Set<Tag> tags,
                                      LocalDateTime createdAt) {

        var existing = articleRepository.findByTitleIgnoreCase(title);
        if (existing.isPresent()) {
            Article article = existing.get();
            article.setCreatedAt(createdAt);
            articleRepository.save(article);
            return;
        }

        Article a = new Article();
        a.setTitle(title);
        a.setSummary(summary);
        a.setContent(content);
        a.setThumbnail(thumbnail);
        a.setStatus(status);
        a.setAuthor(author);
        a.setCategory(category);
        a.setTags(new HashSet<>(tags));
        a.setCreatedAt(createdAt);

        if (status == ArticleStatus.PUBLISHED) {
            List<Comment> comments = new ArrayList<>();

            User member1 = userRepository.findByUsernameIgnoreCase("member01").orElse(author);
            User member2 = userRepository.findByUsernameIgnoreCase("member02").orElse(author);

            Comment c1 = new Comment();
            c1.setAuthor(member1); // Ánh xạ lại user cho comment
            c1.setContent("The article is very easy to understand, looking forward to more posts like this!");
            c1.setArticle(a);
            comments.add(c1);

            if (title.toLowerCase().contains("spring") || title.toLowerCase().contains("jpa")) {
                Comment c2 = new Comment();
                c2.setAuthor(member2);
                c2.setContent("Quick tip: Be careful when using EntityGraph combined with pagination, sometimes Hibernate will pull the entire DB into memory (in-memory pagination) before slicing the list!");
                c2.setArticle(a);
                comments.add(c2);
            }

            a.setComments(comments);
        }

        articleRepository.save(a);
    }
}
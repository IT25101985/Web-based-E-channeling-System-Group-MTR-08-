package com.echanneling.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.*;

/** Explicit second connection: the assignment uses a different physical schema from JPA. */
@Service
public class DddVivaService {
    public record Member(String id, String name, String slug, String feature, List<String> tables,
        List<List<String>> queries, String procedure, String call, String trigger,
        String valid, String invalid, int error, String rule, String file) {}
    public record Result(List<String> columns, List<List<Object>> rows) {}
    private final List<Member> members;
    private final JdbcTemplate jdbc;

    public DddVivaService(ObjectMapper mapper,
        @Value("${app.viva.datasource.url:jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=IT2140_MTR08_Part02_20260927;encrypt=true;trustServerCertificate=true;loginTimeout=8}") String url,
        @Value("${app.viva.datasource.username:${spring.datasource.username}}") String username,
        @Value("${app.viva.datasource.password:${spring.datasource.password}}") String password) throws IOException {
        try (var in = new ClassPathResource("viva/members.json").getInputStream()) {
            members = List.copyOf(mapper.readValue(in, new TypeReference<List<Member>>() {}));
        }
        var source = new DriverManagerDataSource(url, username, password);
        source.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        jdbc = new JdbcTemplate(source);
        jdbc.setQueryTimeout(8);
        jdbc.setMaxRows(100);
    }
    public List<Member> members() { return members; }
    public Member member(String id) {
        return members.stream().filter(m -> m.id().equals(id)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Choose a listed student."));
    }
    public String sql(Member member, int query) {
        if (query < 1 || query > 6) throw new IllegalArgumentException("Choose query 1 to 6.");
        return query == 6 ? member.call() : member.queries().get(query - 1).get(2);
    }
    public Map<String,Object> connection() {
        return jdbc.queryForMap("SELECT DB_NAME() AS DatabaseName, CAST(SERVERPROPERTY('ServerName') AS nvarchar(128)) AS ServerName");
    }
    public Result execute(Member member, int query) {
        // SQL comes only from the bundled catalogue. No request text is executed as SQL.
        return jdbc.query(sql(member, query), rs -> {
            var columns = new ArrayList<String>();
            var rows = new ArrayList<List<Object>>();
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) columns.add(rs.getMetaData().getColumnLabel(i));
            while (rs.next()) {
                var row = new ArrayList<Object>();
                for (int i = 1; i <= columns.size(); i++) row.add(rs.getObject(i));
                rows.add(row);
            }
            return new Result(columns, rows);
        });
    }
}

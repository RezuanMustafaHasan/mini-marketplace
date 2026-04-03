package com.hasan.marketplace.config;

import com.hasan.marketplace.entity.OrderStatus;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderStatusConstraintUpdater implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!isPostgreSql()) {
            return;
        }

        jdbcTemplate.execute(buildRefreshOrderStatusConstraintSql());
    }

    boolean isPostgreSql() {
        try (Connection connection = dataSource.getConnection()) {
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            return databaseProductName != null
                    && databaseProductName.toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to inspect the database product name.", exception);
        }
    }

    static String buildRefreshOrderStatusConstraintSql() {
        String allowedStatuses = Arrays.stream(OrderStatus.values())
                .map(OrderStatus::name)
                .map(status -> "'" + status + "'")
                .collect(Collectors.joining(", "));

        return """
                DO $$
                BEGIN
                    IF EXISTS (
                        SELECT 1
                        FROM information_schema.tables
                        WHERE table_schema = current_schema()
                          AND table_name = 'orders'
                    ) THEN
                        ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;
                        ALTER TABLE orders
                            ADD CONSTRAINT orders_status_check
                            CHECK (status IN (%s));
                    END IF;
                END $$;
                """.formatted(allowedStatuses);
    }
}

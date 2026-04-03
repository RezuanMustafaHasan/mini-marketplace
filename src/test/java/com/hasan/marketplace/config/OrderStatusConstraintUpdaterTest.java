package com.hasan.marketplace.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class OrderStatusConstraintUpdaterTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData databaseMetaData;

    @InjectMocks
    private OrderStatusConstraintUpdater updater;

    @Test
    void runRefreshesOrdersStatusConstraintForPostgreSql() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        updater.run(null);

        verify(jdbcTemplate).execute(OrderStatusConstraintUpdater.buildRefreshOrderStatusConstraintSql());
        verify(connection).close();
    }

    @Test
    void runSkipsConstraintRefreshForNonPostgreSqlDatabase() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("H2");

        updater.run(null);

        verify(jdbcTemplate, never()).execute(org.mockito.ArgumentMatchers.anyString());
        verify(connection).close();
    }

    @Test
    void buildRefreshOrderStatusConstraintSqlIncludesAllSupportedStatuses() {
        String sql = OrderStatusConstraintUpdater.buildRefreshOrderStatusConstraintSql();

        assertTrue(sql.contains("'PENDING'"));
        assertTrue(sql.contains("'CONFIRMED'"));
        assertTrue(sql.contains("'OUT_FOR_DELIVERY'"));
        assertTrue(sql.contains("'DELIVERED'"));
        assertTrue(sql.contains("'CANCELLED'"));
        assertTrue(sql.contains("DROP CONSTRAINT IF EXISTS orders_status_check"));
    }
}

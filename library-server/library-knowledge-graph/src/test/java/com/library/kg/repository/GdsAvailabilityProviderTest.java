package com.library.kg.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GdsAvailabilityProvider")
class GdsAvailabilityProviderTest {

    @Mock
    private Driver driver;
    @Mock
    private Session session;
    @Mock
    private Result result;
    @Mock
    private Record record;
    @Mock
    private Value value;

    private GdsAvailabilityProvider provider;

    @BeforeEach
    void setUp() {
        provider = new GdsAvailabilityProvider(driver);
    }

    @Nested
    @DisplayName("probe")
    class Probe {

        @Test
        @DisplayName("GDS 可用时应标记 available=true")
        void shouldMarkAvailableWhenGdsProceduresExist() {
            when(driver.session()).thenReturn(session);
            when(session.run(anyString())).thenReturn(result);
            when(result.hasNext()).thenReturn(true);
            when(result.next()).thenReturn(record);
            when(record.get("c")).thenReturn(value);
            when(value.asLong()).thenReturn(5L);

            provider.probe();
            assertThat(provider.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("GDS 不可用时应标记 available=false")
        void shouldMarkUnavailableWhenNoProcedures() {
            when(driver.session()).thenReturn(session);
            when(session.run(anyString())).thenReturn(result);
            when(result.hasNext()).thenReturn(false);

            provider.probe();
            assertThat(provider.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("probe (降级)")
    class ProbeFallback {

        @Test
        @DisplayName("驱动异常时应降级为 available=false")
        void shouldFallbackWhenDriverThrows() {
            when(driver.session()).thenThrow(new RuntimeException("Connection refused"));
            provider.probe();
            assertThat(provider.isAvailable()).isFalse();
        }
    }
}

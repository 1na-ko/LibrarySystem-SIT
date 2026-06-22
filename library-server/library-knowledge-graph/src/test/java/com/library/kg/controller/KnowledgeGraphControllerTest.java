package com.library.kg.controller;

import com.library.common.result.Result;
import com.library.kg.service.GraphBuildService;
import com.library.kg.service.GraphQueryService;
import com.library.kg.service.LiteratureTracingService;
import com.library.kg.service.TopicNetworkBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link KnowledgeGraphController} 单元测试.
 * <p>
 * 重点验证 P0-2 新增的 {@code POST /admin/kg/build-topic-network} 端点
 * 正确委托至 {@link TopicNetworkBuilder#buildTopicNetwork()}。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeGraphController")
class KnowledgeGraphControllerTest {

    @Mock
    private GraphQueryService graphQueryService;
    @Mock
    private LiteratureTracingService literatureTracingService;
    @Mock
    private TopicNetworkBuilder topicNetworkBuilder;
    @Mock
    private GraphBuildService graphBuildService;

    @InjectMocks
    private KnowledgeGraphController controller;

    @Test
    @DisplayName("POST /admin/kg/build-topic-network 应委托至 TopicNetworkBuilder.buildTopicNetwork")
    void shouldDelegateBuildTopicNetwork() {
        Result<Void> result = controller.buildTopicNetwork();

        verify(topicNetworkBuilder, times(1)).buildTopicNetwork();
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(200);
    }
}

package g2pc.dp.core.lib;

import com.fasterxml.jackson.databind.ObjectMapper;
import g2pc.core.lib.dto.common.cache.CacheDTO;
import g2pc.core.lib.dto.common.header.RequestHeaderDTO;
import g2pc.core.lib.dto.common.header.ResponseHeaderDTO;
import g2pc.core.lib.exceptions.G2pcError;
import g2pc.core.lib.utils.CommonUtils;
import g2pc.dp.core.lib.service.ResponseBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@SpringBootTest
class ResponseBuilderServiceTests {

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private ResponseBuilderService responseBuilderService;

    @Test
    void testBuildOnSearchScheduler() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerSubtypes(RequestHeaderDTO.class, ResponseHeaderDTO.class);

        String cacheString = commonUtils.readJsonFile("cache.json");
        String refRecordsString = commonUtils.readJsonFile("refRecords.json");

        CacheDTO cacheDTO = objectMapper.readerFor(CacheDTO.class).readValue(cacheString);
        List<Object> refRecordsStringList = objectMapper.readerFor(List.class).readValue(refRecordsString);

        G2pcError result = responseBuilderService.buildOnSearchScheduler(refRecordsStringList, cacheDTO);
        log.info("result: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
        assertNotNull(result);
        log.info("result: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
    }
}

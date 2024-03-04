package g2pc.dp.core.lib;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import g2pc.core.lib.dto.common.cache.CacheDTO;
import g2pc.core.lib.dto.common.header.RequestHeaderDTO;
import g2pc.core.lib.dto.common.header.ResponseHeaderDTO;
import g2pc.core.lib.enums.HeaderStatusENUM;
import g2pc.core.lib.utils.CommonUtils;
import g2pc.dp.core.lib.service.TxnTrackerRedisService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@SpringBootTest
class TxnTrackerRedisServiceTests {

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private TxnTrackerRedisService txnTrackerRedisService;

    @Disabled
    @Test
    void testGetCacheKeys() {
        List<String> result = txnTrackerRedisService.getCacheKeys("request-farmer-*");
        assertNotNull(result);
        log.info("Cache keys: {}", result);
    }

    @Disabled
    @Test
    void testGetRequestData() throws JsonProcessingException {
        String result = txnTrackerRedisService.getRequestData("request-farmer-T372-9157-5163-1015-324");
        assertNotNull(result);
        log.info("Request data: {}", result);
    }

    @Disabled
    @Test
    void testSaveRequestDetails() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        String cacheDtoString = commonUtils.readJsonFile("cache.json");
        CacheDTO cacheDTO = objectMapper.readerFor(CacheDTO.class).readValue(cacheDtoString);
        txnTrackerRedisService.saveRequestDetails(cacheDTO, "demo-farmer-request");
        log.info("Request data saved successfully");
    }

    @Test
    void testUpdateRequestDetails() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        String cacheDtoString = commonUtils.readJsonFile("cache.json");
        CacheDTO cacheDTO = objectMapper.readerFor(CacheDTO.class).readValue(cacheDtoString);
        txnTrackerRedisService.updateRequestDetails("demo-farmer-request", HeaderStatusENUM.SUCC.toValue(), cacheDTO);
        log.info("Request data updated successfully");
    }
}

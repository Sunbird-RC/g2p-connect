package g2pc.dp.core.lib;

import com.fasterxml.jackson.databind.ObjectMapper;
import g2pc.core.lib.dto.common.header.RequestHeaderDTO;
import g2pc.core.lib.dto.common.header.ResponseHeaderDTO;
import g2pc.core.lib.dto.search.message.request.QueryDTO;
import g2pc.core.lib.dto.search.message.request.RequestDTO;
import g2pc.core.lib.dto.search.message.response.SearchResponseDTO;
import g2pc.core.lib.exceptions.G2pcError;
import g2pc.core.lib.utils.CommonUtils;
import g2pc.dp.core.lib.service.TxnTrackerDbService;
import jakarta.transaction.Transactional;
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
@Transactional
class TxnTrackerDbServiceTests {

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private TxnTrackerDbService txnTrackerDbService;

    @Disabled
    @Test
    void testSaveRequestDetails() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerSubtypes(RequestHeaderDTO.class, ResponseHeaderDTO.class);

        String requestDataString = commonUtils.readJsonFile("request.json");
        RequestDTO requestDTO = objectMapper.readerFor(RequestDTO.class).readValue(requestDataString);
        G2pcError result = txnTrackerDbService.saveRequestDetails(requestDTO, "https");
        assertNotNull(result);
        log.info("result: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
    }

    @Test
    void testGetUpdatedSearchResponseList() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerSubtypes(RequestHeaderDTO.class, ResponseHeaderDTO.class);

        String requestDataString = commonUtils.readJsonFile("request.json");
        String refRecordsString = commonUtils.readJsonFile("refRecords.json");
        RequestDTO requestDTO = objectMapper.readerFor(RequestDTO.class).readValue(requestDataString);
        List<Object> refRecordsStringList = objectMapper.readerFor(List.class).readValue(refRecordsString);
        List<SearchResponseDTO> result = txnTrackerDbService.getUpdatedSearchResponseList(requestDTO, refRecordsStringList, "https");
        assertNotNull(result);
        log.info("result: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
    }

    @Disabled
    @Test
    void testGetQueryDTOList() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerSubtypes(RequestHeaderDTO.class, ResponseHeaderDTO.class);

        List<QueryDTO> result = txnTrackerDbService.getQueryDTOList("T452-3162-6707-6329-4947");
        assertNotNull(result);
        log.info("result: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
    }
}

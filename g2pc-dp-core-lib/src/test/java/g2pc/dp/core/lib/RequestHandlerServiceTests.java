package g2pc.dp.core.lib;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import g2pc.core.lib.dto.common.AcknowledgementDTO;
import g2pc.core.lib.dto.common.cache.CacheDTO;
import g2pc.core.lib.enums.HeaderStatusENUM;
import g2pc.core.lib.exceptions.G2pcError;
import g2pc.core.lib.utils.CommonUtils;
import g2pc.dp.core.lib.constants.DpConstants;
import g2pc.dp.core.lib.service.RequestHandlerService;
import g2pc.dp.core.lib.service.ResponseBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@Slf4j
@SpringBootTest
class RequestHandlerServiceTests {


    @Autowired
    RequestHandlerService requestHandlerService;


    @Autowired
    ResponseBuilderService responseBuilderService;

    @Autowired
    private CommonUtils commonUtils;

    @Disabled
    @Test
    void testBuildCacheStatusRequest() throws JsonProcessingException {

        String requestString = "{\"signature\":\"aWfAqVfYju1+QkkPFp+Z6f6quiHLQHbHqY8y3v834NYGFOQjGZLsb4AWFqMOCTYc0WC8yN5sn/51G6DMUALs6uPLUCzcE0lWPE7F/x+LxH6/jeVVTYmVe35NoiU3HEy9VhCknGy+j7uo1Go/72ejJAKo07eJtyBu8hTAn/vPUY1qAKoIV0s4GjzhM02HVfadv6jULxF6Pggw+oOcd5mSE8sxlMEsxwjUXy9DK9fnsTm9l2gzjw9W1rr8mludcmhjdzP7YIM8TrHNUdSRAQv2ltsgov3AUQ+0YzP7YP5vgvMYYKBSiFlf+x/kousF2WOFZ0RciPBbKyWxnAOprzjGlQ==\",\"header\":{\"type\":\"requestHeader\",\"version\":\"1.0.0\",\"action\":\"status\",\"meta\":{\"data\":{\"is_sign\":true}},\"message_id\":\"M558-8024-0269-3337-9527\",\"message_ts\":\"2024-02-20T15:24:05+05:30\",\"total_count\":21800,\"sender_id\":\"spp.example.org\",\"receiver_id\":\"pymts.example.org\",\"is_msg_encrypted\":true,\"sender_uri\":\"https://spp.example.org/{namespace}/callback/on-search\"},\"message\":{\"transaction_id\":\"T472-4838-7947-8934-0179\",\"txnstatus_request\":{\"txn_type\":\"search\",\"attribute_type\":\"transaction_id\",\"attribute_value\":\"T214-6907-4317-0572-4272\"}}}\n";
        AcknowledgementDTO acknowledgementDTO = requestHandlerService.buildCacheStatusRequest(requestString, "dp-farmer", "https");
        assertEquals(acknowledgementDTO.getMessage(), DpConstants.SEARCH_REQUEST_RECEIVED);
        assertEquals(acknowledgementDTO.getStatus(), HeaderStatusENUM.RCVD.toValue());
    }


    @Disabled
    @Test
    void testBuildOnSearchResponse() throws Exception {
        CacheDTO cacheDTO = new CacheDTO();
        cacheDTO.setData("{\"signature\":\"OL861jnC2zLXdmUFSZIpg4mCtMCcRcrpb9ov/AnMmsNRKQjUzkWwq3pzeZ5y3W6W4afNMDIDBSMgoHDCh19mLCSfdPHhtgGBYUymZuf0cltfv9Vc9hRSW3PRF2qc9AYgdDhU1RvaVfObiOcC5cETo0w1MZlj/SnWxAGWybxYXn1vgjFkV5JMlxh1MQ5DR5/tsnpJNUW8Y9cVaEWoOH25/0Nyv8CPwgswUO6VWVjTPLdC4wModVQIa5f1ShXjvSnrTxuOLkXy4YIzv5S+MdPyOoDRMvVmnFQkt+KemrofyDcmhXSbHXQkCCyy+ZIxYOo4/nuXPVHX2Ya6nbbmLpf4zQ==\",\"header\":{\"type\":\"requestHeader\",\"version\":\"1.0.0\",\"action\":\"search\",\"meta\":{\"data\":{\"is_sign\":true}},\"message_id\":\"M744-9925-0843-1756-7770\",\"message_ts\":\"2024-02-20T15:29:55+05:30\",\"total_count\":21800,\"sender_id\":\"spp.example.org\",\"receiver_id\":\"pymts.example.org\",\"is_msg_encrypted\":true,\"sender_uri\":\"https://spp.example.org/{namespace}/callback/on-search\"},\"message\":{\"transaction_id\":\"T129-0242-5944-2673-6476\",\"search_request\":[{\"reference_id\":\"R406-1912-4454-1914-6170\",\"timestamp\":\"2024-02-20T15:29:55+05:30\",\"search_criteria\":{\"version\":\"1.0.0\",\"reg_type\":\"ns:FARMER_REGISTRY\",\"reg_sub_type\":\"\",\"query_type\":\"namedQuery\",\"query\":{\"query_name\":\"paid_farmer\",\"query_params\":{\"farmer_id\":\"F-1\",\"season\":\"2023-xyz\",\"status\":\"\"}},\"sort\":[{\"attribute_name\":\"farmer_id\",\"sort_order\":\"asc\"}],\"pagination\":{\"page_size\":10,\"page_number\":1},\"consent\":{\"ts\":null,\"purpose\":null},\"authorize\":{\"ts\":null,\"purpose\":null}},\"locale\":\"en\"},{\"reference_id\":\"R793-1896-7749-3667-088\",\"timestamp\":\"2024-02-20T15:29:55+05:30\",\"search_criteria\":{\"version\":\"1.0.0\",\"reg_type\":\"ns:FARMER_REGISTRY\",\"reg_sub_type\":\"\",\"query_type\":\"namedQuery\",\"query\":{\"query_name\":\"paid_farmer\",\"query_params\":{\"farmer_id\":\"F-2\",\"season\":\"2023-xyz\",\"status\":\"\"}},\"sort\":[{\"attribute_name\":\"farmer_id\",\"sort_order\":\"asc\"}],\"pagination\":{\"page_size\":10,\"page_number\":1},\"consent\":{\"ts\":null,\"purpose\":null},\"authorize\":{\"ts\":null,\"purpose\":null}},\"locale\":\"en\"}]}}");
        cacheDTO.setStatus("pdng");
        cacheDTO.setProtocol("https");
        cacheDTO.setLastUpdatedDate("2024-02-20T15:29:56+05:30");
        cacheDTO.setCreatedDate("2024-02-20T15:29:56+05:30");
        List<Object> recordList = new ArrayList<>();
        recordList.add("farmer 1");
        recordList.add("farmer 2");
        G2pcError g2pcError = responseBuilderService.buildOnSearchScheduler(recordList, cacheDTO);
        assertEquals("", g2pcError.getCode());
    }

    @Test
    void testBuildCacheRequest() throws Exception {
        String requestDataString = commonUtils.readJsonFile("request.json");
        AcknowledgementDTO result = requestHandlerService.buildCacheRequest(requestDataString, "dp-farmer", "https");
        assertNotNull(result);
        log.info("result: {}", new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result));
    }
}

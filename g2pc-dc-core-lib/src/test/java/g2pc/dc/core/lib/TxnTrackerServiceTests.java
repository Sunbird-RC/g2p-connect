package g2pc.dc.core.lib;

import com.fasterxml.jackson.databind.ObjectMapper;
import g2pc.core.lib.constants.CoreConstants;
import g2pc.core.lib.constants.G2pSecurityConstants;
import g2pc.core.lib.dto.common.cache.CacheDTO;
import g2pc.core.lib.dto.common.header.HeaderDTO;
import g2pc.core.lib.dto.common.header.RequestHeaderDTO;
import g2pc.core.lib.dto.common.header.ResponseHeaderDTO;
import g2pc.core.lib.dto.search.message.response.ResponseDTO;
import g2pc.core.lib.dto.status.message.response.StatusResponseDTO;
import g2pc.core.lib.dto.status.message.response.StatusResponseMessageDTO;
import g2pc.core.lib.enums.ExceptionsENUM;
import g2pc.core.lib.enums.HeaderStatusENUM;
import g2pc.core.lib.exceptions.G2pHttpException;
import g2pc.core.lib.exceptions.G2pcError;
import g2pc.core.lib.security.service.G2pEncryptDecrypt;
import g2pc.dc.core.lib.service.TxnTrackerService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@SpringBootTest
class TxnTrackerServiceTests {

    @Autowired
    private TxnTrackerService txnTrackerService;

    @Autowired
    G2pEncryptDecrypt encryptDecrypt;

    @Disabled
    @Test
    void testCreateCache() throws IOException {
        String payloadMapListString = readJsonFile("payloadMap.json");
        ObjectMapper objectMapper = new ObjectMapper();
        CacheDTO result = txnTrackerService.createCache(
                objectMapper.writeValueAsString(payloadMapListString),
                HeaderStatusENUM.PDNG.toValue(), CoreConstants.SEND_PROTOCOL_HTTPS);
        assertNotNull(result);
        log.info("Result : {}", result);
    }

    @Disabled
    @Test
    void testSaveCache() throws IOException {
        String payloadMapListString = readJsonFile("payloadMap.json");
        ObjectMapper objectMapper = new ObjectMapper();
        CacheDTO cacheDTO = txnTrackerService.createCache(
                objectMapper.writeValueAsString(payloadMapListString),
                HeaderStatusENUM.PDNG.toValue(), CoreConstants.SEND_PROTOCOL_HTTPS);
        txnTrackerService.saveCache(cacheDTO, "testKey");
        log.info("Saved in cache");
    }

    @Disabled
    @Test
    void testSaveInitialTransaction() throws IOException {
        String payloadMapListString = readJsonFile("payloadMap.json");
        ObjectMapper objectMapper = new ObjectMapper();
        txnTrackerService.saveInitialTransaction(
                objectMapper.readValue(payloadMapListString, List.class),
                "testTransactionId",
                HeaderStatusENUM.PDNG.toValue(),
                CoreConstants.SEND_PROTOCOL_HTTPS);
        log.info("Saved in cache");
    }

    @Disabled
    @Test
    void testSaveRequestTransaction() throws IOException {
        String requestString = readJsonFile("request.json");
        txnTrackerService.saveRequestTransaction(
                requestString,
                "ns:FARMER_REGISTRY",
                "testTransactionId",
                CoreConstants.SEND_PROTOCOL_HTTPS);
        log.info("Saved in cache");
    }

    @Disabled
    @Test
    void testSaveRequestInDB() throws IOException {
        String requestString = readJsonFile("request.json");
        G2pcError g2pcErrorDb = txnTrackerService.saveRequestInDB(
                requestString,
                "ns:FARMER_REGISTRY",
                CoreConstants.SEND_PROTOCOL_HTTPS,
                new G2pcError(),
                "testPayloadFilename",
                "testInboundFilename");
        assertNotNull(g2pcErrorDb);
        log.info("Result : {}", new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(g2pcErrorDb));
    }

    @Disabled
    @Test
    void testUpdateTransactionDbAndCache() throws IOException {
        String responseString = readJsonFile("response.json");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerSubtypes(RequestHeaderDTO.class,
                ResponseHeaderDTO.class);
        G2pcError g2pcErrorDb = txnTrackerService.updateTransactionDbAndCache(
                objectMapper.readValue(responseString, ResponseDTO.class),
                "testOutboundFilename");
        assertNotNull(g2pcErrorDb);
        log.info("Result : {}", new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(g2pcErrorDb));
    }


    public String readJsonFile(String filename) throws IOException {
        File file = new File("inputfiles/" + filename);
        return new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
    }

    @Test
    public void testsaveRequestInStatusDB() throws IOException {
        String statusRequestString = "{\n" +
                "  \"signature\" : \"new signature to be generated for request\",\n" +
                "  \"header\" : {\n" +
                "    \"type\" : \"requestHeader\",\n" +
                "    \"version\" : \"1.0.0\",\n" +
                "    \"action\" : \"status\",\n" +
                "    \"meta\" : {\n" +
                "      \"data\" : { }\n" +
                "    },\n" +
                "    \"message_id\" : \"M499-3956-8341-9967-226\",\n" +
                "    \"message_ts\" : \"2024-02-21T14:46:06+05:30\",\n" +
                "    \"total_count\" : 21800,\n" +
                "    \"sender_id\" : \"spp.example.org\",\n" +
                "    \"receiver_id\" : \"pymts.example.org\",\n" +
                "    \"is_msg_encrypted\" : false,\n" +
                "    \"sender_uri\" : \"https://spp.example.org/{namespace}/callback/on-search\"\n" +
                "  },\n" +
                "  \"message\" : {\n" +
                "    \"transaction_id\" : \"T443-9999-0593-5597-6074\",\n" +
                "    \"txnstatus_request\" : {\n" +
                "      \"txn_type\" : \"search\",\n" +
                "      \"attribute_type\" : \"transaction_id\",\n" +
                "      \"attribute_value\" : \"T505-1456-4994-5937-6367\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        String regType = "ns:FARMER_REGISTRY";

        G2pcError g2pcError = txnTrackerService.saveRequestInStatusDB(statusRequestString,regType);

        assertEquals(g2pcError.getCode(), HttpStatus.OK.toString());

    }


    @Test
    public void testupdateStatusTransactionDbAndCache() throws IOException, G2pHttpException {
        // Add respective responseString
        String responseString = "{\"signature\":\"KS8gEK8YudfQ69iTtMyubcPQxbXBvMa2SDe1d7k5nIjUj05gCrADag9YocCOjUNQreeAKyilZAc4JqaacANx2tHlNPUY+nK3ylIYVuJxiu4DQGV/aqRuZtv33yiDOkJ+nUSApPdMAkWc00cF9sl3XchhFt70zxfqIzgZr1Ns1H6JsRHXXyrZbSkiMmJv3/gOqzrAEI5PNctVb5C3wc1eIP3amU2j1yO/7qIabrOE6rt5+hw1BTVYkhWCREIIzFzG9Q3KZ9vNk5PZiFlD6sRivMr4R/RVU70WMoqtbgqPwFawab2xlzK9VDB7FbOuHb2Vpo6NiUAco/sRT1ygqmXxeA==\",\"header\":{\"type\":\"responseHeader\",\"version\":\"1.0.0\",\"action\":\"status\",\"meta\":{\"data\":{\"dp_id\":\"FARMER\",\"is_sign\":true}},\"status\":null,\"message_id\":\"M437-8300-7639-3772-1010\",\"message_ts\":\"2024-02-21T15:22:21+05:30\",\"total_count\":null,\"sender_id\":\"spp.example.org\",\"receiver_id\":\"pymts.example.org\",\"is_msg_encrypted\":true,\"status_reason_code\":null,\"status_reason_message\":null,\"completed_count\":null},\"message\":\"/djhoS6imljAH9HFAHPVPQ==:IeMfbpoPwRjA6H/3SGwsQ2LzzlC08HUZ6z+UgynOxzwY+B449f+NAznJ5ZwO6Wdq+gldsys/I9Ui23O1/6Y+gQb+cDQxtvcH9WhD8KQfOoWrfGmEnHrtJyQPXl3U+VtoG0iCXkyI3FUNkA7Q+axFPEZxc2wkHvgcgszopFumMnKvEVFTEB+YULj7W8Y8L6KcFFJJGZL/V1SeKk4aVYtO24yd+XIxNV3DeBIq4v+s/8uAgAVmV4seGLjqOkdV00nTFsZzSGuA9KFAsMXDDb0zZ/I9WyfmN/0VGnrf6xPGe3Rssa5XgNO2y4QidtVpmjw8YJyT+okCn3LLPhBSdFO85dldVBBoZk1l2Bu3LnR+vlqKONPMsCUEoX0YZLYqaV0TComUyhtV6m5mwYtKWam1dWxQ7+B/UHdpe5BCbeDjv13GljKmQqpf/kiWn6I0YzjNjwAvg0nVO2rIVVONhQukTsuMcvVYu0Ssd4b8RxZMQJgTjs9B9A9tf00Hc/WvsLeY33nEjCnBiyraiMtK8f1n2sMrIpirs4PPK8LbcEiuGZ2FXal0Xtv6SPsYIu/KQgYuinrV/yJ0nphhMJ678oJ+5pjVkszYDhIqQoI6SlnXy9HaLq/IQtV19+uMoOgRcMasH+m6MbzAyDj8y6SZeRxdjdsa3dHYxJSHSavWbCozqi5E5Bt1GAmm3yQz75Yc7xBA/iCnz8/s7/QAtJiJmToWRZwg32km0i1LBMdz7gcpI4lesgZYzFB1SxMRtqfcjY0gYoTB8FdyWOrcmqOpMpIblxvD6YN/dd/2evrJ8mbqZPHjfxMHVGTtO333U69q3X6R0E5lrhkXk30bHF3T+66iOjsa7Y3NToDcUWQ7GcbaI49KZd/UszpP2KNuMIL0rtY1UoFonh5RU1BAZ/7OE9qG8Bxj/6zQbEnqnm+b1OWHbZGtyK40bKC9QR5dXXNM4AUbANREDbiTN4NBgw7qNj7SF7dx7zhpcp6ZjxM+GUuzKIN5WWe7tqJ3EhIj6y0nAhm8utwUf4H0EzuCFVpxlwdc40yn9R2f5JMgrnlRW4L0cLkuVljCQw2mDmqqfofCda1tnQN3h3v9xO1p8tc2bklTLn2oCBUiNNk7wIh2u9+XM0Wl/4lKrSomsknb4k1NjOBCTXPNVCxG16rCDXjRa8KA2HRuJ4p8nUixcOzHdMPeAVcwLcDRs2vYryt3U/C2nNQW\"}";
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerSubtypes(RequestHeaderDTO.class,
                ResponseHeaderDTO.class, HeaderDTO.class);
        StatusResponseDTO statusResponseDTO = objectMapper.readerFor(StatusResponseDTO.class).
                readValue(responseString);
        String deprecatedMessageString = null;
        try{
            String messageString = statusResponseDTO.getMessage().toString();
            deprecatedMessageString= encryptDecrypt.g2pDecrypt(messageString, G2pSecurityConstants.SECRET_KEY);
        } catch (Exception e ){
            throw new G2pHttpException(new G2pcError(ExceptionsENUM.ERROR_ENCRYPTION_INVALID.toValue(),"Error in Encryption/Decryption"));
        }
        log.info("Decrypted Message string ->"+deprecatedMessageString);
        StatusResponseMessageDTO messageDTO  = objectMapper.readerFor(StatusResponseMessageDTO.class).
                readValue(deprecatedMessageString);
        statusResponseDTO.setMessage(messageDTO);
        G2pcError g2pcError = txnTrackerService.updateStatusTransactionDbAndCache(statusResponseDTO);
        assertEquals(g2pcError.getCode(), HttpStatus.OK.toString());
    }

}
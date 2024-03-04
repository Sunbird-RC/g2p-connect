package g2pc.dp.core.lib.serviceimpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import g2pc.core.lib.config.G2pUnirestHelper;
import g2pc.core.lib.constants.CoreConstants;
import g2pc.core.lib.constants.G2pSecurityConstants;
import g2pc.core.lib.constants.SftpConstants;
import g2pc.core.lib.dto.common.cache.CacheDTO;
import g2pc.core.lib.dto.common.header.HeaderDTO;
import g2pc.core.lib.dto.common.header.MetaDTO;
import g2pc.core.lib.dto.common.header.RequestHeaderDTO;
import g2pc.core.lib.dto.common.header.ResponseHeaderDTO;
import g2pc.core.lib.dto.common.security.G2pTokenResponse;
import g2pc.core.lib.dto.common.security.TokenExpiryDto;
import g2pc.core.lib.dto.search.message.request.RequestDTO;
import g2pc.core.lib.dto.search.message.request.RequestMessageDTO;
import g2pc.core.lib.dto.search.message.request.ResponseMessageDTO;
import g2pc.core.lib.dto.search.message.response.ResponseDTO;
import g2pc.core.lib.dto.search.message.response.SearchResponseDTO;
import g2pc.core.lib.dto.sftp.SftpServerConfigDTO;
import g2pc.core.lib.dto.status.message.request.StatusRequestDTO;
import g2pc.core.lib.dto.status.message.request.StatusRequestMessageDTO;
import g2pc.core.lib.dto.status.message.response.StatusResponseDTO;
import g2pc.core.lib.dto.status.message.response.StatusResponseMessageDTO;
import g2pc.core.lib.enums.ExceptionsENUM;
import g2pc.core.lib.exceptions.G2pHttpException;
import g2pc.core.lib.exceptions.G2pcError;
import g2pc.core.lib.security.service.AsymmetricSignatureService;
import g2pc.core.lib.security.service.G2pEncryptDecrypt;
import g2pc.core.lib.security.service.G2pTokenService;
import g2pc.core.lib.service.ElasticsearchService;
import g2pc.core.lib.service.SftpHandlerService;
import g2pc.core.lib.utils.CommonUtils;
import g2pc.dp.core.lib.dto.MsgTrackerDTO;
import g2pc.dp.core.lib.service.ResponseBuilderService;
import g2pc.dp.core.lib.service.TxnTrackerDbService;
import g2pc.dp.core.lib.utils.DpCommonUtils;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

@Service
@Slf4j
public class ResponseBuilderServiceImpl implements ResponseBuilderService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${crypto.to_dc.support_encryption}")
    private boolean isEncrypt;

    @Value("${crypto.to_dc.support_signature}")
    private boolean isSign;

    @Value("${crypto.to_dc.password}")
    private String p12Password;

    @Value("${crypto.to_dc.id}")
    private String dpId;

    @Value("${crypto.to_dc.key_path}")
    private String farmerKeyPath;

    @Value("${client.api_urls.client_search_api}")
    String onSearchURL;

    @Value("${client.api_urls.client_status_api}")
    String onStatusURL;

    @Value("${keycloak.from-dc.client-id}")
    private String dcClientId;

    @Value("${keycloak.from-dc.client-secret}")
    private String dcClientSecret;

    @Value("${keycloak.from-dc.url}")
    private String keyClockClientTokenUrl;

    @Autowired
    G2pUnirestHelper g2pUnirestHelper;

    @Autowired
    G2pEncryptDecrypt encryptDecrypt;

    @Autowired
    G2pTokenService g2pTokenService;

    @Autowired
    AsymmetricSignatureService asymmetricSignatureService;

    @Autowired
    TxnTrackerDbService txnTrackerDbService;

    @Autowired
    private SftpHandlerService sftpHandlerService;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private DpCommonUtils dpCommonUtils;

    @Autowired
    private ElasticsearchService elasticsearchService;

    /**
     * Get response header for sunbird
     *
     * @param msgTrackerDTO msgTrackerEntity used to create ResponseHeaderFto
     * @return ResponseHeaderDTO
     */
    @Override
    public ResponseHeaderDTO getResponseHeaderDTOForSunbird(MsgTrackerDTO msgTrackerDTO) {
        ResponseHeaderDTO responseHeaderDTO = new ResponseHeaderDTO();
        responseHeaderDTO.setVersion(msgTrackerDTO.getVersion());
        responseHeaderDTO.setMessageId(msgTrackerDTO.getMessageId());
        responseHeaderDTO.setMessageTs(msgTrackerDTO.getMessageTs());
        responseHeaderDTO.setAction(msgTrackerDTO.getAction());
        responseHeaderDTO.setSenderId(msgTrackerDTO.getSenderId());
        responseHeaderDTO.setReceiverId(msgTrackerDTO.getReceiverId());
        responseHeaderDTO.setIsMsgEncrypted(msgTrackerDTO.getIsMsgEncrypted());
        responseHeaderDTO.setStatus(msgTrackerDTO.getStatus());
        responseHeaderDTO.setStatusReasonCode(msgTrackerDTO.getStatusReasonCode());
        responseHeaderDTO.setStatusReasonMessage(msgTrackerDTO.getStatusReasonMessage());
        responseHeaderDTO.setTotalCount(msgTrackerDTO.getTotalCount());
        responseHeaderDTO.setCompletedCount(msgTrackerDTO.getCompletedCount());
        Map<String, Object> metaMap = new HashMap<>();
        MetaDTO metaDTO = new MetaDTO();
        metaDTO.setData(metaMap);
        responseHeaderDTO.setMeta(metaDTO);
        return responseHeaderDTO;
    }

    /**
     * Build a response message
     *
     * @param transactionId         transactionId to build response message
     * @param searchResponseDTOList list of searchResponseDto to store in response message
     * @return ResponseMessageDTO
     */
    @Override
    public ResponseMessageDTO buildResponseMessage(String transactionId, List<SearchResponseDTO> searchResponseDTOList) {
        ResponseMessageDTO messageDTO = new ResponseMessageDTO();
        messageDTO.setTransactionId(transactionId);
        messageDTO.setCorrelationId(CommonUtils.generateUniqueId("C"));
        messageDTO.setSearchResponse(searchResponseDTOList);
        return messageDTO;
    }

    /**
     * Build a response string
     *
     * @param signatureString   signature to store
     * @param responseHeaderDTO response header
     * @param messageDTO        message
     * @return response String
     */
    @Override
    public String buildResponseString(String signatureString, ResponseHeaderDTO responseHeaderDTO,
                                      ResponseMessageDTO messageDTO) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        ResponseDTO responseDTO = new ResponseDTO();
        responseDTO.setSignature(signatureString);
        responseDTO.setHeader(responseHeaderDTO);
        responseDTO.setMessage(messageDTO);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseDTO);
    }

    /**
     * @param responseString         response string to store
     * @param uri                    endpoint for transaction
     * @param clientId               keycloak clientId
     * @param clientSecret           keycloak clientSecret
     * @param keyClockClientTokenUrl keycloak ClientTokenUrl
     * @param fis                    .p12 file input stream
     * @param txnType                txnType
     * @return G2pcError
     * @throws Exception
     */
    @Override
    public G2pcError sendOnSearchResponse(String responseString, String uri, String clientId, String clientSecret, String keyClockClientTokenUrl
            , InputStream fis, String txnType) throws Exception {
        log.info("Send on-search response");
        log.info("Is encrypted ? -> " + isEncrypt);
        log.info("Is signed ? -> " + isSign);
        responseString = createSignature(isEncrypt, isSign, responseString, fis, txnType);
        String jwtToken = getValidatedToken(keyClockClientTokenUrl, clientId, clientSecret);
        HttpResponse<String> response = g2pUnirestHelper.g2pPost(uri)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtToken)
                .body(responseString)
                .asString();
        log.info(txnType + "Response status = {}", response.getStatus());
        if (response.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            return new G2pcError(HttpStatus.INTERNAL_SERVER_ERROR.toString(), response.getBody());
        } else if (response.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
            return new G2pcError(HttpStatus.UNAUTHORIZED.toString(), response.getBody());
        } else if (response.getStatus() == HttpStatus.BAD_REQUEST.value()) {
            return new G2pcError(HttpStatus.BAD_REQUEST.toString(), response.getBody());
        } else if (response.getStatus() != HttpStatus.OK.value()) {
            return new G2pcError("err.service.unavailable", response.getBody());
        }
        return new G2pcError(HttpStatus.OK.toString(), response.getBody());
    }

    /**
     * Method to store token in cache
     *
     * @param cacheKey       cacheKey cache key for which data is storing
     * @param tokenExpiryDto token expiry dto
     * @throws JsonProcessingException might be thrown
     */
    @Override
    public void saveToken(String cacheKey, TokenExpiryDto tokenExpiryDto) throws JsonProcessingException {
        ValueOperations<String, String> val = redisTemplate.opsForValue();
        val.set(cacheKey, new ObjectMapper().writeValueAsString(tokenExpiryDto));
    }

    /**
     * Method to get token stored in cache
     *
     * @param clientId client Id
     * @return TokenExpiryDto tokenExpiryDto
     * @throws JsonProcessingException might be thrown
     */
    @Override
    public TokenExpiryDto getTokenFromCache(String clientId) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Set<String> redisKeys = this.redisTemplate.keys(clientId);
        List<String> cacheKeysList = new ArrayList((Collection) Objects.requireNonNull(redisKeys));
        if (!cacheKeysList.isEmpty()) {
            String cacheKey = cacheKeysList.get(0);
            String tokenData = (String) this.redisTemplate.opsForValue().get(cacheKey);
            TokenExpiryDto tokenExpiryDto = objectMapper.readerFor(TokenExpiryDto.class).readValue(tokenData);
            return tokenExpiryDto;
        }
        return null;
    }

    /**
     * The method to get validated token
     *
     * @param keyCloakUrl  keycloak url to validate token
     * @param clientId     client id
     * @param clientSecret client secret
     * @return String validated token
     * @throws IOException      IOException might be thrown
     * @throws ParseException   ParseException might be thrown
     * @throws UnirestException UnirestException might be thrown
     */
    @Override
    public String getValidatedToken(String keyCloakUrl, String clientId, String clientSecret) throws IOException, ParseException, UnirestException {
        TokenExpiryDto tokenExpiryDto = getTokenFromCache(clientId);

        String jwtToken = "";
        if (g2pTokenService.isTokenExpired(tokenExpiryDto)) {
            G2pTokenResponse tokenResponse = g2pTokenService.getToken(keyCloakUrl, clientId, clientSecret);
            jwtToken = tokenResponse.getAccessToken();
            saveToken(clientId, g2pTokenService.createTokenExpiryDto(tokenResponse));
        } else {
            jwtToken = tokenExpiryDto.getToken();
        }
        return jwtToken;
    }

    @Override
    public StatusResponseMessageDTO buildStatusResponseMessage(StatusRequestMessageDTO statusRequestMessageDTO) {
        return null;
    }

    /**
     * The method is to create signature
     *
     * @param isSign         signature flag
     * @param isEncrypt      encryption flag
     * @param responseString response string
     * @return String signature
     * @throws Exception Exception might be thrown
     */
    @SuppressWarnings("unchecked")
    private String createSignature(boolean isEncrypt, boolean isSign, String responseString
            , InputStream fis, String txnType) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerSubtypes(RequestHeaderDTO.class,
                ResponseHeaderDTO.class, HeaderDTO.class);
        ResponseDTO responseDTO = objectMapper.readerFor(ResponseDTO.class).
                readValue(responseString);
        byte[] json = objectMapper.writeValueAsBytes(responseDTO.getMessage());
        ResponseHeaderDTO responseHeaderDTO = (ResponseHeaderDTO) responseDTO.getHeader();
        String responseHeaderString = objectMapper.writeValueAsString(responseHeaderDTO);
        String signature = null;
        String messageString = "";
        if (txnType.equals(CoreConstants.DP_STATUS_URL)) {
            StatusResponseMessageDTO messageDTO = objectMapper.readValue(json, StatusResponseMessageDTO.class);
            messageString = objectMapper.writeValueAsString(messageDTO);
        } else {
            ResponseMessageDTO messageDTO = objectMapper.readValue(json, ResponseMessageDTO.class);
            messageString = objectMapper.writeValueAsString(messageDTO);
        }

        if (isSign) {
            if (isEncrypt) {
                String encryptedMessageString = encryptDecrypt.g2pEncrypt(messageString, G2pSecurityConstants.SECRET_KEY);
                responseDTO.setMessage(encryptedMessageString);
                responseDTO.getHeader().setIsMsgEncrypted(true);
                Map<String, Object> meta = (Map<String, Object>) responseDTO.getHeader().getMeta().getData();
                meta.put(CoreConstants.IS_SIGN, true);
                responseDTO.getHeader().getMeta().setData(meta);
                responseHeaderString = objectMapper.writeValueAsString(responseDTO.getHeader());
                byte[] asymmetricSignature = asymmetricSignatureService.sign(responseHeaderString + encryptedMessageString, fis, p12Password);
                signature = Base64.getEncoder().encodeToString(asymmetricSignature);
                log.info("Encrypted message ->" + encryptedMessageString);
                log.info("Hashed Signature ->" + signature);
            } else {
                responseDTO.getHeader().setIsMsgEncrypted(false);
                Map<String, Object> meta = (Map<String, Object>) responseDTO.getHeader().getMeta().getData();
                meta.put(CoreConstants.IS_SIGN, true);
                responseDTO.getHeader().getMeta().setData(meta);
                responseHeaderString = objectMapper.writeValueAsString(responseDTO.getHeader());
                byte[] asymmetricSignature = asymmetricSignatureService.sign(responseHeaderString + messageString, fis, p12Password);
                signature = Base64.getEncoder().encodeToString(asymmetricSignature);
                log.info("Hashed Signature ->" + signature);
            }
        } else {
            if (isEncrypt) {
                String encryptedMessageString = encryptDecrypt.g2pEncrypt(messageString, G2pSecurityConstants.SECRET_KEY);
                responseDTO.setMessage(encryptedMessageString);
                responseDTO.getHeader().setIsMsgEncrypted(true);
                Map<String, Object> meta = (Map<String, Object>) responseDTO.getHeader().getMeta().getData();
                meta.put(CoreConstants.IS_SIGN, false);
                responseDTO.getHeader().getMeta().setData(meta);
                log.info("Encrypted message ->" + encryptedMessageString);
            } else {
                responseDTO.getHeader().setIsMsgEncrypted(false);
                Map<String, Object> meta = (Map<String, Object>) responseDTO.getHeader().getMeta().getData();
                meta.put(CoreConstants.IS_SIGN, false);
                responseDTO.getHeader().getMeta().setData(meta);
            }
        }
        responseDTO.setSignature(signature);
        responseString = objectMapper.writeValueAsString(responseDTO);
        return responseString;
    }

    /**
     * @param signatureString          signature to be added in request
     * @param responseHeaderDTO        header dto ti be added in request
     * @param statusResponseMessageDTO response message dto to be added
     * @return response dto
     * @throws JsonProcessingException jsonProcessingException might be thrown
     */
    @Override
    public String buildStatusResponseString(String signatureString, ResponseHeaderDTO responseHeaderDTO, StatusResponseMessageDTO statusResponseMessageDTO) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        StatusResponseDTO statusResponseDTO = new StatusResponseDTO();
        statusResponseDTO.setSignature(signatureString);
        statusResponseDTO.setHeader(responseHeaderDTO);
        statusResponseDTO.setMessage(statusResponseMessageDTO);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(statusResponseDTO);
    }

    /**
     * The method is to send on search response using sftp
     *
     * @param responseString required
     * @return String
     * @throws Exception jsonProcessingException might be thrown
     */
    @Override
    public G2pcError sendOnSearchResponseSftp(String responseString, InputStream fis,
                                              String txnType, SftpServerConfigDTO sftpServerConfigDTO) throws Exception {
        log.info("Send on-search response");
        log.info("Is encrypted ? -> " + isEncrypt);
        log.info("Is signed ? -> " + isSign);
        responseString = createSignature(isEncrypt, isSign, responseString, fis, txnType);

        String originalFilename = UUID.randomUUID() + ".json";
        Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), originalFilename);
        Files.createFile(tempFile);
        Files.write(tempFile, responseString.getBytes());

        Boolean status = sftpHandlerService.uploadFileToSftp(sftpServerConfigDTO, tempFile.toString(),
                sftpServerConfigDTO.getRemoteOutboundDirectory());
        Files.delete(tempFile);
        G2pcError g2pcError;
        if (Boolean.FALSE.equals(status)) {
            g2pcError = new G2pcError(ExceptionsENUM.ERROR_SERVICE_UNAVAILABLE.toValue(), SftpConstants.UPLOAD_ERROR_MESSAGE);
            log.error(SftpConstants.UPLOAD_ERROR_MESSAGE);
        } else {
            g2pcError = new G2pcError(HttpStatus.OK.toString(), SftpConstants.UPLOAD_SUCCESS_MESSAGE);
        }
        return g2pcError;
    }


    @SuppressWarnings("unchecked")
    @Override
    public G2pcError buildOnSearchScheduler(List<Object> refRecordsStringsList, CacheDTO cacheDTO) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerSubtypes(RequestHeaderDTO.class, ResponseHeaderDTO.class);
        G2pcError g2pcError = new G2pcError();
        String protocol = cacheDTO.getProtocol();
        RequestDTO requestDTO = objectMapper.readerFor(RequestDTO.class).readValue(cacheDTO.getData());
        RequestMessageDTO messageDTO = objectMapper.convertValue(requestDTO.getMessage(), RequestMessageDTO.class);
        String transactionId = messageDTO.getTransactionId();
        txnTrackerDbService.saveRequestDetails(requestDTO, protocol);
        List<SearchResponseDTO> searchResponseDTOList = txnTrackerDbService.getUpdatedSearchResponseList(
                requestDTO, refRecordsStringsList, protocol);
        ResponseHeaderDTO headerDTO = null;
                    Map<String, String> msgTrackerFieldValues = new HashMap<>();
            msgTrackerFieldValues.put("transaction_id.keyword", transactionId);
            SearchResponse msgTrackerSearchResponse = elasticsearchService.exactSearch("msg_tracker", msgTrackerFieldValues);
            Map<String, Object> msgTrackerResultMap = msgTrackerSearchResponse.getHits().getHits()[0].getSourceAsMap();
            MsgTrackerDTO msgTrackerDTO = objectMapper.convertValue(msgTrackerResultMap, MsgTrackerDTO.class);
            headerDTO = getResponseHeaderDTOForSunbird(msgTrackerDTO);

        ResponseMessageDTO responseMessageDTO = buildResponseMessage(transactionId, searchResponseDTOList);
        Map<String, Object> meta = (Map<String, Object>) headerDTO.getMeta().getData();
        meta.put(CoreConstants.DP_ID, dpId);
        requestDTO.getHeader().getMeta().setData(meta);
        String responseString = buildResponseString("signature",
                headerDTO, responseMessageDTO);
        responseString = CommonUtils.formatString(responseString);
        log.info("on-search response = {}", responseString);
        Resource resource = resourceLoader.getResource(farmerKeyPath);
        InputStream fis = resource.getInputStream();

        if (protocol.equals(CoreConstants.SEND_PROTOCOL_HTTPS)) {
            g2pcError = sendOnSearchResponse(responseString, onSearchURL, dcClientId,
                    dcClientSecret, keyClockClientTokenUrl, fis, CoreConstants.SEARCH_TXN_TYPE);
        } else if (protocol.equals(CoreConstants.SEND_PROTOCOL_SFTP)) {
            SftpServerConfigDTO sftpServerConfigDTO = dpCommonUtils.getSftpConfigForDp();
            g2pcError = sendOnSearchResponseSftp(responseString, fis,
                    CoreConstants.SEARCH_TXN_TYPE, sftpServerConfigDTO);
        }
        if (!Objects.requireNonNull(g2pcError).getCode().equals(HttpStatus.OK.toString())) {
            throw new G2pHttpException(g2pcError);
        }
        return g2pcError;
    }
}

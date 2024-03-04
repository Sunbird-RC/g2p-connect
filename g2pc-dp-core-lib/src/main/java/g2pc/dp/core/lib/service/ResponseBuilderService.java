package g2pc.dp.core.lib.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import g2pc.core.lib.dto.common.cache.CacheDTO;
import g2pc.core.lib.dto.common.header.ResponseHeaderDTO;
import g2pc.core.lib.dto.common.security.TokenExpiryDto;
import g2pc.core.lib.dto.search.message.request.ResponseMessageDTO;
import g2pc.core.lib.dto.search.message.response.SearchResponseDTO;
import g2pc.core.lib.dto.sftp.SftpServerConfigDTO;
import g2pc.core.lib.dto.status.message.request.StatusRequestMessageDTO;
import g2pc.core.lib.dto.status.message.response.StatusResponseMessageDTO;
import g2pc.core.lib.exceptions.G2pcError;
import g2pc.dp.core.lib.dto.MsgTrackerDTO;
import kong.unirest.UnirestException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;

public interface ResponseBuilderService {

    ResponseMessageDTO buildResponseMessage(String transactionId, List<SearchResponseDTO> searchResponseDTOList);

    ResponseHeaderDTO getResponseHeaderDTOForSunbird(MsgTrackerDTO msgTrackerDTO);

    String buildResponseString(String signatureString, ResponseHeaderDTO responseHeaderDTO, ResponseMessageDTO messageDTO) throws JsonProcessingException;

    G2pcError sendOnSearchResponse(String responseString, String uri, String clientId, String clientSecret, String keyClockClientTokenUrl, InputStream fis, String txnType) throws Exception;

    void saveToken(String cacheKey, TokenExpiryDto tokenExpiryDto) throws JsonProcessingException;

    TokenExpiryDto getTokenFromCache(String clientId) throws JsonProcessingException;

    String getValidatedToken(String keyCloakUrl, String clientId, String clientSecret) throws IOException, UnirestException, ParseException;

    StatusResponseMessageDTO buildStatusResponseMessage(StatusRequestMessageDTO statusRequestMessageDTO);

    String buildStatusResponseString(String signatureString, ResponseHeaderDTO responseHeaderDTO, StatusResponseMessageDTO statusResponseMessageDTO) throws JsonProcessingException;

    G2pcError sendOnSearchResponseSftp(String responseString, InputStream fis, String txnType, SftpServerConfigDTO sftpServerConfigDTO) throws Exception;

    G2pcError buildOnSearchScheduler(List<Object> refRecordsStringsList, CacheDTO cacheDTO) throws Exception;
}
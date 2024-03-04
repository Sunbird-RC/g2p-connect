package g2pc.dp.core.lib.serviceimpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import g2pc.core.lib.dto.common.header.HeaderDTO;
import g2pc.core.lib.dto.common.header.RequestHeaderDTO;
import g2pc.core.lib.dto.common.header.ResponseHeaderDTO;
import g2pc.core.lib.dto.search.message.request.*;
import g2pc.core.lib.dto.search.message.response.DataDTO;
import g2pc.core.lib.dto.search.message.response.ResponsePaginationDTO;
import g2pc.core.lib.dto.search.message.response.SearchResponseDTO;
import g2pc.core.lib.enums.ExceptionsENUM;
import g2pc.core.lib.enums.HeaderStatusENUM;
import g2pc.core.lib.enums.LocalesENUM;
import g2pc.core.lib.enums.QueryTypeEnum;
import g2pc.core.lib.exceptions.G2pcError;
import g2pc.core.lib.service.ElasticsearchService;
import g2pc.core.lib.utils.CommonUtils;
import g2pc.dp.core.lib.constants.DpConstants;
import g2pc.dp.core.lib.dto.MsgTrackerDTO;
import g2pc.dp.core.lib.dto.TxnTrackerDTO;
import g2pc.dp.core.lib.service.TxnTrackerDbService;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@Slf4j
public class TxnTrackerDbServiceImpl implements TxnTrackerDbService {

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Value("${sunbird.api_urls.msg_tracker_api}")
    private String msgTrackerURL;

    @Value("${sunbird.api_urls.txn_tracker_api}")
    private String txnTrackerURL;

    /**
     * Save request details
     *
     * @param requestDTO requestDTO to save in Db
     * @return request details entity
     */
    @Override
    public G2pcError saveRequestDetails(RequestDTO requestDTO, String protocol) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerSubtypes(RequestHeaderDTO.class, ResponseHeaderDTO.class);

        HeaderDTO headerDTO = requestDTO.getHeader();
        RequestMessageDTO messageDTO = objectMapper.convertValue(requestDTO.getMessage(), RequestMessageDTO.class);
        String transactionId = messageDTO.getTransactionId();
        G2pcError g2pcError = new G2pcError(HttpStatus.OK.toString(), "Successfully saved data in DB");
        try {
            Map<String, String> fieldValues = new HashMap<>();
            fieldValues.put("transaction_id.keyword", transactionId);
            SearchResponse response = elasticsearchService.exactSearch("msg_tracker", fieldValues);
            if (response.getHits().getTotalHits().value > 0) {
                log.info("response found:");
            } else {
                MsgTrackerDTO msgTrackerDTO = new MsgTrackerDTO();
                msgTrackerDTO.setVersion(headerDTO.getVersion());
                msgTrackerDTO.setMessageId(headerDTO.getMessageId());
                msgTrackerDTO.setMessageTs(headerDTO.getMessageTs());
                msgTrackerDTO.setAction(headerDTO.getAction());
                msgTrackerDTO.setSenderId(headerDTO.getSenderId());
                msgTrackerDTO.setReceiverId(headerDTO.getReceiverId());
                msgTrackerDTO.setIsMsgEncrypted(headerDTO.getIsMsgEncrypted());
                msgTrackerDTO.setTransactionId(messageDTO.getTransactionId());
                msgTrackerDTO.setRawMessage(objectMapper.writeValueAsString(requestDTO));
                msgTrackerDTO.setProtocol(protocol);
                List<SearchRequestDTO> searchRequestDTOList = messageDTO.getSearchRequest();
                for (SearchRequestDTO searchRequestDTO : searchRequestDTOList) {
                    TxnTrackerDTO txnTrackerDTO = new TxnTrackerDTO();
                    txnTrackerDTO.setRegistryTransactionsId(transactionId);
                    txnTrackerDTO.setReferenceId(searchRequestDTO.getReferenceId());
                    txnTrackerDTO.setTimestamp(searchRequestDTO.getTimestamp());
                    txnTrackerDTO.setVersion(searchRequestDTO.getSearchCriteria().getVersion());
                    txnTrackerDTO.setRegType(searchRequestDTO.getSearchCriteria().getRegType());
                    txnTrackerDTO.setRegSubType(searchRequestDTO.getSearchCriteria().getRegSubType());
                    txnTrackerDTO.setQueryType(searchRequestDTO.getSearchCriteria().getQueryType());
                    txnTrackerDTO.setQuery(objectMapper.writeValueAsString(searchRequestDTO.getSearchCriteria().getQuery()));
                    txnTrackerDTO.setCreatedDate(CommonUtils.getCurrentTimeStamp());
                    txnTrackerDTO.setLastUpdatedDate(CommonUtils.getCurrentTimeStamp());
                    String txnDataString = objectMapper.writeValueAsString(txnTrackerDTO);
                    HttpResponse<JsonNode> txnDataResponse = Unirest.post(txnTrackerURL)
                            .header("Content-Type", "application/json")
                            .body(txnDataString)
                            .asJson();
                    log.info("txnDataResponse : {} ", txnDataResponse);
                    if (txnDataResponse.getStatus() != 200) {
                        g2pcError = new G2pcError(ExceptionsENUM.ERROR_SERVICE_UNAVAILABLE.toValue(), txnDataResponse.getBody().toString());
                    }
                }
                String msgTrackerString = objectMapper.writeValueAsString(msgTrackerDTO);
                HttpResponse<JsonNode> msgTrackerResponse = Unirest.post(msgTrackerURL)
                        .header("Content-Type", "application/json")
                        .body(msgTrackerString)
                        .asJson();
                if (msgTrackerResponse.getStatus() != 200) {
                    g2pcError = new G2pcError(ExceptionsENUM.ERROR_SERVICE_UNAVAILABLE.toValue(), msgTrackerResponse.getBody().toString());
                }
                log.info("MsgTracker save response :{} ", response);
            }
        } catch (Exception e) {
            g2pcError = new G2pcError(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Error while saving data in DB");
        }
        return g2pcError;
    }

    /**
     * Get record count
     *
     * @param records required
     * @return record count
     */
    @SuppressWarnings("unchecked")
    @Override
    public int getRecordCount(Object records) {
        Map<String, Object> objectMap = new ObjectMapper().convertValue(records, Map.class);
        return objectMap.size();
    }

    /**
     * Build a search response for sunbird
     *
     * @param txnTrackerDTO txnTrackerEntity to build search response
     * @param dataDTO       dataDTO to build search response dto
     * @return SearchResponseDTO
     */
    @Override
    public SearchResponseDTO buildSearchResponseForSunbird(TxnTrackerDTO txnTrackerDTO, DataDTO dataDTO) {
        ResponsePaginationDTO paginationDTO = new ResponsePaginationDTO();
        paginationDTO.setPageSize(10);
        paginationDTO.setPageNumber(1);
        paginationDTO.setTotalCount(100);

        SearchResponseDTO searchResponseDTO = new SearchResponseDTO();
        searchResponseDTO.setReferenceId(txnTrackerDTO.getReferenceId());
        searchResponseDTO.setTimestamp(txnTrackerDTO.getTimestamp());
        searchResponseDTO.setStatus(txnTrackerDTO.getStatus());
        searchResponseDTO.setStatusReasonCode(txnTrackerDTO.getStatusReasonCode());
        searchResponseDTO.setStatusReasonMessage(txnTrackerDTO.getStatusReasonMessage());
        searchResponseDTO.setData(dataDTO);
        searchResponseDTO.setPagination(paginationDTO);
        searchResponseDTO.setLocale(LocalesENUM.EN.toValue());

        return searchResponseDTO;
    }

    @Override
    public void updateStatusResponseDetails(ResponseMessageDTO responseMessageDTO, String transactionId) {

    }

    @Override
    public void updateMessageTrackerStatusDb(String transactionId) {

    }

    /**
     * Build data for sunbird
     *
     * @param regRecordsString regRecord to store in data dto
     * @return DataDTO
     */
    @Override
    public DataDTO buildDataForSunbird(String regRecordsString, TxnTrackerDTO txnTrackerDTO) {
        DataDTO dataDTO = new DataDTO();
        dataDTO.setVersion(txnTrackerDTO.getVersion());
        dataDTO.setRegType(txnTrackerDTO.getRegType());
        dataDTO.setRegSubType(txnTrackerDTO.getRegSubType());
        dataDTO.setRegRecordType(txnTrackerDTO.getRegRecordType());
        if (StringUtils.isNotEmpty(regRecordsString)) {
            dataDTO.setRegRecords(regRecordsString);
        } else {
            dataDTO.setRegRecords(null);
        }
        return dataDTO;
    }

    /**
     * Get updated search response list
     *
     * @param requestDTO            request dto to be updated
     * @param refRecordsStringsList list of records
     * @return updated search response list
     */
    @Override
    public List<SearchResponseDTO> getUpdatedSearchResponseList(RequestDTO requestDTO,
                                                                List<Object> refRecordsStringsList,
                                                                String protocol) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerSubtypes(RequestHeaderDTO.class, ResponseHeaderDTO.class);
        List<SearchResponseDTO> searchResponseDTOList = new ArrayList<>();
        saveRequestDetails(requestDTO, protocol);
        RequestMessageDTO messageDTO = objectMapper.convertValue(requestDTO.getMessage(), RequestMessageDTO.class);
        String transactionId = messageDTO.getTransactionId();
        Map<String, String> msgTrackerFieldValues = new HashMap<>();
        msgTrackerFieldValues.put("transaction_id.keyword", transactionId);
        SearchResponse msgTrackerSearchResponse = elasticsearchService.exactSearch("msg_tracker", msgTrackerFieldValues);
        if (msgTrackerSearchResponse.getHits().getTotalHits().value > 0) {
            Map<String, Object> msgTrackerResultMap = msgTrackerSearchResponse.getHits().getHits()[0].getSourceAsMap();
            String msgTrackerOsid = msgTrackerResultMap.get(DpConstants.OSID).toString().substring(2);

            Map<String, String> txnTrackerFieldValues = new HashMap<>();
            txnTrackerFieldValues.put("registry_transactions_id.keyword", transactionId);
            SearchResponse txnTrackerSearchResponse = elasticsearchService.exactSearch("txn_tracker", txnTrackerFieldValues);
            long totalCount = txnTrackerSearchResponse.getHits().getTotalHits().value;
            int completedCount = 0;
            int index = 0;
            if (totalCount > 0) {
                SearchHits hits = txnTrackerSearchResponse.getHits();
                for (SearchHit hit : hits) {
                    String refRecordsString = refRecordsStringsList.get(index).toString();
                    Map<String, Object> txnTrackerMap = hit.getSourceAsMap();
                    String txnTrackerOsid = txnTrackerMap.get(DpConstants.OSID).toString().substring(2);
                    TxnTrackerDTO txnTrackerDTO = objectMapper.convertValue(txnTrackerMap, TxnTrackerDTO.class);
                    txnTrackerDTO.setConsent(true);
                    txnTrackerDTO.setAuthorize(true);
                    txnTrackerDTO.setRegRecordType(QueryTypeEnum.NAMEDQUERY.toValue());
                    DataDTO dataDTO = buildDataForSunbird(refRecordsString, txnTrackerDTO);
                    if (refRecordsString.isEmpty()) {
                        txnTrackerDTO.setStatus(HeaderStatusENUM.RJCT.toValue());
                        txnTrackerDTO.setStatusReasonCode(DpConstants.RECORD_NOT_FOUND);
                        txnTrackerDTO.setStatusReasonMessage(DpConstants.RECORD_NOT_FOUND);
                        txnTrackerDTO.setNoOfRecords(0);
                    } else {
                        txnTrackerDTO.setStatus(HeaderStatusENUM.SUCC.toValue());
                        txnTrackerDTO.setStatusReasonCode(HeaderStatusENUM.SUCC.toValue());
                        txnTrackerDTO.setStatusReasonMessage(HeaderStatusENUM.SUCC.toValue());
                        txnTrackerDTO.setNoOfRecords(1);
                        completedCount++;
                    }
                    searchResponseDTOList.add(buildSearchResponseForSunbird(txnTrackerDTO, dataDTO));
                    index++;

                    String txnTrackerString = objectMapper.writeValueAsString(txnTrackerDTO);
                    HttpResponse<JsonNode> responseD = Unirest.put(txnTrackerURL + "/" + txnTrackerOsid)
                            .header("Content-Type", "application/json")
                            .body(txnTrackerString)
                            .asJson();
                    if (responseD.getStatus() != 200) {
                        log.error("Error while updating txn tracker data");
                    }
                }
            }
            MsgTrackerDTO msgTrackerDTO = objectMapper.convertValue(msgTrackerResultMap, MsgTrackerDTO.class);
            msgTrackerDTO.setStatus(HeaderStatusENUM.SUCC.toValue());
            msgTrackerDTO.setStatusReasonCode(HeaderStatusENUM.SUCC.toValue());
            msgTrackerDTO.setStatusReasonMessage(HeaderStatusENUM.SUCC.toValue());
            msgTrackerDTO.setTotalCount((int) totalCount);
            msgTrackerDTO.setCompletedCount(completedCount);
            msgTrackerDTO.setCorrelationId(CommonUtils.generateUniqueId("C"));
            msgTrackerDTO.setLocale(LocalesENUM.EN.toValue());
            String msgTrackerString = objectMapper.writeValueAsString(msgTrackerDTO);
            HttpResponse<JsonNode> responseD = Unirest.put(msgTrackerURL + "/" + msgTrackerOsid)
                    .header("Content-Type", "application/json")
                    .body(msgTrackerString)
                    .asJson();
            if (responseD.getStatus() != 200) {
                log.error("Error while updating msg tracker data");
            }
        }
        return searchResponseDTOList;
    }

    @Override
    public List<QueryDTO> getQueryDTOList(String transactionId) throws IOException {
        List<QueryDTO> queryDTOList;
        queryDTOList = new ArrayList<>();
        Map<String, String> fieldValues = new HashMap<>();
        fieldValues.put("registry_transactions_id.keyword", transactionId);
        SearchResponse response = elasticsearchService.exactSearch("txn_tracker", fieldValues);
        response.getHits().forEach(hit -> {
            try {
                QueryDTO queryDTO = new ObjectMapper().readValue(hit.getSourceAsMap().get("query").toString(), QueryDTO.class);
                queryDTOList.add(queryDTO);
            } catch (IOException e) {
                log.error("Error while parsing response: {}", e.getMessage());
            }
        });
        return queryDTOList;
    }
}

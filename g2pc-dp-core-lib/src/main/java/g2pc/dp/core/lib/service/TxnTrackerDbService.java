package g2pc.dp.core.lib.service;

import g2pc.core.lib.dto.search.message.request.QueryDTO;
import g2pc.core.lib.dto.search.message.request.RequestDTO;
import g2pc.core.lib.dto.search.message.request.ResponseMessageDTO;
import g2pc.core.lib.dto.search.message.response.DataDTO;
import g2pc.core.lib.dto.search.message.response.SearchResponseDTO;
import g2pc.core.lib.exceptions.G2pcError;
import g2pc.dp.core.lib.dto.TxnTrackerDTO;

import java.io.IOException;
import java.util.List;

public interface TxnTrackerDbService {

    G2pcError saveRequestDetails(RequestDTO requestDTO, String protocol);

    int getRecordCount(Object records);

    List<SearchResponseDTO> getUpdatedSearchResponseList(RequestDTO requestDTO,
                                                         List<Object> refRecordsStringsList,
                                                         String protocol) throws IOException;

    DataDTO buildDataForSunbird(String regRecordsString, TxnTrackerDTO txnTrackerDTO);

    SearchResponseDTO buildSearchResponseForSunbird(TxnTrackerDTO txnTrackerDTO, DataDTO dataDTO);

    void updateStatusResponseDetails(ResponseMessageDTO responseMessageDTO, String transactionId);

    void updateMessageTrackerStatusDb(String transactionId);

    List<QueryDTO> getQueryDTOList(String transactionId) throws IOException;
}

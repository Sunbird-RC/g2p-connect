package g2pc.dp.core.lib.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import g2pc.core.lib.utils.CommonUtils;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TxnTrackerDTO {

    public TxnTrackerDTO() {
        this.registryTransactionsId = "";
        this.referenceId = "";
        this.consent = false;
        this.authorize = false;
        this.timestamp = "";
        this.status = "";
        this.statusReasonCode = "";
        this.statusReasonMessage = "";
        this.version = "";
        this.regType = "";
        this.regSubType = "";
        this.queryType = "";
        this.query = "";
        this.regRecordType = "";
        this.noOfRecords = 0;
        this.txnType = "";
        this.txnStatus = "";
        this.createdDate = CommonUtils.getCurrentTimeStamp();
        this.lastUpdatedDate = CommonUtils.getCurrentTimeStamp();
    }

    @JsonProperty("registry_transactions_id")
    private String registryTransactionsId;
    
    @JsonProperty("reference_id")
    private String referenceId;

    @JsonProperty("consent")
    private boolean consent;

    @JsonProperty("authorize")
    private boolean authorize;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("status")
    private String status;

    @JsonProperty("status_reason_code")
    private String statusReasonCode;

    @JsonProperty("status_reason_message")
    private String statusReasonMessage;

    @JsonProperty("version")
    private String version;

    @JsonProperty("reg_type")
    private String regType;

    @JsonProperty("reg_sub_type")
    private String regSubType;

    @JsonProperty("query_type")
    private String queryType;

    @JsonProperty("query")
    private String query;

    @JsonProperty("reg_record_type")
    private String regRecordType;

    @JsonProperty("no_of_records")
    private Integer noOfRecords;

    @JsonProperty("txn_type")
    private String txnType;

    @JsonProperty("txn_status")
    private String txnStatus;

    @JsonProperty("created_date")
    private String createdDate;

    @JsonProperty("last_updated_date")
    private String lastUpdatedDate;
}

package com.fintrack.transaction.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/** Risk-engine assessment emitted before the saga's debit step. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RiskAssessedEvent implements Serializable {
    private String eventId;
    private String transactionUuid;
    private String level;
    private Integer score;
    private Boolean blocked;
    private Boolean requiresReview;
    private List<String> triggeredRules;
    private Instant assessedAt;
    private Instant occurredAt;
}

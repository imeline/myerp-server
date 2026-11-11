package erp.inbound.service;

import erp.global.response.PageResponse;
import erp.inbound.dto.request.InboundFindAllRequest;
import erp.inbound.dto.request.InboundSaveRequest;
import erp.inbound.dto.response.InboundFindAllResponse;

public interface InboundService {

    long saveInbound(InboundSaveRequest request, long tenantId, String accountUuid);

    PageResponse<InboundFindAllResponse> findAllInbound(
            InboundFindAllRequest request,
            long tenantId
    );

    void cancelInbound(long inboundId, long tenantId, String cancelerUuid);
}

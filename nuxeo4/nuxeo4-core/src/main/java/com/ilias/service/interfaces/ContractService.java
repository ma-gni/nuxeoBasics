package com.ilias.service.interfaces;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import java.util.List;

public interface ContractService {
    String IN_REVIEW = "In Review";
    String APPROVED = "Approved";

    /** Plan + apply submit for approval (title prefix + status) */
    DocumentModel submitForApproval(CoreSession session, DocumentModel doc);

    /** Approve a single contract if it’s in review */
    DocumentModel approveContract(CoreSession session, DocumentModel doc);

    /** Bulk approve by NXQL, returns updated doc ids (lightweight) */
    List<String> approveByQuery(CoreSession session, String nxql);

}

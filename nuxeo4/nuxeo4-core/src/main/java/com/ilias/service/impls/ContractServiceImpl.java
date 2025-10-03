package com.ilias.service.impls;

import com.ilias.core.SubmitCore;
import com.ilias.service.interfaces.ContractService;
import org.nuxeo.ecm.core.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ContractServiceImpl implements ContractService {

    private static final String XPATH_TITLE = "dc:title";
    private static final String XPATH_STATUS = "contract:status";

    @Override
    public DocumentModel submitForApproval(CoreSession session, DocumentModel doc) {
        var view = new SubmitCore.DocView(doc.getType(), doc.getTitle(), true);
        var params = new SubmitCore.SubmitParams(doc.getType(), IN_REVIEW, "[SUBMITTED] ", false);
        var patch  = SubmitCore.DocPatch.plan(view, params);
        DocumentModel finalDoc = doc;
        patch.newTitle().ifPresent(t -> finalDoc.setPropertyValue(XPATH_TITLE, t));
        DocumentModel finalDoc1 = doc;
        patch.newStatus().ifPresent(s -> finalDoc1.setPropertyValue(XPATH_STATUS, s));
        doc = session.saveDocument(doc);
        session.save();
        return doc;
    }

    @Override
    public DocumentModel approveContract(CoreSession session, DocumentModel doc) {
        String curr = (String) doc.getPropertyValue(XPATH_STATUS);
        if (!Objects.equals(curr, IN_REVIEW)) {
            return doc; // gate: only promote from In Review
        }
        doc.setPropertyValue(XPATH_STATUS, APPROVED);
        doc = session.saveDocument(doc);
        session.save();
        return doc;
    }

    @Override
    public List<String> approveByQuery(CoreSession session, String nxql) {
        List<DocumentModel> docs = session.query(nxql);
        List<String> updated = new ArrayList<>();
        for (DocumentModel d : docs) {
            String curr = (String) d.getPropertyValue(XPATH_STATUS);
            if (Objects.equals(curr, IN_REVIEW)) {
                d.setPropertyValue(XPATH_STATUS, APPROVED);
                session.saveDocument(d);
                updated.add(d.getId());
            }
        }
        session.save();
        return updated;
    }
}

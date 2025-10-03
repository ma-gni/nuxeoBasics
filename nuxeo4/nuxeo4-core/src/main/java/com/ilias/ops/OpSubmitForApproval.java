package com.ilias.ops;

import com.ilias.core.SubmitCore;
import com.ilias.core.SubmitCore.DocPatch;
import com.ilias.core.SubmitCore.DocView;
import com.ilias.core.SubmitCore.SubmitParams;

import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.*;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import java.util.function.Predicate;

import static com.ilias.fp.DocRule.*;

@Operation(
        id = "Contract.SubmitForApproval",
        category = Constants.CAT_DOCUMENT,
        label = "Contract - Submit For Approval (FP)",
        description = "Marks the document as 'In Review' and (optionally) prefixes the title using a pure SubmitCore plan."
)
public class OpSubmitForApproval {

    @Context
    protected CoreSession session;

    @Param(name = "requiredType", required = false)
    protected String requiredType = "Contract";

    @Param(name = "statusInReview", required = false)
    protected String statusInReview = "In Review";

    @Param(name = "titlePrefix", required = false)
    protected String titlePrefix = "[SUBMITTED] ";

    @Param(name = "enforceContractType", required = false)
    protected boolean enforceType = true;

    private static final String XPATH_TITLE = "dc:title";
    private static final String XPATH_CONTRACT_STATUS = "contract:status";

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws OperationException {
        Predicate<DocumentModel> RULES = typeIs(requiredType)
                .and(hasTitle)
                .and(titleMissingPrefix(titlePrefix))
                .and(hasProperty(XPATH_CONTRACT_STATUS));

        if (!RULES.test(doc)) {
            throw new OperationException("SubmitForApproval preconditions failed (type/title/prefix/status).");
        }

        DocView view = new DocView(doc.getType(), doc.getTitle(), true);
        SubmitParams params = new SubmitParams(requiredType, statusInReview, titlePrefix, enforceType);

        final DocPatch patch;
        try {
            patch = SubmitCore.DocPatch.plan(view, params);
        } catch (IllegalArgumentException iae) {
            throw new OperationException(iae);
        }

        if (patch.isEmpty()) {
            return doc; // nothing to change
        }

        DocumentModel finalDoc = doc;
        patch.newTitle().ifPresent(t -> finalDoc.setPropertyValue(XPATH_TITLE, t));
        DocumentModel finalDoc1 = doc;
        patch.newStatus().ifPresent(s -> finalDoc1.setPropertyValue(XPATH_CONTRACT_STATUS, s));

        doc = session.saveDocument(doc);
        session.save();
        return doc;
    }
}

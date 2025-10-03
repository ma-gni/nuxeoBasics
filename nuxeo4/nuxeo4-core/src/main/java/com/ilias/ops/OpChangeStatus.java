package com.ilias.ops;

import com.ilias.core.SubmitCore;
import com.ilias.core.SubmitCore.DocPatch;
import com.ilias.core.SubmitCore.DocView;
import com.ilias.core.SubmitCore.SubmitParams;
import com.ilias.fp.DocRule;

import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.*;
import org.nuxeo.ecm.core.api.*;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Operation(
        id = "Contract.ChangeStatusInPath",
        category = Constants.CAT_DOCUMENT,
        label = "Contract - Change Status in Path (FP)",
        description = "Find docs under a path, plan with SubmitCore, set status, and return only the docs whose status changed."
)
public class OpChangeStatus {

    @Context
    protected CoreSession session;

    @Param(name = "path")
    protected String path;

    @Param(name = "requiredType", required = false)
    protected String requiredType = "Contract";

    @Param(name = "targetStatus", required = false)
    protected String targetStatus = "Approved";

    @Param(name = "enforceContractType", required = false)
    protected boolean enforceType = true;

    @Param(name = "onlyIfCurrentStatus", required = false)
    protected String onlyIfCurrentStatus = "";

    public static final String XPATH_STATUS = "contract:status";
    private static final String BLANK_PREFIX = ""; // ensures SubmitCore won't touch titles

    @OperationMethod
    public List<DocumentModel> run() throws OperationException {
        if (path == null || path.isBlank()) {
            throw new OperationException("Parameter 'path' is required");
        }

        String nxql = "SELECT * FROM Document WHERE ecm:path STARTSWITH '" +
                path.replace("'", "\\'") + "' AND ecm:isProxy = 0";
        List<DocumentModel> docs = session.query(nxql);

        Predicate<DocumentModel> precheck =
                DocRule.typeIs(requiredType)
                        .and(DocRule.hasProperty(XPATH_STATUS)); // must have the status field

        Predicate<DocumentModel> currentStatusMatches = d -> {
            if (onlyIfCurrentStatus == null || onlyIfCurrentStatus.isBlank()) return true;
            try {
                String curr = (String) d.getPropertyValue(XPATH_STATUS);
                return Objects.equals(curr, onlyIfCurrentStatus);
            } catch (PropertyException e) {
                return false;
            }
        };

        SubmitParams params =
                new SubmitParams(requiredType, targetStatus, BLANK_PREFIX, enforceType);

        List<DocumentModel> updated = docs.stream()
                .filter(precheck.and(currentStatusMatches))
                .map(doc -> {
                    boolean hasStatus = true; // guaranteed by precheck
                    DocView view = new DocView(doc.getType(), doc.getTitle(), hasStatus);

                    final DocPatch patch;
                    try {
                        patch = SubmitCore.DocPatch.plan(view, params);
                    } catch (IllegalArgumentException e) {
                        return Optional.<DocumentModel>empty();
                    }

                    if (patch.newStatus().isEmpty()) {
                        return Optional.<DocumentModel>empty();
                    }

                    String proposed = patch.newStatus().get();
                    String current;
                    try {
                        current = (String) doc.getPropertyValue(XPATH_STATUS);
                    } catch (PropertyException e) {
                        return Optional.<DocumentModel>empty(); // unexpected: schema vanished
                    }

                    if (Objects.equals(current, proposed)) {
                        return Optional.<DocumentModel>empty();
                    }

                    doc.setPropertyValue(XPATH_STATUS, proposed);
                    DocumentModel saved = session.saveDocument(doc);
                    return Optional.of(saved);
                })
                .flatMap(Optional::stream)
                .collect(Collectors.toList());


        session.save();

        return updated;
    }
}

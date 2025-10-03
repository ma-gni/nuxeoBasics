package com.ilias.listeners;

import com.ilias.work.NotifyApprovalWork;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.*;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;

public class ContractAsyncHandler implements EventListener {

    private static final String XPATH_STATUS = "contract:status";

    @Override
    public void handleEvent(Event event) {
        if (!(event.getContext() instanceof DocumentEventContext)) return;
        DocumentEventContext ctx = (DocumentEventContext) event.getContext();
        DocumentModel doc = ctx.getSourceDocument();
        if (doc == null || !"Contract".equals(doc.getType())) return;
        if (!DOCUMENT_UPDATED.equals(event.getName())) return;

        String newStatus = (String) doc.getPropertyValue(XPATH_STATUS);
        if ("Approved".equals(newStatus)) {
            WorkManager wm = Framework.getService(WorkManager.class);
            if (wm != null) {
                wm.schedule(new NotifyApprovalWork(doc.getRepositoryName(), doc.getId()));
            }
        }
    }
}

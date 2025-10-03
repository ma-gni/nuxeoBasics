package com.ilias.listeners;

import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.event.*;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class ContractLifecycleListener implements EventListener {

    private static final String XPATH_STATUS = "contract:status";

    @Override
    public void handleEvent(Event event) {
        var ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) return;
        var docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        if (doc == null || !"Contract".equals(doc.getType())) return;

        if (doc.getTitle() == null || doc.getTitle().isBlank()) {
            event.markBubbleException();
            return;
        }
        try {
            doc.getPropertyValue(XPATH_STATUS);
        } catch (PropertyException e) {
            event.markBubbleException();
        }
    }
}

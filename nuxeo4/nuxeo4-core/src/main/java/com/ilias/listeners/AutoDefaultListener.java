package com.ilias.listeners;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class AutoDefaultListener implements PostCommitFilteringEventListener {

    private static final List<String> HANDLED = Arrays.asList(
            "documentCreated",
            "documentModified"
    );

    @Override
    public boolean acceptEvent(Event event) {
        return HANDLED.contains(event.getName());
    }

    @Override
    public void handleEvent(EventBundle events) {
        for (Event event : events) {
            if (acceptEvent(event)) {
                handleOne(event);
            }
        }
    }

    /** Process a single event from the bundle. */
    private void handleOne(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }

        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        CoreSession session = docCtx.getCoreSession();
        DocumentModel doc = docCtx.getSourceDocument();
        if (doc == null || doc.isProxy() || doc.isVersion()) {
            return;
        }
        if (!"Contract".equals(doc.getType())) {
            return;
        }

        String eventName = event.getName();

        if ("documentCreated".equals(eventName)) {

            try {
                Object current = doc.getPropertyValue("contract:status");
                if (current == null || !"Draft".equals(current)) {
                    doc.setPropertyValue("contract:status", "Draft");
                }
            } catch (PropertyException ignoreIfSchemaMissing) {
            }

            String title = doc.getTitle() == null ? "" : doc.getTitle();
            if (!title.startsWith("[CONTRACT] ")) {
                doc.setPropertyValue("dc:title", "[CONTRACT] " + title);
            }
            session.saveDocument(doc);
            session.save();
        }

        if ("documentModified".equals(eventName)) {


            try {
                String status = (String) doc.getPropertyValue("contract:status");
            } catch (PropertyException ignoreIfSchemaMissing) {
                ignoreIfSchemaMissing.printStackTrace();
            }
        }
    }
}

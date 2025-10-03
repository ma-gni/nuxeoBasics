package com.ilias.work;

import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.api.Framework;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static jdk.internal.icu.impl.Utility.escape;

public class NotifyApprovalWork extends AbstractWork {

    private static final Logger log = Logger.getLogger(NotifyApprovalWork.class.getName());

    public static final String CATEGORY = "contractNotify";
    private final String repositoryName;
    private final String docId;

    public NotifyApprovalWork(String repositoryName, String docId) {
        super("notify-approval:" + repositoryName + ":" + docId); // unique job id
        this.repositoryName = repositoryName;
        this.docId = docId;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getTitle() {
        return "";
    }

    @Override
    public void work() {
        setStatus("Preparing notification...");
        CoreInstance.doPrivileged(repositoryName, (CoreSession session) -> {
            DocumentModel doc = null;
            try {
                doc = session.getDocument(new IdRef(docId));
            } catch (Exception e) {
                log.warning("NotifyApprovalWork: document not found: " + docId);
                return;
            }

            String title = doc.getTitle();
            String path  = doc.getPathAsString();
            String status = safeString(doc.getPropertyValue("contract:status"));
            log.info(() -> String.format(
                    "Contract approved -> id=%s title=%s path=%s status=%s", docId, title, path, status
            ));

            String webhook = Framework.getProperty("ilias.webhook.url");
            if (webhook != null && !webhook.isBlank()) {
                try {
                    sendWebhook(webhook, docId, title, path, status);
                    log.info("NotifyApprovalWork: webhook sent to " + webhook);
                } catch (Exception ex) {
                    log.warning("NotifyApprovalWork: webhook failed: " + ex.getMessage());
                }
            }
        });
        setStatus("Done");
    }

    private static String safeString(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static void sendWebhook(String url, String id, String title, String path, String status) throws Exception {
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        String body = String.format(
                "{\"id\":\"%s\",\"title\":\"%s\",\"path\":\"%s\",\"status\":\"%s\"}",
                id, escape(title), escape(path), escape(status)
        );
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        conn.getOutputStream().write(bytes);
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("HTTP " + code + " from webhook");
        }
    }

}

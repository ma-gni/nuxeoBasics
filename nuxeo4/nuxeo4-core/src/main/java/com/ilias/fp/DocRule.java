package com.ilias.fp;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;

import java.util.Objects;
import java.util.function.Predicate;

public final class DocRule {
    private DocRule() {}

    public static final Predicate<DocumentModel> typeIsContract =
            doc -> "Contract".equals(doc.getType());

    public static Predicate<DocumentModel> typeIs(String requiredType) {
        String req = requiredType == null ? "" : requiredType;
        return doc -> Objects.equals(req, doc.getType());
    }

    public static Predicate<DocumentModel> pathStartsWith(String prefix) {
        String p = prefix == null ? "" : prefix;
        return d -> {
            String path = d.getPathAsString();
            return path != null && path.startsWith(p);
        };
    }

    public static final Predicate<DocumentModel> hasTitle =
            d -> {
                String t = d.getTitle();
                return t != null && !t.isBlank();
            };

    public static Predicate<DocumentModel> titleMissingPrefix(String prefix) {
        String px = prefix == null ? "" : prefix;
        return d -> {
            String t = d.getTitle() == null ? "" : d.getTitle();
            return !px.isBlank() && !t.startsWith(px);
        };
    }

    public static Predicate<DocumentModel> hasProperty(String xpath) {
        String xp = xpath == null ? "" : xpath;
        return d -> {
            try {
                d.getPropertyValue(xp);
                return true;
            } catch (PropertyException e) {
                return false; // schema/field missing
            }
        };
    }
}

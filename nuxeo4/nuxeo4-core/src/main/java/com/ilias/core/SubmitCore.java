package com.ilias.core;

import java.util.Optional;

public class SubmitCore {
    private SubmitCore() {}

    public record SubmitParams(String requiredType, String statusInReview, String titlePrefix, boolean enforceType){
        public SubmitParams {
            requiredType   = requiredType   == null ? "" : requiredType;
            statusInReview = statusInReview == null ? "" : statusInReview;
            titlePrefix    = titlePrefix    == null ? "" : titlePrefix;
        }
    }

    public record DocView (String type, String title, boolean hasStatusProperty){
        public DocView {
            type = type == null ? "" : type;
            title = title == null ? "" : title;
        }
    }

    public record DocPatch (Optional<String> newTitle, Optional<String> newStatus){
        public static DocPatch empty() { return new DocPatch(Optional.empty(), Optional.empty()); }
        public boolean isEmpty() { return newTitle.isEmpty() && newStatus.isEmpty(); }
        public DocPatch merge(DocPatch other) {
            return new DocPatch(other.newTitle.or(() -> newTitle),
                    other.newStatus.or(() -> newStatus));
        }
        public static DocPatch plan(DocView v, SubmitParams p) {
            if (p.enforceType() && !p.requiredType().equals(v.type())) {
                throw new IllegalArgumentException(
                        "SubmitForApproval can only run on type '" + p.requiredType() + "'"
                );
            }
            DocPatch titlePatch =
                    (p.titlePrefix().isBlank() || v.title().startsWith(p.titlePrefix()))
                            ? DocPatch.empty()
                            : new DocPatch(Optional.of(p.titlePrefix() + v.title()), Optional.empty());

            DocPatch statusPatch =
                    v.hasStatusProperty()
                            ? new DocPatch(Optional.empty(), Optional.of(p.statusInReview()))
                            : DocPatch.empty();

            return titlePatch.merge(statusPatch);
        }
    }
}

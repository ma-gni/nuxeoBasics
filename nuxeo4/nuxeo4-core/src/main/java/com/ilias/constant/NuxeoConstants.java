package com.ilias.constant;

public final class NuxeoConstants {

    private NuxeoConstants() {
        // prevent instantiation
    }

    // ==== Operations IDs ====
    public static final String OP_SUBMIT_FOR_APPROVAL = "Contract.SubmitForApproval";
    public static final String OP_APPROVE_CONTRACT = "Contract.Approve";
    public static final String OP_REJECT_CONTRACT = "Contract.Reject";

    // ==== Schemas ====
    public static final String SCHEMA_CONTRACT = "contract";

    // ==== Properties ====
    public static final String PROP_CONTRACT_STATUS = SCHEMA_CONTRACT + ":status";

    // ==== Status Values ====
    public static final String STATUS_DRAFT = "Draft";
    public static final String STATUS_IN_REVIEW = "In Review";
    public static final String STATUS_APPROVED = "Approved";
    public static final String STATUS_REJECTED = "Rejected";

    // ==== Document Types ====
    public static final String DOC_TYPE_CONTRACT = "Contract";
}

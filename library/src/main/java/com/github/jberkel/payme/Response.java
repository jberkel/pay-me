package com.github.jberkel.payme;

enum Response {
    BILLING_RESPONSE_RESULT_OK(0, "OK"),
    BILLING_RESPONSE_RESULT_USER_CANCELED(1, "User Canceled"),
    BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE(3, "Billing Unavailable"),
    BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE(4, "Item Unavailable"),
    BILLING_RESPONSE_RESULT_DEVELOPER_ERROR(5, "Developer Error"),
    BILLING_RESPONSE_RESULT_ERROR(6, "Error"),
    BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED(7, "Item Already Owned"),
    BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED(8, "Item not owned"),

    IABHELPER_ERROR_BASE(-1000, ""),
    IABHELPER_REMOTE_EXCEPTION(-1001, "Remote exception during initialization"),
    IABHELPER_BAD_RESPONSE(-1002, "Bad response received"),
    IABHELPER_VERIFICATION_FAILED(-1003, "Purchase signature verification failed"),
    IABHELPER_SEND_INTENT_FAILED(-1004, "Send intent failed"),
    IABHELPER_USER_CANCELLED(-1005, "User cancelled"),
    IABHELPER_UNKNOWN_PURCHASE_RESPONSE(-1006, "Unknown purchase response"),
    IABHELPER_MISSING_TOKEN(-1007, "Missing token"),
    IABHELPER_UNKNOWN_ERROR(-1008, "Unknown error"),
    IABHELPER_SUBSCRIPTIONS_NOT_AVAILABLE(-1009, "Subscriptions not available"),
    IABHELPER_INVALID_CONSUMPTION(-1010, "Invalid consumption attempt"),
    IABHELPER_BILLING_NOT_AVAILABLE(-1011, "Billing not available");

    public final int code;
    public final String description;

    Response(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static Response fromCode(int code) {
        for (Response s : Response.values()) {
            if (s.code == code) return s;
        }
        return IABHELPER_UNKNOWN_ERROR;
    }

    public static String getDescription(int code) {
        return fromCode(code).description;
    }
}

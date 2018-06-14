package javax.websocket;

public enum CloseReasonCloseCodes implements CloseReasonCloseCode {

    NORMAL_CLOSURE(1000),
    GOING_AWAY(1001),
    PROTOCOL_ERROR(1002),
    CANNOT_ACCEPT(1003),
    RESERVED(1004),
    NO_STATUS_CODE(1005),
    CLOSED_ABNORMALLY(1006),
    NOT_CONSISTENT(1007),
    VIOLATED_POLICY(1008),
    TOO_BIG(1009),
    NO_EXTENSION(1010),
    UNEXPECTED_CONDITION(1011),
    SERVICE_RESTART(1012),
    TRY_AGAIN_LATER(1013),
    TLS_HANDSHAKE_FAILURE(1015);

    private int code;

    CloseReasonCloseCodes(int code) {
        this.code = code;
    }

    public static CloseReasonCloseCode getCloseCode(final int code) {
        if (code > 2999 && code < 5000) {
            return new CloseReasonCloseCode() {
                @Override
                public int getCode() {
                    return code;
                }
            };
        }
        switch (code) {
            case 1000:
                return CloseReasonCloseCodes.NORMAL_CLOSURE;
            case 1001:
                return CloseReasonCloseCodes.GOING_AWAY;
            case 1002:
                return CloseReasonCloseCodes.PROTOCOL_ERROR;
            case 1003:
                return CloseReasonCloseCodes.CANNOT_ACCEPT;
            case 1004:
                return CloseReasonCloseCodes.RESERVED;
            case 1005:
                return CloseReasonCloseCodes.NO_STATUS_CODE;
            case 1006:
                return CloseReasonCloseCodes.CLOSED_ABNORMALLY;
            case 1007:
                return CloseReasonCloseCodes.NOT_CONSISTENT;
            case 1008:
                return CloseReasonCloseCodes.VIOLATED_POLICY;
            case 1009:
                return CloseReasonCloseCodes.TOO_BIG;
            case 1010:
                return CloseReasonCloseCodes.NO_EXTENSION;
            case 1011:
                return CloseReasonCloseCodes.UNEXPECTED_CONDITION;
            case 1012:
                return CloseReasonCloseCodes.SERVICE_RESTART;
            case 1013:
                return CloseReasonCloseCodes.TRY_AGAIN_LATER;
            case 1015:
                return CloseReasonCloseCodes.TLS_HANDSHAKE_FAILURE;
            default:
                throw new IllegalArgumentException(
                        "Invalid close code: [" + code + "]");
        }
    }

    @Override
    public int getCode() {
        return code;
    }
}
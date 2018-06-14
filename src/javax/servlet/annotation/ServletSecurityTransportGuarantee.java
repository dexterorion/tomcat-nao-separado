package javax.servlet.annotation;

/**
 * Represents the two possible values of data transport, encrypted or not.
 */
public enum ServletSecurityTransportGuarantee {

    /**
     * User data must not be encrypted by the container during transport
     */
    NONE,

    /**
     * The container MUST encrypt user data during transport
     */
    CONFIDENTIAL
}
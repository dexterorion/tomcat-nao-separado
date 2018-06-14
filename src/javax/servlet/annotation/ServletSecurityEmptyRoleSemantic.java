package javax.servlet.annotation;

/**
 * Represents the two possible values of the empty role semantic, active
 * when a list of role names is empty.
 */
public enum ServletSecurityEmptyRoleSemantic {

    /**
     * Access MUST be permitted, regardless of authentication state or
     * identity
     */
    PERMIT,

    /**
     * Access MUST be denied, regardless of authentication state or identity
     */
    DENY
}
package org.apache.catalina.realm;

import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

/**
 * Private class used when parsing the XML database file.
 */
public final class MemoryRuleSetMemoryUserRule extends Rule {


    /**
     * Construct a new instance of this <code>Rule</code>.
     */
    public MemoryRuleSetMemoryUserRule() {
        // No initialisation required
    }


    /**
     * Process a <code>&lt;user&gt;</code> element from the XML database file.
     *
     * @param attributes The attribute list for this element
     */
    @Override
    public void begin(String namespace, String name, Attributes attributes)
        throws Exception {

        String username = attributes.getValue("username");
        if (username == null) {
            username = attributes.getValue("name");
        }
        String password = attributes.getValue("password");
        String roles = attributes.getValue("roles");

        MemoryRealm realm =
            (MemoryRealm) getDigester().peek(getDigester().getCount() - 1);
        realm.addUser(username, password, roles);

    }


}
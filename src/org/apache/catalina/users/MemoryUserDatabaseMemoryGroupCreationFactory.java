package org.apache.catalina.users;

import org.apache.catalina.Group;
import org.apache.catalina.Role;
import org.apache.tomcat.util.digester.AbstractObjectCreationFactory;
import org.xml.sax.Attributes;

/**
 * Digester object creation factory for group instances.
 */
public class MemoryUserDatabaseMemoryGroupCreationFactory extends AbstractObjectCreationFactory {

    public MemoryUserDatabaseMemoryGroupCreationFactory(MemoryUserDatabase database) {
        this.database = database;
    }

    @Override
    public Object createObject(Attributes attributes) {
        String groupname = attributes.getValue("groupname");
        if (groupname == null) {
            groupname = attributes.getValue("name");
        }
        String description = attributes.getValue("description");
        String roles = attributes.getValue("roles");
        Group group = database.createGroup(groupname, description);
        if (roles != null) {
            while (roles.length() > 0) {
                String rolename = null;
                int comma = roles.indexOf(',');
                if (comma >= 0) {
                    rolename = roles.substring(0, comma).trim();
                    roles = roles.substring(comma + 1);
                } else {
                    rolename = roles.trim();
                    roles = "";
                }
                if (rolename.length() > 0) {
                    Role role = database.findRole(rolename);
                    if (role == null) {
                        role = database.createRole(rolename, null);
                    }
                    group.addRole(role);
                }
            }
        }
        return (group);
    }

    private MemoryUserDatabase database = null;
}
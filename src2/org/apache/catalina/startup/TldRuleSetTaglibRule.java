package org.apache.catalina.startup;

import org.apache.tomcat.util.digester.Rule;

/*
 * This rule only exists to reset the duplicateUri flag on the TaglibUriRule.
 */
public final class TldRuleSetTaglibRule extends Rule {
    private final TldRuleSetTaglibUriRule taglibUriRule;
    
    public TldRuleSetTaglibRule(TldRuleSetTaglibUriRule taglibUriRule) {
        this.taglibUriRule = taglibUriRule;
    }
    
    @Override
    public void body(String namespace, String name, String text)
    throws Exception {
        taglibUriRule.setDuplicateUri(false);
    }

}
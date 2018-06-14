package org.apache.catalina.startup;

import org.apache.tomcat.util.digester.Rule;

public final class TldRuleSetTaglibListenerRule extends Rule {
    
    private final TldRuleSetTaglibUriRule taglibUriRule;
    
    public TldRuleSetTaglibListenerRule(TldRuleSetTaglibUriRule taglibUriRule) {
        this.taglibUriRule = taglibUriRule;
    }

    @Override
    public void body(String namespace, String name, String text)
            throws Exception {
        TldConfig tldConfig =
            (TldConfig) getDigester().peek(getDigester().getCount() - 1);
        
        // Only process the listener if the URI is not a duplicate
        if (!taglibUriRule.isDuplicateUri()) {
            tldConfig.addApplicationListener(text.trim());
        }
    }
    
}
package org.apache.catalina.startup;

import org.apache.tomcat.util.digester.Rule;

public final class TldRuleSetTaglibUriRule extends Rule {
    
    // This is set to false for each file processed by the TaglibRule
    private boolean duplicateUri;
    
    public TldRuleSetTaglibUriRule() {
    }

    @Override
    public void body(String namespace, String name, String text)
            throws Exception {
        TldConfig tldConfig =
            (TldConfig) getDigester().peek(getDigester().getCount() - 1);
        if (tldConfig.isKnownTaglibUri(text)) {
            // Already seen this URI
            duplicateUri = true;
            // This is expected if the URI was defined in web.xml
            // Log message at debug in this case
            if (tldConfig.isKnownWebxmlTaglibUri(text)) {
                if (getDigester().getLogger().isDebugEnabled()) {
                    getDigester().getLogger().debug(
                            "TLD skipped. URI: " + text + " is already defined");
                }
            } else {
                getDigester().getLogger().info(
                        "TLD skipped. URI: " + text + " is already defined");
            }
        } else {
            // New URI. Add it to known list and carry on
            tldConfig.addTaglibUri(text);
        }
    }
    
    public boolean isDuplicateUri() {
        return duplicateUri;
    }

    public void setDuplicateUri(boolean duplciateUri) {
        this.duplicateUri = duplciateUri;
    }

}
package org.apache.jasper.compiler;

import java.io.IOException;

import javax.servlet.jsp.tagext.TagFileInfo;

import org.apache.jasper.JasperException;

/*
 * Visitor which scans the page and looks for tag handlers that are tag
 * files, compiling (if necessary) and loading them.
 */
public class TagFileProcessorTagFileLoaderVisitor extends NodeVisitor {

    /**
	 * 
	 */
	private final TagFileProcessor tagFileProcessor;

	private Compiler2 compiler;

    private PageInfo pageInfo;

    public TagFileProcessorTagFileLoaderVisitor(TagFileProcessor tagFileProcessor, Compiler2 compiler) {

        this.tagFileProcessor = tagFileProcessor;
		this.compiler = compiler;
        this.pageInfo = compiler.getPageInfo();
    }

    @Override
    public void visit(NodeCustomTag n) throws JasperException {
        TagFileInfo tagFileInfo = n.getTagFileInfo();
        if (tagFileInfo != null) {
            String tagFilePath = tagFileInfo.getPath();
            if (tagFilePath.startsWith("/META-INF/")) {
                // For tags in JARs, add the TLD and the tag as a dependency
                TldLocation location =
                    compiler.getCompilationContext().getTldLocation(
                        tagFileInfo.getTagInfo().getTagLibrary().getURI());
                JarResource jarResource = location.getJarResource();
                if (jarResource != null) {
                    try {
                        // Add TLD
                        pageInfo.addDependant(jarResource.getEntry(location.getName()).toString(),
                                Long.valueOf(jarResource.getJarFile().getEntry(location.getName()).getTime()));
                        // Add Tag
                        pageInfo.addDependant(jarResource.getEntry(tagFilePath.substring(1)).toString(),
                                Long.valueOf(jarResource.getJarFile().getEntry(tagFilePath.substring(1)).getTime()));
                    } catch (IOException ioe) {
                        throw new JasperException(ioe);
                    }
                }
                else {
                    pageInfo.addDependant(tagFilePath,
                            compiler.getCompilationContext().getLastModified(
                                    tagFilePath));
                }
            } else {
                pageInfo.addDependant(tagFilePath,
                        compiler.getCompilationContext().getLastModified(
                                tagFilePath));
            }
            Class<?> c = this.tagFileProcessor.loadTagFile(compiler, tagFilePath, n.getTagInfo(),
                    pageInfo);
            n.setTagHandlerClass(c);
        }
        visitBody(n);
    }
}
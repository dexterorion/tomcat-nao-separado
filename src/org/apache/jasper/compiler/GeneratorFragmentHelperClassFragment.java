package org.apache.jasper.compiler;

public class GeneratorFragmentHelperClassFragment {
    private GeneratorGenBuffer genBuffer;

    private int id;

    public GeneratorFragmentHelperClassFragment(int id, Node node) {
        this.id = id;
        genBuffer = new GeneratorGenBuffer(null, node.getBody());
    }

    public GeneratorGenBuffer getGeneratorGenBuffer() {
        return this.genBuffer;
    }

    public int getId() {
        return this.id;
    }
}
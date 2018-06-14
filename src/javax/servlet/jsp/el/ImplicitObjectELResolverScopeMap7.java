package javax.servlet.jsp.el;

import java.util.Enumeration;

import javax.servlet.jsp.PageContext;

public class ImplicitObjectELResolverScopeMap7 extends ImplicitObjectELResolverScopeMap<String>{
	private PageContext page;
	
	public ImplicitObjectELResolverScopeMap7(PageContext page){
		this.page = page;
	}
	
	@Override
    protected Enumeration<String> getAttributeNames() {
        return page.getRequest().getParameterNames();
    }

    @Override
    protected String getAttribute(String name) {
        return page.getRequest().getParameter(name);
    }

}

package javax.servlet.jsp.el;

import java.util.Enumeration;

import javax.servlet.jsp.PageContext;

public class ImplicitObjectELResolverScopeMap5 extends ImplicitObjectELResolverScopeMap<String>{
	private PageContext page;
	
	public ImplicitObjectELResolverScopeMap5(PageContext page){
		this.page = page;
	}
	
	@Override
    protected Enumeration<String> getAttributeNames() {
        return page.getServletContext().getInitParameterNames();
    }

    @Override
    protected String getAttribute(String name) {
        return page.getServletContext().getInitParameter(name);
    }

}

package javax.servlet.jsp.el;

import java.util.Enumeration;

import javax.servlet.jsp.PageContext;

public class ImplicitObjectELResolverScopeMap1 extends ImplicitObjectELResolverScopeMap<Object>{
	private PageContext page;
	
	public ImplicitObjectELResolverScopeMap1(PageContext page){
		this.page = page;
	}
	
	@Override
    protected void setAttribute(String name, Object value) {
        page.getServletContext().setAttribute(name, value);
    }

    @Override
    protected void removeAttribute(String name) {
        page.getServletContext().removeAttribute(name);
    }

    @Override
    protected Enumeration<String> getAttributeNames() {
        return page.getServletContext().getAttributeNames();
    }

    @Override
    protected Object getAttribute(String name) {
        return page.getServletContext().getAttribute(name);
    }
}

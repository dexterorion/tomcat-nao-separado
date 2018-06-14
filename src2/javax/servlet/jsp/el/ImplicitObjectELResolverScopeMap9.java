package javax.servlet.jsp.el;

import java.util.Enumeration;

import javax.servlet.jsp.PageContext;

public class ImplicitObjectELResolverScopeMap9 extends ImplicitObjectELResolverScopeMap<Object>{
	private PageContext page;
	
	public ImplicitObjectELResolverScopeMap9(PageContext page){
		this.page = page;
	}
	
	@Override
    protected void setAttribute(String name, Object value) {
        page.getRequest().setAttribute(name, value);
    }

    @Override
    protected void removeAttribute(String name) {
        page.getRequest().removeAttribute(name);
    }

    @Override
    protected Enumeration<String> getAttributeNames() {
        return page.getRequest().getAttributeNames();
    }

    @Override
    protected Object getAttribute(String name) {
        return page.getRequest().getAttribute(name);
    }

}

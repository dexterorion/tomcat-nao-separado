package javax.servlet.jsp.el;

import java.util.Enumeration;

import javax.servlet.jsp.PageContext;

public class ImplicitObjectELResolverScopeMap6 extends ImplicitObjectELResolverScopeMap<Object>{
	private PageContext page;
	
	public ImplicitObjectELResolverScopeMap6(PageContext page){
		this.page = page;
	}
	
	@Override
    protected void setAttribute(String name, Object value) {
        page.setAttribute(name, value);
    }

    @Override
    protected void removeAttribute(String name) {
        page.removeAttribute(name);
    }

    @Override
    protected Enumeration<String> getAttributeNames() {
        return page.getAttributeNamesInScope(
                PageContext.getPageScope());
    }

    @Override
    protected Object getAttribute(String name) {
        return page.getAttribute(name);
    }

}

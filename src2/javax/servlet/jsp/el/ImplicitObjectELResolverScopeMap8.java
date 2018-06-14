package javax.servlet.jsp.el;

import java.util.Enumeration;

import javax.servlet.jsp.PageContext;

public class ImplicitObjectELResolverScopeMap8 extends ImplicitObjectELResolverScopeMap<String []>{
	private PageContext page;
	
	public ImplicitObjectELResolverScopeMap8(PageContext page){
		this.page = page;
	}

	@Override
    protected String[] getAttribute(String name) {
        return page.getRequest().getParameterValues(name);
    }

    @Override
    protected Enumeration<String> getAttributeNames() {
        return page.getRequest().getParameterNames();
    }
}

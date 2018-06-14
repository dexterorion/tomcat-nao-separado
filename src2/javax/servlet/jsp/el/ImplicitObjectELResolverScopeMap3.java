package javax.servlet.jsp.el;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

public class ImplicitObjectELResolverScopeMap3 extends ImplicitObjectELResolverScopeMap<String>{
	private PageContext page;
	
	public ImplicitObjectELResolverScopeMap3(PageContext page){
		this.page = page;
	}

	
	@Override
    protected Enumeration<String> getAttributeNames() {
        return ((HttpServletRequest) page.getRequest())
                .getHeaderNames();
    }

    @Override
    protected String getAttribute(String name) {
        return ((HttpServletRequest) page.getRequest())
                .getHeader(name);
    }
}

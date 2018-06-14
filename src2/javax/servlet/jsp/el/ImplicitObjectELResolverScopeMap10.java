package javax.servlet.jsp.el;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

public class ImplicitObjectELResolverScopeMap10 extends ImplicitObjectELResolverScopeMap<Object>{
	private PageContext page;
	
	public ImplicitObjectELResolverScopeMap10(PageContext page){
		this.page = page;
	}
	
	@Override
    protected void setAttribute(String name, Object value) {
        ((HttpServletRequest) page.getRequest()).getSession()
                .setAttribute(name, value);
    }

    @Override
    protected void removeAttribute(String name) {
        HttpSession session = page.getSession();
        if (session != null) {
            session.removeAttribute(name);
        }
    }

    @Override
    protected Enumeration<String> getAttributeNames() {
        HttpSession session = page.getSession();
        if (session != null) {
            return session.getAttributeNames();
        }
        return null;
    }

    @Override
    protected Object getAttribute(String name) {
        HttpSession session = page.getSession();
        if (session != null) {
            return session.getAttribute(name);
        }
        return null;
    }
}

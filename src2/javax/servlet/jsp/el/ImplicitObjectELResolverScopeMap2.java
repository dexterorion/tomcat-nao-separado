package javax.servlet.jsp.el;

import java.util.Enumeration;
import java.util.Vector;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

public class ImplicitObjectELResolverScopeMap2 extends ImplicitObjectELResolverScopeMap<Cookie>{
	private PageContext page;
	
	public ImplicitObjectELResolverScopeMap2(PageContext page){
		this.page = page;
	}
	
	@Override
    protected Enumeration<String> getAttributeNames() {
        Cookie[] c = ((HttpServletRequest) page.getRequest())
                .getCookies();
        if (c != null) {
            Vector<String> v = new Vector<String>();
            for (int i = 0; i < c.length; i++) {
                v.add(c[i].getName());
            }
            return v.elements();
        }
        return null;
    }

    @Override
    protected Cookie getAttribute(String name) {
        Cookie[] c = ((HttpServletRequest) page.getRequest())
                .getCookies();
        if (c != null) {
            for (int i = 0; i < c.length; i++) {
                if (name.equals(c[i].getName())) {
                    return c[i];
                }
            }
        }
        return null;
    }
}

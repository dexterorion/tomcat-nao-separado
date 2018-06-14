package javax.servlet.jsp.el;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

public class ImplicitObjectELResolverScopeMap4 extends ImplicitObjectELResolverScopeMap<String[]>{
	private PageContext page;
	
	public ImplicitObjectELResolverScopeMap4(PageContext page){
		this.page = page;
	}
	
	@Override
    protected Enumeration<String> getAttributeNames() {
        return ((HttpServletRequest) page.getRequest())
                .getHeaderNames();
    }

    @Override
    protected String[] getAttribute(String name) {
        Enumeration<String> e =
            ((HttpServletRequest) page.getRequest())
                    .getHeaders(name);
        if (e != null) {
            List<String> list = new ArrayList<String>();
            while (e.hasMoreElements()) {
                list.add(e.nextElement());
            }
            return list.toArray(new String[list.size()]);
        }
        return null;
    }
	
}

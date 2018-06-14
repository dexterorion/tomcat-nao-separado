package javax.servlet.jsp.el;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

public class ImplicitObjectELResolverScopeManager {
    private static final String MNGR_KEY = ImplicitObjectELResolverScopeManager.class.getName();

    private final PageContext page;

    private Map<String,Object> applicationScope;

    private Map<String,Cookie> cookie;

    private Map<String,String> header;

    private Map<String,String[]> headerValues;

    private Map<String,String> initParam;

    private Map<String,Object> pageScope;

    private Map<String,String> param;

    private Map<String,String[]> paramValues;

    private Map<String,Object> requestScope;

    private Map<String,Object> sessionScope;

    public ImplicitObjectELResolverScopeManager(PageContext page) {
        this.page = page;
    }

    public static ImplicitObjectELResolverScopeManager get(PageContext page) {
    	ImplicitObjectELResolverScopeManager mngr = (ImplicitObjectELResolverScopeManager) page.getAttribute(MNGR_KEY);
        if (mngr == null) {
            mngr = new ImplicitObjectELResolverScopeManager(page);
            page.setAttribute(MNGR_KEY, mngr);
        }
        return mngr;
    }

    public Map<String,Object> getApplicationScope() {
        if (this.applicationScope == null) {
            this.applicationScope = new ImplicitObjectELResolverScopeMap1(this.page);
        }
        return this.applicationScope;
    }

    public Map<String,Cookie> getCookie() {
        if (this.cookie == null) {
            this.cookie = new ImplicitObjectELResolverScopeMap2(this.page);
        }
        return this.cookie;
    }

    public Map<String,String> getHeader() {
        if (this.header == null) {
            this.header = new ImplicitObjectELResolverScopeMap3(this.page);
        }
        return this.header;
    }

    public Map<String,String[]> getHeaderValues() {
        if (this.headerValues == null) {
            this.headerValues = new ImplicitObjectELResolverScopeMap4(this.page);
        }
        return this.headerValues;
    }

    public Map<String,String> getInitParam() {
        if (this.initParam == null) {
            this.initParam = new ImplicitObjectELResolverScopeMap5(this.page);
        }
        return this.initParam;
    }

    public PageContext getPageContext() {
        return this.page;
    }

    public Map<String,Object> getPageScope() {
        if (this.pageScope == null) {
            this.pageScope = new ImplicitObjectELResolverScopeMap6(this.page);
        }
        return this.pageScope;
    }

    public Map<String,String> getParam() {
        if (this.param == null) {
            this.param = new ImplicitObjectELResolverScopeMap7(this.page);
        }
        return this.param;
    }

    public Map<String,String[]> getParamValues() {
        if (this.paramValues == null) {
            this.paramValues = new ImplicitObjectELResolverScopeMap8(this.page);
        }
        return this.paramValues;
    }

    public Map<String,Object> getRequestScope() {
        if (this.requestScope == null) {
            this.requestScope = new ImplicitObjectELResolverScopeMap9(this.page);
        }
        return this.requestScope;
    }

    public Map<String,Object> getSessionScope() {
        if (this.sessionScope == null) {
            this.sessionScope = new ImplicitObjectELResolverScopeMap10(this.page);
        }
        return this.sessionScope;
    }
}
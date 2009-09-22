package org.zotero.integration.ooo;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.AllPermission;

public class PrivilegedURLClassLoader extends URLClassLoader {
	public PrivilegedURLClassLoader(URL[] urls) {
		super(urls);
	}

	public PrivilegedURLClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	public PrivilegedURLClassLoader(URL[] urls, ClassLoader parent,
			URLStreamHandlerFactory factory) {
		super(urls, parent, factory);
	}
	
    protected PermissionCollection getPermissions(CodeSource codesource)
    {
        PermissionCollection perms = super.getPermissions(codesource);
        perms.add(new AllPermission());
        return perms;
    }
}

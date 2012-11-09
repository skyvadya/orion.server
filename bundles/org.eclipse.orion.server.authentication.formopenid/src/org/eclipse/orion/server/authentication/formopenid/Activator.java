/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.orion.server.authentication.formopenid;

import org.eclipse.orion.internal.server.core.IWebResourceDecorator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	/**
	 * The symbolic id of this bundle.
	 */
	public static final String PI_FORMOPENID_SERVLETS = "org.eclipse.orion.server.authentication.formopenid"; //$NON-NLS-1$

	private static volatile BundleContext bundleContext;

	private OpenIdUserDecorator resourceDecorator;

	public static Activator singleton;

	public static Activator getDefault() {
		return singleton;
	}

	public static BundleContext getBundleContext() {
		return bundleContext;
	}

	public void start(BundleContext context) throws Exception {
		singleton = this;
		resourceDecorator = new OpenIdUserDecorator();
		context.registerService(IWebResourceDecorator.class, resourceDecorator, null);
		bundleContext = context;
	}

	public void stop(BundleContext context) throws Exception {
		bundleContext = null;
		resourceDecorator = null;
	}

	public OpenIdUserDecorator getResourceDecorator() {
		return resourceDecorator;
	}

}
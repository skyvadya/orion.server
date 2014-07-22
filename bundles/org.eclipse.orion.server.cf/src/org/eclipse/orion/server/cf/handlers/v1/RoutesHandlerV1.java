/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.orion.server.cf.handlers.v1;

import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.core.runtime.*;
import org.eclipse.orion.internal.server.servlets.ServletResourceHandler;
import org.eclipse.orion.server.cf.CFProtocolConstants;
import org.eclipse.orion.server.cf.commands.*;
import org.eclipse.orion.server.cf.jobs.CFJob;
import org.eclipse.orion.server.cf.objects.Route;
import org.eclipse.orion.server.cf.objects.Target;
import org.eclipse.orion.server.cf.servlets.AbstractRESTHandler;
import org.eclipse.orion.server.cf.utils.HttpUtil;
import org.eclipse.orion.server.core.IOUtilities;
import org.eclipse.orion.server.core.ServerStatus;
import org.eclipse.osgi.util.NLS;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutesHandlerV1 extends AbstractRESTHandler<Route> {

	private final Logger logger = LoggerFactory.getLogger("org.eclipse.orion.server.cf"); //$NON-NLS-1$

	public RoutesHandlerV1(ServletResourceHandler<IStatus> statusHandler) {
		super(statusHandler);
	}

	@Override
	protected Route buildResource(HttpServletRequest request, String path) throws CoreException {
		return null;
	}

	@Override
	protected CFJob handleGet(Route route, HttpServletRequest request, HttpServletResponse response, final String path) {
		final JSONObject targetJSON = extractJSONData(IOUtilities.getQueryParameter(request, CFProtocolConstants.KEY_TARGET));

		return new CFJob(request, false) {
			@Override
			protected IStatus performJob() {
				try {
					ComputeTargetCommand computeTargetCommand = new ComputeTargetCommand(this.userId, targetJSON);
					computeTargetCommand.doIt();
					Target target = computeTargetCommand.getTarget();
					if (target == null) {
						return HttpUtil.createErrorStatus(IStatus.WARNING, "CF-TargetNotSet", "Target not set");
					}

					return new GetRoutesCommand(target, false).doIt();
				} catch (Exception e) {
					String msg = NLS.bind("Failed to handle request for {0}", path); //$NON-NLS-1$
					ServerStatus status = new ServerStatus(IStatus.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg, e);
					logger.error(msg, e);
					return status;
				}
			}
		};
	}

	@Override
	protected CFJob handleDelete(Route route, HttpServletRequest request, HttpServletResponse response, final String path) {
		final JSONObject targetJSON = extractJSONData(IOUtilities.getQueryParameter(request, CFProtocolConstants.KEY_TARGET));
		final JSONObject routeJSON = extractJSONData(IOUtilities.getQueryParameter(request, CFProtocolConstants.KEY_ROUTE));
		final String orphaned = IOUtilities.getQueryParameter(request, CFProtocolConstants.KEY_ORPHANED);

		return new CFJob(request, false) {
			@Override
			protected IStatus performJob() {
				try {
					ComputeTargetCommand computeTargetCommand = new ComputeTargetCommand(this.userId, targetJSON);
					computeTargetCommand.doIt();
					Target target = computeTargetCommand.getTarget();
					if (target == null) {
						return HttpUtil.createErrorStatus(IStatus.WARNING, "CF-TargetNotSet", "Target not set");
					}

					JSONArray deletedRoutesJSON = new JSONArray();

					//					GetDomainsCommand getDomainsCommand = new GetDomainsCommand(target, routeJSON.getString(CFProtocolConstants.KEY_DOMAIN_NAME));
					//					IStatus getDomainsStatus = getDomainsCommand.doIt();
					//					if (!getDomainsStatus.isOK())
					//						return getDomainsStatus;
					//					List<Domain> domains = getDomainsCommand.getDomains();
					//					if (domains == null || domains.size() == 0)
					//						return new ServerStatus(IStatus.OK, HttpServletResponse.SC_NOT_FOUND, "Domain not found", null);

					//					for (Iterator<Domain> iterator = domains.iterator(); iterator.hasNext();) {
					//						Domain domain = iterator.next();

					GetRoutesCommand getRoutesCommand = null;
					if (Boolean.parseBoolean(orphaned)) {
						getRoutesCommand = new GetRoutesCommand(target, true);
					} else {
						getRoutesCommand = new GetRoutesCommand(target, routeJSON.getString(CFProtocolConstants.KEY_DOMAIN_NAME), routeJSON.getString(CFProtocolConstants.KEY_HOST));
					}

					IStatus getRoutesStatus = getRoutesCommand.doIt();
					if (!getRoutesStatus.isOK())
						return getRoutesStatus;
					List<Route> routes = getRoutesCommand.getRoutes();
					if (routes == null || routes.size() == 0)
						return new ServerStatus(IStatus.OK, HttpServletResponse.SC_NOT_FOUND, "Host not found", null);

					for (Iterator<Route> iterator2 = routes.iterator(); iterator2.hasNext();) {
						Route route = iterator2.next();

						DeleteRouteCommand deleteRouteCommand = new DeleteRouteCommand(target, route);
						IStatus deleteRouteStatus = deleteRouteCommand.doIt();
						if (!deleteRouteStatus.isOK())
							return deleteRouteStatus;

						deletedRoutesJSON.put(route.toJSON());
					}
					//					}

					JSONObject result = new JSONObject();
					result.put("Routes", deletedRoutesJSON);
					return new ServerStatus(Status.OK_STATUS, HttpServletResponse.SC_OK, result);
				} catch (Exception e) {
					String msg = NLS.bind("Failed to handle request for {0}", path); //$NON-NLS-1$
					ServerStatus status = new ServerStatus(IStatus.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg, e);
					logger.error(msg, e);
					return status;
				}
			}
		};
	}
}

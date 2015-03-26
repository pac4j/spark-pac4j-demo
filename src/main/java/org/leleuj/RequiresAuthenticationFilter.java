package org.leleuj;

import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.CommonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Filter;
import spark.Request;
import spark.Response;

public class RequiresAuthenticationFilter implements Filter {

	private static final Logger logger = LoggerFactory.getLogger(RequiresAuthenticationFilter.class);

	private final Clients clients;

	private final String clientName;

	public RequiresAuthenticationFilter(final Clients clients, final String clientName) {
		this.clients = clients;
		this.clientName = clientName;
	}

	@Override
	public void handle(Request request, Response response) {
        final CommonProfile profile = UserUtils.getProfile(request);
        logger.debug("profile : {}", profile);

        // profile null, not authenticated
        if (profile == null) {
            // no authentication tried -> redirect to provider
            // keep the current url
            String requestedUrl = request.url();
            String queryString = request.queryString();
            if (CommonHelper.isNotBlank(queryString)) {
                requestedUrl += "?" + queryString;
            }
            logger.debug("requestedUrl : {}", requestedUrl);
            request.session().attribute(Pac4jConstants.REQUESTED_URL, requestedUrl);
            // compute and perform the redirection
            final SparkWebContext context = new SparkWebContext(request, response);
			@SuppressWarnings("unchecked")
			Client<Credentials, CommonProfile> client = clients.findClient(this.clientName);
            try {
                client.redirect(context, true, false);
            } catch (RequiresHttpAction e) {
                logger.debug("extra HTTP action required : {}", e.getCode());
            }
        }
	}
}

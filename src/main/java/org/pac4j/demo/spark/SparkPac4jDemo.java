package org.pac4j.demo.spark;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.val;
import org.pac4j.core.adapter.FrameworkAdapter;
import org.pac4j.core.client.Client;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.FrameworkParameters;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.util.Pac4jConstants;
import org.pac4j.http.client.indirect.FormClient;

import org.pac4j.jee.context.session.JEESessionStore;
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration;
import org.pac4j.jwt.profile.JwtGenerator;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.sparkjava.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.mustache.MustacheTemplateEngine;

import static spark.Spark.*;

public class SparkPac4jDemo {

	private final static String JWT_SALT = "12345678901234567890123456789012";

	private final static Logger logger = LoggerFactory.getLogger(SparkPac4jDemo.class);

	private final static MustacheTemplateEngine templateEngine = new MustacheTemplateEngine();

	private static Config CONFIG;

	public static void main(String[] args) {
		port(8080);
		final Config config = new DemoConfigFactory(JWT_SALT, templateEngine).build();
		CONFIG = config;

		FrameworkAdapter.INSTANCE.applyDefaultSettingsIfUndefined(config);

		get("/", SparkPac4jDemo::index, templateEngine);
		final CallbackRoute callback = new CallbackRoute(config, null, true);
		//callback.setRenewSession(false);
		get("/callback", callback);
		post("/callback", callback);
        final SecurityFilter facebookFilter = new SecurityFilter(config, "FacebookClient", "", "excludedPath");
        before("/facebook", facebookFilter);
		before("/facebook/*", facebookFilter);
		before("/facebookadmin", new SecurityFilter(config, "FacebookClient", "admin"));
		before("/facebookcustom", new SecurityFilter(config, "FacebookClient", "custom"));
		before("/twitter", new SecurityFilter(config, "TwitterClient,FacebookClient"));
		before("/form", new SecurityFilter(config, "FormClient"));
		before("/basicauth", new SecurityFilter(config, "IndirectBasicAuthClient"));
		before("/cas", new SecurityFilter(config, "CasClient"));
		before("/saml2", new SecurityFilter(config, "SAML2Client"));
		before("/oidc", new SecurityFilter(config, "OidcClient"));
		before("/protected", new SecurityFilter(config, null));
		before("/dba", new SecurityFilter(config, "DirectBasicAuthClient,ParameterClient"));
		before("/rest-jwt", new SecurityFilter(config, "ParameterClient"));
		get("/facebook", SparkPac4jDemo::protectedIndex, templateEngine);
        get("/facebook/notprotected", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/facebookadmin", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/facebookcustom", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/twitter", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/form", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/basicauth", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/cas", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/saml2", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/saml2-metadata", (rq, rs) -> {
			val samlclient = (SAML2Client) config.getClients().findClient("SAML2Client").get();
			samlclient.init();
			return samlclient.getServiceProviderMetadataResolver().getMetadata();
		});
		get("/jwt", SparkPac4jDemo::jwt, templateEngine);
		get("/oidc", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/protected", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/dba", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/rest-jwt", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/loginForm", (rq, rs) -> form(config), templateEngine);
		final LogoutRoute localLogout = new LogoutRoute(config, "/?defaulturlafterlogout");
		localLogout.setDestroySession(true);
		get("/logout", localLogout);
		final LogoutRoute centralLogout = new LogoutRoute(config);
		centralLogout.setDefaultUrl("http://localhost:8080/?defaulturlafterlogoutafteridp");
		centralLogout.setLogoutUrlPattern("http://localhost:8080/.*");
		centralLogout.setLocalLogout(false);
		centralLogout.setCentralLogout(true);
		centralLogout.setDestroySession(true);
		get("/centralLogout", centralLogout);
		get("/forceLogin", (rq, rs) -> forceLogin(config, rq, rs));

		/*before("/body", (request, response) -> {
			logger.debug("before /body");
		});*/
		//before("/body", new SecurityFilter(config, "AnonymousClient"));
		before("/body", new SecurityFilter(config, "HeaderClient"));
		post("/body", (request, response) -> {
			logger.debug("Body: " + request.body());
			return "done: " + getProfiles(request, response);
		});

		exception(Exception.class, (e, request, response) -> {
			logger.error("Unexpected exception", e);
			response.body(templateEngine.render(new ModelAndView(new HashMap<>(), "error500.mustache")));
		});
    }

	private static ModelAndView index(final Request request, final Response response) {
		val map = new HashMap();
		map.put("profiles", getProfiles(request, response));
		val fp = new SparkFrameworkParameters(request, response);
		val ctx = new SparkWebContext(request, response);
		val sessionStore = CONFIG.getSessionStoreFactory().newSessionStore(fp);
		map.put("sessionId", sessionStore.getSessionId(ctx, false));
		return new ModelAndView(map, "index.mustache");
	}

	private static ModelAndView jwt(final Request request, final Response response) {
		val context = new SparkWebContext(request, response);
		val fp = new SparkFrameworkParameters(request, response);
		val sessionStore = CONFIG.getSessionStoreFactory().newSessionStore(fp);
		val manager = CONFIG.getProfileManagerFactory().apply(context, sessionStore);
		var profile = manager.getProfile();
		String token = "";
		if (profile.isPresent()) {
			JwtGenerator generator = new JwtGenerator(new SecretSignatureConfiguration(JWT_SALT));
			token = generator.generate(profile.get());
		}
		val map = new HashMap();
		map.put("token", token);
		return new ModelAndView(map, "jwt.mustache");
	}

	private static ModelAndView form(final Config config) {
		val map = new HashMap();
		val formClient = (FormClient) config.getClients().findClient("FormClient").get();
		map.put("callbackUrl", formClient.getCallbackUrl());
		return new ModelAndView(map, "loginForm.mustache");
	}

	private static ModelAndView protectedIndex(final Request request, final Response response) {
		val map = new HashMap();
		map.put("profiles", getProfiles(request, response));
		return new ModelAndView(map, "protectedIndex.mustache");
	}

	private static List<UserProfile> getProfiles(final Request request, final Response response) {
		val context = new SparkWebContext(request, response);
		val fp = new SparkFrameworkParameters(request, response);
		val sessionStore = CONFIG.getSessionStoreFactory().newSessionStore(fp);
		val manager = CONFIG.getProfileManagerFactory().apply(context, sessionStore);
		return manager.getProfiles();
	}

	private static ModelAndView forceLogin(final Config config, final Request request, final Response response) {
        val context = new SparkWebContext(request, response);
        val clientName = context.getRequestParameter(Pac4jConstants.DEFAULT_CLIENT_NAME_PARAMETER).get();
		val client = config.getClients().findClient(clientName).get();
		val fp = new SparkFrameworkParameters(request, response);
		val sessionStore = CONFIG.getSessionStoreFactory().newSessionStore(fp);
		HttpAction action;
		try {
			action = client.getRedirectionAction(new CallContext(context, sessionStore)).get();
		} catch (final HttpAction e) {
			action = e;
		}
		SparkHttpActionAdapter.INSTANCE.adapt(action, context);
		return null;
    }
}

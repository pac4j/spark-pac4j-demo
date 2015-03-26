package org.leleuj;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.pac4j.core.context.WebContext;

import spark.Request;
import spark.Response;

public class SparkWebContext implements WebContext {

	private final Request request;
	
	private final Response response;
	
	public SparkWebContext(final Request request, final Response response) {
		this.request = request;
		this.response = response;
	}

	@Override
	public String getRequestParameter(String name) {
		return request.queryParams(name);
	}

	@Override
	public Map<String, String[]> getRequestParameters() {
		Map<String, String[]> newParams = new HashMap<>();
		Set<String> keys = request.queryParams();
		for (String key : keys) {
			String[] params = new String[1];
			params[0] = request.queryParams(key);
			newParams.put(key, params);
		}
		return newParams;
	}

	@Override
	public String getRequestHeader(String name) {
		return request.headers(name);
	}

	@Override
	public void setSessionAttribute(String name, Object value) {
		request.session().attribute(name, value);
	}

	@Override
	public Object getSessionAttribute(String name) {
		return request.session().attribute(name);
	}

	@Override
	public String getRequestMethod() {
		return request.requestMethod();
	}

	@Override
	public void writeResponseContent(String content) {
		response.body(content);
	}

	@Override
	public void setResponseStatus(int code) {
		response.status(code);
	}

	@Override
	public void setResponseHeader(String name, String value) {
		response.header(name, value);
	}

	@Override
	public String getServerName() {
		return request.host();
	}

	@Override
	public int getServerPort() {
		return request.port();

	}

	@Override
	public String getScheme() {
		return request.scheme();
	}

	@Override
	public String getFullRequestURL() {
		return request.url();
	}
}

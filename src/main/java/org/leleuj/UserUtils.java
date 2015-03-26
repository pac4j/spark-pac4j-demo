package org.leleuj;

import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.profile.CommonProfile;

import spark.Request;

public class UserUtils {

	public static CommonProfile getProfile(final Request request) {
		return request.session().attribute(Pac4jConstants.USER_PROFILE);
	}
	
	public static void setProfile(final Request request, final CommonProfile profile) {
		request.session().attribute(Pac4jConstants.USER_PROFILE, profile);
	}
	
	public static void logout(final Request request) {
		setProfile(request, null);
	}
}

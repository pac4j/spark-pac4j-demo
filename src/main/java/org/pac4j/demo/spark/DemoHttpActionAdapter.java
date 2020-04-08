package org.pac4j.demo.spark;

import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.sparkjava.SparkHttpActionAdapter;
import org.pac4j.sparkjava.SparkWebContext;
import spark.ModelAndView;
import spark.TemplateEngine;

import java.util.HashMap;

public class DemoHttpActionAdapter extends SparkHttpActionAdapter {

    private final TemplateEngine templateEngine;

    public DemoHttpActionAdapter(final TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public Object adapt(final HttpAction action, final SparkWebContext context) {
        if (action != null) {
            final int code = action.getCode();
            if (code == HttpConstants.UNAUTHORIZED) {
                stop(401, templateEngine.render(new ModelAndView(new HashMap<>(), "error401.mustache")));
            } else if (code == HttpConstants.FORBIDDEN) {
                stop(403, templateEngine.render(new ModelAndView(new HashMap<>(), "error403.mustache")));
            } else {
                return super.adapt(action, context);
            }
        }
        throw new TechnicalException("No action provided");
    }
}

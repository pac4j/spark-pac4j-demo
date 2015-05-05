package org.leleuj;

import org.pac4j.core.exception.RequiresHttpAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.halt;

public abstract class ExtraHttpActionHandler {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected void handle(final RequiresHttpAction e) {
        int status = e.getCode();
        logger.debug("extra HTTP action required : {}", status);
        halt(status, e.getMessage());
    }
}

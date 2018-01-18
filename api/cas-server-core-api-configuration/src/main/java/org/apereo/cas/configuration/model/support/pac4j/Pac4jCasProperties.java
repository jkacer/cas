package org.apereo.cas.configuration.model.support.pac4j;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.configuration.support.RequiresModule;
import org.apereo.cas.configuration.support.RequiredProperty;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * This is {@link Pac4jCasProperties}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@RequiresModule(name = "cas-server-support-pac4j-webflow")
@Slf4j
@Getter
@Setter
public class Pac4jCasProperties implements Serializable {

    private static final long serialVersionUID = -2738631545437677447L;

    /**
     * The CAS server login url.
     */
    @RequiredProperty
    private String loginUrl;

    /**
     * CAS protocol to use.
     * Acceptable values are {@code CAS10, CAS20, CAS20_PROXY, CAS30, CAS30_PROXY, SAML}.
     */
    @RequiredProperty
    private String protocol;

    /**
     * Name of the client mostly for UI purposes and uniqueness.
     */
    private String clientName;
}

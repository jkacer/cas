package org.apereo.cas.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.CentralAuthenticationService;
import org.apereo.cas.authentication.AuthenticationSystemSupport;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.rest.ChainingRestHttpRequestCredentialFactory;
import org.apereo.cas.rest.RestHttpRequestCredentialFactory;
import org.apereo.cas.rest.RestHttpRequestCredentialFactoryConfigurer;
import org.apereo.cas.rest.UsernamePasswordRestHttpRequestCredentialFactory;
import org.apereo.cas.support.rest.factory.DefaultServiceTicketResourceEntityResponseFactory;
import org.apereo.cas.support.rest.factory.DefaultTicketGrantingTicketResourceEntityResponseFactory;
import org.apereo.cas.support.rest.factory.DefaultUserAuthenticationResourceEntityResponseFactory;
import org.apereo.cas.support.rest.factory.ServiceTicketResourceEntityResponseFactory;
import org.apereo.cas.support.rest.factory.TicketGrantingTicketResourceEntityResponseFactory;
import org.apereo.cas.support.rest.factory.UserAuthenticationResourceEntityResponseFactory;
import org.apereo.cas.support.rest.resources.ServiceTicketResource;
import org.apereo.cas.support.rest.resources.TicketGrantingTicketResource;
import org.apereo.cas.support.rest.resources.TicketStatusResource;
import org.apereo.cas.support.rest.resources.UserAuthenticationResource;
import org.apereo.cas.ticket.registry.TicketRegistrySupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * This is {@link CasRestConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Configuration("casRestConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
public class CasRestConfiguration extends WebMvcConfigurerAdapter implements RestHttpRequestCredentialFactoryConfigurer {

    @Autowired
    @Qualifier("centralAuthenticationService")
    private CentralAuthenticationService centralAuthenticationService;

    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired(required = false)
    @Qualifier("defaultAuthenticationSystemSupport")
    private AuthenticationSystemSupport authenticationSystemSupport;

    @Autowired
    @Qualifier("webApplicationServiceFactory")
    private ServiceFactory webApplicationServiceFactory;

    @Autowired
    @Qualifier("defaultTicketRegistrySupport")
    private TicketRegistrySupport ticketRegistrySupport;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(restAuthenticationThrottle()).addPathPatterns("/v1/**");
    }

    @Bean
    public TicketStatusResource ticketStatusResource() {
        return new TicketStatusResource(centralAuthenticationService);
    }

    @Bean
    public ServiceTicketResource serviceTicketResource() {
        return new ServiceTicketResource(authenticationSystemSupport, ticketRegistrySupport,
            webApplicationServiceFactory, serviceTicketResourceEntityResponseFactory());
    }

    @Bean
    @ConditionalOnMissingBean(name = "serviceTicketResourceEntityResponseFactory")
    public ServiceTicketResourceEntityResponseFactory serviceTicketResourceEntityResponseFactory() {
        return new DefaultServiceTicketResourceEntityResponseFactory(centralAuthenticationService);
    }

    @Bean
    @ConditionalOnMissingBean(name = "ticketGrantingTicketResourceEntityResponseFactory")
    public TicketGrantingTicketResourceEntityResponseFactory ticketGrantingTicketResourceEntityResponseFactory() {
        return new DefaultTicketGrantingTicketResourceEntityResponseFactory();
    }

    @Bean
    @ConditionalOnMissingBean(name = "userAuthenticationResourceEntityResponseFactory")
    public UserAuthenticationResourceEntityResponseFactory userAuthenticationResourceEntityResponseFactory() {
        return new DefaultUserAuthenticationResourceEntityResponseFactory();
    }

    @Autowired
    @Bean
    public TicketGrantingTicketResource ticketResourceRestController(
        @Qualifier("restHttpRequestCredentialFactory") final RestHttpRequestCredentialFactory restHttpRequestCredentialFactory) {
        return new TicketGrantingTicketResource(authenticationSystemSupport, restHttpRequestCredentialFactory,
            centralAuthenticationService, webApplicationServiceFactory, ticketGrantingTicketResourceEntityResponseFactory());
    }

    @Autowired
    @Bean
    public UserAuthenticationResource userAuthenticationRestController(
        @Qualifier("restHttpRequestCredentialFactory") final RestHttpRequestCredentialFactory restHttpRequestCredentialFactory) {
        return new UserAuthenticationResource(authenticationSystemSupport, restHttpRequestCredentialFactory,
            webApplicationServiceFactory, userAuthenticationResourceEntityResponseFactory());
    }

    @ConditionalOnMissingBean(name = "restAuthenticationThrottle")
    @Bean
    public HandlerInterceptor restAuthenticationThrottle() {
        final String throttler = casProperties.getRest().getThrottler();
        if (StringUtils.isNotBlank(throttler) && this.applicationContext.containsBean(throttler)) {
            return this.applicationContext.getBean(throttler, HandlerInterceptor.class);
        }
        return new HandlerInterceptorAdapter() {
            @Override
            public boolean preHandle(final HttpServletRequest request,
                                     final HttpServletResponse response,
                                     final Object handler) {
                return true;
            }
        };
    }

    @Autowired
    @Bean
    public RestHttpRequestCredentialFactory restHttpRequestCredentialFactory(final List<RestHttpRequestCredentialFactoryConfigurer> configurers) {
        final ChainingRestHttpRequestCredentialFactory factory = new ChainingRestHttpRequestCredentialFactory();
        configurers.forEach(c -> c.registerCredentialFactory(factory));
        return factory;
    }

    @Override
    public void registerCredentialFactory(final ChainingRestHttpRequestCredentialFactory factory) {
        factory.registerCredentialFactory(new UsernamePasswordRestHttpRequestCredentialFactory());
    }
}

package com.echanneling.config;
@org.springframework.context.annotation.Configuration
@org.springframework.context.annotation.Profile("prod")
public class ProductionConfigGuard {
    public ProductionConfigGuard(org.springframework.core.env.Environment env) {
        String url=env.getRequiredProperty("spring.datasource.url").toLowerCase(java.util.Locale.ROOT);
        if (!url.contains("encrypt=true") || url.contains("trustservercertificate=true"))
            throw new IllegalStateException("Production requires encrypted SQL Server connections with certificate verification.");
        if ("sa".equalsIgnoreCase(env.getRequiredProperty("spring.datasource.username")))
            throw new IllegalStateException("Use a dedicated production database account.");
        String publicUrl=env.getRequiredProperty("app.recovery.base-url");
        java.net.URI origin=java.net.URI.create(publicUrl);
        if(!"https".equals(origin.getScheme()) || origin.getHost()==null || origin.getUserInfo()!=null
                || origin.getQuery()!=null || origin.getFragment()!=null)
            throw new IllegalStateException("Use an HTTPS public origin for password recovery.");
        if (env.getProperty("app.seed-demo", Boolean.class, false) || env.getProperty("app.payments.demo-card-enabled", Boolean.class, false))
            throw new IllegalStateException("Demo seeding and card simulation are prohibited in production.");
    }
}

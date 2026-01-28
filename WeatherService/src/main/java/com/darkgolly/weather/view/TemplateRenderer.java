package com.darkgolly.weather.view;

import com.darkgolly.weather.model.WeatherViewModel;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class TemplateRenderer {
    private final TemplateEngine engine;

    public TemplateRenderer() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(false);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        this.engine = templateEngine;
    }

    public String renderWeatherPage(WeatherViewModel model) {
        Context context = new Context(Locale.ROOT);
        context.setVariable("city", model.city());
        context.setVariable("country", model.country());
        context.setVariable("timezone", model.timezone());
        context.setVariable("fetchedAt", model.fetchedAt());
        context.setVariable("hourly", model.hourly());
        return engine.process("weather", context);
    }
}

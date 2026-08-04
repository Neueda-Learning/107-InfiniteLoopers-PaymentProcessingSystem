package com.payment.payment_processing_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Mail configuration for JavaMailSender.
 *
 * All values are sourced from application.properties via spring.mail.* properties.
 */
@Configuration
@EnableConfigurationProperties(MailConfig.MailSettings.class)
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(MailSettings settings) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(settings.getHost());
        if (settings.getPort() != null) {
            mailSender.setPort(settings.getPort());
        }
        mailSender.setUsername(settings.getUsername());
        mailSender.setPassword(settings.getPassword());
        mailSender.setProtocol(settings.getProtocol());

        if (settings.getDefaultEncoding() != null) {
            mailSender.setDefaultEncoding(settings.getDefaultEncoding().name());
        }

        Properties javaMailProps = new Properties();
        javaMailProps.putAll(settings.getProperties());
        mailSender.setJavaMailProperties(javaMailProps);

        return mailSender;
    }

    /**
     * Binds all spring.mail.* keys from application.properties.
     */
    @ConfigurationProperties(prefix = "spring.mail")
    public static class MailSettings {
        private String host;
        private Integer port;
        private String username;
        private String password;
        private String protocol = "smtp";
        private Charset defaultEncoding = StandardCharsets.UTF_8;
        private Map<String, String> properties = new HashMap<>();

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public Charset getDefaultEncoding() {
            return defaultEncoding;
        }

        public void setDefaultEncoding(Charset defaultEncoding) {
            this.defaultEncoding = defaultEncoding;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }
    }
}

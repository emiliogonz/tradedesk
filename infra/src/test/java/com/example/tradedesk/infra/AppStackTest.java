package com.example.tradedesk.infra;

import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Match;
import software.amazon.awscdk.assertions.Template;

import java.util.List;
import java.util.Map;

class AppStackTest {

    private final Template template;

    AppStackTest() {
        App app = new App();
        template = Template.fromStack(new AppStack(app, "App", StackProps.builder().build(), "test"));
    }

    @Test
    void jwtSecretComesFromSecretsManager() {
        assertOption("aws:elasticbeanstalk:application:environmentsecrets", "JWT_SECRET");
    }

    @Test
    void databasePasswordComesFromSecretsManager() {
        assertOption("aws:elasticbeanstalk:application:environmentsecrets", "SPRING_DATASOURCE_PASSWORD");
    }

    @Test
    void deploymentsAreImmutable() {
        assertOption("aws:elasticbeanstalk:command", "DeploymentPolicy", "Immutable");
    }

    @Test
    void healthCheckUsesActuator() {
        assertOption("aws:elasticbeanstalk:environment:process:default", "HealthCheckPath", "/actuator/health");
    }

    @Test
    void servesHttpsOnly() {
        assertOption("aws:elbv2:listener:443", "Protocol", "HTTPS");
        assertOption("aws:elbv2:listener:default", "ListenerEnabled", "false");
    }

    @Test
    void apiDomainPointsAtTheEnvironment() {
        template.hasResourceProperties("AWS::Route53::RecordSet", Map.of(
                "Name", DataStack.API_DOMAIN + ".",
                "Type", "CNAME"));
    }

    private void assertOption(String namespace, String name) {
        template.hasResourceProperties("AWS::ElasticBeanstalk::Environment", Map.of(
                "OptionSettings", Match.arrayWith(List.of(Match.objectLike(Map.of(
                        "Namespace", namespace, "OptionName", name))))));
    }

    private void assertOption(String namespace, String name, String value) {
        template.hasResourceProperties("AWS::ElasticBeanstalk::Environment", Map.of(
                "OptionSettings", Match.arrayWith(List.of(Match.objectLike(Map.of(
                        "Namespace", namespace, "OptionName", name, "Value", value))))));
    }
}
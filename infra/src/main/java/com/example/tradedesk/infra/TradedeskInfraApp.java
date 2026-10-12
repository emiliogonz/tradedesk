package com.example.tradedesk.infra;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Tags;

public final class TradedeskInfraApp {

    public static void main(String[] args) {
        App app = new App();

        Environment env = Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region("ap-southeast-1")
                .build();

        DataStack data = new DataStack(app, "TradedeskData", StackProps.builder().env(env).build());

        String imageTag = (String) app.getNode().tryGetContext("imageTag");
        if (imageTag != null) {
            if (imageTag.isBlank()) {
                throw new IllegalArgumentException(
                        "imageTag is empty. Pass a real tag, e.g. -c imageTag=$(git rev-parse --short HEAD)");
            }
            new AppStack(app, "TradedeskApp", StackProps.builder().env(env).build(), data, imageTag);
        }

        Tags.of(app).add("project", "tradedesk");
        app.synth();
    }
}
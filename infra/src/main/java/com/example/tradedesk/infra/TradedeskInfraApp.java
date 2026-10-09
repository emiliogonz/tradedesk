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

        new DataStack(app, "TradedeskData", StackProps.builder().env(env).build());

        Tags.of(app).add("project", "tradedesk");
        app.synth();
    }
}
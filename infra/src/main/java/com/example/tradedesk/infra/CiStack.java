package com.example.tradedesk.infra;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.OpenIdConnectProvider;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.WebIdentityPrincipal;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

/** Lets GitHub Actions authenticate to AWS with short-lived OIDC tokens. No stored keys. */
public class CiStack extends Stack {

    static final String GITHUB_REPO = "emiliogonz/tradedesk";
    private static final String GITHUB_TOKEN_HOST = "token.actions.githubusercontent.com";

    public CiStack(Construct scope, String id, StackProps props, DataStack data) {
        super(scope, id, props);

        OpenIdConnectProvider github = OpenIdConnectProvider.Builder.create(this, "GitHubOidc")
                .url("https://" + GITHUB_TOKEN_HOST)
                .clientIds(List.of("sts.amazonaws.com"))
                .build();

        // Build job: runs on pushes to master, may only push images to this one repository.
        Role buildRole = Role.Builder.create(this, "BuildRole")
                .description("GitHub Actions: build and push images from master")
                .assumedBy(githubPrincipal(github, "repo:" + GITHUB_REPO + ":ref:refs/heads/master"))
                .build();
        data.getRepository().grantPullPush(buildRole);
        data.getRepository().grant(buildRole, "ecr:DescribeImages");

        // Deploy job: only reachable through the approved 'production' environment.
        Role deployRole = Role.Builder.create(this, "DeployRole")
                .description("GitHub Actions: deploy the app stack after approval")
                .assumedBy(githubPrincipal(github, "repo:" + GITHUB_REPO + ":environment:production"))
                .build();
        deployRole.addToPolicy(PolicyStatement.Builder.create()
                .actions(List.of("sts:AssumeRole"))
                .resources(List.of("arn:aws:iam::" + getAccount() + ":role/cdk-hnb659fds-*"))
                .build());

        CfnOutput.Builder.create(this, "BuildRoleArn").value(buildRole.getRoleArn()).build();
        CfnOutput.Builder.create(this, "DeployRoleArn").value(deployRole.getRoleArn()).build();
    }

    private static WebIdentityPrincipal githubPrincipal(OpenIdConnectProvider provider, String subject) {
        return new WebIdentityPrincipal(provider.getOpenIdConnectProviderArn(), Map.of(
                "StringEquals", Map.of(
                        GITHUB_TOKEN_HOST + ":aud", "sts.amazonaws.com",
                        GITHUB_TOKEN_HOST + ":sub", subject)));
    }
}
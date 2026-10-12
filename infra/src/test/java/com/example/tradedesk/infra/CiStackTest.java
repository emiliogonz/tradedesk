package com.example.tradedesk.infra;

import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Match;
import software.amazon.awscdk.assertions.Template;

import java.util.List;
import java.util.Map;

class CiStackTest {

    private final Template template;

    CiStackTest() {
        App app = new App();
        DataStack data = new DataStack(app, "Data", StackProps.builder().build());
        template = Template.fromStack(new CiStack(app, "Ci", StackProps.builder().build(), data));
    }

    @Test
    void buildRoleTrustsOnlyMasterOfThisRepo() {
        assertTrustedSubject(CiStack.GITHUB_SUBJECT + ":ref:refs/heads/master");
    }

    @Test
    void deployRoleTrustsOnlyTheProductionEnvironment() {
        assertTrustedSubject(CiStack.GITHUB_SUBJECT + ":environment:production");
    }

    private void assertTrustedSubject(String subject) {
        var subjectCondition = Match.objectLike(Map.of(
                "token.actions.githubusercontent.com:sub", subject));
        var statement = Match.objectLike(Map.of(
                "Condition", Match.objectLike(Map.of("StringEquals", subjectCondition))));
        var trustPolicy = Match.objectLike(Map.of(
                "Statement", Match.arrayWith(List.of(statement))));

        template.hasResourceProperties("AWS::IAM::Role", Map.of(
                "AssumeRolePolicyDocument", trustPolicy));
    }
}
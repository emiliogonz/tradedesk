package com.example.tradedesk.infra;

import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Match;
import software.amazon.awscdk.assertions.Template;

import java.util.Map;

class DataStackTest {

    private final Template template = Template.fromStack(
            new DataStack(new App(), "Test", StackProps.builder().build()));

    @Test
    void databaseIsPostgresEncryptedAndNotPublic() {
        template.hasResourceProperties("AWS::RDS::DBInstance", Map.of(
                "Engine", "postgres",
                "StorageEncrypted", true,
                "PubliclyAccessible", false));
    }

    @Test
    void repositoryHasImmutableTagsAndScanOnPush() {
        template.hasResourceProperties("AWS::ECR::Repository", Map.of(
                "ImageTagMutability", "IMMUTABLE",
                "ImageScanningConfiguration", Map.of("ScanOnPush", true)));
    }

    @Test
    void databaseAcceptsPostgresOnlyFromAnotherSecurityGroup() {
        template.hasResourceProperties("AWS::EC2::SecurityGroupIngress", Map.of(
                "IpProtocol", "tcp",
                "FromPort", 5432,
                "ToPort", 5432,
                "SourceSecurityGroupId", Match.anyValue()));
    }

    @Test
    void publishesValuesForOtherStacksToSsm() {
        template.resourceCountIs("AWS::SSM::Parameter", 7);
        template.hasResourceProperties("AWS::SSM::Parameter", Map.of(
                "Name", DataStack.PARAM_PREFIX + "db-secret-arn"));
    }

    @Test
    void noNatGatewaysAreCreated() {
        template.resourceCountIs("AWS::EC2::NatGateway", 0);
    }
}
package com.example.tradedesk.infra;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.elasticbeanstalk.CfnApplication;
import software.amazon.awscdk.services.elasticbeanstalk.CfnApplicationVersion;
import software.amazon.awscdk.services.elasticbeanstalk.CfnEnvironment;
import software.amazon.awscdk.services.iam.CfnInstanceProfile;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.s3.assets.Asset;
import software.constructs.Construct;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** The running application. Safe to destroy and recreate; all state lives in DataStack. */
public class AppStack extends Stack {

    /** From: aws elasticbeanstalk list-available-solution-stacks (see README). */
    private static final String SOLUTION_STACK = "64bit Amazon Linux 2023 v4.13.10 running Docker";

    private static final String ENV = "aws:elasticbeanstalk:application:environment";
    private static final String SECRETS = "aws:elasticbeanstalk:application:environmentsecrets";

    public AppStack(Construct scope, String id, StackProps props, DataStack data, String imageTag) {
        super(scope, id, props);

        // --- What to run: a Dockerrun file pointing at one exact image in ECR ---
        String imageUri = "%s.dkr.ecr.%s.amazonaws.com/%s:%s"
                .formatted(getAccount(), getRegion(), DataStack.REPOSITORY_NAME, imageTag);
        Asset sourceBundle = Asset.Builder.create(this, "SourceBundle")
                .path(writeSourceBundle(imageUri).toString())
                .build();

        CfnApplication application = CfnApplication.Builder.create(this, "Application")
                .applicationName("tradedesk")
                .build();

        CfnApplicationVersion version = CfnApplicationVersion.Builder.create(this, "Version")
                .applicationName(application.getRef())
                .description("Image " + imageTag)
                .sourceBundle(CfnApplicationVersion.SourceBundleProperty.builder()
                        .s3Bucket(sourceBundle.getS3BucketName())
                        .s3Key(sourceBundle.getS3ObjectKey())
                        .build())
                .build();

        // --- Who the instances are: pull this image, read these two secrets, nothing more ---
        Role instanceRole = Role.Builder.create(this, "InstanceRole")
                .assumedBy(new ServicePrincipal("ec2.amazonaws.com"))
                .managedPolicies(List.of(ManagedPolicy.fromAwsManagedPolicyName("AWSElasticBeanstalkWebTier")))
                .build();
        data.getRepository().grantPull(instanceRole);
        data.getDatabase().getSecret().grantRead(instanceRole);
        data.getJwtSecret().grantRead(instanceRole);

        CfnInstanceProfile instanceProfile = CfnInstanceProfile.Builder.create(this, "InstanceProfile")
                .roles(List.of(instanceRole.getRoleName()))
                .build();

        Role serviceRole = Role.Builder.create(this, "ServiceRole")
                .assumedBy(new ServicePrincipal("elasticbeanstalk.amazonaws.com"))
                .managedPolicies(List.of(
                        ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSElasticBeanstalkEnhancedHealth"),
                        ManagedPolicy.fromAwsManagedPolicyName("AWSElasticBeanstalkManagedUpdatesCustomerRolePolicy")))
                .build();

        // --- Where and how it runs ---
        String publicSubnets = String.join(",",
                data.getVpc().getPublicSubnets().stream().map(ISubnet::getSubnetId).toList());
        String dbSecretArn = data.getDatabase().getSecret().getSecretArn();
        String jdbcUrl = "jdbc:postgresql://" + data.getDatabase().getDbInstanceEndpointAddress()
                + ":" + data.getDatabase().getDbInstanceEndpointPort() + "/tradedesk";

        List<CfnEnvironment.OptionSettingProperty> options = List.of(
                // Network: instances and load balancer in public subnets, no NAT gateway needed
                opt("aws:ec2:vpc", "VPCId", data.getVpc().getVpcId()),
                opt("aws:ec2:vpc", "Subnets", publicSubnets),
                opt("aws:ec2:vpc", "ELBSubnets", publicSubnets),
                opt("aws:ec2:vpc", "AssociatePublicIpAddress", "true"),

                // Instances: one Graviton instance, in the security group the database trusts
                opt("aws:ec2:instances", "InstanceTypes", "t4g.small"),
                opt("aws:autoscaling:launchconfiguration", "IamInstanceProfile", instanceProfile.getRef()),
                opt("aws:autoscaling:launchconfiguration", "SecurityGroups",
                        data.getAppSecurityGroup().getSecurityGroupId()),
                opt("aws:autoscaling:asg", "MinSize", "1"),
                opt("aws:autoscaling:asg", "MaxSize", "1"),

                // Load balancer, health, deployments, logs
                opt("aws:elasticbeanstalk:environment", "EnvironmentType", "LoadBalanced"),
                opt("aws:elasticbeanstalk:environment", "LoadBalancerType", "application"),
                opt("aws:elasticbeanstalk:environment", "ServiceRole", serviceRole.getRoleArn()),
                opt("aws:elasticbeanstalk:environment:process:default", "HealthCheckPath", "/actuator/health"),
                opt("aws:elasticbeanstalk:healthreporting:system", "SystemType", "enhanced"),
                opt("aws:elasticbeanstalk:command", "DeploymentPolicy", "Immutable"),
                opt("aws:elasticbeanstalk:cloudwatch:logs", "StreamLogs", "true"),
                opt("aws:elasticbeanstalk:cloudwatch:logs", "RetentionInDays", "7"),
                opt("aws:elasticbeanstalk:cloudwatch:logs", "DeleteOnTerminate", "true"),

                // Plain configuration: not sensitive
                opt(ENV, "SPRING_PROFILES_ACTIVE", "prod"),
                opt(ENV, "SPRING_DATASOURCE_URL", jdbcUrl),

                // Secrets: only ARNs here; each instance fetches the values at startup
                opt(SECRETS, "SPRING_DATASOURCE_USERNAME", dbSecretArn + ":username"),
                opt(SECRETS, "SPRING_DATASOURCE_PASSWORD", dbSecretArn + ":password"),
                opt(SECRETS, "JWT_SECRET", data.getJwtSecret().getSecretArn())
        );

        CfnEnvironment environment = CfnEnvironment.Builder.create(this, "Environment")
                .applicationName(application.getRef())
                .environmentName("tradedesk-prod")
                .solutionStackName(SOLUTION_STACK)
                .versionLabel(version.getRef())
                .optionSettings(options)
                .build();

        CfnOutput.Builder.create(this, "Url")
                .value("http://" + environment.getAttrEndpointUrl())
                .build();
    }

    private static CfnEnvironment.OptionSettingProperty opt(String namespace, String name, String value) {
        return CfnEnvironment.OptionSettingProperty.builder()
                .namespace(namespace).optionName(name).value(value).build();
    }

    /** Writes the Dockerrun file Beanstalk reads to know which image to pull and which port it listens on. */
    private static Path writeSourceBundle(String imageUri) {
        Path dir = Path.of("target", "eb-bundle");
        String dockerrun = """
                {
                  "AWSEBDockerrunVersion": "1",
                  "Image": { "Name": "%s", "Update": "true" },
                  "Ports": [ { "ContainerPort": 8080 } ]
                }
                """.formatted(imageUri);
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("Dockerrun.aws.json"), dockerrun);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return dir;
    }
}
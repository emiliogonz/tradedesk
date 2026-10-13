package com.example.tradedesk.infra;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.elasticbeanstalk.CfnApplication;
import software.amazon.awscdk.services.elasticbeanstalk.CfnApplicationVersion;
import software.amazon.awscdk.services.elasticbeanstalk.CfnEnvironment;
import software.amazon.awscdk.services.iam.CfnInstanceProfile;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.route53.CnameRecord;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneAttributes;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.s3.assets.Asset;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The running application. Safe to destroy and recreate; all state lives in DataStack.
 * Reads DataStack's values from SSM Parameter Store, so the two stacks deploy independently.
 */
public class AppStack extends Stack {

    /** From: aws elasticbeanstalk list-available-solution-stacks (see README). */
    private static final String SOLUTION_STACK = "64bit Amazon Linux 2023 v4.13.10 running Docker";

    private static final String ENV = "aws:elasticbeanstalk:application:environment";
    private static final String SECRETS = "aws:elasticbeanstalk:application:environmentsecrets";

    public AppStack(Construct scope, String id, StackProps props, String imageTag) {
        super(scope, id, props);

        // --- Values published by DataStack, resolved by CloudFormation at deploy time ---
        String vpcId = param("vpc-id");
        String publicSubnets = param("public-subnet-ids");
        String appSecurityGroupId = param("app-security-group-id");
        String dbSecretArn = param("db-secret-arn");
        String jwtSecretArn = param("jwt-secret-arn");
        String jdbcUrl = "jdbc:postgresql://" + param("db-endpoint-address")
                + ":" + param("db-endpoint-port") + "/tradedesk";
        String apiCertificateArn = param("api-certificate-arn");
        String hostedZoneId = param("hosted-zone-id");

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
                .managedPolicies(List.of(
                        ManagedPolicy.fromAwsManagedPolicyName("AWSElasticBeanstalkWebTier"),
                        ManagedPolicy.fromAwsManagedPolicyName("AmazonSSMManagedInstanceCore")))
                .build();
        Repository.fromRepositoryName(this, "AppRepository", DataStack.REPOSITORY_NAME).grantPull(instanceRole);
        Secret.fromSecretCompleteArn(this, "DbSecret", dbSecretArn).grantRead(instanceRole);
        Secret.fromSecretCompleteArn(this, "JwtSecret", jwtSecretArn).grantRead(instanceRole);

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
        List<CfnEnvironment.OptionSettingProperty> options = List.of(
                // Network: instances and load balancer in public subnets, no NAT gateway needed
                opt("aws:ec2:vpc", "VPCId", vpcId),
                opt("aws:ec2:vpc", "Subnets", publicSubnets),
                opt("aws:ec2:vpc", "ELBSubnets", publicSubnets),
                opt("aws:ec2:vpc", "AssociatePublicIpAddress", "true"),

                // Instances: one Graviton instance, in the security group the database trusts
                opt("aws:ec2:instances", "InstanceTypes", "t4g.small"),
                opt("aws:autoscaling:launchconfiguration", "IamInstanceProfile", instanceProfile.getRef()),
                opt("aws:autoscaling:launchconfiguration", "SecurityGroups", appSecurityGroupId),
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

                // HTTPS only: port 443 with the ACM certificate; plain HTTP is not served at all
                opt("aws:elbv2:listener:443", "Protocol", "HTTPS"),
                opt("aws:elbv2:listener:443", "SSLCertificateArns", apiCertificateArn),
                opt("aws:elbv2:listener:443", "SSLPolicy", "ELBSecurityPolicy-TLS13-1-2-2021-06"),
                opt("aws:elbv2:listener:default", "ListenerEnabled", "false"),

                // Plain configuration: not sensitive
                opt(ENV, "SPRING_PROFILES_ACTIVE", "prod"),
                opt(ENV, "SPRING_DATASOURCE_URL", jdbcUrl),

                // Secrets: only ARNs here; each instance fetches the values at startup
                opt(SECRETS, "SPRING_DATASOURCE_USERNAME", dbSecretArn + ":username"),
                opt(SECRETS, "SPRING_DATASOURCE_PASSWORD", dbSecretArn + ":password"),
                opt(SECRETS, "JWT_SECRET", jwtSecretArn)
        );

        CfnEnvironment environment = CfnEnvironment.Builder.create(this, "Environment")
                .applicationName(application.getRef())
                .environmentName("tradedesk-prod")
                .solutionStackName(SOLUTION_STACK)
                .versionLabel(version.getRef())
                .optionSettings(options)
                .build();

        IHostedZone zone = HostedZone.fromHostedZoneAttributes(this, "Zone", HostedZoneAttributes.builder()
                .hostedZoneId(hostedZoneId)
                .zoneName(DataStack.DOMAIN)
                .build());

        CnameRecord.Builder.create(this, "ApiRecord")
                .zone(zone)
                .recordName(DataStack.API_DOMAIN)
                .domainName(environment.getAttrEndpointUrl())
                .ttl(Duration.minutes(1))
                .build();

        CfnOutput.Builder.create(this, "Url")
                .value("https://" + DataStack.API_DOMAIN)
                .build();
    }

    /** A value DataStack published to SSM, resolved by CloudFormation when this stack deploys. */
    private String param(String name) {
        return StringParameter.valueForStringParameter(this, DataStack.PARAM_PREFIX + name);
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
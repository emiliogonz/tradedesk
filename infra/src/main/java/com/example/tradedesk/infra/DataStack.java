package com.example.tradedesk.infra;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.PostgresEngineVersion;
import software.amazon.awscdk.services.rds.PostgresInstanceEngineProps;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;
import software.constructs.Construct;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecr.TagMutability;
import software.amazon.awscdk.services.ecr.LifecycleRule;


import java.util.List;

/** Long-lived resources: network, database, and secrets. Survives app redeploys and teardowns. */
public class DataStack extends Stack {

    private final Vpc vpc;
    private final SecurityGroup appSecurityGroup;
    private final DatabaseInstance database;
    private final Secret jwtSecret;
    public static final String REPOSITORY_NAME = "tradedesk";

    private final Repository repository;

    public DataStack(Construct scope, String id, StackProps props) {
        super(scope, id, props);

        vpc = Vpc.Builder.create(this, "Vpc")
                .maxAzs(2)
                .natGateways(0)
                .subnetConfiguration(List.of(
                        SubnetConfiguration.builder()
                                .name("public").subnetType(SubnetType.PUBLIC).cidrMask(24).build(),
                        SubnetConfiguration.builder()
                                .name("isolated").subnetType(SubnetType.PRIVATE_ISOLATED).cidrMask(24).build()))
                .build();

        appSecurityGroup = SecurityGroup.Builder.create(this, "AppSecurityGroup")
                .vpc(vpc)
                .description("TradeDesk application instances")
                .allowAllOutbound(true)
                .build();

        SecurityGroup dbSecurityGroup = SecurityGroup.Builder.create(this, "DbSecurityGroup")
                .vpc(vpc)
                .description("TradeDesk Postgres")
                .allowAllOutbound(false)
                .build();
        dbSecurityGroup.addIngressRule(appSecurityGroup, Port.tcp(5432), "Postgres from app instances only");

        database = DatabaseInstance.Builder.create(this, "Postgres")
                .engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder()
                        .version(PostgresEngineVersion.VER_16)
                        .build()))
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE4_GRAVITON, InstanceSize.MICRO))
                .vpc(vpc)
                .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PRIVATE_ISOLATED).build())
                .securityGroups(List.of(dbSecurityGroup))
                .databaseName("tradedesk")
                .credentials(Credentials.fromGeneratedSecret("tradedesk"))
                .allocatedStorage(20)
                .storageEncrypted(true)
                .multiAz(false)
                .publiclyAccessible(false)
                .backupRetention(Duration.days(1))
                .deletionProtection(false)
                .removalPolicy(RemovalPolicy.SNAPSHOT)
                .build();

        jwtSecret = Secret.Builder.create(this, "JwtSecret")
                .description("HS256 signing key for TradeDesk JWTs")
                .generateSecretString(SecretStringGenerator.builder()
                        .passwordLength(64)
                        .excludePunctuation(true)
                        .build())
                .build();

        repository = Repository.Builder.create(this, "AppRepository")
                .repositoryName(REPOSITORY_NAME)
                .imageTagMutability(TagMutability.IMMUTABLE)
                .imageScanOnPush(true)
                .lifecycleRules(List.of(LifecycleRule.builder()
                        .description("Keep the 10 most recent images")
                        .maxImageCount(10)
                        .build()))
                .build();

        CfnOutput.Builder.create(this, "RepositoryUri")
                .value(repository.getRepositoryUri()).build();
        CfnOutput.Builder.create(this, "DatabaseEndpoint")
                .value(database.getDbInstanceEndpointAddress()).build();
        CfnOutput.Builder.create(this, "DatabaseSecretArn")
                .value(database.getSecret().getSecretArn()).build();
        CfnOutput.Builder.create(this, "JwtSecretArn")
                .value(jwtSecret.getSecretArn()).build();
    }

    public Vpc getVpc() { return vpc; }
    public SecurityGroup getAppSecurityGroup() { return appSecurityGroup; }
    public DatabaseInstance getDatabase() { return database; }
    public Secret getJwtSecret() { return jwtSecret; }
    public Repository getRepository() { return repository; }
}
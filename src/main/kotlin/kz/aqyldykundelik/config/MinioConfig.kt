package kz.aqyldykundelik.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

@Configuration
@EnableConfigurationProperties(MinioProperties::class)
class MinioConfig(private val minioProperties: MinioProperties) {

    @Bean
    fun s3Client(): S3Client {
        val credentials = AwsBasicCredentials.create(
            minioProperties.accessKey,
            minioProperties.secretKey
        )

        return S3Client.builder()
            .region(Region.of(minioProperties.region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .endpointOverride(URI.create(minioProperties.endpoint))
            .forcePathStyle(minioProperties.pathStyleAccess)  // Важно для MinIO
            .build()
    }

    @Bean
    fun s3Presigner(): S3Presigner {
        val credentials = AwsBasicCredentials.create(
            minioProperties.accessKey,
            minioProperties.secretKey
        )

        // Для MinIO нужно использовать кастомный S3Client с path-style
        val s3Client = S3Client.builder()
            .region(Region.of(minioProperties.region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .endpointOverride(URI.create(minioProperties.endpoint))
            .forcePathStyle(true)  // Path-style для MinIO
            .build()

        return S3Presigner.builder()
            .region(Region.of(minioProperties.region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .endpointOverride(URI.create(minioProperties.endpoint))
            .build()
    }
}

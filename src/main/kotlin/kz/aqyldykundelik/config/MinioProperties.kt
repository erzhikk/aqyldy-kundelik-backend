package kz.aqyldykundelik.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "minio")
data class MinioProperties(
    val endpoint: String,
    val accessKey: String,
    val secretKey: String,
    val bucketName: String,
    val region: String = "us-east-1",
    val pathStyleAccess: Boolean = true
)

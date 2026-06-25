pluginManagement {
    repositories {
        // 阿里云镜像（优先级最高）
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
        maven { url = uri("https://maven.aliyun.com/repository/google/") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin/") }

        // 腾讯云镜像（备用）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // 官方源（最后备用）
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 阿里云镜像（优先级最高）
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
        maven { url = uri("https://maven.aliyun.com/repository/google/") }

        // 腾讯云镜像（备用）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // JitPack（第三方库如 MPAndroidChart）
        maven { url = uri("https://jitpack.io") }

        // 官方源（最后备用）
        google()
        mavenCentral()
    }
}

rootProject.name = "LibrarySystem"
include(":app")
plugins {
    `java-library`
    `maven-publish`
    id("io.freefair.lombok") version "9.2.0"

}

group = "me.shurikennen"
version = "1.2.1"

java {
    withSourcesJar()
    withJavadocJar()

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }

    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}


publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/BertGarretsen/swing-kit")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("gpr") {
            groupId = project.group as String?
            artifactId = project.name
            version = project.version as String?

            from(components["java"])
        }
    }
}


repositories {
    mavenLocal()
    mavenCentral()
}


dependencies {
    api("com.miglayout:miglayout:3.7.4")
    api("com.formdev:flatlaf:3.7")
    api("com.formdev:flatlaf-extras:3.7")
    api("com.formdev:flatlaf-fonts-roboto:2.137")
}
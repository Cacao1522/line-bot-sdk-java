/*
 * Copyright 2023 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

// repositories {
//     mavenCentral()
// }



plugins {
    // id("java")
    id("sdk.java-library")
    id("sdk.spring-boot")
}

dependencies {
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))

    implementation(project(":spring-boot:line-bot-spring-boot-handler"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")
    implementation("org.jsoup:jsoup:1.13.1")
    implementation("org.springframework.boot:spring-boot-starter")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.12")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// -Werror を無効化するための設定
// tasks.withType(JavaCompile) {
//     options.compilerArgs = options.compilerArgs.findAll { it != "-Werror" }
// }

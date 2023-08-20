/*
 * Copyright (C) 2016-2023 Sandip Vaghela
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//check project.path is in list of projects
val composeProjects = arrayOf(":data", ":ui:repo", ":ui:common-compose")
if (project.path in composeProjects) {
    println("- INFO: Compose Enabled for ${project.path}")
    apply(from = "$rootDir/gradle/compose.gradle")
}

apply(from = "$rootDir/gradle/apply-common-deps.gradle")
apply(from = "$rootDir/gradle/apply-core.gradle")
